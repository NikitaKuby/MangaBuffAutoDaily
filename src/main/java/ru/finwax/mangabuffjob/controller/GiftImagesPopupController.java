package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
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

        // Update the title label
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (date.isEqual(today)) {
            popupTitleLabel.setText("Подарки за сегодня");
        } else {
            // Format date nicely, e.g., "31 мая 2025"
            // Need a date formatter for Russian locale
            // For simplicity now, use toString()
            popupTitleLabel.setText("Подарки за " + date.toString()); // TODO: Localize date format
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
            System.out.println("[{}] Нет изображений подарков для отображения для даты: {}".replace("{}", accountId.toString()).replace("{}", date.toString()));
            // Optionally display a message in the popup indicating no gifts for this date
            Label noGiftsLabel = new Label("Нет подарков за эту дату."); // TODO: Localize
            imageFlowPane.getChildren().add(noGiftsLabel);
            return;
        }

        for (String imagePath : imagePaths) {
            try {
                Image image = new Image(imagePath);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);
                imageFlowPane.getChildren().add(imageView);
            } catch (Exception e) {
                System.err.println("Ошибка загрузки изображения подарка из URI " + imagePath + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void generateDateButtons() {
        if (accountId == null) {
            // Cannot generate buttons without accountId, will be called from setAccountId
            return;
        }

        dateButtonContainer.getChildren().clear();

        List<LocalDate> availableDates = getAvailableGiftDates(accountId);
        Collections.sort(availableDates, Collections.reverseOrder()); // Sort dates in descending order

        // Add "NEW" button for today if there are gifts today
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (hasGiftsForDate(accountId, today)) { // Check if there are gifts today
             Button todayButton = new Button("NEW"); // TODO: Localize
             todayButton.setOnAction(event -> loadImagesForDate(today));
             dateButtonContainer.getChildren().add(todayButton);
        }

        // Add buttons for previous dates
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
                        System.err.println("Skipping non-date directory in gifts folder: " + dateDir.getName());
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

    // Helper method to format date for button text (e.g., "31 мая")
    private String formatDateButtonText(LocalDate date) {
        // Basic formatting for now. Can be improved with DateFormat for localization.
        return date.getDayOfMonth() + " " + getMonthName(date.getMonthValue()); // TODO: Localize month name
    }

    // Helper method to get Russian month name (can be moved to a utility class)
    private String getMonthName(int month) {
        String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return months[month - 1];
    }
} 
