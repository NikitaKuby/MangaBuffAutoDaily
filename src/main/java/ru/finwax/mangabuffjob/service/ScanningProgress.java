package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import ru.finwax.mangabuffjob.Entity.MangaProgress;
import ru.finwax.mangabuffjob.repository.MangaProgressRepository;
import ru.finwax.mangabuffjob.repository.GiftStatisticRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;
import ru.finwax.mangabuffjob.Entity.UserCookie;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanningProgress {
    private final RequestModel requestModel;
    private final MangaProgressRepository mangaProgressRepository;
    private final GiftStatisticRepository giftRepository;
    private final UserCookieRepository userRepository;
    private static final String AVATARS_DIR = "avatars";

    private Document fetchDocument(Long id, String url)  {
        HttpHeaders headers = requestModel.getHeaderBase(id);
        ResponseEntity<String> response = RequestModel.sendGetRequest(headers, url);
        String html = response.getBody();
        String baseUrl = "https://mangabuff.ru"; // Базовый URL для абсолютных ссылок
        return Jsoup.parse(html, baseUrl);
    }

    // Новый метод для парсинга баланса алмазов
    private Long parseDiamondBalance(Document doc) {
        try {
            Element balanceElement = doc.selectFirst(".menu__balance");
            if (balanceElement != null) {
                // Удаляем все дочерние элементы (включая div.diamond) и оставляем только текст
                String balanceText = balanceElement.text()
                    .replaceAll("[^0-9]", "") // Оставляем только цифры
                    .trim();

                if (!balanceText.isEmpty()) {
                    return Long.parseLong(balanceText);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при парсинге баланса алмазов", e);
        }
        return 0L; // Возвращаем 0 если не удалось распарсить
    }

    private Integer parseEventGiftCount(Long id) {
        try {
            Document eventDoc = fetchDocument(id, "https://mangabuff.ru/event/pack");
            Element eventGiftElement = eventDoc.selectFirst(".halloween-pack__sweets div:contains(Мои арбузики:) span");
            if (eventGiftElement != null) {
                String countText = eventGiftElement.text().trim();
                if (!countText.isEmpty()) {
                    return Integer.parseInt(countText);
                }
            }
        } catch (NumberFormatException e) {
            log.error("Ошибка парсинга количества event-gift: Нечисловое значение", e);
            return -1; // Возвращаем -1 при ошибке парсинга
        } catch (Exception e) {
            log.error("Ошибка при парсинге количества event-gift", e);
        }
        return null; // Возвращаем null если элемент не найден или другая ошибка
    }

    public void sendGetRequestWithCookies(Long id) {
        try {
            log.info("Пытаемся проверить статус глав/комментариев, прогресс шахты и скачать аватар для пользователя {}", id);

            // Сканируем страницу баланса
            Document balanceDoc = null;
            try {
                balanceDoc = fetchDocument(id, "https://mangabuff.ru/balance");
            } catch (org.springframework.web.client.HttpServerErrorException.BadGateway e) {
                log.error("Ошибка 502 Bad Gateway при сканировании для пользователя {}", id, e);
                return;
            }

            Long diamondBalance = parseDiamondBalance(balanceDoc);
            Integer eventGiftCount = parseEventGiftCount(id);
            log.info("Ответ сервера для /balance: {}", requestModel.sendGetRequest(requestModel.getHeaderBase(id), "https://mangabuff.ru/balance").getStatusCode());
            Document balanceDocPage2 = null;
            try {
                balanceDocPage2 = fetchDocument(id, "https://mangabuff.ru/balance?page=2");
            } catch (org.springframework.web.client.HttpServerErrorException.BadGateway e) {
                log.error("Ошибка 502 Bad Gateway при сканировании для пользователя {}", id, e);
                return;
            }
            // Получаем текущую дату
            LocalDate today = LocalDate.now();
            String todayStr = today.getDayOfMonth() + " " + getMonthName(today.getMonthValue()) + " " + today.getYear();

            // Парсим транзакции со страницы баланса
            Elements transactions = balanceDoc.select(".user-transactions__item");
            Elements transactions2 = balanceDocPage2.select(".user-transactions__item");
            Elements allTransactions = new Elements();
            allTransactions.addAll(transactions);
            if (!transactions2.isEmpty()) {
                allTransactions.addAll(transactions2);
            }
            boolean quizDoneToday = false;
            int advWatchedToday = 0;

            for (Element transaction : allTransactions) {
                String date = transaction.select(".user-transactions__date").text();
                if (date.contains(todayStr)) {
                    String info = transaction.select(".user-transactions__info").text();
                    if (info.contains("Ежедневное прохождение квиза")) {
                        quizDoneToday = true;
                    } else if (info.contains("Просмотр рекламы")) {
                        advWatchedToday++;
                    }
                }
            }

            // Извлекаем информацию об аватаре со страницы баланса
            Element avatarImg = balanceDoc.selectFirst(".header-profile img");
            String avatarUrl = null;
            String avatarAltText = null;
            if (avatarImg != null) {
                avatarAltText = avatarImg.attr("alt");
                String srcAttr = avatarImg.attr("src");
                if (srcAttr != null && !srcAttr.isEmpty()) {
                    avatarUrl = avatarImg.absUrl("src");
                }
            }

            // Изменяем размер аватара в URL с x35 на 150
            if (avatarUrl != null && avatarUrl.contains("/x35/")) {
                avatarUrl = avatarUrl.replace("/x35/", "/x150/");
            }

            // Ищем блоки с прогрессом со страницы баланса
            Elements questItems = balanceDoc.select(".user-quest__item");

            int commentsDone = 0;
            int chaptersDone = 0;
            int totalComments = 0;
            int totalChapters = 0;

            for (Element item : questItems) {
                String text = item.select(".user-quest__text").text();

                if (text.contains("Комментариев")) {
                    String[] parts = text.split(" из ");
                    if (parts.length == 2) {
                        commentsDone = Integer.parseInt(parts[0].split(" ")[1]);
                        totalComments = Integer.parseInt(parts[1]);
                    }
                } else if (text.contains("Глав")) {
                    String[] parts = text.split(" из ");
                    if (parts.length == 2) {
                        chaptersDone = Integer.parseInt(parts[0].split(" ")[1]);
                        totalChapters = Integer.parseInt(parts[1]);
                    }
                }
            }

            // Сканируем страницу шахты
            Document mineDoc = null;
            try {
                mineDoc = fetchDocument(id, "https://mangabuff.ru/mine");
            } catch (org.springframework.web.client.HttpServerErrorException.BadGateway e) {
                log.error("Ошибка 502 Bad Gateway при сканировании для пользователя {}", id, e);
                return;
            }
            log.info("Ответ сервера для /mine: {}", requestModel.sendGetRequest(requestModel.getHeaderBase(id), "https://mangabuff.ru/mine").getStatusCode());

            // Парсим количество оставшихся ударов
            Element hitsLeftElement = mineDoc.selectFirst(".main-mine__game-hits-left");
            int mineHitsLeft = 100; // По умолчанию 100 ударов
            if (hitsLeftElement != null) {
                try {
                    mineHitsLeft = Integer.parseInt(hitsLeftElement.text());
                } catch (NumberFormatException e) {
                    log.error("Не удалось распарсить количество ударов в шахте для пользователя {}", id, e);
                }
            }

            Optional<MangaProgress> existingProgress = mangaProgressRepository.findByUserId(id);

            if(existingProgress.isPresent()){
                MangaProgress progress = existingProgress.get();
                progress.setReaderDone(chaptersDone);
                progress.setCommentDone(commentsDone);
                progress.setTotalReaderChapters(totalChapters);
                progress.setTotalCommentChapters(totalComments);
                progress.setQuizDone(quizDoneToday);
                progress.setAdvDone(advWatchedToday);
                progress.setMineHitsLeft(mineHitsLeft);
                progress.setLastUpdated(LocalDate.now());
                progress.setDiamond(diamondBalance);
                if (avatarUrl != null && !avatarUrl.isEmpty() && progress.getAvatarPath() == null) { // Скачиваем только если аватара еще нет и URL не пустой
                    try {
                        String savedAvatarPath = saveAvatar(avatarUrl, id);
                        progress.setAvatarPath(savedAvatarPath);
                        progress.setAvatarAltText(avatarAltText);
                    } catch (IOException e) {
                        log.error("Ошибка при сохранении аватара для пользователя {}", id, e);
                    }
                }
                mangaProgressRepository.save(progress);

                // Обновляем количество event-gift в базе данных
                if (eventGiftCount != null && eventGiftCount != -1) { // Пропускаем обновление, если была ошибка парсинга или значение null
                    List<GiftStatistic> existingStats = giftRepository.findByUserIdAndDate(id, today);
                    if (!existingStats.isEmpty()) {
                        existingStats.forEach(stat -> {
                            stat.setCountEventGift(eventGiftCount);
                            giftRepository.save(stat);
                        });
                    } else {
                        GiftStatistic newStat = new GiftStatistic();
                        UserCookie user = userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
                        newStat.setUser(user);
                        newStat.setCountEventGift(eventGiftCount);
                        newStat.setDate(today);
                        giftRepository.save(newStat);
                    }
                } else {
                     log.warn("[{}] Пропущено обновление event-gift в БД из-за ошибки парсинга или null значения", id);
                }
            } else {
                MangaProgress mangaProgress = MangaProgress.builder()
                    .userId(id)
                    .commentDone(commentsDone)
                    .readerDone(chaptersDone)
                    .totalReaderChapters(totalChapters)
                    .totalCommentChapters(totalComments)
                    .quizDone(quizDoneToday)
                    .advDone(advWatchedToday)
                    .mineHitsLeft(mineHitsLeft)
                    .diamond(diamondBalance)
                    .lastUpdated(LocalDate.now())
                    .avatarPath(avatarUrl != null && !avatarUrl.isEmpty() ? saveAvatar(avatarUrl, id) : null) // Скачиваем и сохраняем при создании, если URL не пустой
                    .avatarAltText(avatarAltText)
                    .build();
                mangaProgressRepository.save(mangaProgress);
            }

            log.info("Статус {}: Комментариев: {}/{}, Глав: {}/{}, Квиз: {}, Реклама: {}/3, Шахта: {}/100",
                     id, commentsDone, totalComments, chaptersDone, totalChapters,
                     quizDoneToday, advWatchedToday, (100 - mineHitsLeft));

        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.warn("Попытка: Лимит комментариев. Ждем...");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            // Для 401 ошибки просто логируем сообщение без стектрейса
            log.warn("Unauthorized error for user {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при сканинге", e);
            throw new RuntimeException("Failed to send comment", e);
        }
    }

    private String saveAvatar(String avatarUrl, Long userId) throws IOException {
        Path avatarsDir = Paths.get(AVATARS_DIR);
        if (!Files.exists(avatarsDir)) {
            Files.createDirectories(avatarsDir);
        }
        String fileExtension = avatarUrl.substring(avatarUrl.lastIndexOf('.'));
        String fileName = userId + fileExtension;
        Path filePath = avatarsDir.resolve(fileName);

        try (InputStream in = new URL(avatarUrl).openStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        return filePath.toString();
    }

    private String getMonthName(int month) {
        String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return months[month - 1];
    }
}
