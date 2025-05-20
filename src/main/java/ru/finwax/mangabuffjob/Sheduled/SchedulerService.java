package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
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

import java.io.IOException;
import java.time.LocalTime;
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
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final AdvertisingScheduler advertisingScheduler;
    private final CommentScheduler commentScheduler;
    private final MbAuth mbAuth;
    private final UserCookieRepository userCookieRepository;

    private static final int CHAPTERS_PER_HOUR = 5;
    private static final int CHAPTERS_PER_DAY = 50;
    private volatile boolean mainTaskInProgress = false;

    // Запускается каждый день в 00:01 ночи
    @Scheduled(cron = "0 1 0 * * ?")
    public void startScheduledPlan() {
        mainTaskInProgress = true;
        killChromeDrivers();
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
        ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        try {
//
//            log.info("--------SCHEDULER: START QUIZ--------");
//            executeUserTasks(executor, userIds,
//                quizScheduler::monitorQuizRequests,
//                4, "Quiz");
//            log.info("--------SCHEDULER: STOP QUIZ--------");


//            log.info("--------SCHEDULER: START MINE--------");
//            executeUserTasks(executor, userIds,
//                mineScheduler::performMining,
//                4, "Mining");
//            log.info("--------SCHEDULER: STOP MINE--------");


//            log.info("--------SCHEDULER: START ADV--------");
//            executeUserTasks(executor, userIds,
//                advertisingScheduler::performAdv,
//                4, "ADV");
//            log.info("--------SCHEDULER: STOP ADV--------");


            log.info("--------SCHEDULER: START READER--------");
            executeUserTasks(executor, userIds,
                (driver, userId) ->
                    mangaReadScheduler.readMangaChapters(driver,userId,CHAPTERS_PER_DAY),
                120, "READER");
            log.info("--------SCHEDULER: STOP READER--------");

//            log.info("--------SCHEDULER: START COMMENT--------");
//            executeUserTasks(executorService, userIds,
//                (driver, userId) ->
//                    commentScheduler.startDailyCommentSending(driver, userId),
//                20, "Comment");
//            log.info("--------SCHEDULER: STOP COMMENT--------");


            log.info("--------SCHEDULER: SUCCESSFULLY--------");

        } catch (Exception e) {
            log.error("Error in scheduled task execution", e);
        } finally {
            mainTaskInProgress = false;
        }
    }

    private void executeUserTasks(ExecutorService executor,
                                  List<Long> userIds,
                                  BiConsumer<WebDriver, Long> task,
                                  long timeout,
                                  String taskName) {

        List<Future<?>> futures = userIds.stream()
            .map(userId -> executor.submit(() -> {
                WebDriver driver = null;
                long taskStartTime = System.currentTimeMillis();
                try {
                    // 1. Создаем драйвер
                    driver = mbAuth.getActualDriver(userId, taskName, true);
                    log.info("{} started for user ID: {}", taskName, userId);

                    // 2. Засекаем время выполнения
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + TimeUnit.MINUTES.toMillis(timeout);

                    // 3. Выполняем задачу в цикле (для периодических операций)
                    while (mangaReadScheduler.isDriverAlive((ChromeDriver) driver)) {
                        task.accept(driver, userId);

                        // Для квизов добавляем паузу между проверками
                        if ("Quiz".equals(taskName)) {
                            long remainingTime = endTime - System.currentTimeMillis();
                            if (remainingTime <= 0) break;
                            Thread.sleep(Math.min(remainingTime, 30_000)); // Не более 30 сек
                        }
                    }

                    double durationMin = (System.currentTimeMillis() - taskStartTime) / 60000.0;
                    String formattedDuration = String.format("%.1f", durationMin);
                    log.info("{} успешно выполненна для ID: [{}] за {} min (timeout: {} min)",
                        taskName,
                        userId,
                        formattedDuration,
                        TimeUnit.MINUTES.toMinutes(timeout));

                } catch (Exception e) {
                    double durationMin = (System.currentTimeMillis() - taskStartTime) / 60000.0;
                    log.error("Task {} failed for user [{}] after {} min: {}",
                        taskName,
                        userId,
                        String.format("%.1f", durationMin),
                        e.toString());
                } finally {
                    // 4. Гарантированное закрытие драйвера
                    if (driver != null) {
                        try {
                            driver.quit();
                            log.debug("Driver closed for user [{}]", userId);
                        } catch (Exception e) {
                            log.error("Error closing driver for user [{}]: {}", userId, e.getMessage());
                        }
                    } else {log.info("Драйвер закрыт на уровне Task");}
                }
            }))
            .collect(Collectors.toList());

        // 5. Ожидаем завершения всех задач с таймаутом
        long timeoutMillis = TimeUnit.MINUTES.toMillis(timeout);
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

    @Scheduled(initialDelay = 5000, fixedRate = 90 * 60 * 1000)
    public void executeHourlyWithTimeCheck() {
        if (mainTaskInProgress) {
            log.info("Пропускаем hourly task, так как main task выполняется");
            return;
        }
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(2, 0))&& now.isBefore(LocalTime.of(23, 50))){
            killChromeDrivers();
            List<Long> userIds = userCookieRepository.findAll()
                .stream()
                .map(UserCookie::getId)
                .collect(Collectors.toList());


            int maxParallelTasks = Math.min(userIds.size(), 5); // Не более 5 параллельных задач
            ExecutorService executor = Executors.newFixedThreadPool(maxParallelTasks);

            // 6. Этап чтения манги (5 минут)
            log.info("--------[SCHEDULER: START SHADOW READER]--------");
            executeUserTasks(executor, userIds,
                (driver, userId) ->
                    mangaReadScheduler.readMangaChapters(driver,userId,CHAPTERS_PER_HOUR),
                7, "SHADOW READER");
            log.info("--------[SCHEDULER: STOP SHADOW READER]--------");
        }
    }

    public static void killChromeDrivers() {
        try {
            Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM chrome.exe /T");
            log.info("УСПЕШНОЕ УБИЙСТВО ДРАЙВЕРОВ");
        } catch (IOException e) {
            log.warn("Failed to kill chrome processes", e);
        }
    }

    public void executeMainTask(List<Long> userIds, boolean periodicReading, boolean checkViews) {
        if (mainTaskInProgress) {
            log.info("Пропускаем main task, так как он уже выполняется");
            return;
        }
        mainTaskInProgress = true;

        int maxParallelTasks = Math.min(userIds.size(), 5); // Не более 5 параллельных задач
        ExecutorService executor = Executors.newFixedThreadPool(maxParallelTasks);
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelTasks);

        try {
//            log.info("--------SCHEDULER: START QUIZ--------");
//            executeUserTasks(executor, userIds,
//                quizScheduler::monitorQuizRequests,
//                4, "Quiz", checkViews);
//            log.info("--------SCHEDULER: STOP QUIZ--------");

            log.info("--------SCHEDULER: START READER--------");
            executeUserTasks(executor, userIds,
                (driver, userId) ->
                    mangaReadScheduler.readMangaChapters(driver,userId,CHAPTERS_PER_DAY),
                120, "READER", checkViews);
            log.info("--------SCHEDULER: STOP READER--------");

//            log.info("--------SCHEDULER: START COMMENT--------");
//            executeUserTasks(executorService, userIds,
//                commentScheduler::startDailyCommentSending,
//                20, "Comment", checkViews);
//            log.info("--------SCHEDULER: STOP COMMENT--------");

            log.info("--------SCHEDULER: SUCCESSFULLY--------");

        } catch (Exception e) {
            log.error("Error in scheduled task execution", e);
        } finally {
            mainTaskInProgress = false;
        }
    }

    private void executeUserTasks(ExecutorService executor,
                                  List<Long> userIds,
                                  BiConsumer<WebDriver, Long> task,
                                  long timeout,
                                  String taskName,
                                  boolean checkViews) {

        List<Future<?>> futures = userIds.stream()
            .map(userId -> executor.submit(() -> {
                WebDriver driver = null;
                long taskStartTime = System.currentTimeMillis();
                try {
                    // 1. Создаем драйвер
                    driver = mbAuth.getActualDriver(userId, taskName, checkViews);
                    log.info("{} started for user ID: {}", taskName, userId);

                    // 2. Засекаем время выполнения
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + TimeUnit.MINUTES.toMillis(timeout);

                    // 3. Выполняем задачу в цикле (для периодических операций)
                    while (mangaReadScheduler.isDriverAlive((ChromeDriver) driver)) {
                        task.accept(driver, userId);

                        // Для квизов добавляем паузу между проверками
                        if ("Quiz".equals(taskName)) {
                            long remainingTime = endTime - System.currentTimeMillis();
                            if (remainingTime <= 0) break;
                            Thread.sleep(Math.min(remainingTime, 30_000)); // Не более 30 сек
                        }
                    }

                    double durationMin = (System.currentTimeMillis() - taskStartTime) / 60000.0;
                    String formattedDuration = String.format("%.1f", durationMin);
                    log.info("{} completed for user ID: {} in {} minutes", taskName, userId, formattedDuration);

                } catch (Exception e) {
                    log.error("Error in {} task for user ID: {}", taskName, userId, e);
                } finally {
                    if (driver != null) {
                        try {
                            driver.quit();
                        } catch (Exception e) {
                            log.error("Error closing driver for user ID: {}", userId, e);
                        }
                    }
                }
            }))
            .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        long timeoutMillis = TimeUnit.MINUTES.toMillis(timeout);

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