package ru.finwax.mangabuffjob.Sheduled.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v136.network.Network;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.auth.MbAuth;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {
    private final MbAuth mbAuth;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final int MAX_CLICKS = 15;

    public void monitorQuizRequests(Long id, boolean checkViews) {
        ChromeDriver driver = (ChromeDriver) mbAuth.getActualDriver(id, "mining", checkViews);
        AtomicInteger clickCounter = new AtomicInteger(0);
        clickCounter.set(0);
        // Получаем готовый driver из MangaBuffAuth

        DevTools devTools = driver.getDevTools();
        devTools.createSession();

        try {

            // Включаем мониторинг сети
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            // Ловим ответы
            devTools.addListener(Network.responseReceived(), response -> {
                if (clickCounter.get() >= MAX_CLICKS) {
                    devTools.disconnectSession();
                    return;
                }
                if (response.getResponse().getUrl().contains("https://mangabuff.ru/quiz/")) {
                    try {
                            // Получаем тело ответа сразу при получении события
                            String body =
                                devTools.send(Network.getResponseBody(response.getRequestId())).getBody();
                            JsonNode root = objectMapper.readTree(body);
                            String correctAnswer = root.path("question")
                                .path("correct_text")
                                .asText();
                            responseToQuestion(correctAnswer, driver, clickCounter, id);
                            if(root.path("status").asText().equals("restart")){
                                clickCounter.set(0);
                            }
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

            devTools.disconnectSession();

        } catch (Exception e) {
            log.error("Error in quiz monitoring", e);
            throw new RuntimeException("Quiz monitoring failed", e);
        } finally {
            devTools.disconnectSession();  // Закрываем только DevTools
            driver.quit();
        }

    }

    private void responseToQuestion(String correctAnswer, ChromeDriver driver, AtomicInteger clickCounter, Long id) throws InterruptedException {
        humanDelay();
        List<WebElement> answers = driver.findElements(
            By.cssSelector(".quiz__answer-item.button")
        );

        for (WebElement answer : answers) {
            if (answer.getText().equals(correctAnswer)) {
                try {
                    if (clickCounter.get() < MAX_CLICKS) {
                        // Прокручиваем к элементу
                        ((JavascriptExecutor)driver).executeScript(
                            "arguments[0].scrollIntoView({block: 'center'});",
                            answer
                        );
                        answer.click();
                        clickCounter.incrementAndGet();
                        if(clickCounter.intValue() % 2==1){ log.info("[{}]"+"/[{}]", clickCounter.get(),MAX_CLICKS);}
                    } else {
                        log.info("[{}]Лимит кликов достигнут, пропускаем ответ", id);
                    }
                } catch (Exception e) {
                    log.error("[{}]Ошибка при клике на ответ {}", id, e.getMessage().substring(0, 250));
                }
            }
        }
    }

    private void humanDelay() throws InterruptedException {
        int QUESTION_DELAY_MS = 3000;
        Thread.sleep(QUESTION_DELAY_MS + (int)(Math.random() * 2000));
    }
}