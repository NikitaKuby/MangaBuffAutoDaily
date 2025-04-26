package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.service.ChapterThanksGeneratorService;
import ru.finwax.mangabuffjob.service.CommentParserService;
import ru.finwax.mangabuffjob.service.CommentService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class CommentScheduler {

    private final ChapterThanksGeneratorService chapterThanksGeneratorService;
    private final MangaChapterRepository mangaChapterRepository;
    private final CommentService commentService;
    private final CommentParserService commentParserService;
    private final AtomicInteger counter = new AtomicInteger(0);
    private List<String> commentIds;

    @Transactional
    public void startDailyCommentSending() {
        commentIds = commentParserService.getNewChapterIds();
        counter.set(0); // Сбрасываем счётчик
        scheduleNextComment();
        mangaChapterRepository.markMultipleAsCommented(commentIds);
    }

    private void scheduleNextComment() {
        if (counter.get() < 10) {
            // Генерация разных данных для каждого вызова
            String idComment = commentIds.get(counter.getAndIncrement());
            String textMessage = chapterThanksGeneratorService.generateThanks();

            commentService.sendPostRequestWithCookies(textMessage, idComment);
            // Планируем следующий вызов через 2/2 минуты (120 000 мс)
            new Thread(() -> {
                try {
                    Thread.sleep(30_000);
                    scheduleNextComment();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }




}