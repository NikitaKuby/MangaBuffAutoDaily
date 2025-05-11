package ru.finwax.mangabuffjob.Sheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;
import ru.finwax.mangabuffjob.Entity.MangaData;
import ru.finwax.mangabuffjob.Entity.MangaReadingProgress;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.GiftStatisticRepository;
import ru.finwax.mangabuffjob.repository.MangaDataRepository;
import ru.finwax.mangabuffjob.repository.MangaReadingProgressRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class MangaReadScheduler {

    private final ConcurrentHashMap<Long, AtomicInteger> remainingChaptersMap = new ConcurrentHashMap<>();
    private final GiftStatisticRepository giftRepository;
    private static final int CHAPTER_READ_TIME_MS =  80 * 1000;
    private static final Random random = new Random();

    private final MangaDataRepository mangaRepository;
    private final UserCookieRepository userRepository;
    private final MangaReadingProgressRepository progressRepository;

    public void readMangaChapters(WebDriver driverWeb, Long id, int countChapter) {
        long startTime = System.currentTimeMillis();
        ChromeDriver driver = (ChromeDriver) driverWeb;
        remainingChaptersMap.putIfAbsent(id, new AtomicInteger(countChapter));
        AtomicInteger remainingChapters = remainingChaptersMap.get(id);

        log.debug("[{}] Состояние remainingChapters: hash={}, value={}",
            id,
            System.identityHashCode(remainingChapters),
            remainingChapters.get());

        if (!isDriverAlive((ChromeDriver)driverWeb)) {
            log.error("[{}] Драйвер не активен", id);
            return;
        }
        try {
            int maxIterations = 100;
            int iterations = 0;
            int actuallyRead = 0;
            while (remainingChapters.get() > 0 && iterations++ < maxIterations) {
                Optional<MangaData> mangaOpt = getNextMangaToRead(id);
                if (mangaOpt.isEmpty()) {
                    log.warn("[{}] Нет доступных манг для чтения", id);
                    break;
                }

                MangaData manga = mangaOpt.get();
                int chaptersRead = getChaptersRead(manga.getId(), id);
                log.debug("[{}] chaptersRead: {}", id, chaptersRead);
                int unreadChapters = manga.getCountChapters() - chaptersRead;

                log.debug("[{}] unreadChapters: {}", id, unreadChapters);
                if (unreadChapters <= 0) {
                    updateProgress(manga, id, manga.getCountChapters());
                    continue;
                }

                int chaptersToRead = Math.min(unreadChapters, remainingChapters.get());
                log.debug("[{}] chaptersToRead: {}", id, chaptersToRead);
                actuallyRead = readMangaChaptersInternal(manga, id, chaptersToRead, chaptersRead, driver);
                log.info("[{}] actuallyRead: {}", id, actuallyRead);

                if (actuallyRead > 0) {
                    // Атомарное обновление
                    int newRemaining = remainingChapters.addAndGet(-actuallyRead);
                    log.debug("[{}] Состояние remainingChapters: hash={}, value={}",
                        id,
                        System.identityHashCode(remainingChapters),
                        remainingChapters.get());
                    log.debug("[{}]Осталось глав: {}", id, newRemaining);

                    // Немедленный выход при достижении 0
                    if (newRemaining <= 0) {
                        log.debug("[{}] Все главы прочитаны", id);
                        break;
                    }
                } else {
                    log.warn("[{}] Не удалось прочитать главы, воможно все главы прочитанны", id);
                }
            }
            log.info("[{}] Чтение глав завершено: {}/{} за {} мин",
                id,
                actuallyRead,
                countChapter,
                String.format("%.1f", (System.currentTimeMillis() - startTime) / 60000.0));

        } catch (Exception e) {
            log.error("[{}] Ошибка: {}", id, e.getMessage());
        } finally {
            driver.quit();
            remainingChaptersMap.remove(id);
        }
    }

    private Optional<MangaData> getNextMangaToRead(Long userId) {
        // 1. Поиск незаконченной манги
        Optional<MangaReadingProgress> unfinishedManga = progressRepository
            .findMangaReadingProgressByUserCookieIdAndHasReadedIsFalse(userId);
        if (unfinishedManga.isPresent()) {
            return mangaRepository.findById(unfinishedManga.get().getManga().getId());
        }

        // 2. Поиск следующей манги после последней прочитанной
        Optional<MangaReadingProgress> lastReadManga = progressRepository.findMangaProgress(userId);
        if (lastReadManga.isPresent()) {
            Optional<MangaData> nextManga = mangaRepository.findNextAfterId(lastReadManga.get().getManga().getId());
            if (nextManga.isPresent()) {
                return nextManga;
            }
        }

        // 3. Попытка начать с первой манги
        Optional<MangaData> firstManga = mangaRepository.findFirstByOrderByIdAsc();
        if (firstManga.isPresent()) {
            // Проверяем, не прочитана ли уже первая манга
            int chaptersRead = getChaptersRead(firstManga.get().getId(), userId);
            if (chaptersRead < firstManga.get().getCountChapters()) {
                return firstManga;
            }
        }

        log.info("[{}] Все доступные манги прочитаны", userId);
        return Optional.empty();
    }

    private int getChaptersRead(Long mangaId, Long id) {
        return progressRepository.findByMangaIdAndUserCookieId(mangaId, id)
            .map(MangaReadingProgress::getChapterReaded)
            .orElse(0);
    }



    private int readMangaChaptersInternal(MangaData manga,
                                   Long accountId,
                                   int chaptersToRead,
                                   int chaptersRead,
                                   ChromeDriver driver) {

        if (chaptersToRead <= 0) {
            log.warn("[{}] Запрошено чтение 0 глав", accountId);
            return 0;
        }

        int newChaptersRead = 0;
        try {
            synchronized (driver) {
                driver.get(manga.getUrl());
                Thread.sleep(1000);

                // Кликаем на вкладку "Главы" через JS
                try {
                    ((JavascriptExecutor) driver).executeScript(
                        "document.querySelector('button.tabs__item[data-page=\"chapters\"]').click()"
                    );
                } catch (Exception e) {
                    log.error("[{}] Не удалось кликнуть через JS: {}", accountId, e.getMessage());
                }

                Thread.sleep(500);

                if (manga.getCountChapters() >= 100) {
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                }
                Thread.sleep(1000);

                // Находим и фильтруем только непрочитанные главы
                List<WebElement> chapterItems = driver.findElements(
                    By.cssSelector(".chapters__list .chapters__item:not(:has(.chapters__item-mark))")
                );
                Collections.reverse(chapterItems);

                // Логируем общее количество найденных глав
                log.debug("[{}]Найдено {} непрочитанных глав для манги {} (ID: {})",
                    accountId, chapterItems.size(), manga.getTitle(), manga.getId());

                // Читаем указанное количество глав
                int chaptersToProcess = Math.min(chaptersToRead, chapterItems.size());
                log.debug("chapterToProcess: {}", chaptersToProcess);
                for (int i = 0; i < Math.min(chaptersToRead, chapterItems.size()); i++) {
                    if (readSingleChapter(driver, accountId, chapterItems.get(i))) {
                        newChaptersRead++;
                    }
                    Thread.sleep(500 + random.nextInt(1000));
                }

                if (newChaptersRead > 0) {
                    int totalRead = manga.getCountChapters() - chapterItems.size()+newChaptersRead;
                    updateProgress(manga, accountId, totalRead);
                }

                if(newChaptersRead==0&&chapterItems.isEmpty()&&chaptersRead<manga.getCountChapters()){
                    updateProgress(manga, accountId, manga.getCountChapters());
                }

                return newChaptersRead;
            }
        } catch (Exception e) {
            log.error("[{}]Ошибка при чтении манги {}: {}", accountId, manga.getTitle(), e.getMessage());
            return 0;
        }

    }

    private void readChapter(ChromeDriver driver,Long accountId) throws InterruptedException {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + CHAPTER_READ_TIME_MS;
            Dimension windowSize = driver.manage().window().getSize();
            int maxYOffset = windowSize.getHeight();

            // Получаем общее количество глав
            WebElement pageCounter = driver.findElement(By.cssSelector("div.reader-menu__item--page"));
            String textAfterSpan = pageCounter.getText().substring(
                pageCounter.findElement(By.tagName("span")).getText().length()
            );
            int totalChapters = Integer.parseInt(textAfterSpan.replace("/", "").trim());
            // Если глав >40, то скроллим вдвое быстрее
            double scrollMultiplier = totalChapters > 40 ? 2.0 : 1.0;


            // Оптимизированные параметры
            final int SCROLL_CYCLES = 15; // Уменьшаем количество циклов
            final int BASE_DELAY = 50;    // Увеличиваем задержку
            final double SCROLL_MULTIPLIER = 2.0; // Уменьшаем множитель
            int accumulatedScroll = 0;    // Аккумулятор скролла

            int totalScroll = 0;

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
                        double progress = (double) i / SCROLL_CYCLES;
                        int scrollStep = (int) (Math.pow(Math.sin(progress * Math.PI), 2) *
                            (maxYOffset * SCROLL_MULTIPLIER * scrollMultiplier / SCROLL_CYCLES));

                        // Гарантируем минимальный скролл вниз
                        scrollStep = Math.max(3, scrollStep);

                        // Плавный скролл с аккумуляцией
                        ((JavascriptExecutor) driver).executeScript(
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
                    int boostScroll = (int) (maxYOffset * 0.3);
                    ((JavascriptExecutor) driver).executeScript(
                        "window.scrollBy(0, arguments[0])",
                        boostScroll
                    );
                    accumulatedScroll += boostScroll;

                } catch (Exception e) {
                    if (e instanceof StaleElementReferenceException) {
                        log.warn("Элемент устарел, перезагружаем страницу");
                        driver.navigate().refresh();
                        Thread.sleep(200);
                        continue;
                    }
                    resetScrollPosition(driver);
                    Thread.sleep(200);
                    continue;
                }
            }
        }

        catch (Exception e) {
            if (e instanceof StaleElementReferenceException) {
                log.warn("Элемент устарел, перезагружаем страницу");
                driver.navigate().refresh();
                Thread.sleep(200);
            } else {
                log.error("[{}]Ошибка чтения главы", accountId, e);
                throw e; // Пробрасываем исключение выше
            }
        }

    }

    private boolean readSingleChapter(ChromeDriver driver, Long accountId, WebElement chapterItem) {
        String originalWindow = driver.getWindowHandle();
        try {
            // Открываем главу в новой вкладке
            String chapterUrl = chapterItem.getAttribute("href");
            ((JavascriptExecutor)driver).executeScript("window.open(arguments[0])", chapterUrl);

            // Переключаемся на новую вкладку
            for (String windowHandle : driver.getWindowHandles()) {
                if (!originalWindow.contentEquals(windowHandle)) {
                    driver.switchTo().window(windowHandle);
                    break;
                }
            }
            long chapterStart = System.currentTimeMillis();
            // Читаем главу
            readChapter(driver, accountId);
            log.info("[{}] Глава прочитана за {} сек",
                accountId,
                (System.currentTimeMillis() - chapterStart)/1000 );
            return true;
        } catch (Exception e) {
            log.error("[{}] Ошибка при чтении главы: {}", accountId, e.getMessage());
            return false;
        } finally {
            // Закрываем вкладку с главой
            driver.close();
            driver.switchTo().window(originalWindow);
        }
    }

    private void resetScrollPosition(ChromeDriver driver) throws InterruptedException {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -50)");
        } catch (Exception e) {
            if (isDriverAlive(driver)) {
                log.debug("Не удалось сбросить скролл, но драйвер жив");
            }
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
            log.info("[{}]Обнаружен активный подарок, обрабатываем...", accountId);

            try {
                // 1. Плавный скролл к элементу
                ((JavascriptExecutor)driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                    gift
                );

                // 2. Клик через JavaScript (наиболее надежный способ)
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", gift);

                // 3. Обработка возможного popup (если появится)
                Thread.sleep(500);
                tryAlternativeCloseMethods(driver, accountId);

            } catch (Exception e) {
                log.warn("[{}]Ошибка при взаимодействии с подарком: {}", accountId, e.getMessage());
            }
        }
    }

    private void updateProgress(MangaData manga, Long id, int newChaptersRead) {
        try {
            boolean hasReaded = newChaptersRead >= manga.getCountChapters();

            if (newChaptersRead < 0 || newChaptersRead > manga.getCountChapters()) {
                log.warn("[{}]Некорректное количество глав: {}", id, newChaptersRead);
                return;
            }

            progressRepository.upsertProgress(manga.getId(), id, newChaptersRead, hasReaded);
            log.info("[{}]Обновлен прогресс для mangaId={}: глав={}/{}",
                id, manga.getId(), newChaptersRead, manga.getCountChapters());
        } catch (Exception e) {
            log.error("[{}]Ошибка обновления прогресса: {}", id, e.getMessage());
        }
    }


    private void tryAlternativeCloseMethods(ChromeDriver driver,Long accountId) {
        try {
            // Способ 2: Клик вне модального окна
            new Actions(driver)
                .moveByOffset(0, 0)
                .click()
                .perform();
            log.info("[{}]Пытаемся закрыть окно подарка", accountId);
            updateGift(accountId);

        } catch (Exception e) {
            log.warn("[{}]Альтернативные методы закрытия не сработали", accountId);
        }
    }



    public void updateGift(Long accountId) {
        try {
            // Получаем текущую дату (без времени)
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            Optional<GiftStatistic> existingStat = giftRepository.findByUserIdAndDate(accountId, today);

            if (existingStat.isPresent()) {
                GiftStatistic stat = existingStat.get();
                stat.setCountGift(stat.getCountGift() + 1);
                giftRepository.save(stat);
                log.info("Обновлена статистика подарков для userId={}: текущее кол-во подарков: {}",
                    accountId, stat.getCountGift());
            } else {
                // Создаем новую запись
                GiftStatistic newStat = new GiftStatistic();
                UserCookie userRef = userRepository.getReferenceById(accountId);
                newStat.setUser(userRef);
                newStat.setCountGift(1);
                giftRepository.save(newStat);
                log.info("Создана новая статистика подарков для userId={}: {} подарков", accountId, newStat.getCountGift());
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении статистики подарков: {}", e.getMessage());
        }
    }

    public boolean isDriverAlive(ChromeDriver driver) {
        try {
            driver.getTitle(); // Простая проверка активности драйвера
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

