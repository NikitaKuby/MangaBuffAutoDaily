package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final RequestModel requestModel;

    // Конфигурация повторных попыток
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_DELAY_MS = 5000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    // Пул потоков для повторных попыток
    private final ScheduledExecutorService retryExecutor =
        Executors.newScheduledThreadPool(4); // Оптимально для 3 аккаунтов

    public void sendPostRequestWithCookies(String commentText, String commentId, Long id) {
        CompletableFuture.runAsync(() -> {
            int attempt = 0;
            boolean success = false;

            while (!success && attempt < MAX_RETRIES) {
                attempt++;
                try {
                    log.info("[{}] Попытка {}/{} - Отправка комментария", id, attempt, MAX_RETRIES);

                    String url = "https://mangabuff.ru/comments";
                    String encodedText = URLEncoder.encode(commentText, StandardCharsets.UTF_8);

                    String requestBody = "text=" + encodedText +
                        "&commentable_id=" + commentId +
                        "&commentable_type=mangaChapter&parent_id=&gif_image=&is_trade=0&is_raffle=0";

                    HttpHeaders headers = requestModel.getHeaderBase(id);
                    headers.add("x-csrf-token", requestModel.getValidCsrf(id));

                    ResponseEntity<String> response = RequestModel.sendPostRequest(headers, requestBody, url);

                    log.info("[{}] Успешно отправлен комментарий. Статус: {}", id, response.getStatusCode());
                    success = true;

                } catch (HttpClientErrorException.UnprocessableEntity e) {
                    long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                    log.warn("[{}] Лимит комментариев. Следующая попытка через {} мс", id, delay);

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.error("[{}] Поток был прерван", id);
                        return;
                    }

                } catch (Exception e) {
                    log.error("[{}] Критическая ошибка при отправке комментария", id, e);
                    break;
                }
            }

            if (!success) {
                log.error("[{}] Не удалось отправить комментарий после {} попыток", id, MAX_RETRIES);
            }
        }, retryExecutor).exceptionally(ex -> {
            log.error("[{}] Необработанное исключение в потоке {}", id, ex);
            return null;
        });
    }
}
