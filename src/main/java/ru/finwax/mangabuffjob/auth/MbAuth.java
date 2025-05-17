package ru.finwax.mangabuffjob.auth;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.service.CookieService;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MbAuth {

    private final Map<Long, String> userAgents;
    private final CookieService cookieService;


    public ChromeOptions setUpDriver(Long id) {
        WebDriverManager.chromedriver()
//            .driverVersion("136.0.7103.93")
//            .clearDriverCache()
            .setup();

        ChromeOptions options = new ChromeOptions();

        // Получаем User-Agent для пользователя или используем дефолтный
        String userAgent = userAgents.getOrDefault(id, userAgents.get(-1L));
        options.addArguments("--user-agent="+userAgent);

// Дополнительные настройки для анонимности
        options.addArguments("--disable-webrtc");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-application-cache");
        options.addArguments("--disk-cache-size=0");
        options.addArguments("--disable-cache");
        options.addArguments("--force-device-scale-factor=0.5");
        options.addArguments("--blink-setting=imagesEnabled=false");


        options.addArguments("--headless=new"); // Новый headless-режим (Chrome 109+)
        options.addArguments("--disable-gpu"); // В новых версиях необязателен, но можно оставить
        options.addArguments("--window-size=1920,1080");
        return options;
    }


    public WebDriver getActualDriver(Long id, String taskname) {
        ChromeOptions options = setUpDriver(id);
        options.addArguments("user-data-dir=C:\\path\\to\\dir" + id+taskname+id);
        WebDriver driver = new ChromeDriver(options);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            driver.get("https://mangabuff.ru");


            cookieService.loadCookies(id).ifPresent(cookies -> {
                cookies.forEach(driver.manage()::addCookie);
            });

            driver.navigate().refresh();
            log.info("Загрузка с куками");

            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));

            // Обновляем CSRF токен
            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name='csrf-token']"));
            String csrfToken = csrfMetaTag.getAttribute("content");

            // Обновляем запись в БД
            cookieService.saveCookies(id, driver.manage().getCookies(), csrfToken);

            return driver;
        } catch (Exception e) {
            driver.quit();
            throw e;
        }
    }

}
