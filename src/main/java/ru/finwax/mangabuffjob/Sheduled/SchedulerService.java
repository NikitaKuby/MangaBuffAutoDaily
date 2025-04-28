package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {
    private final CommentScheduler commentScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final MbAuth mbAuth;
    private final UserCookieRepository userCookieRepository;

    // Запускается каждый день в 00:01 ночи
    @Scheduled(cron = "0 47 1 * * ?")
    public void startScheduledPlan() {
        // Получаем список всех аккаунтов
        List<Long> userIds = userCookieRepository.findAll()
            .stream()
            .map(UserCookie::getId)
            .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            log.warn("No accounts found for scheduled tasks");
            return;
        }

        log.info("Starting scheduled tasks for {} active accounts", userIds.size());

        // 2. Ограничиваем количество одновременных потоков
        int maxParallelTasks = Math.min(userIds.size(), 5); // Не более 5 параллельных задач
        ExecutorService executor = Executors.newFixedThreadPool(maxParallelTasks);

        try {

            // 5. Этап квизов (7 минут на аккаунт)
            executeUserTasks(executor, userIds,
                (driver, userId) -> quizScheduler.monitorQuizRequests(driver),
                7, TimeUnit.MINUTES, "Quiz");

            executeUserTasks(executor, userIds,
                (driver, userId) -> mineScheduler.performMining(driver),
                10, TimeUnit.MINUTES, "Mining");

            // 4. Пауза между этапами (7 минут)
            log.info("Mining completed. Waiting 7 minutes before quizzes...");
            TimeUnit.MINUTES.sleep(7);


            log.info("All scheduled tasks completed successfully");
        } catch (InterruptedException e) {
            log.error("Scheduled task was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error in scheduled task execution", e);
        }
    }

    // Универсальный метод для выполнения задач пользователей
    private void executeUserTasks(ExecutorService executor,
                                  List<Long> userIds,
                                  BiConsumer<WebDriver, Long> task,
                                  long timeout,
                                  TimeUnit timeUnit,
                                  String taskName) {

        List<Future<?>> futures = userIds.stream()
            .map(userId -> executor.submit(() -> {
                WebDriver driver = null;
                try {
                    // 1. Создаем драйвер
                    driver = mbAuth.getActualDriver(userId);
                    log.info("{} started for user ID: {}", taskName, userId);

                    // 2. Засекаем время выполнения
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + timeUnit.toMillis(timeout);

                    // 3. Выполняем задачу в цикле (для периодических операций)
                    while (System.currentTimeMillis() < endTime) {
                        task.accept(driver, userId);

                        // Для квизов добавляем паузу между проверками
                        if ("Quiz".equals(taskName)) {
                            long remainingTime = endTime - System.currentTimeMillis();
                            if (remainingTime <= 0) break;
                            Thread.sleep(Math.min(remainingTime, 30_000)); // Не более 30 сек
                        }
                    }

                    log.info("{} completed for user ID: {}", taskName, userId);
                } catch (Exception e) {
                    log.error("Error during {} for user ID: {}: {}", taskName, userId, e.getMessage());
                } finally {
                    // 4. Гарантированное закрытие драйвера
                    if (driver != null) {
                        driver.quit();
                        log.debug("Driver closed for user ID: {}", userId);
                    }
                }
            }))
            .collect(Collectors.toList());

        // 5. Ожидаем завершения всех задач с таймаутом
        long timeoutMillis = timeUnit.toMillis(timeout);
        long startTime = System.currentTimeMillis();

        for (Future<?> future : futures) {
            try {
                long remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime);
                if (remainingTime > 0) {
                    future.get(remainingTime, TimeUnit.MILLISECONDS);
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("{} task timed out for a user", taskName);
            } catch (Exception e) {
                log.error("Error waiting for {} task completion", taskName, e);
            }
        }
    }
}