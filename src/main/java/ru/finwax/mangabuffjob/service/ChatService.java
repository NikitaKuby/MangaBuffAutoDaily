package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Random;


import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final RequestModel requestModel;
    private static final Random random = new Random();

    public void sendRequestWithCookies(Long id) {
        try {
            log.info("[{}]Пытаемся забрать алмаз из чата", id);

            String baseUrl = "https://mc.yandex.ru/clmap/92394178";
            String pageUrl = "https://mangabuff.ru/chat";
            String t = "gdpr(14)ti(1)";

            String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("page-url", pageUrl)
                .queryParam("pointer-click", generatePointerClick())
                .queryParam("browser-info", generateBrowserInfo())
                .queryParam("t", t)
                .build()
                .toUriString();

            HttpHeaders headers = requestModel.getHeaderBase(id);
            headers.add("x-csrf-token", requestModel.getValidCsrf(id));
            headers.add("Referer", "https://mangabuff.ru/");
            ResponseEntity<String> response = RequestModel.sendGetRequest(headers, url);

            // Отправляем запрос
            log.info("[{}]Ответ сервера: {} при попытке забрать алмаз из чата {}", id, response.getStatusCode(), response.getBody());
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.warn("[{}] HttpClientErrorException(37~ строка). Ждем...", id);
            int timeout = 5000;
            try {
                TimeUnit.SECONDS.sleep(timeout);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            log.error("[{}]Ошибка при получении алмаза из чата",id, e);
            throw new RuntimeException("Failed to send comment", e);
        }
    }

    public static String generatePointerClick() {
        int rn = random.nextInt(1_000_000_000);
        int x = 15000 + random.nextInt(40000); // диапазон подбери под свои нужды
        int y = 20000 + random.nextInt(30000);
        int t = random.nextInt(10000);
        String p = "A1?A2A2AAAAAAA1A";
        int X = 20 + random.nextInt(40);
        int Y = 900 + random.nextInt(30);

        return String.format("rn:%d:x:%d:y:%d:t:%d:p:%s:X:%d:Y:%d", rn, x, y, t, p, X, Y);
    }

    public static String generateBrowserInfo() {
        long u = Math.abs(random.nextLong()); // уникальный id
        int v = 2120;
        String vf = randomString(24);
        int rqnl = 1;
        long st = System.currentTimeMillis() / 1000;

        return String.format("u:%d:v:%d:vf:%s:rqnl:%d:st:%d", u, v, vf, rqnl, st);
    }

    private static String randomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
