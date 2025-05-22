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
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
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
    private Label giftCountLabel;
    @FXML
    private ImageView giftImageView;

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

    private final AccountService accountService;
    private final MangaBuffJobViewController parentController;
    private final AdvertisingScheduler advertisingScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final CommentScheduler commentScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final GiftStatisticRepository giftRepository;
    private CheckBox viewsCheckBox;

    private AccountProgress account;

    private Timeline loadingTimelineChapters;
    private Timeline loadingTimelineComments;
    private Timeline loadingTimelineQuiz;
    private Timeline loadingTimelineMining;
    private Timeline loadingTimelineAdv;

    private Popup giftImagesPopup;

    public AccountItemController(
        AccountService accountService,
        MangaBuffJobViewController parentController,
        AdvertisingScheduler advertisingScheduler,
        MineScheduler mineScheduler,
        QuizScheduler quizScheduler,
        CommentScheduler commentScheduler,
        MangaReadScheduler mangaReadScheduler,
        GiftStatisticRepository giftRepository
    ) {
        this.accountService = accountService;
        this.parentController = parentController;
        this.advertisingScheduler = advertisingScheduler;
        this.mineScheduler = mineScheduler;
        this.quizScheduler = quizScheduler;
        this.commentScheduler = commentScheduler;
        this.mangaReadScheduler = mangaReadScheduler;
        this.giftRepository = giftRepository;
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

        avatarAltTextLabel.setText(account.getAvatarAltText());

        // Добавляем обработчики событий мыши для показа подарков
        giftImageView.setOnMouseEntered(event -> showGiftImagesPopup());
        giftImageView.setOnMouseExited(event -> hideGiftImagesPopup());

        startChaptersButton.setOnAction(event -> handleStartChapters());
        startCommentsButton.setOnAction(event -> handleStartComments());
        startQuizButton.setOnAction(event -> handleStartQuiz());
        startMiningButton.setOnAction(event -> handleStartMining());
        startAdvButton.setOnAction(event -> handleStartAdv());
        deleteButton.setOnAction(event -> handleDeleteAccount());
    }

    @Transactional
    public void updateGiftCount() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<GiftStatistic> gifts = giftRepository.findByUserIdAndDate(account.getUserId(), today);
        int giftCount = gifts.size();
        Platform.runLater(() -> giftCountLabel.setText(String.valueOf(giftCount)));
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

    private void stopLoadingAnimation(Button button) {
        if (button == startChaptersButton && loadingTimelineChapters != null) { loadingTimelineChapters.stop(); loadingTimelineChapters = null; }
        if (button == startCommentsButton && loadingTimelineComments != null) { loadingTimelineComments.stop(); loadingTimelineComments = null; }
        if (button == startQuizButton && loadingTimelineQuiz != null) { loadingTimelineQuiz.stop(); loadingTimelineQuiz = null; }
        if (button == startMiningButton && loadingTimelineMining != null) { loadingTimelineMining.stop(); loadingTimelineMining = null; }
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
                        boolean allRead = account.getReaderProgress() != null && account.getReaderProgress().split("/")[0].equals(account.getReaderProgress().split("/")[1]);
                        if (allRead) {
                            setButtonState(startChaptersButton, "green");
                        } else {
                            setButtonState(startChaptersButton, "white");
                        }
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
                        boolean allComments = account.getCommentProgress() != null &&
                            account.getCommentProgress().split("/")[0].equals(account.getCommentProgress().split("/")[1]);
                        if (allComments) {
                            setButtonState(startCommentsButton, "green");
                        } else {
                            setButtonState(startCommentsButton, "white");
                        }
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
                    Platform.runLater(() -> {
                        if (Boolean.TRUE.equals(account.getQuizDone())) {
                            setButtonState(startQuizButton, "green");
                        } else {
                            setButtonState(startQuizButton, "white");
                        }
                    });
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
                        Platform.runLater(() -> {
                            if (mineHitsLeft == 0) {
                                setButtonState(startMiningButton, "green");
                            } else {
                                setButtonState(startMiningButton, "white");
                            }
                        });
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
                        Platform.runLater(() -> {
                            if (advDone >= 3) {
                                setButtonState(startAdvButton, "green");
                            } else {
                                setButtonState(startAdvButton, "white");
                            }
                        });
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
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            List<GiftStatistic> gifts = giftRepository.findByUserIdAndDate(account.getUserId(), today);
            if (gifts.isEmpty()) {
                return; // Нет подарков для отображения
            }

            List<String> imagePaths = gifts.stream()
                                          .map(GiftStatistic::getPathImage)
                                          .filter(path -> path != null && !path.isEmpty())
                                          .collect(Collectors.toList());

            if (imagePaths.isEmpty()) {
                return; // Нет изображений для отображения
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/GiftImagesPopup.fxml"));
            VBox popupContent = loader.load();
            GiftImagesPopupController controller = loader.getController();
            controller.setGiftImages(imagePaths);

            giftImagesPopup = new Popup();
            giftImagesPopup.getContent().add(popupContent);
            giftImagesPopup.setAutoHide(true);
            giftImagesPopup.show(giftImageView.getScene().getWindow(),
                                  giftImageView.localToScreen(giftImageView.getBoundsInLocal()).getMinX(),
                                  giftImageView.localToScreen(giftImageView.getBoundsInLocal()).getMaxY());
        } catch (Exception e) {
            System.err.println("Ошибка при показе окна подарков: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void hideGiftImagesPopup() {
        if (giftImagesPopup != null && giftImagesPopup.isShowing()) {
            giftImagesPopup.hide();
        }
    }
}

