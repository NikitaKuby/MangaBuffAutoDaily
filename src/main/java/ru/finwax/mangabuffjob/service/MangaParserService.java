package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.MangaData;
import ru.finwax.mangabuffjob.repository.MangaDataRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        try {
            Document doc = fetchWithRetry(BASE_URL);
            if (doc == null) return;

            int maxPage = calculateMaxPage(doc);

            for (int i = 1; i <= maxPage; i++) {
                String pageUrl = BASE_URL + "?page=" + i;
                log.info("Обработка страницы {}/{}", i, maxPage);

                try {
                    createMangaPage(pageUrl);
                    TimeUnit.MILLISECONDS.sleep(DELAY_BETWEEN_REQUESTS_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Парсинг прерван");
                    return;
                } catch (Exception e) {
                    log.error("Ошибка при обработке страницы {}: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Критическая ошибка в createMangaList: {}", e.getMessage());
        }
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
                Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .ignoreHttpErrors(true); // Важно: разрешаем получать страницы с ошибками

                Connection.Response response = connection.execute();
                int statusCode = response.statusCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    return connection.get();
                } else {
                    log.warn("Попытка {} из {}: URL {} вернул статус {}",
                        attempt, RETRY_ATTEMPTS, url, statusCode);

                    if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        // Для 404 нет смысла повторять запрос
                        log.error("Страница не найдена (404), пропускаем: {}", url);
                        return null;
                    }
                }
            } catch (IOException e) {
                log.warn("Попытка {} из {} не удалась для URL {}: {}",
                    attempt, RETRY_ATTEMPTS, url, e.getMessage());
            }

            if (attempt < RETRY_ATTEMPTS) {
                try {
                    TimeUnit.SECONDS.sleep(attempt * 2L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        log.error("Не удалось загрузить URL после {} попыток: {}", RETRY_ATTEMPTS, url);
        return null;
    }
}