package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.service.ChapterThanksGeneratorService;
import ru.finwax.mangabuffjob.service.CommentParserService;
import ru.finwax.mangabuffjob.service.CommentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
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
    private static final int MIN_DELAY_SEC = 40;
    private static final int MAX_DELAY_SEC = 50;

    @SneakyThrows
    @Transactional
    public CompletableFuture<Void> startDailyCommentSending(Long id, Integer COUNT_OF_COMMENTS){
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(0);


        List<String> newIds = commentParserService.getNewChapterIds(COUNT_OF_COMMENTS, id);
        if (newIds.isEmpty()) {
            log.warn("[{}]Нет новых глав для комментирования", id);
            future.complete(null);
            return future;
        }
        CopyOnWriteArrayList<String> commentIds = new CopyOnWriteArrayList<>(newIds);
        log.debug("try scheduleComments");

        try {
            Thread.sleep((long) (getDelayForUser(id)));
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            AtomicInteger completedCount = new AtomicInteger(0);

            for (int i = 0; i < COUNT_OF_COMMENTS; i++) {
                int delay = MIN_DELAY_SEC + (int)(Math.random() * (MAX_DELAY_SEC - MIN_DELAY_SEC));
                executor.schedule(() -> {
                    try {
                        int currentCount = counter.getAndIncrement();
                        if (currentCount >= COUNT_OF_COMMENTS) return;

                        String idComment = commentIds.get(currentCount);
                        String textMessage = chapterThanksGeneratorService.generateThanks();
                        log.info("[{}]sendPostRequestWithCookies", id);
                        commentService.sendPostRequestWithCookies(textMessage, idComment, id);
                        log.info("[{}] Отправлен комментарий {}/{}", id, currentCount + 1, COUNT_OF_COMMENTS);

                        if (completedCount.incrementAndGet() == COUNT_OF_COMMENTS) {
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        log.error("[{}] Ошибка: {}", id, e.getMessage());
                        if (completedCount.incrementAndGet() == COUNT_OF_COMMENTS) {
                            future.complete(null);
                        }
                    }
                }, delay * i, TimeUnit.SECONDS);
            }

            executor.schedule(executor::shutdown, COUNT_OF_COMMENTS * MAX_DELAY_SEC, TimeUnit.SECONDS);
            mangaChapterRepository.markMultipleAsCommented(newIds, id);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }


    public long getDelayForUser(Long userId) {
        long remainder = userId % 10;
        // Корректируем формулу, чтобы minDelaySec всегда был >= 0
        long minDelaySec = Math.max(0, 2 * remainder - 2);
        long maxDelaySec = Math.max(1, 2 * remainder - 1);  // Не меньше 1 мс

        // Генерируем случайную задержку в этом диапазоне
        return ThreadLocalRandom.current().nextLong(minDelaySec * 1000, maxDelaySec * 1000);
    }

}


