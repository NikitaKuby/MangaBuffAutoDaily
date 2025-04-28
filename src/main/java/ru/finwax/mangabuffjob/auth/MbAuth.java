package ru.finwax.mangabuffjob.auth;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.service.CookieService;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MbAuth {

    @Value("${mb.login}")
    private String mbLogin;


    private final CookieService cookieService;

    @Value("${mb.password}")
    private String mbPassword;


    public ChromeOptions setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-application-cache");
        options.addArguments("--disk-cache-size=0");
        options.addArguments("--disable-cache");
        options.addArguments("--force-device-scale-factor=0.5");
        /*options.addArguments("--headless=new"); // Новый headless-режим (Chrome 109+)
        options.addArguments("--disable-gpu"); // В новых версиях необязателен, но можно оставить
        options.addArguments("--window-size=1920,1080");*/
        return options;
    }
    public void authenticate() {
        WebDriver driver = new ChromeDriver(setUpDriver());
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            driver.get("https://mangabuff.ru/login");

            // Ввод email
            WebElement emailField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input.form__field[type='email']")
                )
            );
            emailField.sendKeys(mbLogin);

            // Ввод пароля
            WebElement passwordField = driver.findElement(
                By.cssSelector("input.form__field[type='password']")
            );
            passwordField.sendKeys(mbPassword);

            // Клик по кнопке входа
            WebElement loginButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button.login-button")
                )
            );
            loginButton.click();

            // Ожидание завершения авторизации
            wait.until(ExpectedConditions.urlContains("mangabuff.ru"));
            log.info("Успешный вход");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Set<Cookie> cookies = driver.manage().getCookies();
            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name='csrf-token']"));
            String csrfToken = csrfMetaTag.getAttribute("content");

            // Сохраняем в БД вместо файла
            cookieService.saveCookies(mbLogin, cookies, csrfToken);

        } catch (Exception e) {
            log.error("Ошибка при аутентификации", e);
            throw e;
        }
        finally {
            driver.quit();
        }
    }

    public WebDriver getActualDriver(Long id) {
        WebDriver driver = new ChromeDriver(setUpDriver());
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
