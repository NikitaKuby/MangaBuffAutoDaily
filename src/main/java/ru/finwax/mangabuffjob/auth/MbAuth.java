package ru.finwax.mangabuffjob.auth;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import ru.finwax.mangabuffjob.service.CookieService;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Getter
@Slf4j
@Service
@RequiredArgsConstructor
public class MbAuth {

    private final Map<Long, String> userAgents;
    private final CookieService cookieService;
    private final UserCookieRepository userCookieRepository;


    public ChromeOptions setUpDriver(Long id) {
        log.info("[{}] Настройка драйвера...", id);
        try {
            WebDriverManager.chromedriver()
                .clearDriverCache()
                .clearResolutionCache()
                .setup();
        } catch (Exception e) {
            log.warn("Не удалось очистить кэш драйвера: {}", id);
        }

        ChromeOptions options = new ChromeOptions();

        // Получаем User-Agent для пользователя или используем дефолтный
        String userAgent = userAgents.getOrDefault(id, userAgents.get(-1L));
        options.addArguments("--user-agent="+userAgent);
        log.info("[{}] Использован User-Agent: {}", id, userAgent);

// Дополнительные настройки для анонимности
        options.addArguments("--disable-webrtc");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-application-cache");
        options.addArguments("--disk-cache-size=0");
        options.addArguments("--disable-cache");
        options.addArguments("--force-device-scale-factor=0.5");
        options.addArguments("--blink-setting=imagesEnabled=false");
        log.info("[{}] Добавлены ChromeOptions аргументы.", id);
        return options;
    }


    public ChromeDriver getActualDriver(Long id, String taskname, boolean checkViews) {
        ChromeOptions options = setUpDriver(id);

        if(!checkViews){
            options.addArguments("--headless=new"); // Новый headless-режим (Chrome 109+)
            options.addArguments("--disable-gpu"); // В новых версиях необязателен, но можно оставить
            options.addArguments("--window-size=1920,1080");
        }

        String userDataDir = "C:\\path\\to\\dir" + id + taskname + id;
        options.addArguments("user-data-dir=" + userDataDir);

        WebDriver driver = null;
        try {
            WebDriver tempDriver = new ChromeDriver(options);
            driver = tempDriver;

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Увеличил время ожидания

            driver.get("https://mangabuff.ru");

            cookieService.loadCookies(id).ifPresent(cookies -> {
                cookies.forEach(tempDriver.manage()::addCookie);
                log.info("[{}] Куки добавлены.", id);
            });

            driver.navigate().refresh();

            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));

            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name=\'csrf-token\']"));
            String csrfToken = csrfMetaTag.getAttribute("content");

            log.info("[{}] Сохранение куки и CSRF токена в БД...", id);
            cookieService.saveCookies(id, driver.manage().getCookies(), csrfToken);
            log.info("[{}] Куки и CSRF токен сохранены.", id);


            return (ChromeDriver)driver;
        } catch (Exception e) {
            log.error("[{}] Ошибка при получении драйвера: {}", id, e.getMessage(), e);
            if (driver != null) {
                driver.quit();
                log.info("[{}] Драйвер закрыт после ошибки.", id);
            }
            throw new RuntimeException("Не удалось получить драйвер для пользователя " + id + ": " + e.getMessage(), e);
        }
    }

    public void killUserDriver(Long userId, String taskname) {
        String userDataDir = "C:\\path\\to\\dir" + userId + taskname + userId;
        String escapedPath = userDataDir.replace("\\", "\\\\");

        // Команды для выполнения
        String[] commands = {
            // Убить chrome.exe
            "taskkill /F /FI \"CommandLine like '%" + escapedPath + "%'\" /T",

            // Убить chromedriver.exe (более агрессивно)
            "wmic process where \"CommandLine like '%" + escapedPath + "%'\" delete"
        };

        for (String cmd : commands) {
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor(); // Ждем завершения

                if (process.exitValue() != 0) {
                    log.debug("[user{}:{}] Команда завершилась с кодом {}: {}",
                        userId, taskname, process.exitValue(), cmd);
                }
            } catch (Exception e) {
                log.warn("[user{}:{}] Ошибка выполнения команды '{}': {}",
                    userId, taskname, cmd, e.getMessage());
            }
        }
    }

}
