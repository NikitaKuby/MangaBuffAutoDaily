package ru.finwax.mangabuffjob.auth;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.repository.CookieRepository;

import java.time.Duration;
import java.util.Set;

@Getter
@Slf4j
@Service
@RequiredArgsConstructor
public class MangaBuffAuth {

    private final CookieRepository cookieRepository;

    @Value("${vk.login}")
    private String vkLogin;

    @Value("${vk.password}")
    private String vkPassword;

    private String cookieMangaBuff;
    private ChromeOptions options;
    private WebDriver driver;
    private WebDriverWait wait;
    private String csrfToken;

    public void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        options = new ChromeOptions();
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-application-cache");
        options.addArguments("--disk-cache-size=0");
        options.addArguments("--disable-cache");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }
    public void authenticate() {
        this.setUpDriver();
        try {
            // Переход на страницу логина
            driver.get("https://mangabuff.ru/login");

            // Клик по кнопке VK
            WebElement vkButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class, 'social__vk') and contains(@href, 'vkontakte')]")
            ));
            vkButton.click();

            // Ожидание завершения авторизации
            wait.until(ExpectedConditions.urlContains("mangabuff.ru"));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            log.error("Ошибка при аутентификации", e);
            throw e;
        }
        finally {
            /*driver.close();*/
        }
    }


    public void refreshCookies() {
        try {
            // Обновляем страницу
            driver.navigate().refresh();

            // Ждём обновления страницы
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));

            // Получаем обновлённые куки
            Set<Cookie> cookies = driver.manage().getCookies();
            cookieMangaBuff = buildCookieString(cookies);

            // Получаем CSRF-токен из мета-тега
            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name='csrf-token']"));
            this.csrfToken = csrfMetaTag.getAttribute("content");

            ru.finwax.mangabuffjob.Entity.Cookie cookie = new ru.finwax.mangabuffjob.Entity.Cookie();
            cookie.setCookie(cookieMangaBuff);
            cookie.setCsrf(csrfToken);
            log.info("Куки успешно обновлены");
            cookieRepository.save(cookie);
        } catch (Exception e) {
            log.error("Ошибка при обновлении кук", e);
            throw new RuntimeException("Не удалось обновить куки", e);
        }
    }

    private String buildCookieString(Set<Cookie> cookies) {
        StringBuilder cookiesBuilder = new StringBuilder();
        for (Cookie seleniumCookie : cookies) {
            cookiesBuilder.append(seleniumCookie.getName())
                .append("=")
                .append(seleniumCookie.getValue())
                .append("; ");
        }
        // Удаляем последнюю "; "
        if (cookiesBuilder.length() > 0) {
            cookiesBuilder.setLength(cookiesBuilder.length() - 2);
        }
        return cookiesBuilder.toString();
    }
}
