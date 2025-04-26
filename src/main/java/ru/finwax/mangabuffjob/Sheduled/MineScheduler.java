package ru.finwax.mangabuffjob.Sheduled;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;

@Slf4j
@Component
public class MineScheduler {
    private final MangaBuffAuth mangaBuffAuth;
    private static final String MINE_PAGE_URL = "https://mangabuff.ru/mine";
    private static final String MINE_BUTTON_CSS = "button.main-mine__game-tap";
    private static final int TOTAL_CLICKS = 100;
    private static final int CLICK_INTERVAL_MS = 1000; // 1 секунда

    public MineScheduler(MangaBuffAuth mangaBuffAuth) {
        this.mangaBuffAuth = mangaBuffAuth;
    }

    public void performMining() {
        WebDriver driver = mangaBuffAuth.getDriver();
        WebDriverWait wait = mangaBuffAuth.getWait();
        try {
            // Переходим на страницу майнинга
            driver.get(MINE_PAGE_URL);

            // Находим кнопку майнинга
            WebElement mineButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(MINE_BUTTON_CSS))
            );

            // Выполняем 100 кликов с интервалом 1 секунда
            for (int i = 0; i < TOTAL_CLICKS; i++) {
                mineButton.click();
                Thread.sleep(CLICK_INTERVAL_MS);

                // Логируем прогресс каждые 10 кликов
                if ((i + 1) % 10 == 0) {
                }
            }

            log.info("Майнинг завершен! Всего кликов: " + TOTAL_CLICKS);

        } catch (Exception e) {
            log.error("Ошибка при выполнении майнинга: " + e.getMessage());
        }
    }
}
