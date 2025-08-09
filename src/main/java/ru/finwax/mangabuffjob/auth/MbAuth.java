package ru.finwax.mangabuffjob.auth;

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
import ru.finwax.mangabuffjob.service.DriverManager;

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
    private final DriverManager driverManager;

    private static final String CARDS_TASK_NAME = "cards";

    public ChromeOptions setUpDriver(Long id) {
        log.debug("[{}] Настройка драйвера...", id);

        log.debug("[{}] Драйвер готов к использованию", id);

        ChromeOptions options = new ChromeOptions();

        // Получаем User-Agent для пользователя или используем дефолтный
        String userAgent = userAgents.getOrDefault(id%10, userAgents.get(-1L));
        options.addArguments("--user-agent="+userAgent);
        log.debug("[{}] Использован User-Agent: {}", id, userAgent);

        // Дополнительные настройки для анонимности
        options.addArguments("--disable-webrtc");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-application-cache");
        options.addArguments("--disk-cache-size=0");
        options.addArguments("--disable-logging");
        options.addArguments("--log-level=3"); // 0 = DEBUG, 3 = NONE
        options.addArguments("--disable-cache");
        options.addArguments("--force-device-scale-factor=0.5");
        options.addArguments("--blink-setting=imagesEnabled=false");
        log.info("[{}] Добавлены ChromeOptions аргументы.", id);
        return options;
    }


    public ChromeDriver getActualDriver(Long id, String taskname, boolean checkViews) {
        ChromeOptions options = setUpDriver(id);

        if (taskname.equals(CARDS_TASK_NAME)) {
            options.addArguments("--force-device-scale-factor=1");
        }

        if (!checkViews) {
            options.addArguments("--headless=new"); // Новый headless-режим (Chrome 109+)
            options.addArguments("--disable-gpu"); // В новых версиях необязателен, но можно оставить
            options.addArguments("--window-size=1920,1080");
        }

        String driverId;
        WebDriver driver = null;


        driverId = driverManager.generateDriverId(id, taskname);
        try {
            WebDriver tempDriver = new ChromeDriver(options);
            driver = tempDriver;


            driverManager.registerDriver(driverId, driver);
            log.debug("[{}] Обычный драйвер зарегистрирован: {}", id, driverId);


            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Увеличил время ожидания

            driver.get("https://mangabuff.ru");

            cookieService.loadCookies(id).ifPresent(cookies -> {
                cookies.forEach(tempDriver.manage()::addCookie);
                log.debug("[{}] Куки добавлены.", id);
            });

            driver.navigate().refresh();

            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));

            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name=\'csrf-token\']"));
            String csrfToken = csrfMetaTag.getAttribute("content");

            log.debug("[{}] Сохранение куки и CSRF токена в БД...", id);
            cookieService.saveCookies(id, driver.manage().getCookies(), csrfToken);
            log.debug("[{}] Куки и CSRF токен сохранены.", id);

            return (ChromeDriver) driver;
        } catch (Exception e) {
            log.error("[{}] Ошибка при получении драйвера: {}", id, e.getMessage(), e);
            if (driver != null) {
                // Закрываем драйвер в соответствующем менеджере
                driverManager.unregisterDriver(driverId);
                log.info("[{}] Обычный драйвер закрыт после ошибки.", id);
            }
            throw new RuntimeException("Не удалось получить драйвер для пользователя " + id + ": " + e.getMessage(), e);
        }


    }

    public void killUserDriver(Long userId, String taskname) {
        // Проверяем, должна ли задача использовать защищенные драйверы
            String driverId = driverManager.generateDriverId(userId, taskname);
            driverManager.unregisterDriver(driverId);
            log.debug("[user{}:{}] Обычный драйвер закрыт", userId, taskname);
    }


}
