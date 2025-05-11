package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final RequestModel requestModel;

    public void sendPostRequestWithCookies(String commentText, String commentId, Long id) {
        try {
            log.info("[{}]Пытаемся отправить коментарий", id);
            // URL и тело запроса
            String url = "https://mangabuff.ru/comments";
            String encodedText = URLEncoder.encode(commentText, StandardCharsets.UTF_8);

            String requestBody = "text=" + encodedText +
                "&commentable_id=" + commentId +
                "&commentable_type=mangaChapter&parent_id=&gif_image=&is_trade=0&is_raffle=0";

            HttpHeaders headers = requestModel.getHeaderBase(id);
            ResponseEntity<String> response = RequestModel.sendPostRequest(headers, requestBody, url);

            // Отправляем запрос
            log.info("[{}]Ответ сервера: {}, комментарий отправленн", id, response.getStatusCode());
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.warn("[{}]Попытка: Лимит комментариев. Ждем...", id);
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            log.error("[{}]Ошибка при отправке комментария",id, e);
            throw new RuntimeException("Failed to send comment", e);
        }
    }


}
