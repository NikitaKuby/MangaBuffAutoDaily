package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import ru.finwax.mangabuffjob.Entity.MangaProgress;
import ru.finwax.mangabuffjob.repository.MangaProgressRepository;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanningProgress {
    private final RequestModel requestModel;
    private final MangaProgressRepository mangaProgressRepository;
    public void sendGetRequestWithCookies(Long id) {
        try {
            log.info("Пытаемся проверить статус глав/комментариев");

            String url = "https://mangabuff.ru/balance";

            HttpHeaders headers = requestModel.getHeaderBase(id);
            ResponseEntity<String> response = RequestModel.sendGetRequest(headers, url);
            // Отправляем запрос

            String html = response.getBody();
            log.info("Ответ сервера: {}", response.getStatusCode());

            Document doc = Jsoup.parse(html);

            // Ищем блоки с прогрессом
            Elements questItems = doc.select(".user-quest__item");

            int commentsDone = 0;
            int chaptersDone = 0;

            for (Element item : questItems) {
                String text = item.select(".user-quest__text").text();

                if (text.contains("Комментариев")) {
                    commentsDone = Integer.parseInt(text.split(" ")[1]); // "14 из 13" → берем "14"
                } else if (text.contains("Глав")) {
                    chaptersDone = Integer.parseInt(text.split(" ")[1]); // "75 из 75" → берем "75"
                }
            }
            if(mangaProgressRepository.existsByUserIdIs(id)){
                mangaProgressRepository.updateProgressComAndRead(id, chaptersDone, commentsDone, LocalDate.now());
            } else {
                MangaProgress mangaProgress = MangaProgress.builder()
                    .userId(id)
                    .commentDone(commentsDone)
                    .readerDone(chaptersDone)
                    .lastUpdated(LocalDate.now())
                    .build();
                mangaProgressRepository.save(mangaProgress);
            }

            log.info("Комментариев выполнено: {}, Глав прочитано: {}", commentsDone, chaptersDone);


        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.warn("Попытка: Лимит комментариев. Ждем...");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            log.error("Ошибка при сканинге", e);
            throw new RuntimeException("Failed to send comment", e);
        }
    }
}
