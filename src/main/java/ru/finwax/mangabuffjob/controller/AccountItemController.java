package ru.finwax.mangabuffjob.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.model.AccountProgress;
import ru.finwax.mangabuffjob.service.AccountService;
import ru.finwax.mangabuffjob.repository.GiftStatisticRepository;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.stage.Popup;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;

import java.awt.Desktop;
import java.net.URI;

@Component
@Getter
public class AccountItemController {

    @FXML
    private ImageView avatarImageView;
    @FXML
    private Label avatarAltTextLabel;
    @FXML
    private Label readerProgressLabel;
    @FXML
    private Label commentProgressLabel;
    @FXML
    private Label quizProgressLabel;
    @FXML
    private Label mineProgressLabel;
    @FXML
    private Label advProgressLabel;
    @FXML
    private Label diamondCountLabel;
    @FXML
    private ImageView diamondImageView;
    @FXML
    private Label giftCountLabel;
    @FXML
    private ImageView giftImageView;
    @FXML
    private Label eventGiftCountLabel;
    @FXML
    private ImageView eventGiftImageView;
    @FXML
    private Label accountNameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label diamondLabel;
    @FXML
    private Label readersLabel;
    @FXML
    private Label commentsLabel;
    @FXML
    private Label mineHitsLabel;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private Label quizLabel;
    @FXML
    private Label advLabel;
    @FXML
    private VBox progressBarContainer;
    @FXML
    private ProgressBar readingProgressBar;
    @FXML
    private ProgressBar commentProgressBar;

    @FXML
    private Button startChaptersButton;
    @FXML
    private Button startCommentsButton;
    @FXML
    private Button startQuizButton;
    @FXML
    private Button startMiningButton;
    @FXML
    private Button startAdvButton;
    @FXML
    private Button deleteButton;
    @FXML
    private HBox accountItem;

    @FXML
    private StackPane overlayPane;
    @FXML
    private Button reloginButton;

    @FXML
    private Button openCardsButton;

    private final AccountService accountService;
    private final MangaBuffJobViewController parentController;
    private final AdvertisingScheduler advertisingScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final CommentScheduler commentScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final GiftStatisticRepository giftRepository;
    private final MbAuth mbAuth;
    private CheckBox viewsCheckBox;

    private AccountProgress account;

    private Timeline loadingTimelineChapters;
    private Timeline loadingTimelineComments;
    private Timeline loadingTimelineQuiz;
    private Timeline loadingTimelineMining;
    private Timeline loadingTimelineAdv;

    private Popup giftImagesPopup;

    private static final String CARDS_TASK_NAME = "cards";

    private Timeline hidePopupTimeline;

    public AccountItemController(
        AccountService accountService,
        MangaBuffJobViewController parentController,
        AdvertisingScheduler advertisingScheduler,
        MineScheduler mineScheduler,
        QuizScheduler quizScheduler,
        CommentScheduler commentScheduler,
        MangaReadScheduler mangaReadScheduler,
        GiftStatisticRepository giftRepository,
        MbAuth mbAuth
    ) {
        this.accountService = accountService;
        this.parentController = parentController;
        this.advertisingScheduler = advertisingScheduler;
        this.mineScheduler = mineScheduler;
        this.quizScheduler = quizScheduler;
        this.commentScheduler = commentScheduler;
        this.mangaReadScheduler = mangaReadScheduler;
        this.giftRepository = giftRepository;
        this.mbAuth = mbAuth;
    }

    public void setAccount(AccountProgress account) {
        this.account = account;
        readerProgressLabel.setText(account.getReaderProgress());
        commentProgressLabel.setText(account.getCommentProgress());
        quizProgressLabel.setText(String.valueOf(account.getQuizDone()));
        mineProgressLabel.setText(account.getMineProgress());
        advProgressLabel.setText(account.getAdvProgress());

        // Установка количества подарков
        updateGiftCount();

        // Автоматическая установка цвета кнопок
        updateButtonStates();

        // Отображение аватара
        if (account.getAvatarPath() != null) {
            File avatarFile = new File(account.getAvatarPath());
            if (avatarFile.exists()) {
                Image avatarImage = new Image(avatarFile.toURI().toString());
                avatarImageView.setImage(avatarImage);
            } else {
                avatarImageView.setImage(null);
            }
        } else {
            avatarImageView.setImage(null);
        }

        // Установка иконки подарка
        Image giftImage = new Image(getClass().getResourceAsStream("/static/card-gift.png"));
        giftImageView.setImage(giftImage);

        Image diamondImage = new Image(getClass().getResourceAsStream("/static/diamond.png"));
        diamondImageView.setImage(diamondImage);

        diamondCountLabel.setText(String.valueOf(account.getDiamond()));

        if(account.getAvatarAltText().length()>6) {
            avatarAltTextLabel.setText(account.getAvatarAltText().substring(0, 6));
        }else {
            avatarAltTextLabel.setText(account.getAvatarAltText());
        }

        // Добавляем обработчики событий мыши для показа подарков
        giftImageView.setOnMouseEntered(event -> showGiftImagesPopup());
        giftImageView.setOnMouseExited(event -> hideGiftImagesPopupWithDelay());

        startChaptersButton.setOnAction(event -> handleStartChapters());
        startCommentsButton.setOnAction(event -> handleStartComments());
        startQuizButton.setOnAction(event -> handleStartQuiz());
        startMiningButton.setOnAction(event -> handleStartMining());
        startAdvButton.setOnAction(event -> handleStartAdv());
        deleteButton.setOnAction(event -> handleDeleteAccount());

        // Add event handler for open cards button
        openCardsButton.setOnAction(event -> handleOpenCards());

        // Initially hide overlay and relogin button
        overlayPane.setVisible(false);
        reloginButton.setVisible(false);
        accountItem.setDisable(false);
    }

    @Transactional
    public void updateGiftCount() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // Define the directory path for gift images
        String giftDirPath = "gifts/account_" + account.getUserId() + "/" + today;
        File giftDir = new File(giftDirPath);

        int calculatedGiftCount = 0;
        if (giftDir.exists() && giftDir.isDirectory()) {
            File[] giftFiles = giftDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));
            if (giftFiles != null) {
                calculatedGiftCount = giftFiles.length;
            }
        }

        final int finalGiftCount = calculatedGiftCount;

        // Fetch event gift count from DB
        List<GiftStatistic> gifts = giftRepository.findByUserIdAndDate(account.getUserId(), today);
        int eventGiftCount = gifts.isEmpty() ? 0 : gifts.get(0).getCountEventGift();
        Image eventGiftImage = new Image(getClass().getResourceAsStream("/static/icon/watermelon.png"));
        Platform.runLater(() -> {
            giftCountLabel.setText(String.valueOf(finalGiftCount));
            eventGiftCountLabel.setText(String.valueOf(eventGiftCount));

            // Обновляем иконку event-gift
            if (eventGiftCount == -1) { // Используем -1 для индикации ошибки сканирования
                eventGiftImageView.setImage(eventGiftImage);
            } else {
                eventGiftImageView.setImage(eventGiftImage);
            }
        });
    }

    public void updateButtonStates() {
        // Главы
        boolean allRead = account.getReaderProgress() != null && account.getReaderProgress().split("/")[0].equals(account.getReaderProgress().split("/")[1]);
        setButtonState(startChaptersButton, allRead ? "green" : "white");
        // Комментарии
        boolean allComments = account.getCommentProgress() != null && account.getCommentProgress().split("/")[0].equals(account.getCommentProgress().split("/")[1]);
        setButtonState(startCommentsButton, allComments ? "green" : "white");
        // Квиз
        setButtonState(startQuizButton, Boolean.TRUE.equals(account.getQuizDone()) ? "green" : "white");
        // Майнинг
        boolean mineDone = account.getMineHitsLeft() != null && account.getMineHitsLeft() == 0;
        setButtonState(startMiningButton, mineDone ? "green" : "white");
        // Реклама
        boolean advDone = account.getAdvDone() != null && account.getAdvDone() >= 3;
        setButtonState(startAdvButton, advDone ? "green" : "white");

        // Disable task buttons if overlay is visible
        boolean disableTaskButtons = overlayPane != null && overlayPane.isVisible();
        startChaptersButton.setDisable(disableTaskButtons || allRead);
        startCommentsButton.setDisable(disableTaskButtons || allComments);
        startQuizButton.setDisable(disableTaskButtons || Boolean.TRUE.equals(account.getQuizDone()));
        startMiningButton.setDisable(disableTaskButtons || (account.getMineHitsLeft() != null && account.getMineHitsLeft() == 0));
        startAdvButton.setDisable(disableTaskButtons || (account.getAdvDone() != null && account.getAdvDone() >= 3));

        // Hide task buttons if overlay is visible
        startChaptersButton.setVisible(!disableTaskButtons);
        startCommentsButton.setVisible(!disableTaskButtons);
        startQuizButton.setVisible(!disableTaskButtons);
        startMiningButton.setVisible(!disableTaskButtons);
        startAdvButton.setVisible(!disableTaskButtons);

        // Show relogin button only if overlay is visible
        if (reloginButton != null) {
            reloginButton.setVisible(overlayPane != null && overlayPane.isVisible());
        }

        // Ensure delete and open cards buttons are always enabled unless overlay is visible
        boolean disableAccountButtons = overlayPane != null && overlayPane.isVisible();
        if (deleteButton != null) {
            deleteButton.setDisable(disableAccountButtons);
            deleteButton.setVisible(!disableAccountButtons);
        }
        if (openCardsButton != null) {
            openCardsButton.setDisable(disableAccountButtons);
            openCardsButton.setVisible(!disableAccountButtons);
        }
    }

    void setButtonState(Button button, String state) {
        button.getStyleClass().removeAll("btn-green", "btn-white", "btn-red", "btn-blue", "btn-loading");
        stopLoadingAnimation(button);
        switch (state) {
            case "green":
                button.getStyleClass().add("btn-green");
                button.setText("готово");
                button.setDisable(true);
                break;
            case "white":
                button.getStyleClass().add("btn-white");
                button.setText("начать");
                button.setDisable(false);
                break;
            case "red":
                button.getStyleClass().add("btn-red");
                button.setText("ошибка");
                button.setDisable(false);
                break;
            case "blue":
                button.getStyleClass().add("btn-blue");
                button.getStyleClass().add("btn-loading");
                button.setDisable(true);
                startLoadingAnimation(button);
                break;
        }
    }

    private boolean isButtonGreen(Button button) {
        return button.getStyleClass().contains("btn-green");
    }

    private void startLoadingAnimation(Button button) {
        // Check if the button is one of the task buttons before starting animation
        if (button == startChaptersButton || button == startCommentsButton || button == startQuizButton || button == startMiningButton || button == startAdvButton) {
            Timeline timeline = new Timeline();
            final String baseText = "Загрузка";
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(0), e -> button.setText(baseText)));
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(0.5), e -> button.setText(baseText + ".")));
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> button.setText(baseText + "..")));
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), e -> button.setText(baseText + "...")));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            // Привязываем timeline к кнопке
            if (button == startChaptersButton) loadingTimelineChapters = timeline;
            if (button == startCommentsButton) loadingTimelineComments = timeline;
            if (button == startQuizButton) loadingTimelineQuiz = timeline;
            if (button == startMiningButton) loadingTimelineMining = timeline;
            if (button == startAdvButton) loadingTimelineAdv = timeline;
        }
    }

    private void stopLoadingAnimation(Button button) {
        if (button == startChaptersButton && loadingTimelineChapters != null) { loadingTimelineChapters.stop(); loadingTimelineChapters = null; } else
        if (button == startCommentsButton && loadingTimelineComments != null) { loadingTimelineComments.stop(); loadingTimelineComments = null; } else
        if (button == startQuizButton && loadingTimelineQuiz != null) { loadingTimelineQuiz.stop(); loadingTimelineQuiz = null; } else
        if (button == startMiningButton && loadingTimelineMining != null) { loadingTimelineMining.stop(); loadingTimelineMining = null; } else
        if (button == startAdvButton && loadingTimelineAdv != null) { loadingTimelineAdv.stop(); loadingTimelineAdv = null; }
    }

    private boolean isAnyButtonLoading() {
        return (loadingTimelineChapters != null) || (loadingTimelineComments != null) || (loadingTimelineQuiz != null) || (loadingTimelineMining != null) || (loadingTimelineAdv != null);
    }

    private void handleStartChapters() {
        if (isButtonGreen(startChaptersButton)) return;
        System.out.println("Start Chapters button clicked for account: " + account.getUsername());
        setButtonState(startChaptersButton, "blue"); // Синяя кнопка и блокировка

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Integer countOfChapters = Integer.parseInt(Objects.requireNonNull(account.getReaderProgress()).split("/")[1]) -
                        Integer.parseInt(account.getReaderProgress().split("/")[0]);
                    System.out.println(countOfChapters);

                    mangaReadScheduler.readMangaChapters(account.getUserId(), countOfChapters, viewsCheckBox.isSelected());
                    Platform.runLater(() -> {
                        parentController.scanAccount(account.getUserId());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startChaptersButton, "red"));
                    System.err.println("Ошибка при чтении глав: " + e.getMessage());
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartComments() {
        if (isButtonGreen(startCommentsButton)) return;
        System.out.println("Start Comments button clicked for account: " + account.getUsername());
        setButtonState(startCommentsButton, "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Integer countOfComments = Integer.parseInt(Objects.requireNonNull(account.getCommentProgress()).split("/")[1]) -
                        Integer.parseInt(account.getCommentProgress().split("/")[0]);

                    CompletableFuture<Void> future = commentScheduler.startDailyCommentSending(account.getUserId(), countOfComments);

                    // Ждем завершения всех комментариев
                    future.get();

                    Platform.runLater(() -> {

                        parentController.scanAccount(account.getUserId());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startCommentsButton, "red"));
                    System.err.println("Ошибка при отправке комментариев: " + e.getMessage());
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartQuiz() {
        if (isButtonGreen(startQuizButton)) return;
        System.out.println("Start Quiz button clicked for account: " + account.getUsername());
        setButtonState(startQuizButton, "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    quizScheduler.monitorQuizRequests(account.getUserId(),  viewsCheckBox.isSelected());
                    parentController.scanAccount(account.getUserId());
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startQuizButton, "red"));
                    System.err.println("Ошибка при запуске квиза: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartMining() {
        if (isButtonGreen(startMiningButton)) return;
        setButtonState(startMiningButton, "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Integer mineHitsLeft = account.getMineHitsLeft();
                    if (mineHitsLeft != null && mineHitsLeft > 0) {
                        mineScheduler.performMining(account.getUserId(), mineHitsLeft, viewsCheckBox.isSelected());
                        parentController.scanAccount(account.getUserId());
                    } else {
                        Platform.runLater(() -> setButtonState(startMiningButton, "green"));
                        System.out.println("Нет доступных кликов для майнинга у аккаунта: " + account.getUsername());
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startMiningButton, "red"));
                    System.err.println("Ошибка при запуске майнинга: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartAdv() {
        if (isButtonGreen(startAdvButton)) return;
        System.out.println("Start Adv button clicked for account: " + account.getUsername());
        setButtonState(startAdvButton, "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    int advDone = account.getAdvDone();
                    int countAdv = 3 - advDone;
                    if (countAdv > 0) {
                        advertisingScheduler.performAdv(account.getUserId(), countAdv, viewsCheckBox.isSelected());
                        parentController.scanAccount(account.getUserId());
                    } else {
                        Platform.runLater(() -> setButtonState(startAdvButton, "green"));
                        System.out.println("No available ads for account: " + account.getUsername());
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startAdvButton, "red"));
                    System.err.println("Error starting ads: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleDeleteAccount() {
        try {
            System.out.println("Удаление аккаунта: " + account.getUsername());
            accountService.deleteAccount(account.getUsername());
            System.out.println("Аккаунт успешно удален");
            System.out.println("Account successfully deleted");
            // Обновляем список аккаунтов
            parentController.loadAccountsFromDatabase();
        } catch (Exception e) {
            System.err.println("Account deletion error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void resetRedButtonsToWhite() {
        if (startChaptersButton.getStyleClass().contains("btn-red")) setButtonState(startChaptersButton, "white");
        if (startCommentsButton.getStyleClass().contains("btn-red")) setButtonState(startCommentsButton, "white");
        if (startQuizButton.getStyleClass().contains("btn-red")) setButtonState(startQuizButton, "white");
        if (startMiningButton.getStyleClass().contains("btn-red")) setButtonState(startMiningButton, "white");
        if (startAdvButton.getStyleClass().contains("btn-red")) setButtonState(startAdvButton, "white");
    }

    public void setViewsCheckBox(CheckBox viewsCheckBox) {
        this.viewsCheckBox = viewsCheckBox;
    }
    public HBox getAccountItem() {
        return accountItem;
    }

    private void showGiftImagesPopup() {
        try {
            if (giftImagesPopup == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/GiftImagesPopup.fxml"));
                VBox popupContent = loader.load();
                GiftImagesPopupController controller = loader.getController();
                 // Pass the account ID to the popup controller
                controller.setAccountId(account.getUserId());
                // Load images for the current date initially
                controller.loadImagesForDate(LocalDate.now(ZoneId.systemDefault()));

                giftImagesPopup = new Popup();
                giftImagesPopup.getContent().add(popupContent);
                // Don't use autoHide, we will manage visibility manually
                // giftImagesPopup.setAutoHide(true);

                // Add mouse event handlers to the popup content
                popupContent.setOnMouseEntered(event -> {
                    if (hidePopupTimeline != null) {
                        hidePopupTimeline.stop();
                        hidePopupTimeline = null;
                    }
                });
                popupContent.setOnMouseExited(event -> hideGiftImagesPopupWithDelay());

                 // Set mouse exited handler for the giftImageView
                giftImageView.setOnMouseExited(event -> hideGiftImagesPopupWithDelay());
            }

            // Only show if not already showing
            if (!giftImagesPopup.isShowing()) {
                giftImagesPopup.show(giftImageView.getScene().getWindow(),
                                  giftImageView.localToScreen(giftImageView.getBoundsInLocal()).getMinX(),
                                  giftImageView.localToScreen(giftImageView.getBoundsInLocal()).getMaxY());
            }

        } catch (Exception e) {
            System.err.println("Ошибка при показе окна подарков: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void hideGiftImagesPopupWithDelay() {
        if (hidePopupTimeline != null) {
            hidePopupTimeline.stop();
        }
        hidePopupTimeline = new Timeline(new KeyFrame(Duration.millis(200), event -> {
            if (giftImagesPopup != null && giftImagesPopup.isShowing()) {
                // Check if the mouse is still over the giftImageView or the popup content
                // This check is simplified and might need refinement based on actual mouse position
                // For now, we rely on mouse exited events on both elements stopping the timeline
                giftImagesPopup.hide();
            }
        }));
        hidePopupTimeline.play();
    }

    private void hideGiftImagesPopup() {
        // This method is now primarily triggered by the timeline
        if (giftImagesPopup != null && giftImagesPopup.isShowing()) {
            giftImagesPopup.hide();
        }
    }

    public void showReloginRequiredState() {
        Platform.runLater(() -> {
            if (overlayPane != null && reloginButton != null && accountItem != null) {
                overlayPane.setVisible(true);
                reloginButton.setVisible(true);
                accountItem.setDisable(true); // Disable interactions with the account item

                // Hide all task buttons
                startChaptersButton.setVisible(false);
                startCommentsButton.setVisible(false);
                startQuizButton.setVisible(false);
                startMiningButton.setVisible(false);
                startAdvButton.setVisible(false);
                if (deleteButton != null) {
                    deleteButton.setVisible(false);
                }
            }
        });
    }

    private void handleRelogin() {
        System.out.println("Relogin button clicked for account: " + account.getUsername());
        // TODO: Implement relogin logic here.
        // After successful re-authentication, call updateUI() and hideReloginRequiredState().

        // Placeholder to revert the state for testing
        hideReloginRequiredState();
    }

    public void hideReloginRequiredState() {
         Platform.runLater(() -> {
            if (overlayPane != null && reloginButton != null && accountItem != null) {
                overlayPane.setVisible(false);
                reloginButton.setVisible(false);
                accountItem.setDisable(false);

                // Show task buttons again and update their states
                startChaptersButton.setVisible(true);
                startCommentsButton.setVisible(true);
                startQuizButton.setVisible(true);
                startMiningButton.setVisible(true);
                startAdvButton.setVisible(true);
                 if (deleteButton != null) {
                    deleteButton.setVisible(true);
                }
                updateButtonStates(); // Update button states based on current account data
            }
        });
    }

    private void handleOpenCards() {
        System.out.println("Open Cards button clicked for account: " + account.getUsername());
        new Thread(() -> {
            try {
                ChromeDriver driver = mbAuth.getActualDriver(account.getUserId(), CARDS_TASK_NAME, true);
                driver.get("https://mangabuff.ru/cards/pack");
            } catch (Exception e) {
                System.err.println("Error opening cards page with Selenium: " + e.getMessage());
                e.printStackTrace();
                // Optionally, show an error message to the user
            }
        }).start();
    }
}

