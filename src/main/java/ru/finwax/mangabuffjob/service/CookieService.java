package ru.finwax.mangabuffjob.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.Entity.dto.CookieDTO;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {

    private final UserCookieRepository userCookieRepository;
    private final ObjectMapper objectMapper; // Jackson

    public void saveCookies(String username, Set<Cookie> seleniumCookies, String csrfToken) {
        try {
            Set<CookieDTO> cookieDTOs = seleniumCookies.stream()
                .map(CookieDTO::fromSeleniumCookie)
                .collect(Collectors.toSet());

            String cookiesJson = objectMapper.writeValueAsString(cookieDTOs);

            userCookieRepository.findByUsername(username)
                .ifPresentOrElse(
                    cookie -> {
                        cookie.setCookiesJson(cookiesJson);
                        cookie.setCsrfToken(csrfToken);
                        userCookieRepository.save(cookie);
                    },
                    () -> {
                        UserCookie newCookie = new UserCookie();
                        newCookie.setUsername(username);
                        newCookie.setCookiesJson(cookiesJson);
                        newCookie.setCsrfToken(csrfToken);
                        userCookieRepository.save(newCookie);
                    }
                );

        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации кук", e);
        }
    }

    public void saveCookies(Long id, Set<Cookie> seleniumCookies, String csrfToken) {
        try {
            Set<CookieDTO> cookieDTOs = seleniumCookies.stream()
                .map(CookieDTO::fromSeleniumCookie)
                .collect(Collectors.toSet());

            String cookiesJson = objectMapper.writeValueAsString(cookieDTOs);

            userCookieRepository.findById(id)
                .ifPresentOrElse(
                    cookie -> {
                        cookie.setCookiesJson(cookiesJson);
                        log.debug("saveNewCookie for user {}", id);
                        cookie.setCsrfToken(csrfToken);
                        userCookieRepository.save(cookie);
                    },
                    () -> {
                        UserCookie newCookie = new UserCookie();
                        newCookie.setCookiesJson(cookiesJson);
                        newCookie.setCsrfToken(csrfToken);
                        userCookieRepository.save(newCookie);
                    }
                );

        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации кук", e);
        }
    }

    public Optional<Set<Cookie>> loadCookies(Long id) {
        return userCookieRepository.findById(id)
            .map(cookie -> {
                try {
                    Set<CookieDTO> cookieDTOs = objectMapper.readValue(
                        cookie.getCookiesJson(),
                        new TypeReference<Set<CookieDTO>>() {}
                    );

                    return cookieDTOs.stream()
                        .map(CookieDTO::toSeleniumCookie)
                        .collect(Collectors.toSet());
                } catch (JsonProcessingException e) {
                    log.error("Ошибка десериализации кук", e);
                    return null;
                }
            });
    }

}