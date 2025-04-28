package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {
    private final CommentScheduler commentScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final AdvertisingScheduler advertisingScheduler;
    private final MbAuth mbAuth;
    private final UserCookieRepository userCookieRepository;

    // Запускается каждый день в 00:01 ночи
    @Scheduled(cron = "0 22 0 * * ?")
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
//
//            // 5. Этап квизов (5 минут)
//            log.info("--------SCHEDULER: START QUIZ--------");
//            executeUserTasks(executor, userIds,
//                (driver, userId) -> quizScheduler.monitorQuizRequests(driver),
//                5, TimeUnit.MINUTES, "Quiz");
//            log.info("--------SCHEDULER: STOP QUIZ--------");
//
//            TimeUnit.MINUTES.sleep(2);
//
//            // 5. Этап майнинга (5 минут)
//            log.info("--------SCHEDULER: START MINE--------");
//            executeUserTasks(executor, userIds,
//                (driver, userId) -> mineScheduler.performMining(driver),
//                5, TimeUnit.MINUTES, "Mining");
//            log.info("--------SCHEDULER: STOP MINE--------");
//
//            TimeUnit.MINUTES.sleep(2);
//
//            // 6. Этап рекламы (5 минут)
//            log.info("--------SCHEDULER: START ADV--------");
//            executeUserTasks(executor, userIds,
//                (driver, userId) -> advertisingScheduler.performAdv(driver),
//                5, TimeUnit.MINUTES, "Mining");
//            log.info("--------SCHEDULER: STOP ADV--------");
//
//            TimeUnit.MINUTES.sleep(2);

            // 6. Этап чтения манги (5 минут)
            log.info("--------SCHEDULER: START READER--------");
            executeUserTasks(executor, userIds,
                mangaReadScheduler::readMangaChapters,
                5, TimeUnit.MINUTES, "Mining");
            log.info("--------SCHEDULER: STOP READER--------");


            log.info("--------SCHEDULER: SUCCESSFULLY--------");
//        } catch (InterruptedException e) {
//            log.error("Scheduled task was interrupted", e);
//            Thread.currentThread().interrupt();
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