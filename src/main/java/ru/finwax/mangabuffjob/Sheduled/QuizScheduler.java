package ru.finwax.mangabuffjob.Sheduled;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v135.network.Network;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int MAX_CLICKS = 15;
    private final AtomicInteger clickCounter = new AtomicInteger(0);
    private final int QUESTION_DELAY_MS = 3000;
    private final MangaBuffAuth mangaBuffAuth;

    public void monitorQuizRequests() {
        clickCounter.set(0);
        // Получаем готовый driver из MangaBuffAuth
         ChromeDriver driver = (ChromeDriver) mangaBuffAuth.getDriver();

        try {
            // Инициализируем DevTools
            DevTools devTools = driver.getDevTools();
            devTools.createSession();

            // Включаем мониторинг сети
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            // Ловим ответы
            devTools.addListener(Network.responseReceived(), response -> {
                if (clickCounter.get() >= MAX_CLICKS) {
                    log.info("Достигнут лимит в {} кликов. Завершаем работу.", MAX_CLICKS);
                    devTools.disconnectSession();
                    return;
                }
                if (response.getResponse().getUrl().contains("https://mangabuff.ru/quiz/")) {
                    try {
                            // Получаем тело ответа сразу при получении события
                            String body = devTools.send(Network.getResponseBody(response.getRequestId())).getBody();
                            JsonNode root = objectMapper.readTree(body);
                            String correctAnswer = root.path("question")
                                .path("correct_text")
                                .asText();
                            responseToQuestion(correctAnswer, driver);
                    } catch (Exception e) {
                        log.warn("Failed to get response body for URL: {}", response.getResponse().getUrl());
                    }
                }
            });

            // Переходим на страницу квиза
            driver.get("https://mangabuff.ru/quiz");
            // Ждем, пока не будет выполнено нужное количество кликов
            while (clickCounter.get() < MAX_CLICKS) {
                Thread.sleep(5000); // Проверяем каждые 5 секунд
            }

            log.info("Успешно выполнено {} кликов. Завершаем работу.", MAX_CLICKS);
            devTools.disconnectSession();

        } catch (Exception e) {
            log.error("Error in quiz monitoring", e);
            throw new RuntimeException("Quiz monitoring failed", e);
        }
    }

    private void responseToQuestion(String correctAnswer, ChromeDriver driver) throws InterruptedException {
        humanDelay();
        List<WebElement> answers = driver.findElements(
            By.cssSelector(".quiz__answer-item.button")
        );
        ;
        for (WebElement answer : answers) {
            if (answer.getText().equals(correctAnswer)) {
                try {
                    if (clickCounter.get() < MAX_CLICKS) {
                        answer.click();
                        clickCounter.incrementAndGet();
                        log.info("[{}]"+"/[{}]", clickCounter.get(),MAX_CLICKS);
                    } else {
                        log.info("Лимит кликов достигнут, пропускаем ответ");
                    }
                } catch (Exception e) {
                    log.error("Ошибка при клике на ответ");
                }
            }
        }
    }

    private void humanDelay() throws InterruptedException {
        Thread.sleep(QUESTION_DELAY_MS + (int)(Math.random() * 2000));
    }
}