package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.service.ChapterThanksGeneratorService;
import ru.finwax.mangabuffjob.service.CommentParserService;
import ru.finwax.mangabuffjob.service.CommentService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommentScheduler {

    private final ChapterThanksGeneratorService chapterThanksGeneratorService;
    private final MangaChapterRepository mangaChapterRepository;
    private final CommentService commentService;
    private final CommentParserService commentParserService;
    private final MbAuth mbAuth;
    private static final int COUNT_OF_COMMENTS = 15;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final CopyOnWriteArrayList<String> commentIds = new CopyOnWriteArrayList<>();

    private static final int MIN_DELAY_SEC = 30;
    private static final int MAX_DELAY_SEC = 40;

    @Transactional
    public void startDailyCommentSending(Long id) {
        // Очищаем предыдущие данные
        commentIds.clear();
        counter.set(0);

        // Получаем новые ID и проверяем их наличие
        List<String> newIds = commentParserService.getNewChapterIds();
        if (newIds.isEmpty()) {
            log.warn("Нет новых глав для комментирования");
            return;
        }

        commentIds.addAll(newIds);
        mbAuth.getActualDriver(id).quit();

        // Запускаем отправку комментариев через ThreadPool
        scheduleComments(id);
        mangaChapterRepository.markMultipleAsCommented(newIds);
    }

    private void scheduleComments(Long id) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        Runnable commentTask = () -> {
            int currentCount = counter.getAndIncrement();
            if (currentCount >= COUNT_OF_COMMENTS || currentCount >= commentIds.size()) {
                executor.shutdown();
                return;
            }

            try {
                String idComment = commentIds.get(currentCount);
                String textMessage = chapterThanksGeneratorService.generateThanks();
                commentService.sendPostRequestWithCookies(textMessage, idComment, id);

                log.info("Отправлен комментарий {}/{} к главе {}",
                    currentCount + 1, Math.min(COUNT_OF_COMMENTS, commentIds.size()),
                    idComment);
            } catch (Exception e) {
                log.error("Ошибка при отправке комментария: {}", e.getMessage());
            }
        };

        // Планируем с случайной задержкой
        for (int i = 0; i < Math.min(COUNT_OF_COMMENTS, commentIds.size()); i++) {
            int delay = MIN_DELAY_SEC + (int)(Math.random() * (MAX_DELAY_SEC - MIN_DELAY_SEC));
            executor.schedule(commentTask, delay * i, TimeUnit.SECONDS);
        }
    }
}


