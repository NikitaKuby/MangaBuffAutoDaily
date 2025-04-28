package ru.finwax.mangabuffjob.Sheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.Entity.MangaData;
import ru.finwax.mangabuffjob.Entity.MangaReadingProgress;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.repository.MangaDataRepository;
import ru.finwax.mangabuffjob.repository.MangaReadingProgressRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MangaReadScheduler {

    private final ConcurrentHashMap<Long, AtomicInteger> giftCounters = new ConcurrentHashMap<>();
    private static final int CHAPTERS_PER_DAY = 75;
    private static final int CHAPTER_READ_TIME_MS =  100 * 1000;
    private static final Random random = new Random();

    private final MangaDataRepository mangaRepository;
    private final MangaReadingProgressRepository progressRepository;

    @Transactional
    public void readMangaChapters(Long id) {
        // Инициализируем счетчик для этого аккаунта
        giftCounters.putIfAbsent(id, new AtomicInteger(0));
        ChromeDriver driver = new ChromeDriver();
        //driver = (ChromeDriver)mbAuth.getActualDriver(id);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        int remainingChapters = CHAPTERS_PER_DAY;

        try {
            while (remainingChapters > 0) {
                Optional<MangaData> mangaOpt = mangaRepository.findNextMangaToRead();
                if (mangaOpt.isEmpty()) {
                    log.info("Нет доступных манг для чтения");
                    break;
                }

                MangaData manga = mangaOpt.get();
                int chaptersRead = getChaptersRead(manga.getId());
                int chaptersToRead = calculateChaptersToRead(manga, chaptersRead, remainingChapters);

                readMangaChapters(manga, id, chaptersRead, chaptersToRead, driver, wait);
                updateProgress(manga, chaptersRead + chaptersToRead);

                remainingChapters -= chaptersToRead;
            }
            log.info("Количество найденных подарков для аккаунта {}: {}",
                id, giftCounters.get(id).get());
        }finally {
            driver.quit();
        }
    }

    private int getChaptersRead(Long mangaId) {
        return progressRepository.findByMangaId(mangaId)
            .map(MangaReadingProgress::getChapterReaded)
            .orElse(0);
    }

    private int calculateChaptersToRead(MangaData manga, int chaptersRead, int remainingChapters) {
        return Math.min(remainingChapters, manga.getCountChapters() - chaptersRead);
    }

    private void readMangaChapters(MangaData manga,
                                   Long accountId,
                                   int startChapter,
                                   int chaptersToRead,
                                   ChromeDriver driver,
                                   WebDriverWait wait) {
        try {
            // Переходим на страницу с главами манги
            driver.get(manga.getUrl());

            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'tabs__item') and @data-page='chapters']")
            ));
            button.click();

            Thread.sleep(5000);

            // Находим все элементы глав
            List<WebElement> chapterItems = driver.findElements(
                By.cssSelector(".chapters__list .chapters__item")
            );
            log.info("countChp" + chapterItems.size());

            // Проверяем, что нашли главы
            if (chapterItems.isEmpty()) {
                log.warn("Не найдено глав для манги {} (ID: {})", manga.getTitle(), manga.getId());
                return;
            }

            // Логируем общее количество найденных глав
            log.info("Найдено {} глав для манги {} (ID: {})",
                chapterItems.size(), manga.getTitle(), manga.getId());

            // Читаем указанное количество глав
            for (int i = startChapter; i < startChapter + chaptersToRead; i++) {
                if (i >= chapterItems.size()) {
                    log.info("Достигнут конец списка глав для манги {} (ID: {})",
                        manga.getTitle(), manga.getId());
                    break;
                }

                WebElement chapterItem = chapterItems.get(chapterItems.size()-i-1);

                try {
                    // Получаем информацию о главе
                    String chapterNumber = chapterItem.getAttribute("data-chapter");
                    log.info("chapterNumber: " + chapterNumber);


                    // Кликаем на главу (открываем в новой вкладке)
                    String chapterUrl = chapterItem.getAttribute("href");
                    ((JavascriptExecutor)driver).executeScript("window.open(arguments[0])", chapterUrl);

                    // Переключаемся на новую вкладку
                    String originalWindow = driver.getWindowHandle();
                    for (String windowHandle : driver.getWindowHandles()) {
                        if (!originalWindow.contentEquals(windowHandle)) {
                            driver.switchTo().window(windowHandle);
                            break;
                        }
                    }

                    // Читаем главу
                    readChapter(driver, accountId);

                    log.info("Прочитали главу");
                    // Закрываем вкладку с главой и возвращаемся к списку глав
                    driver.close();
                    driver.switchTo().window(originalWindow);

                    // Обновляем список глав после возврата (на случай динамической загрузки)
                    chapterItems = driver.findElements(By.cssSelector(".chapters__list .chapters__item"));

                } catch (Exception e) {
                    log.error("Ошибка при чтении главы {} манги {}: {}",
                        i+1, manga.getTitle(), e.getMessage());

                    // В случае ошибки пробуем обновить список глав
                    chapterItems = driver.findElements(By.cssSelector(".chapters__list .chapters__item"));
                }

                // Небольшая пауза между главами
                Thread.sleep(1000 + random.nextInt(2000));
            }
        } catch (Exception e) {
            log.error("Ошибка при чтении манги {}: {}", manga.getTitle(), e.getMessage());
        }
    }

    private void readChapter(ChromeDriver driver,Long accountId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + CHAPTER_READ_TIME_MS;
        Dimension windowSize = driver.manage().window().getSize();
        int maxYOffset = windowSize.getHeight();

        // Оптимизированные параметры
        final int SCROLL_CYCLES = 15; // Уменьшаем количество циклов
        final int BASE_DELAY = 50;    // Увеличиваем задержку
        final double SCROLL_MULTIPLIER = 2.0; // Уменьшаем множитель
        int accumulatedScroll = 0;    // Аккумулятор скролла

        // Для проверки подарков
        long lastGiftCheckTime = System.currentTimeMillis();
        final int GIFT_CHECK_INTERVAL = 5000;

        while (System.currentTimeMillis() < endTime) {
            try {
                // Проверка подарка
                if (System.currentTimeMillis() - lastGiftCheckTime > GIFT_CHECK_INTERVAL) {
                    handleGiftIfPresent(driver, accountId);
                    lastGiftCheckTime = System.currentTimeMillis();
                }

                // Односторонний плавный скролл вниз
                for (int i = 0; i < SCROLL_CYCLES && System.currentTimeMillis() < endTime; i++) {
                    // Модифицированная синусоида (только положительные значения)
                    double progress = (double)i / SCROLL_CYCLES;
                    int scrollStep = (int) (Math.pow(Math.sin(progress * Math.PI), 2) *
                        (maxYOffset * SCROLL_MULTIPLIER / SCROLL_CYCLES));

                    // Гарантируем минимальный скролл вниз
                    scrollStep = Math.max(3, scrollStep);

                    // Плавный скролл с аккумуляцией
                    ((JavascriptExecutor)driver).executeScript(
                        "window.scrollBy({top: arguments[0], behavior: 'auto'})",
                        scrollStep
                    );
                    accumulatedScroll += scrollStep;

                    // Минимальные движения курсора
                    if (random.nextDouble() < 0.08) {
                        new Actions(driver)
                            .moveByOffset(10 + random.nextInt(20), 5 + random.nextInt(10))
                            .pause(Duration.ofMillis(15))
                            .perform();
                    }

                    Thread.sleep(BASE_DELAY);
                }

                // Ускоренный бустер-скролл
                int boostScroll = (int)(maxYOffset * 0.3);
                ((JavascriptExecutor)driver).executeScript(
                    "window.scrollBy(0, arguments[0])",
                    boostScroll
                );
                accumulatedScroll += boostScroll;

            } catch (Exception e) {
                if (e instanceof StaleElementReferenceException) {
                    log.warn("Элемент устарел, перезагружаем страницу");
                    driver.navigate().refresh();
                    Thread.sleep(2000);
                    continue;
                }
                resetScrollPosition(driver);
                Thread.sleep(200); // Добавляем паузу после ошибки
            }
        }
    }

    private void resetScrollPosition(ChromeDriver driver) {
        try {
            new Actions(driver).moveByOffset(0, 0).perform();
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -100)");
        } catch (Exception e) {
            log.warn("Ошибка сброса позиции");
        }
    }

    private void handleGiftIfPresent(ChromeDriver driver, Long accountId) {
        // Универсальный селектор для подарков (учитывает все возможные варианты)
        String giftSelector = "div.card-notification, div[class*='card-notification']";

        // Поиск ВИДИМЫХ подарков (без ожидания)
        List<WebElement> gifts = driver.findElements(By.cssSelector(giftSelector))
            .stream()
            .filter(WebElement::isDisplayed)
            .toList();

        if (!gifts.isEmpty()) {
            WebElement gift = gifts.get(0);
            log.info("Обнаружен активный подарок, обрабатываем...");

            try {
                // 1. Плавный скролл к элементу
                ((JavascriptExecutor)driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                    gift
                );

                // 2. Клик через JavaScript (наиболее надежный способ)
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", gift);
                log.info("Успешно кликнули на подарок");

                // 3. Обработка возможного popup (если появится)
                Thread.sleep(1000);
                tryAlternativeCloseMethods(driver, accountId);

            } catch (Exception e) {
                log.warn("Ошибка при взаимодействии с подарком: {}", e.getMessage());
            }
        }
    }

    private void updateProgress(MangaData manga, int newChaptersRead) {
        boolean hasReaded = newChaptersRead >= manga.getCountChapters();
        progressRepository.upsertProgress(manga.getId(), newChaptersRead, hasReaded);
    }


    private void tryAlternativeCloseMethods(ChromeDriver driver,Long accountId) {
        try {
            // Способ 2: Клик вне модального окна
            new Actions(driver)
                .moveByOffset(0, 0)
                .click()
                .perform();
            log.info("Попытка закрыть через клик вне окна");
            increment(accountId);

        } catch (Exception e) {
            log.warn("Альтернативные методы закрытия не сработали");
        }
    }
    public void increment(Long accountId) {
        giftCounters.get(accountId).incrementAndGet();
    }

    public int getValue(Long accountId) {
        return giftCounters.getOrDefault(accountId, new AtomicInteger(0)).get();
    }

    // Метод для получения всех счетчиков (для мониторинга)
    public Map<Long, Integer> getAllGiftCounts() {
        return giftCounters.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get()
            ));
    }

}

