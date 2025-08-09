package ru.finwax.mangabuffjob.auth;

import lombok.Getter;
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
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import ru.finwax.mangabuffjob.service.CookieService;

import java.time.Duration;
import java.util.Set;

@Getter
@Slf4j
@Service
@RequiredArgsConstructor
public class MangaBuffAuth {
    private final CookieService cookieService;
    private final UserCookieRepository userCookieRepository;


    public ChromeOptions setUpDriver() {
        // Убеждаемся, что драйвер инициализирован
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-application-cache");
        options.addArguments("--disk-cache-size=0");
        options.addArguments("--disable-cache");
        options.addArguments("--disable-logging");
        options.addArguments("--log-level=3"); // 0 = DEBUG, 3 = NONE
        options.addArguments("--force-device-scale-factor=0.8");
        options.addArguments("--blink-setting=imagesEnabled=false");
        return options;
    }

    public UserCookie authenticate(String login){
        WebDriver driver = new ChromeDriver(setUpDriver());
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
        UserCookie userCookie = null;
        try {
            // Переход на страницу логина
            driver.get("https://mangabuff.ru/login");
            log.info(login);

            // Ждем успешной авторизации (появления аватара)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".header__item.header-profile img")));

            // Дополнительная проверка успешной авторизации
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".header__item.header-profile")));

            // Получение куки и CSRF токена
            Set<Cookie> cookies = driver.manage().getCookies();
            WebElement csrfMetaTag = driver.findElement(By.cssSelector("meta[name='csrf-token']"));
            String csrfToken = csrfMetaTag.getAttribute("content");

            // Проверка существования пользователя и сохранение/обновление
            userCookie = userCookieRepository.findByUsername(login)
                .map(existingUser -> {
                    existingUser.setCookiesJson(cookieService.cookiesToJson(cookies));
                    existingUser.setCsrfToken(csrfToken);
                    return existingUser;
                })
                .orElseGet(() -> UserCookie.builder()
                    .username(login)
                    .cookiesJson(cookieService.cookiesToJson(cookies))
                    .csrfToken(csrfToken)
                    .build());

            userCookie = userCookieRepository.save(userCookie);

        } catch (Exception e) {
            log.error("Ошибка при аутентификации", e);
            throw e;
        } finally {
            driver.quit();
        }
        return userCookie;
    }
}
