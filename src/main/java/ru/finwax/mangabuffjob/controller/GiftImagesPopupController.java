package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
// import java.io.File; // This import is no longer needed
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class GiftImagesPopupController {

    @FXML
    private FlowPane imageFlowPane;
    @FXML
    private HBox dateButtonContainer;
    @FXML
    private Label popupTitleLabel;

    private Long accountId;

    @FXML
    public void initialize() {
        // Initialization logic will go here
        // For now, the accountId is not available in initialize, it will be set later.
        // We will generate buttons and load images once accountId is set.
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
        // Once accountId is set, generate buttons and load images
        generateDateButtons();
        loadImagesForDate(LocalDate.now(ZoneId.systemDefault())); // Load current date gifts initially
    }

    public void loadImagesForDate(LocalDate date) {
        if (accountId == null) {
            System.err.println("Account ID is not set in GiftImagesPopupController.");
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (date.isEqual(today)) {
            popupTitleLabel.setText("Подарки за сегодня");
        } else {
            popupTitleLabel.setText("Подарки за " + date.toString());
        }

        imageFlowPane.getChildren().clear();

        String giftDirPath = "gifts/account_" + accountId + "/" + date.toString();
        File giftDir = new File(giftDirPath);

        List<String> imagePaths = new ArrayList<>();
        if (giftDir.exists() && giftDir.isDirectory()) {
            File[] giftFiles = giftDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));
            if (giftFiles != null) {
                // Sort files by name to ensure consistent order
                Arrays.sort(giftFiles, Comparator.comparing(File::getName));
                for (File file : giftFiles) {
                    imagePaths.add(file.toURI().toString());
                }
            }
        }

        if (imagePaths.isEmpty()) {
            log.debug("[{}] Нет изображений подарков для отображения для даты: {}".replace("{}", accountId.toString()).replace("{}", date.toString()));
            // Optionally display a message in the popup indicating no gifts for this date
            Label noGiftsLabel = new Label("Нет подарков за эту дату.");
            imageFlowPane.getChildren().add(noGiftsLabel);
            return;
        }

        for (String imagePath : imagePaths) {
            try {
                // Принудительно создаем новое изображение без кэширования
                Image image = new Image(imagePath, false);
                
                // Создаем новый ImageView для каждого изображения
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(120);
                imageView.setPreserveRatio(true);
                
                // Добавляем обработчик ошибок загрузки
                image.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        log.warn("Ошибка загрузки изображения: {}", imagePath);
                    }
                });
                
                imageFlowPane.getChildren().add(imageView);
            } catch (Exception e) {
                log.error("Ошибка загрузки изображения подарка из URI " + imagePath + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Принудительно перезагружает все изображения подарков
     * Очищает кэш JavaFX и перезагружает изображения
     */
    public void forceReloadImages() {
        if (accountId == null) {
            log.warn("Account ID is not set in GiftImagesPopupController.");
            return;
        }
        
        log.debug("[{}] Принудительная перезагрузка изображений подарков", accountId);
        
        // Очищаем все изображения из кэша JavaFX
        imageFlowPane.getChildren().clear();
        
        // Принудительно очищаем кэш изображений
        System.gc(); // Запускаем сборщик мусора для очистки кэша
        
        // Перезагружаем изображения для текущей даты
        loadImagesForDate(LocalDate.now(ZoneId.systemDefault()));
        
        log.debug("[{}] Изображения подарков перезагружены", accountId);
    }

    private void generateDateButtons() {
        if (accountId == null) {
            // Cannot generate buttons without accountId, will be called from setAccountId
            return;
        }

        dateButtonContainer.getChildren().clear();

        List<LocalDate> availableDates = getAvailableGiftDates(accountId);
        Collections.sort(availableDates, Collections.reverseOrder());

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (hasGiftsForDate(accountId, today)) {
             Button todayButton = new Button("NEW");
             todayButton.setOnAction(event -> loadImagesForDate(today));
             dateButtonContainer.getChildren().add(todayButton);
        }

        for (LocalDate date : availableDates) {
            if (!date.isEqual(today)) { // Don't add a button for today again
                Button dateButton = new Button(formatDateButtonText(date)); // Format date for button text
                dateButton.setOnAction(event -> loadImagesForDate(date));
                dateButtonContainer.getChildren().add(dateButton);
            }
        }

        // If no buttons were added (no gifts found for any date), maybe display a message?
        // For now, the container will just be empty.
    }

    private List<LocalDate> getAvailableGiftDates(Long accountId) {
        List<LocalDate> dates = new ArrayList<>();
        String accountGiftDir = "gifts/account_" + accountId;
        File accountDir = new File(accountGiftDir);

        if (accountDir.exists() && accountDir.isDirectory()) {
            File[] dateDirs = accountDir.listFiles(File::isDirectory);
            if (dateDirs != null) {
                for (File dateDir : dateDirs) {
                    try {
                        // Attempt to parse directory name as a date
                        LocalDate date = LocalDate.parse(dateDir.getName());
                        dates.add(date);
                    } catch (java.time.format.DateTimeParseException e) {
                        log.error("Skipping non-date directory in gifts folder: " + dateDir.getName());
                    }
                }
            }
        }
        return dates;
    }

    private boolean hasGiftsForDate(Long accountId, LocalDate date) {
        String giftDirPath = "gifts/account_" + accountId + "/" + date.toString();
        File giftDir = new File(giftDirPath);
        if (giftDir.exists() && giftDir.isDirectory()) {
            File[] giftFiles = giftDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));
            return giftFiles != null && giftFiles.length > 0;
        }
        return false;
    }

    private String formatDateButtonText(LocalDate date) {
        return date.getDayOfMonth() + " " + getMonthName(date.getMonthValue());
    }

    private String getMonthName(int month) {
        String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return months[month - 1];
    }
} 
