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
        WebDriverManager.chromedriver()
//            .driverVersion("136.0.7103.94")
            .clearDriverCache()
            .setup();
        log.info("[{}] WebDriverManager setup завершен.", id);

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


    public WebDriver getActualDriver(Long id, String taskname, boolean checkViews) {
        log.info("[{}] Попытка получить драйвер для задачи: {}", id, taskname);
        ChromeOptions options = setUpDriver(id);

        if(!checkViews){
            options.addArguments("--headless=new"); // Новый headless-режим (Chrome 109+)
            options.addArguments("--disable-gpu"); // В новых версиях необязателен, но можно оставить
            options.addArguments("--window-size=1920,1080");
        }

        String userDataDir = "C:\\path\\to\\dir" + id + taskname + id;
        options.addArguments("user-data-dir=" + userDataDir);
        log.info("[{}] Установлен user-data-dir: {}", id, userDataDir);
        
        WebDriver driver = null;
        try {
            log.info("[{}] Создание экземпляра ChromeDriver...", id);
            WebDriver tempDriver = new ChromeDriver(options);
            driver = tempDriver;
            log.info("[{}] ChromeDriver успешно создан.", id);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Увеличил время ожидания
            log.info("[{}] Установлено ожидание WebDriver до 30 секунд.", id);

            log.info("[{}] Переход на https://mangabuff.ru", id);
            driver.get("https://mangabuff.ru");
            log.info("[{}] Страница https://mangabuff.ru загружена.", id);

            log.info("[{}] Загрузка и добавление куки...", id);
            cookieService.loadCookies(id).ifPresent(cookies -> {
                cookies.forEach(tempDriver.manage()::addCookie);
                log.info("[{}] Куки добавлены.", id);
            });

            log.info("[{}] Обновление страницы после добавления куки...", id);
            driver.navigate().refresh();
            log.info("[{}] Страница обновлена.", id);

            log.info("[{}] Ожидание состояния 'complete' документа...", id);
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            log.info("[{}] Документ в состоянии 'complete'.", id);

            log.info("[{}] Получение CSRF токена...", id);
            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name=\'csrf-token\']"));
            String csrfToken = csrfMetaTag.getAttribute("content");
            log.info("[{}] CSRF токен получен.", id);

            log.info("[{}] Сохранение куки и CSRF токена в БД...", id);
            cookieService.saveCookies(id, driver.manage().getCookies(), csrfToken);
            log.info("[{}] Куки и CSRF токен сохранены.", id);

            return driver;
        } catch (Exception e) {
            log.error("[{}] Ошибка при получении драйвера: {}", id, e.getMessage(), e);
            if (driver != null) {
                driver.quit();
                log.info("[{}] Драйвер закрыт после ошибки.", id);
            }
            throw new RuntimeException("Не удалось получить драйвер для пользователя " + id + ": " + e.getMessage(), e);
        }
    }

}
