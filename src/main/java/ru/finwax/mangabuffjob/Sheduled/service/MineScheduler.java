package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.service.DriverManager;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MineScheduler {
    private final MbAuth mbAuth;
    private static final String MINE_PAGE_URL = "https://mangabuff.ru/mine";
    private static final String MINE_HIT_URL = "https://mangabuff.ru/mine/hit";
    private static final String MINE_BUTTON_CSS = "button.main-mine__game-tap";
    private static final int CLICK_INTERVAL_MS = 1000;
    private final DriverManager driverManager;


    public void performMining(Long id, Integer TOTAL_CLICKS, boolean checkViews, boolean autoUpgrade, boolean autoExchange) {
        log.info("Зашли в PerformMining");
        String driverId = driverManager.generateDriverId(id, "mining");
        ChromeDriver driver = mbAuth.getActualDriver(id, "mining", checkViews);
        log.info("Создали драйвер");
        
        try {
            // Проверяем состояние драйвера перед использованием
            if (driverManager.isShuttingDown()) {
                log.warn("[{}] Приложение выключается, прерываем майнинг", id);
                return;
            }
            
            DevTools devTools = driver.getDevTools();
            devTools.createSession();
            try {
                performMiningOperations(driver, null, id, TOTAL_CLICKS, autoUpgrade, autoExchange);
            } finally {
                cleanupResources(devTools);
            }
        } catch (Exception e) {
            log.error("[{}]Critical mining error: {}", id, e.getMessage());
        } finally {
            driverManager.unregisterDriver(driverId);
        }
    }

    private void performMiningOperations(WebDriver driver,
                                         CompletableFuture<Void> limitCheckFuture,
                                         Long id, Integer TOTAL_CLICKS,
                                         boolean autoUpgrade,
                                         boolean autoExchange) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.get(MINE_PAGE_URL);

        WebElement mineButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(MINE_BUTTON_CSS))
        );

        
        int errorCountClick = 0;
        int clicksPerformed = 0;
        while (clicksPerformed < TOTAL_CLICKS) {
            try {
                mineButton.click();
                clicksPerformed++;
                Thread.sleep(CLICK_INTERVAL_MS);
            } catch (Exception e) {
                if(clicksPerformed%10==0 && errorCountClick==clicksPerformed) {
                    log.error("[{}] Click error. Exception type: (MineScheduler.java 72", id);
                }
                errorCountClick=clicksPerformed+10;
                if (e.getMessage() != null && e.getMessage().contains("element is not attached") || e.getClass().getName().contains("StaleElementReferenceException")) {
                    log.warn("[{}] Element is stale or detached, stopping mining for this session.", id);
                    break;
                }
            }
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // После всех кликов - логика для чекбоксов
        if(autoExchange||autoUpgrade){
            try {
                WebElement modalOverview = driver.findElement(By.cssSelector(".main-mine__header_score-count"));
                modalOverview.click();
                log.info("[{}] Клик по кнопке 'Модального окна' выполнен.", id);
            } catch (Exception e) {
                log.warn("[{}] Не удалось кликнуть по кнопке 'Модального окна': {}", id, e.getMessage());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (autoUpgrade) {
                try {
                    log.info("Зашли в автоапгрейд");
                    WebElement upgradeBtn = driver.findElement(By.cssSelector("button.mine-shop__upgrade-btn"));
                    if (upgradeBtn.isDisplayed() && upgradeBtn.isEnabled()) {
                        upgradeBtn.click();
                        log.info("[{}] Клик по кнопке 'Улучшить' выполнен.", id);
                    }
                } catch (Exception e) {
                    log.warn("[{}] Не удалось кликнуть по кнопке 'Улучшить': {}", id, e.getMessage());
                }
            } else if (autoExchange) {
                try {
                    log.info("Зашли в автоОбмен");
                    WebElement exchangeBtn = driver.findElement(By.cssSelector("button.mine-shop__ore-change-btn"));
                    if (exchangeBtn.isDisplayed() && exchangeBtn.isEnabled()) {
                        exchangeBtn.click();
                        log.info("[{}] Клик по кнопке 'Обменять' выполнен.", id);
                    }
                } catch (Exception e) {
                    log.warn("[{}] Не удалось кликнуть по кнопке 'Обменять': {}", id, e.getMessage());
                }
            }
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logMiningResult(clicksPerformed, id);
    }

    private void logMiningResult(int clicksPerformed, Long id) {
        log.info("[{}]Mining completed. Total clicks: {}", id, clicksPerformed);
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
