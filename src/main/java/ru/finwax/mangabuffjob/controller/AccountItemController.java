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
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.model.AccountProgress;
import ru.finwax.mangabuffjob.model.CountScroll;
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
import javafx.scene.control.Tooltip;

import java.awt.Desktop;
import java.net.URI;
import javafx.scene.control.Alert;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.model.TaskType;

import java.util.Map;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

@Component
@Getter
@Slf4j
public class AccountItemController {

    @FXML
    private ImageView avatarImageView;
    @FXML
    private Label avatarAltTextLabel;

    @FXML
    private ImageView readerStatusIcon;
    @FXML
    private Tooltip readerProgressTooltip;

    @FXML
    private ImageView commentStatusIcon;
    @FXML
    private Tooltip commentProgressTooltip;

    @FXML
    private ImageView quizStatusIcon;
    @FXML
    private Tooltip quizProgressTooltip;

    @FXML
    private ImageView mineStatusIcon;
    @FXML
    private Tooltip mineProgressTooltip;

    @FXML
    private ImageView advStatusIcon;
    @FXML
    private Tooltip advProgressTooltip;

    @FXML
    private Label diamondCountLabel;
    @FXML
    private ImageView diamondImageView;
    @FXML
    private ImageView scrollImageView;
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
    private StackPane taskOverlayPane;
    @FXML
    private StackPane taskButtonsStack;
    @FXML
    private HBox taskButtonsHBox;

    @FXML
    private Button reloginButton;

    @FXML
    private Button openCardsButton;

    @FXML
    private CheckBox readerCheckBox;

    @FXML
    private CheckBox commentCheckBox;

    @FXML
    private CheckBox quizCheckBox;

    @FXML
    private CheckBox mineCheckBox;

    @FXML
    private CheckBox advCheckBox;

    private Popup scrollStatsPopup;
    private ImageView scrollIcon1;
    private Label scrollLabel1;
    private ImageView scrollIcon2;
    private Label scrollLabel2;
    private ImageView scrollIcon3;
    private Label scrollLabel3;
    private ImageView scrollIcon4;
    private Label scrollLabel4;
    private ImageView scrollIcon5;
    private Label scrollLabel5;
    private ImageView scrollIcon6;
    private Label scrollLabel6;
    private ImageView scrollIcon7;
    private Label scrollLabel7;
    private ImageView scrollIcon8;
    private Label scrollLabel8;
    private ImageView scrollIcon9;
    private Label scrollLabel9;

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

    private final MangaBuffAuth mangaBuffAuth;

    public AccountItemController(
        AccountService accountService,
        MangaBuffJobViewController parentController,
        AdvertisingScheduler advertisingScheduler,
        MineScheduler mineScheduler,
        QuizScheduler quizScheduler,
        CommentScheduler commentScheduler,
        MangaReadScheduler mangaReadScheduler,
        GiftStatisticRepository giftRepository,
        MbAuth mbAuth,
        MangaBuffAuth mangaBuffAuth
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
        this.mangaBuffAuth = mangaBuffAuth;
    }

    @FXML
    public void initialize() {
        if (taskOverlayPane == null) {
            System.err.println("taskOverlayPane is null in initialize()");
            return;
        }
        if (reloginButton == null) {
            System.err.println("reloginButton is null in initialize()");
            return;
        }
        if (accountItem == null) {
            System.err.println("accountItem is null in initialize()");
            return;
        }
        // Подключаем CSS программно
        if (accountItem.getScene() != null) {
            accountItem.getScene().getStylesheets().add(getClass().getResource("/ru/finwax/mangabuffjob/view/style.css").toExternalForm());
        } else {
            accountItem.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.getStylesheets().add(getClass().getResource("/ru/finwax/mangabuffjob/view/style.css").toExternalForm());
                }
            });
        }
        taskOverlayPane.setVisible(false);
        taskOverlayPane.getStyleClass().setAll("relogin-overlay");
        taskOverlayPane.setMouseTransparent(false);
        taskOverlayPane.setViewOrder(-1.0);
        taskOverlayPane.toFront();
        reloginButton.setVisible(false);
        reloginButton.setText("Войти снова");
        reloginButton.getStyleClass().setAll("relogin-btn");
        reloginButton.setOnAction(event -> handleRelogin());
        reloginButton.setMouseTransparent(false);
        reloginButton.toFront();
        accountItem.setViewOrder(0.0);
        accountItem.setDisable(false);
        taskOverlayPane.toFront();
        reloginButton.toFront();
        accountItem.getParent().layout();
        taskOverlayPane.requestLayout();
        reloginButton.requestLayout();

        // Create the scroll stats popup programmatically
        scrollStatsPopup = new Popup();
        VBox popupContent = new VBox();
        popupContent.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 15; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        popupContent.setSpacing(8);

        String[] iconPaths = {
            "/static/icon/scroll/X.png",
            "/static/icon/scroll/S.png",
            "/static/icon/scroll/A.png",
            "/static/icon/scroll/P.png",
            "/static/icon/scroll/G.png",
            "/static/icon/scroll/B.png",
            "/static/icon/scroll/C.png",
            "/static/icon/scroll/D.png",
            "/static/icon/scroll/E.png"
        };

        ImageView[] scrollIcons = new ImageView[iconPaths.length];
        Label[] scrollLabels = new Label[iconPaths.length];

        for (int i = 0; i < iconPaths.length; i++) {
            HBox row = new HBox();
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setSpacing(5);

            scrollIcons[i] = new ImageView();
            try {
                java.net.URL iconUrl = getClass().getResource(iconPaths[i]);
                if (iconUrl != null) {
                    Image iconImage = new Image(iconUrl.openStream());
                    scrollIcons[i].setImage(iconImage);
                    scrollIcons[i].setFitHeight(31.2);
                    scrollIcons[i].setFitWidth(31.2);
                    scrollIcons[i].setPickOnBounds(true);
                    scrollIcons[i].setPreserveRatio(true);
                } else {
                     System.err.println("Scroll icon resource not found: " + iconPaths[i]);
                     // Handle missing resource, maybe set a default blank image or hide the ImageView
                }
            } catch (Exception e) {
                System.err.println("Error loading scroll icon: " + iconPaths[i] + " - " + e.getMessage());
                // Use a placeholder or handle the error as needed
            }

            scrollLabels[i] = new Label("0 | 0");

            row.getChildren().addAll(scrollIcons[i], scrollLabels[i]);
            popupContent.getChildren().add(row);
        }

        scrollStatsPopup.getContent().add(popupContent);

        // Add mouse hover events for scroll icon
        scrollImageView.setOnMouseEntered(event -> {
            if (scrollStatsPopup != null) {
                // Calculate position relative to the main stage
                javafx.stage.Window ownerWindow = accountItem.getScene().getWindow();
                double x = scrollImageView.localToScreen(scrollImageView.getBoundsInLocal()).getMinX();
                double y = scrollImageView.localToScreen(scrollImageView.getBoundsInLocal()).getMinY();

                // Adjust position slightly to be next to the icon
                scrollStatsPopup.show(ownerWindow, x + scrollImageView.getBoundsInLocal().getWidth(), y);
            }
        });

        scrollImageView.setOnMouseExited(event -> {
            // Keep popup visible if mouse enters the popup itself
            if (scrollStatsPopup != null && scrollStatsPopup.getContent() != null && !scrollStatsPopup.getContent().isEmpty() && scrollStatsPopup.getContent().get(0).isHover()) {
                // Do nothing, mouse is over the popup content
            } else if (scrollStatsPopup != null) {
                scrollStatsPopup.hide();
            }
        });

        // Add mouse exit event for the popup content to hide it
        if (scrollStatsPopup != null && scrollStatsPopup.getContent() != null && !scrollStatsPopup.getContent().isEmpty()) {
             scrollStatsPopup.getContent().get(0).setOnMouseExited(event -> {
                // Hide if mouse exits the popup content and is not over the scroll icon
                 if (!scrollImageView.isHover()) {
                     scrollStatsPopup.hide();
                 }
             });
        }

        // Add mouse event handlers for gift icon
        giftImageView.setOnMouseEntered(event -> {
            giftImageView.setFitHeight(23.04); // 19.2 * 1.2
            giftImageView.setFitWidth(23.04);  // 19.2 * 1.2
            showGiftImagesPopup();
        });

        giftImageView.setOnMouseExited(event -> {
            giftImageView.setFitHeight(19.2);
            giftImageView.setFitWidth(19.2);
            hideGiftImagesPopupWithDelay();
        });
    }

    public void setAccount(AccountProgress account) {
        this.account = account;
        if (account != null) {
            // Устанавливаем состояние чекбоксов
            readerCheckBox.setSelected(account.isReaderEnabled());
            commentCheckBox.setSelected(account.isCommentEnabled());
            quizCheckBox.setSelected(account.isQuizEnabled());
            mineCheckBox.setSelected(account.isMineEnabled());
            advCheckBox.setSelected(account.isAdvEnabled());

            // Добавляем слушатели изменений для сохранения состояния
            readerCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                account.setReaderEnabled(newVal);
                new Thread(() -> accountService.updateAccountProgressEnabledStates(account.getUserId(), newVal, account.isCommentEnabled(), account.isQuizEnabled(), account.isMineEnabled(), account.isAdvEnabled())).start();
            });
            commentCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                account.setCommentEnabled(newVal);
                new Thread(() -> accountService.updateAccountProgressEnabledStates(account.getUserId(), account.isReaderEnabled(), newVal, account.isQuizEnabled(), account.isMineEnabled(), account.isAdvEnabled())).start();
            });
            quizCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                account.setQuizEnabled(newVal);
                new Thread(() -> accountService.updateAccountProgressEnabledStates(account.getUserId(), account.isReaderEnabled(), account.isCommentEnabled(), newVal, account.isMineEnabled(), account.isAdvEnabled())).start();
            });
            mineCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                account.setMineEnabled(newVal);
                new Thread(() -> accountService.updateAccountProgressEnabledStates(account.getUserId(), account.isReaderEnabled(), account.isCommentEnabled(), account.isQuizEnabled(), newVal, account.isAdvEnabled())).start();
            });
            advCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                account.setAdvEnabled(newVal);
                new Thread(() -> accountService.updateAccountProgressEnabledStates(account.getUserId(), account.isReaderEnabled(), account.isCommentEnabled(), account.isQuizEnabled(), account.isMineEnabled(), newVal)).start();
            });

            // Обновление иконок статуса и всплывающих подсказок
            updateStatusIcon(readerStatusIcon, readerProgressTooltip, account.getReaderProgress(), isTaskCompleted(account.getReaderProgress()) ? "green" : "grey");
            updateStatusIcon(commentStatusIcon, commentProgressTooltip, account.getCommentProgress(), isTaskCompleted(account.getCommentProgress()) ? "green" : "grey");
            updateStatusIcon(quizStatusIcon, quizProgressTooltip, String.valueOf(account.getQuizDone()), Boolean.TRUE.equals(account.getQuizDone()) ? "green" : "grey");
            updateStatusIcon(mineStatusIcon, mineProgressTooltip, account.getMineProgress(), isMineCompleted(account.getMineHitsLeft()) ? "green" : "grey");
            updateStatusIcon(advStatusIcon, advProgressTooltip, account.getAdvProgress(), isAdvCompleted(account.getAdvDone()) ? "green" : "grey");

            // Привязка Tooltip к ImageView программно
            Tooltip.install(readerStatusIcon, readerProgressTooltip);
            Tooltip.install(commentStatusIcon, commentProgressTooltip);
            Tooltip.install(quizStatusIcon, quizProgressTooltip);
            Tooltip.install(mineStatusIcon, mineProgressTooltip);
            Tooltip.install(advStatusIcon, advProgressTooltip);

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
            taskOverlayPane.setVisible(false);
            reloginButton.setVisible(false);
            accountItem.setDisable(false);
            // Если требуется перелогин, показать оверлей и кнопку
            if (account.isReloginRequired()) {
                showReloginRequiredState();
            }

            // Update button states after setting the account
            updateButtonStates();

            // Обновляем информацию о свитках
            updateScrollStatsPopup();
        }
    }

    private boolean isTaskCompleted(String progress) {
        if (progress == null) return false;
        String[] parts = progress.split("/");
        if (parts.length != 2) return false;
        try {
            return Integer.parseInt(parts[0]) >= Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isMineCompleted(Integer mineHitsLeft) {
        return mineHitsLeft != null && mineHitsLeft == 0;
    }

    private boolean isAdvCompleted(Integer advDone) {
        return advDone != null && advDone >= 3;
    }

    private void updateStatusIcon(ImageView iconView, Tooltip tooltip, String progressText, String status) {
        Platform.runLater(() -> {
            String iconPath;
            switch (status) {
                case "blue":
                    iconPath = "/static/icon/indicator/blue.png";
                    break;
                case "green":
                    iconPath = "/static/icon/indicator/green.png";
                    break;
                case "red":
                    iconPath = "/static/icon/indicator/red.png";
                    break;
                case "grey":
                default:
                    iconPath = "/static/icon/indicator/grey.png";
                    break;
            }
            Image statusIcon = new Image(getClass().getResourceAsStream(iconPath));
            iconView.setImage(statusIcon);
            tooltip.setText(progressText);
        });
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
        // EventGift update - Disabled on 2024-03-19 due to event end
        /*
        int eventGiftCount = gifts.isEmpty() ? 0 : gifts.get(0).getCountEventGift();
        Image eventGiftImage = new Image(getClass().getResourceAsStream("/static/icon/watermelon.png"));
        */
        Platform.runLater(() -> {
            giftCountLabel.setText(String.valueOf(finalGiftCount));
            // EventGift update - Disabled on 2024-03-19 due to event end
            /*
            eventGiftCountLabel.setText(String.valueOf(eventGiftCount));
            
            // Обновляем иконку event-gift
            if (eventGiftCount == -1) { // Используем -1 для индикации ошибки сканирования
                eventGiftImageView.setImage(eventGiftImage);
            } else {
                eventGiftImageView.setImage(eventGiftImage);
            }
            */
        });
    }

    public void updateButtonStates() {
        if (this.account == null) {
            // Account is not yet set, cannot update button states
            return;
        }

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
        boolean disableTaskButtons = taskOverlayPane != null && taskOverlayPane.isVisible();
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
            reloginButton.setVisible(taskOverlayPane != null && taskOverlayPane.isVisible());
        }

        // Ensure delete and open cards buttons are always enabled unless overlay is visible
        boolean disableAccountButtons = taskOverlayPane != null && taskOverlayPane.isVisible();
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
        log.info("Start Chapters button clicked for account: " + account.getUsername());
        setButtonState(startChaptersButton, "blue"); // Синяя кнопка и блокировка
        updateStatusIcon(readerStatusIcon, readerProgressTooltip, account.getReaderProgress(), "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Integer countOfChapters = Integer.parseInt(Objects.requireNonNull(account.getReaderProgress()).split("/")[1]) -
                        Integer.parseInt(account.getReaderProgress().split("/")[0]);
                    log.info(String.valueOf(countOfChapters));

                    mangaReadScheduler.readMangaChapters(account.getUserId(), countOfChapters, isTaskEnabled(TaskType.READER));
                    Platform.runLater(() -> {
                        parentController.scanAccount(account.getUserId());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startChaptersButton, "red"));
                    updateStatusIcon(readerStatusIcon, readerProgressTooltip, account.getReaderProgress(), "red");
                    log.error("Ошибка при чтении глав: " + e.getMessage());
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartComments() {
        if (isButtonGreen(startCommentsButton)) return;
        log.info("Start Comments button clicked for account: " + account.getUsername());
        setButtonState(startCommentsButton, "blue");
        updateStatusIcon(commentStatusIcon, commentProgressTooltip, account.getCommentProgress(), "blue");

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
                    updateStatusIcon(commentStatusIcon, commentProgressTooltip, account.getCommentProgress(), "red");
                    log.error("Ошибка при отправке комментариев: " + e.getMessage());
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartQuiz() {
        if (isButtonGreen(startQuizButton)) return;
        log.info("Start Quiz button clicked for account: " + account.getUsername());
        setButtonState(startQuizButton, "blue");
        updateStatusIcon(quizStatusIcon, quizProgressTooltip, String.valueOf(account.getQuizDone()), "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    quizScheduler.monitorQuizRequests(account.getUserId(), isTaskEnabled(TaskType.QUIZ));
                    parentController.scanAccount(account.getUserId());
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startQuizButton, "red"));
                    updateStatusIcon(quizStatusIcon, quizProgressTooltip, String.valueOf(account.getQuizDone()), "red");
                    log.error("Ошибка при запуске квиза: " + e.getMessage());
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
        updateStatusIcon(mineStatusIcon, mineProgressTooltip, account.getMineProgress(), "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Integer mineHitsLeft = account.getMineHitsLeft();
                    if (mineHitsLeft != null && mineHitsLeft > 0) {
                        mineScheduler.performMining(account.getUserId(), mineHitsLeft, isTaskEnabled(TaskType.MINE));
                        parentController.scanAccount(account.getUserId());
                    } else {
                        Platform.runLater(() -> setButtonState(startMiningButton, "green"));
                        log.info("Нет доступных кликов для майнинга у аккаунта: " + account.getUsername());
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startMiningButton, "red"));
                    updateStatusIcon(mineStatusIcon, mineProgressTooltip, account.getMineProgress(), "red");
                    log.error("Ошибка при запуске майнинга: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleStartAdv() {
        if (isButtonGreen(startAdvButton)) return;
        log.info("Start Adv button clicked for account: " + account.getUsername());
        setButtonState(startAdvButton, "blue");
        updateStatusIcon(advStatusIcon, advProgressTooltip, account.getAdvProgress(), "blue");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    int advDone = account.getAdvDone();
                    int countAdv = 3 - advDone;
                    if (countAdv > 0) {
                        advertisingScheduler.performAdv(account.getUserId(), countAdv, isTaskEnabled(TaskType.ADV));
                        parentController.scanAccount(account.getUserId());
                    } else {
                        Platform.runLater(() -> setButtonState(startAdvButton, "green"));
                        log.info("No available ads for account: " + account.getUsername());
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(startAdvButton, "red"));
                    updateStatusIcon(advStatusIcon, advProgressTooltip, account.getAdvProgress(), "red");
                    log.info("Error starting ads: " + e.getMessage());
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
            log.info("Удаление аккаунта: " + account.getUsername());
            accountService.deleteAccount(account.getUsername());
            log.info("Аккаунт успешно удален");
            log.info("Account successfully deleted");
            // Обновляем список аккаунтов
            parentController.loadAccountsFromDatabase();
        } catch (Exception e) {
            log.error("Account deletion error: " + e.getMessage());
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
                controller.setAccountId(account.getUserId());
                controller.loadImagesForDate(LocalDate.now(ZoneId.systemDefault()));

                giftImagesPopup = new Popup();
                giftImagesPopup.getContent().add(popupContent);

                popupContent.setOnMouseEntered(event -> {
                    if (hidePopupTimeline != null) {
                        hidePopupTimeline.stop();
                        hidePopupTimeline = null;
                    }
                });
                popupContent.setOnMouseExited(event -> hideGiftImagesPopupWithDelay());
                giftImageView.setOnMouseExited(event -> hideGiftImagesPopupWithDelay());
            }

            if (!giftImagesPopup.isShowing()) {
                // Get screen bounds
                Screen screen = Screen.getPrimary();
                Rectangle2D screenBounds = screen.getVisualBounds();

                // Get popup size
                giftImagesPopup.show(giftImageView.getScene().getWindow(), 0, 0);
                double popupWidth = giftImagesPopup.getWidth();
                double popupHeight = giftImagesPopup.getHeight();
                giftImagesPopup.hide();

                // Calculate position
                Bounds bounds = giftImageView.localToScreen(giftImageView.getBoundsInLocal());
                double x = bounds.getMinX();
                double y = bounds.getMaxY();

                // Adjust position if popup would go off screen
                if (x + popupWidth > screenBounds.getMaxX()) {
                    x = screenBounds.getMaxX() - popupWidth - 10; // Add some padding
                }
                if (y + popupHeight > screenBounds.getMaxY()) {
                    y = bounds.getMinY() - popupHeight - 10; // Add some padding
                }

                // Ensure minimum position
                x = Math.max(10, x);
                y = Math.max(10, y);

                // Show popup at adjusted position
                giftImagesPopup.show(giftImageView.getScene().getWindow(), x, y);
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
        if (taskOverlayPane == null || reloginButton == null) {
            log.error("UI components not initialized");
            return;
        }
        Platform.runLater(() -> {
            log.info("Showing relogin state for account: " + account.getUsername());
            taskOverlayPane.getStyleClass().setAll("relogin-overlay");
            taskOverlayPane.setVisible(true);
            taskOverlayPane.setMouseTransparent(false);
            taskOverlayPane.toFront();
            reloginButton.getStyleClass().setAll("relogin-btn");
            reloginButton.setText("Войти снова, сессия истекла");
            reloginButton.setVisible(true);
            reloginButton.toFront();
            if (taskButtonsHBox != null) {
                taskButtonsHBox.setVisible(false);
            }
            taskOverlayPane.requestLayout();
            reloginButton.requestLayout();
            log.info("Relogin state shown for account: " + account.getUsername());
        });
    }

    public void hideReloginRequiredState() {
        log.info("Attempting to hide relogin state for account: " + (account != null ? account.getUsername() : "null"));
        if (taskOverlayPane == null || reloginButton == null || taskButtonsStack == null || taskButtonsHBox == null) {
            System.err.println("Critical error: UI components are null (task overlay)");
            return;
        }
        Platform.runLater(() -> {
            try {
                // Показываем весь блок task-кнопок
                taskButtonsHBox.setVisible(true);

                if (startChaptersButton != null) startChaptersButton.setVisible(true);
                if (startCommentsButton != null) startCommentsButton.setVisible(true);
                if (startQuizButton != null) startQuizButton.setVisible(true);
                if (startMiningButton != null) startMiningButton.setVisible(true);
                if (startAdvButton != null) startAdvButton.setVisible(true);
                if (deleteButton != null) deleteButton.setVisible(true);
                if (openCardsButton != null) openCardsButton.setVisible(true);

                // Скрываем overlay
                taskOverlayPane.setVisible(false);
                reloginButton.setVisible(false);

                // Обновляем состояния кнопок
                updateButtonStates();

                // Принудительно обновляем layout
                taskButtonsStack.requestLayout();
                taskOverlayPane.requestLayout();
                reloginButton.requestLayout();
                log.info("Successfully hid relogin state for account: " + account.getUsername());
            } catch (Exception e) {
                log.info("Error hiding relogin state: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleRelogin() {
        log.info("Relogin button clicked for account: " + account.getUsername());
        try {
            // Выполняем повторную аутентификацию
            UserCookie userCookie = mangaBuffAuth.authenticate(account.getUsername());
            
            // После успешной аутентификации обновляем состояние UI
            Platform.runLater(() -> {
                hideReloginRequiredState();
                // Обновляем данные аккаунта
                parentController.scanAccount(account.getUserId());
            });
        } catch (Exception e) {
            log.error("Ошибка при повторном входе: " + e.getMessage());
            e.printStackTrace();
            // Показываем сообщение об ошибке
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка входа");
                alert.setHeaderText("Не удалось войти в аккаунт");
                alert.setContentText("Пожалуйста, проверьте правильность логина и попробуйте снова.\n\n" + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void handleOpenCards() {
        log.info("Open Cards button clicked for account: " + account.getUsername());
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

    public void updateTaskStatusIcon(TaskType taskType, String status) {
        String progressText = ""; // Будем получать текст прогресса из AccountProgress
        ImageView iconView = null;
        Tooltip tooltip = null;

        switch (taskType) {
            case READER:
                iconView = readerStatusIcon;
                tooltip = readerProgressTooltip;
                progressText = account.getReaderProgress();
                break;
            case COMMENT:
                iconView = commentStatusIcon;
                tooltip = commentProgressTooltip;
                progressText = account.getCommentProgress();
                break;
            case QUIZ:
                iconView = quizStatusIcon;
                tooltip = quizProgressTooltip;
                progressText = String.valueOf(account.getQuizDone());
                break;
            case MINE:
                iconView = mineStatusIcon;
                tooltip = mineProgressTooltip;
                progressText = account.getMineProgress();
                break;
            case ADV:
                iconView = advStatusIcon;
                tooltip = advProgressTooltip;
                progressText = account.getAdvProgress();
                break;
        }

        if (iconView != null && tooltip != null) {
            updateStatusIcon(iconView, tooltip, progressText, status);
        }
    }

    public boolean isTaskEnabled(TaskType taskType) {
        switch (taskType) {
            case READER:
                return readerCheckBox.isSelected();
            case COMMENT:
                return commentCheckBox.isSelected();
            case QUIZ:
                return quizCheckBox.isSelected();
            case MINE:
                return mineCheckBox.isSelected();
            case ADV:
                return advCheckBox.isSelected();
            default:
                return false;
        }
    }

    public void setTaskEnabled(TaskType taskType, boolean enabled) {
        switch (taskType) {
            case READER:
                readerCheckBox.setSelected(enabled);
                break;
            case COMMENT:
                commentCheckBox.setSelected(enabled);
                break;
            case QUIZ:
                quizCheckBox.setSelected(enabled);
                break;
            case MINE:
                mineCheckBox.setSelected(enabled);
                break;
            case ADV:
                advCheckBox.setSelected(enabled);
                break;
        }
    }

    private void updateScrollStatsPopup() {
        if (account == null || account.getScrollCounts() == null) return;

        Map<String, CountScroll> scrollCounts = account.getScrollCounts();
        String[] ranks = {"X", "S", "A", "P", "G", "B", "C", "D", "E"};

        // Получаем существующий VBox из POPUP
        VBox popupContent = (VBox) scrollStatsPopup.getContent().get(0);
        if (popupContent == null) return;

        // Обновляем метки для каждого ранга
        for (int i = 0; i < ranks.length; i++) {
            CountScroll count = scrollCounts.get(ranks[i]);
            if (count != null) {
                HBox row = (HBox) popupContent.getChildren().get(i);
                Label label = (Label) row.getChildren().get(1);
                label.setText(count.getCount() + " | " + count.getBlessedCount());
            }
        }
    }
}

