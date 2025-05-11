package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v133.network.Network;
import org.openqa.selenium.devtools.v133.network.model.Response;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class MineScheduler {
    private static final String MINE_PAGE_URL = "https://mangabuff.ru/mine";
    private static final String MINE_BUTTON_CSS = "button.main-mine__game-tap";
    private static final String MINE_HIT_URL = "https://mangabuff.ru/mine/hit";
    private static final int TOTAL_CLICKS = 100;
    private static final int CLICK_INTERVAL_MS = 1000;
    private static final int MAX_PARALLEL_SESSIONS = 5;

    private final AtomicBoolean limitReached = new AtomicBoolean(false);
    private final AtomicInteger activeMiningSessions = new AtomicInteger(0);
    private final ReentrantLock miningLock = new ReentrantLock(true);
    private final Semaphore concurrentSessionsSemaphore = new Semaphore(MAX_PARALLEL_SESSIONS);

    public void performMining(WebDriver driver, Long id) {
        if (!tryAcquireMiningPermission()) {
            log.warn("[{}]Max parallel mining sessions reached ({})", id, MAX_PARALLEL_SESSIONS);
            return;
        }

        try {
            if (!(driver instanceof ChromeDriver chromeDriver)) {
                throw new IllegalArgumentException("Only ChromeDriver is supported for mining");
            }

            limitReached.set(false);
            CompletableFuture<Void> limitCheckFuture = new CompletableFuture<>();

            DevTools devTools = chromeDriver.getDevTools();
            devTools.createSession();

            try {
                setupNetworkMonitoring(devTools, limitCheckFuture);
                performMiningOperations(driver, limitCheckFuture, id);
            } finally {
                cleanupResources(devTools);
                releaseMiningPermission();
            }
        } catch (Exception e) {
            log.error("[{}]Critical mining error: {}", id, e.getMessage());
            driver.quit();
            releaseMiningPermission();
        }finally {
            driver.quit();
        }
    }

    private boolean tryAcquireMiningPermission() {
        try {
            if (!concurrentSessionsSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                return false;
            }

            miningLock.lock();
            try {
                if (activeMiningSessions.get() >= MAX_PARALLEL_SESSIONS) {
                    return false;
                }
                activeMiningSessions.incrementAndGet();
                return true;
            } finally {
                miningLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void releaseMiningPermission() {
        miningLock.lock();
        try {
            activeMiningSessions.decrementAndGet();
        } finally {
            miningLock.unlock();
            concurrentSessionsSemaphore.release();
        }
    }

    private void performMiningOperations(WebDriver driver, CompletableFuture<Void> limitCheckFuture, Long id) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.get(MINE_PAGE_URL);

        WebElement mineButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(MINE_BUTTON_CSS))
        );

        int clicksPerformed = 0;
        while (clicksPerformed < TOTAL_CLICKS && !limitReached.get()) {
            try {
                mineButton.click();
                clicksPerformed++;
                Thread.sleep(CLICK_INTERVAL_MS);
                try {
                    limitCheckFuture.get(CLICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    break;
                } catch (TimeoutException e) {
                    // Continue mining
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                log.error("Click error: {}", e.getMessage());
                if (e.getMessage().contains("element is not attached")) {
                    break;
                }
            }
        }

        logMiningResult(clicksPerformed, id);
    }

    private void logMiningResult(int clicksPerformed, Long id) {
        if (limitReached.get()) {
            log.info("[{}]Mining stopped. Limit reached. Clicks: {}", id, clicksPerformed);
        } else {
            log.info("[{}]Mining completed. Total clicks: {}", id, clicksPerformed);
        }
    }

    private void setupNetworkMonitoring(DevTools devTools, CompletableFuture<Void> limitCheckFuture) {
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        // Обработка ответов сервера
        devTools.addListener(Network.responseReceived(), response -> {
            Response receivedResponse = response.getResponse();
            if (receivedResponse.getUrl().contains(MINE_HIT_URL)) {
                if (receivedResponse.getStatus() == 403) {
                    handleLimitReached(limitCheckFuture, "403 status code received");
                }
            }
        });

        // Обработка ошибок загрузки
        devTools.addListener(Network.loadingFailed(), event -> {
            if (event.getRequestId() != null && event.getErrorText().toLowerCase().contains("blocked")) {
                handleLimitReached(limitCheckFuture, "Request blocked: " + event.getErrorText());
            }
        });
    }

    private void handleLimitReached(CompletableFuture<Void> limitCheckFuture, String reason) {
        log.info("Limit detected: {}", reason);
        limitReached.set(true);
        limitCheckFuture.complete(null);
    }

    private void cleanupResources(DevTools devTools) {
        try {
            if (devTools != null) {
                devTools.disconnectSession();
            }
        } catch (Exception e) {
            log.error("DevTools cleanup error: {}", e.getMessage());
        }
    }
}
