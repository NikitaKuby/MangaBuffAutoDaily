package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class AdvertisingScheduler {

    private static final Integer COUNT_ADV = 3;
    private static final String ADV_PAGE_URL = "https://mangabuff.ru/balance";

    public void performAdv(WebDriver driverWeb) {
        ChromeDriver driver = (ChromeDriver) driverWeb;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            driver.get(ADV_PAGE_URL);
            int countNow = 0;
            while(countNow<COUNT_ADV) {
                try {
                    WebElement loginButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button.button--primary.user-quest__watch-ads-btn")
                        )
                    );
                    loginButton.click();

                    boolean adClosedSuccessfully = waitAndCloseAd(driver, wait);

                    if(adClosedSuccessfully) {
                        countNow++;
                        log.info("Успешно закрыли рекламу {}/{}", countNow, COUNT_ADV);
                    } else {
                        log.warn("Не удалось закрыть рекламу, возможно лимит исчерпан");
                        break;
                    }

                } catch (Exception e) {
                    log.error("Ошибка при обработке рекламы: " + e.getMessage());
                    break;
                }
            }


        } finally {
            driver.quit();
        }
    }

    private boolean waitAndCloseAd(ChromeDriver driver, WebDriverWait wait) {
        try {

            Thread.sleep(35000);

            try {
                // Ищем кнопку закрытия по data-атрибуту и классам
                WebElement closeButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[data-fullscreen-element-name='close-btn']")
                ));

                // Кликаем через JavaScript для надежности
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeButton);
                log.info("Реклама успешно закрыта");
                return true;
            } catch (Exception e) {
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
