package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.MangaData;
import ru.finwax.mangabuffjob.repository.MangaDataRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MangaParserService {

    private final MangaDataRepository mangaRepository;
    private static final String BASE_URL = "https://mangabuff.ru/manga";
    private static final int RETRY_ATTEMPTS = 3;
    private static final int DELAY_BETWEEN_REQUESTS_MS = 1000;

    private int counterChp;

    public void createMangaList() {
        log.info("Начинаем создание списка манги");
        // Очищаем существующие данные
        mangaRepository.deleteAll();
        log.info("Существующие данные очищены");

        // Парсим первую страницу
        createMangaPage(BASE_URL);
        log.info("Первая страница обработана");

        // Парсим следующие страницы
        int page = 2;
        while (true) {
            String pageUrl = BASE_URL + "?page=" + page;
            Document pageDoc = fetchWithRetry(pageUrl);
            if (pageDoc == null || pageDoc.select(".cards__item").isEmpty()) {
                break;
            }
            createMangaPage(pageUrl);
            log.info("Страница {} обработана", page);
            page++;
            try {
                Thread.sleep(DELAY_BETWEEN_REQUESTS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Прерывание во время задержки между запросами: {}", e.getMessage());
                break;
            }
        }
        log.info("Создание списка манги завершено. Всего страниц обработано: {}", page - 1);
    }

    private int calculateMaxPage(Document doc) {
        Elements pageItems = doc.select("ul.pagination li.pagination__button a[href*=page]");
        int maxPage = 1;

        for (Element item : pageItems) {
            String href = item.attr("href");
            try {
                int pageNum = Integer.parseInt(href.split("page=")[1]);
                maxPage = Math.max(maxPage, pageNum);
            } catch (NumberFormatException ignored) {
                // Пропускаем нечисловые значения
            }
        }
        return maxPage;
    }

    public void createMangaPage(String pageUrl) {
        try {
            Document pageDoc = fetchWithRetry(pageUrl);
            if (pageDoc == null) return;

            Elements mangaElements = pageDoc.select(".cards__item");
            counterChp=0;
            for (Element mangaElement : mangaElements) {
                try {
                    processMangaElement(mangaElement);
                } catch (Exception e) {
                    log.error("Ошибка при обработке элемента манги: {}", e.getMessage());
                }
            }

            log.info("Глав сохранено: {}/30", counterChp);

        } catch (Exception e) {
            log.error("Ошибка при обработке страницы {}: {}", pageUrl, e.getMessage());
        }
    }

    private void processMangaElement(Element mangaElement) {
        String url = mangaElement.select("a").attr("href");
        Document mangaDoc = fetchWithRetry(url);
        if (mangaDoc == null) return;
        counterChp++;
        String title = extractTitle(mangaDoc);
        int chapters = extractChaptersCount(mangaDoc);

        if (chapters==0){ --counterChp; return;}
        if (!mangaRepository.existsByUrl(url)) {
            MangaData manga = new MangaData();
            manga.setTitle(title);
            manga.setUrl(url);
            manga.setCountChapters(chapters);
            manga.setLastUpdated(LocalDateTime.now());

            mangaRepository.save(manga);
        }
    }

    private String extractTitle(Document doc) {
        try {
            return doc.title().split("/")[0].trim();
        } catch (Exception e) {
            log.warn("Не удалось извлечь заголовок");
            return "Неизвестный заголовок";
        }
    }

    private int extractChaptersCount(Document doc) {
        try {
            Element button = doc.select("button.tabs__item[data-page=chapters]").first();
            if (button != null) {
                Matcher matcher = Pattern.compile("\\((\\d+)\\)").matcher(button.text());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось извлечь количество глав");
        }
        return 0;
    }

    private Document fetchWithRetry(String url) {
        for (int attempt = 1; attempt <= RETRY_ATTEMPTS; attempt++) {
            try {
                Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .execute();

                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    return response.parse();
                }
                log.warn("Попытка {} не удалась для URL {}, код статуса: {}", attempt, url, response.statusCode());
            } catch (IOException e) {
                log.error("Ошибка при попытке {} для URL {}: {}", attempt, url, e.getMessage());
            }
            try {
                Thread.sleep(DELAY_BETWEEN_REQUESTS_MS * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Прерывание во время задержки между попытками: {}", ie.getMessage());
                return null;
            }
        }
        log.error("Все попытки для URL {} исчерпаны", url);
        return null;
    }
}