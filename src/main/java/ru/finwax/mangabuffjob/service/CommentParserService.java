package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.MangaChapter;
import ru.finwax.mangabuffjob.Entity.MangaData;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.repository.MangaDataRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentParserService {
    private final MangaChapterRepository mangaChapterRepository;
    private final MangaDataRepository mangaDataRepository;
    private final UserCookieRepository userCookieRepository;


    public List<String> getNewChapterIds(int count, Long id) {
        if (mangaChapterRepository.hasMoreThanTenUncommentedChapters(count, id)){
            return mangaChapterRepository.findFirstTenUncommentedChapterIds(PageRequest.of(0, count), id);
        } else {
            log.debug("parse");
            parseMangaChapter(id);
            return getNewChapterIds(count, id);
        }
    }

    private void parseMangaChapter(Long id){
        // Получаем следующую мангу для парсинга
        MangaData mangaToParse = getNextMangaForParsing(id);
        createMangaChapter(mangaToParse, id);
    }

    private MangaData getNextMangaForParsing(Long id) {
        // Если таблица mangaChapter пустая, берем первую мангу из manga_parsing_data
        if (mangaChapterRepository.countByUserId(id) == 0) {
            return mangaDataRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No manga found in manga_parsing_data"));
        }

        // Иначе получаем mangaId последней записи в mangaChapter
        Long lastMangaId = mangaChapterRepository.findLastMangaIdByUserId(id)
            .orElseThrow(() -> new RuntimeException("No chapters found"));
        log.debug("[{}] lastmangaId = {}",id, lastMangaId);
        // Ищем следующую мангу после lastMangaId
        log.debug("[{}] nextId = {}",id,mangaDataRepository.findNextAfterId(lastMangaId).get().getId() );

        return mangaDataRepository.findNextAfterId(lastMangaId)
                .orElseThrow(() -> new RuntimeException("No more manga to parse"));
    }

    public void createMangaChapter(MangaData mangaData, Long id) {
        try {
            Document pageDoc = fetchWithRetry(mangaData.getUrl());
            if (pageDoc == null) return;
            Elements chapterItems = pageDoc.select("a.chapters__item");
            if (chapterItems.isEmpty()) {
                log.info("Манга ID: {} не содержит глав для комментариев, помечаем как обработанную", mangaData.getId());
                MangaChapter markerChapter = new MangaChapter();
                markerChapter.setManga(mangaData);
                markerChapter.setUser(userCookieRepository.getReferenceById(id));
                markerChapter.setChapterNumber((double)-1); // Специальное значение для маркера
                markerChapter.setHasComment(true); // Помечаем как "обработанную"

                mangaChapterRepository.save(markerChapter);
                return;
            }
            for (int i = chapterItems.size() - 1; i >= 0; i--) {
                try {
                    processMangaElement(chapterItems.get(i), mangaData, id);
                } catch (Exception e) {
                    log.error("Ошибка при обработке элемента манги: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке страницы {}: {}", mangaData.getUrl(), e.getMessage());
        }
    }

    private void processMangaElement(Element mangaElement, MangaData manga, Long id) {
        Element chapterValue = mangaElement.selectFirst("div.chapters__value span");
        log.info("chapter value = "+ Objects.requireNonNull(chapterValue).text());
        double chapterNumber = Double.parseDouble(Objects.requireNonNull(chapterValue).text());

        Element likeButton = mangaElement.selectFirst("button.chapters__like-btn");
        String chapterId = Objects.requireNonNull(likeButton).attr("data-id");
        if (!mangaChapterRepository.existsByMangaIdAndChapterNumberAndUserId(manga.getId(), chapterNumber, id)) {
            MangaChapter mangaChapter = new MangaChapter();
            mangaChapter.setManga(manga);
            UserCookie userRef = userCookieRepository.getReferenceById(id);
            mangaChapter.setUser(userRef);
            mangaChapter.setChapterNumber(chapterNumber);
            mangaChapter.setCommentId(chapterId);
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
