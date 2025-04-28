package ru.finwax.mangabuffjob.Sheduled;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v133.network.Network;
import org.openqa.selenium.devtools.v133.network.model.*;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class MineScheduler {
    private static final String MINE_PAGE_URL = "https://mangabuff.ru/mine";
    private static final String MINE_BUTTON_CSS = "button.main-mine__game-tap";
    private static final String MINE_HIT_URL = "https://mangabuff.ru/mine/hit";
    private static final String LIMIT_MESSAGE = "Лимит ударов на сегодня исчерпан";
    private static final int TOTAL_CLICKS = 100;
    private static final int CLICK_INTERVAL_MS = 1000;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private volatile boolean limitReached = false;
    private CompletableFuture<Void> limitCheckFuture;


    public void performMining(WebDriver driver) {
        if (!(driver instanceof ChromeDriver chromeDriver)) {
            throw new IllegalArgumentException("Only ChromeDriver is supported for mining");
        }

        DevTools devTools = chromeDriver.getDevTools();
        devTools.createSession();

        try {
            // Настройка DevTools для перехвата сетевых запросов
            setupNetworkMonitoring(devTools);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            driver.get(MINE_PAGE_URL);

            WebElement mineButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(MINE_BUTTON_CSS))
            );

            for (int i = 0; i < TOTAL_CLICKS && !limitReached; i++) {
                mineButton.click();

                // Проверяем не достигнут ли лимит
                if (limitReached) {
                    log.info("Обнаружен лимит ударов. Майнинг остановлен.");
                    break;
                }

                try {
                    // Ждем с таймаутом, чтобы не блокировать проверку лимита
                    limitCheckFuture.get(CLICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // Таймаут ожидания - продолжаем клики
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Ошибка при проверке лимита: {}", e.getMessage());
                    break;
                }
            }

            if (!limitReached) {
                log.info("Майнинг завершен! Всего кликов: {}", TOTAL_CLICKS);
            }

        } catch (Exception e) {
            log.error("Ошибка при выполнении майнинга: {}", e.getMessage());
        } finally {
            devTools.disconnectSession();
            driver.quit();
        }
    }

    private void setupNetworkMonitoring(DevTools devTools) {
        // Включаем мониторинг сети
        devTools.send(Network.enable(
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));

        Map<RequestId, String> requestIdToUrlMap = new ConcurrentHashMap<>();

        limitCheckFuture = new CompletableFuture<>();

        // Слушаем входящие ответы
        devTools.addListener(Network.responseReceived(), response -> {

            if (response.getResponse().getUrl().contains(MINE_HIT_URL)) {
                try {
                    String body =
                        devTools.send(Network.getResponseBody(response.getRequestId())).getBody();
                    JsonNode root = MAPPER.readTree(body);
                    if (LIMIT_MESSAGE.equals(root.get("message").asText())) {
                        limitReached = true;
                        limitCheckFuture.complete(null);
                        log.info("Обнаружено сообщение о лимите в ответе");
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обработке ответа: {}", e.getMessage());
                }
            }
        });


        // 3. Слушаем ошибки загрузки
        devTools.addListener(Network.loadingFailed(), event -> {
            RequestId requestId = event.getRequestId();
            String url = requestIdToUrlMap.remove(requestId); // Удаляем и получаем URL

            if (url != null && url.contains(MINE_HIT_URL)) {
                log.warn("Ошибка запроса майнинга ({}): {}", url, event.getErrorText());
            }
        });
    }
}
