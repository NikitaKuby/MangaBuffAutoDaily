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
    private final MbAuth mbAuth;
    private final UserCookieRepository userCookieRepository;

    private static final int CHAPTERS_PER_HOUR = 2;
    private static final int CHAPTERS_PER_DAY = 75;

    // Запускается каждый день в 00:01 ночи
    @Scheduled(cron = "0 1 0 * * ?")
    public void startScheduledPlan() {
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

        try {

            log.info("--------SCHEDULER: START QUIZ--------");
            executeUserTasks(executor, userIds,
                (driver, userId) -> quizScheduler.monitorQuizRequests(driver),
                2, TimeUnit.MINUTES, "Quiz");
            log.info("--------SCHEDULER: STOP QUIZ--------");


            log.info("--------SCHEDULER: START MINE--------");
            executeUserTasks(executor, userIds,
                (driver, userId) -> mineScheduler.performMining(driver),
                2, TimeUnit.MINUTES, "Mining");
            log.info("--------SCHEDULER: STOP MINE--------");


            log.info("--------SCHEDULER: START ADV--------");
            executeUserTasks(executor, userIds,
                (driver, userId) -> advertisingScheduler.performAdv(driver),
                4, TimeUnit.MINUTES, "ADV");
            log.info("--------SCHEDULER: STOP ADV--------");


            log.info("--------SCHEDULER: START READER--------");
            executeUserTasks(executor, userIds,
                (driver, userId) ->
                    mangaReadScheduler.readMangaChapters(driver,userId,CHAPTERS_PER_DAY),
                120, TimeUnit.MINUTES, "READER");

            log.info("--------SCHEDULER: STOP READER--------");
            log.info("--------SCHEDULER: SUCCESSFULLY--------");

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
                    driver = mbAuth.getActualDriver(userId, taskName);
                    log.info("{} started for user ID: {}", taskName, userId);

                    // 2. Засекаем время выполнения
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + timeUnit.toMillis(timeout);

                    // 3. Выполняем задачу в цикле (для периодических операций)
                    while (System.currentTimeMillis() < endTime && mangaReadScheduler.isDriverAlive((ChromeDriver) driver)) {
                        task.accept(driver, userId);

                        // Для квизов добавляем паузу между проверками
                        if ("Quiz".equals(taskName)) {
                            long remainingTime = endTime - System.currentTimeMillis();
                            if (remainingTime <= 0) break;
                            Thread.sleep(Math.min(remainingTime, 30_000)); // Не более 30 сек
                        }
                    }
                    log.info("--------------------------");
                    log.info("--------" + mangaReadScheduler.getAllGiftCounts().toString() + "--------");
                    log.info("--------------------------");
                    log.info("{} completed for user ID: {}", taskName, userId);
                } catch (Exception e) {
                    log.error("Error during {} for user ID: {}: {}", taskName, userId, e.getMessage());
                } finally {
                    // 4. Гарантированное закрытие драйвера
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

    @Scheduled(initialDelay = 60000, fixedRate = 60 * 60 * 1000)
    public void executeHourlyWithTimeCheck() {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(2, 0))){

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
                6, TimeUnit.MINUTES, "SHADOW READER");
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


}