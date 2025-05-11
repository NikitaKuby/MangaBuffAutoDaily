package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
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
    private static final int COUNT_OF_COMMENTS = 13;
    private static final int MIN_DELAY_SEC = 30;
    private static final int MAX_DELAY_SEC = 40;

    @SneakyThrows
    @Transactional
    public void startDailyCommentSending(WebDriver driver, Long id){
        AtomicInteger counter = new AtomicInteger(0);

        log.debug("startDailyCommentSending");

        List<String> newIds = commentParserService.getNewChapterIds(COUNT_OF_COMMENTS, id);
        if (newIds.isEmpty()) {
            log.warn("[{}]Нет новых глав для комментирования", id);
            return;
        }
        log.info("{}: "+newIds.toString(), id);
        CopyOnWriteArrayList<String> commentIds = new CopyOnWriteArrayList<>(newIds);
        // Запускаем отправку комментариев через ThreadPool
        log.debug("try scheduleComments");
        try {
            scheduleComments(id, counter, commentIds);
            mangaChapterRepository.markMultipleAsCommented(newIds, id);
        }finally {
            Thread.sleep(4000);
            driver.quit();
        }
    }

    private void scheduleComments(Long userId,
                                  AtomicInteger counter,
                                  CopyOnWriteArrayList<String> commentIds) {
        log.debug("[{}]start scheduleComments", userId);
        try {
            Thread.sleep((long) ((Math.random()*10+1)*15));
        } catch (InterruptedException e) {
            log.info("[{}]все по пизде...", userId);
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            for (int i = 0; i < COUNT_OF_COMMENTS; i++) {
                int delay = MIN_DELAY_SEC + (int)(Math.random() * (MAX_DELAY_SEC - MIN_DELAY_SEC));
                executor.schedule(() -> {
                    int currentCount = counter.getAndIncrement();
                    if (currentCount >= COUNT_OF_COMMENTS) return;

                    try {
                        String idComment = commentIds.get(currentCount);
                        String textMessage = chapterThanksGeneratorService.generateThanks();
                        log.debug("[{}]sendPostRequestWithCookies", userId);
                        commentService.sendPostRequestWithCookies(textMessage, idComment, userId);
                        log.info("[{}] Отправлен комментарий {}/{}",
                            userId, currentCount + 1, COUNT_OF_COMMENTS);
                    } catch (Exception e) {
                        log.error("[{}] Ошибка: {}", userId, e.getMessage());
                    }
                }, delay * i, TimeUnit.SECONDS);
            }
        } finally {
            // Гарантированное завершение
            executor.schedule(executor::shutdown,
                COUNT_OF_COMMENTS * MAX_DELAY_SEC, TimeUnit.SECONDS);
        }
    }
}


