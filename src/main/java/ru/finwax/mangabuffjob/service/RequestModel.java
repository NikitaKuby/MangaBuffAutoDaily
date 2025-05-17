package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestModel {
    private final UserCookieRepository userCookieRepository;
    private final CookieService cookieService;
    private final Map<Long, String> userAgents;

    public HttpHeaders getHeaderBase(Long id){
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        String userAgent = userAgents.getOrDefault(id, userAgents.get(-1L));
        headers.add("User-Agent", userAgent);

        try {
            UserCookie latestCookie = userCookieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No cookies found"));

            if (latestCookie.getCookiesJson() != null) {
                headers.add("Cookie", buildCookieString(
                    cookieService.loadCookies(id)
                        .orElseThrow()));
            }
            if (latestCookie.getCsrfToken() != null) {
                headers.add("x-csrf-token", latestCookie.getCsrfToken());
            }
        } catch (Exception e) {
            log.error("Не удалось получить куки из БД", e);
        }

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

        return headers;
    }

    private String buildCookieString(Set<Cookie> cookies) {
        StringBuilder cookiesBuilder = new StringBuilder();
        for (Cookie seleniumCookie : cookies) {
            cookiesBuilder.append(seleniumCookie.getName())
                .append("=")
                .append(seleniumCookie.getValue())
                .append("; ");
        }
        // Удаляем последнюю "; "
        if (cookiesBuilder.length() > 0) {
            cookiesBuilder.setLength(cookiesBuilder.length() - 2);
        }
        return cookiesBuilder.toString();
    }


    public static ResponseEntity<String> sendPostRequest(HttpHeaders headers, String requestBody, String url) {

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
    }
    public static ResponseEntity<String> sendGetRequest(HttpHeaders headers, String url) {

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    }


}
