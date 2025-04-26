package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {
    private final CommentScheduler commentScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final MangaBuffAuth mangaBuffAuth;
    private final MangaReadScheduler mangaReadScheduler;

    // Запускается каждый день в 1:00 ночи
    @Scheduled(cron = "0 30 0 * * ?")
    public void startScheduledPlan() {
        try {
            // 1. Запускаем комментарии асинхронно, так как они работают через REST
            log.info("Starting comment scheduling...");
            commentScheduler.startDailyCommentSending();

            // 2. Работа с Selenium - сначала квизы
            log.info("Starting quiz monitoring...");
            quizScheduler.monitorQuizRequests();

            // 3. Пауза 5 минут между Selenium задачами
            log.info("Waiting 5 minutes before mining...");
            TimeUnit.MINUTES.sleep(5);

            // 4. Затем майнинг
            log.info("Starting mining process...");
            mineScheduler.performMining();

            TimeUnit.MINUTES.sleep(5);

            // 5. Читаем мангу
            log.info("Starting read manga...");
            mangaReadScheduler.readMangaChapters();

            log.info("All scheduled tasks completed successfully");
        } catch (InterruptedException e) {
            log.error("Scheduled task was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error in scheduled task execution", e);
        }
    }

    // Обновляем куки каждые 5 минут, с начальной задержкой в 1 минуту после старта
    @Scheduled(initialDelay = 60_000, fixedRate = 10 * 60_000)
    public void refreshAuthCookies() {
        try {
            log.info("Refreshing authentication cookies...");
            mangaBuffAuth.refreshCookies();
            log.info("Authentication cookies refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to refresh authentication cookies", e);
        }
    }
}
