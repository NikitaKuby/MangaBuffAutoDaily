package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final RequestModel requestModel;

    public void sendPostRequestWithCookies(String commentText, String commentId, Long id) {
        try {
            log.info("Пытаемся отправить коментарий");
            // URL и тело запроса
            String url = "https://mangabuff.ru/comments";
            String encodedText = URLEncoder.encode(commentText, StandardCharsets.UTF_8);

            String requestBody = "text=" + encodedText +
                "&commentable_id=" + commentId +
                "&commentable_type=mangaChapter&parent_id=&gif_image=&is_trade=0&is_raffle=0";

            HttpHeaders headers = requestModel.getHeaderBase(id);
            ResponseEntity<String> response = RequestModel.sendPostRequest(headers, requestBody, url);

            // Отправляем запрос
            log.info("Ответ сервера: {}, комментарий отправленн", response.getStatusCode());
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.warn("Попытка: Лимит комментариев. Ждем...");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке комментария", e);
            throw new RuntimeException("Failed to send comment", e);
        }
    }


}
