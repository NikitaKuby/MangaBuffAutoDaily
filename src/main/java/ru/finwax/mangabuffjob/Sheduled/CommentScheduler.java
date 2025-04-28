package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.service.ChapterThanksGeneratorService;
import ru.finwax.mangabuffjob.service.CommentParserService;
import ru.finwax.mangabuffjob.service.CommentService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class CommentScheduler {

    private final ChapterThanksGeneratorService chapterThanksGeneratorService;
    private final MangaChapterRepository mangaChapterRepository;
    private final CommentService commentService;
    private final CommentParserService commentParserService;
    private final MbAuth mbAuth;
    private final AtomicInteger counter = new AtomicInteger(0);
    private static final int COUNT_OF_COMMENTS = 15;
    private static final int DELAY_BETWEEN_COMMENTS = 30;
    private List<String> commentIds;

    @Transactional
    public void startDailyCommentSending(Long id) {
        commentIds = commentParserService.getNewChapterIds();
        counter.set(0); // Сбрасываем счётчик
        mbAuth.getActualDriver(id).quit();
        scheduleNextComment(id);
        mangaChapterRepository.markMultipleAsCommented(commentIds);
    }

    private void scheduleNextComment(Long id) {
        if (counter.get() < COUNT_OF_COMMENTS) {
            String idComment = commentIds.get(counter.getAndIncrement());
            String textMessage = chapterThanksGeneratorService.generateThanks();

            commentService.sendPostRequestWithCookies(textMessage, idComment, id);
            try {
                // Добавляем задержку перед отправкой (3-5 секунд)
                TimeUnit.SECONDS.sleep(DELAY_BETWEEN_COMMENTS + (long)(Math.random() * 10));
                scheduleNextComment(id);
                // Остальной код...
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }




}