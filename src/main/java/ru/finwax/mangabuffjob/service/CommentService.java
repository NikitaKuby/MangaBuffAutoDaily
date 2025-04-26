package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.finwax.mangabuffjob.Entity.Cookie;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.repository.CookieRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final MangaBuffAuth mangaBuffAuth;
    private final CookieRepository cookieRepository;
    public void sendPostRequestWithCookies(String commentText, String commentId) {
        try {
            mangaBuffAuth.refreshCookies();
        } catch (Exception e) {
            log.error("Не удалось обновить куки, продолжаем со старыми", e);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            // URL и тело запроса
            String url = "https://mangabuff.ru/comments";
            String encodedText = URLEncoder.encode(commentText, StandardCharsets.UTF_8);
            String requestBody = "text=" + encodedText +
                "&commentable_id=" + commentId +
                "&commentable_type=mangaChapter&parent_id=&gif_image=&is_trade=0&is_raffle=0";

            // Заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 OPR/117.0.0.0");

            // Пытаемся получить куки, даже если refresh не сработал
            try {
                Cookie latestCookie = cookieRepository.findTopByOrderByIdDesc()
                    .orElseThrow(() -> new RuntimeException("No cookies found"));

                if (latestCookie.getCookie() != null) {
                    headers.add("Cookie", latestCookie.getCookie());
                }
                if (latestCookie.getCsrf() != null) {
                    headers.add("x-csrf-token", latestCookie.getCsrf());
                }
            } catch (Exception e) {
                log.error("Не удалось получить куки из БД", e);
            }

            // Остальные заголовки
            headers.add("authority", "mangabuff.ru");
            headers.add("accept", "*/*");
            headers.add("accept-language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
            headers.add("origin", "https://mangabuff.ru");
            headers.add("referer", "https://mangabuff.ru/manga/zlodei-hochet-zhit/1/31");
            headers.add("sec-ch-ua", "\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Opera\";v=\"117\"");
            headers.add("sec-ch-ua-mobile", "?0");
            headers.add("sec-ch-ua-platform", "\"Windows\"");
            headers.add("sec-fetch-dest", "empty");
            headers.add("sec-fetch-mode", "cors");
            headers.add("sec-fetch-site", "same-origin");
            headers.add("x-requested-with", "XMLHttpRequest");

            // Создаем HTTP-сущность
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Отправляем запрос
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            log.info("Ответ сервера: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("Ошибка при отправке комментария", e);
            throw new RuntimeException("Failed to send comment", e);
        }
    }
}
