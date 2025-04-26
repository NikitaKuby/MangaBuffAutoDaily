package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.MangaChapter;
import ru.finwax.mangabuffjob.Entity.MangaData;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.repository.MangaDataRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentParserService {
    private final MangaChapterRepository mangaChapterRepository;
    private final MangaDataRepository mangaDataRepository;


    public List<String> getNewChapterIds() {
        if (mangaChapterRepository.hasMoreThanTenUncommentedChapters()){
            return mangaChapterRepository.findFirstTenUncommentedChapterIds();
        } else {
            parseMangaChapter();
            return getNewChapterIds();
        }
    }

    private void parseMangaChapter(){
        // Получаем следующую мангу для парсинга
        MangaData mangaToParse = getNextMangaForParsing();
        createMangaChapter(mangaToParse);
    }

    private MangaData getNextMangaForParsing() {
        // Если таблица mangaChapter пустая, берем первую мангу из manga_parsing_data
        if (mangaChapterRepository.count() == 0) {
            return mangaDataRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No manga found in manga_parsing_data"));
        }

        // Иначе получаем mangaId последней записи в mangaChapter
        Long lastMangaId = mangaChapterRepository.findTopByOrderByIdDesc()
            .orElseThrow(() -> new RuntimeException("No chapters found"))
            .getManga().getId();

        // Ищем следующую мангу после lastMangaId
        return mangaDataRepository.findFirstByIdGreaterThanOrderByIdAsc(lastMangaId)
            .orElseGet(() -> mangaDataRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No more manga to parse")));
    }

    public void createMangaChapter(MangaData mangaData) {
        try {
            Document pageDoc = fetchWithRetry(mangaData.getUrl());
            if (pageDoc == null) return;

            Elements chapterItems = pageDoc.select("a.chapters__item");
            for (int i = chapterItems.size() - 1; i >= 0; i--) {
                try {
                    processMangaElement(chapterItems.get(i), mangaData);
                } catch (Exception e) {
                    log.error("Ошибка при обработке элемента манги: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке страницы {}: {}", mangaData.getUrl(), e.getMessage());
        }
    }

    private void processMangaElement(Element mangaElement, MangaData manga) {
        Element chapterValue = mangaElement.selectFirst("div.chapters__value span");
        int chapterNumber = Integer.parseInt(Objects.requireNonNull(chapterValue).text());

        Element likeButton = mangaElement.selectFirst("button.chapters__like-btn");
        String dataId = Objects.requireNonNull(likeButton).attr("data-id");

        if (!mangaChapterRepository.existsByMangaIdAndChapterNumber(manga.getId(), chapterNumber)) {
            MangaChapter mangaChapter = new MangaChapter();
            mangaChapter.setManga(manga);
            mangaChapter.setChapterNumber(chapterNumber);
            mangaChapter.setCommentId(dataId);
            mangaChapter.setHasComment(false);

            mangaChapterRepository.save(mangaChapter);
        }
    }

    private Document fetchWithRetry(String url) {
            try {
                Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .ignoreHttpErrors(true);

                Connection.Response response = connection.execute();
                int statusCode = response.statusCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    return connection.get();
                } else {
                    if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        log.error("Страница не найдена (404), пропускаем: {}", url);
                        return null;
                    }
                }
            } catch (IOException e) {
                log.warn("Попытка подключения не удалась");
            }
            return null;
    }

}
