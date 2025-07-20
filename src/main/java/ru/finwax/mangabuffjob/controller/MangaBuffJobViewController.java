package ru.finwax.mangabuffjob.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Popup;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Entity.MangaProgress;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.model.AccountProgress;
import ru.finwax.mangabuffjob.model.CountScroll;
import ru.finwax.mangabuffjob.repository.MangaProgressRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import ru.finwax.mangabuffjob.service.ChatService;
import ru.finwax.mangabuffjob.service.MangaParserService;
import ru.finwax.mangabuffjob.service.ScanningProgress;
import ru.finwax.mangabuffjob.model.TaskType;
import ru.finwax.mangabuffjob.service.TaskExecutor;
import ru.finwax.mangabuffjob.model.MangaTask;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.service.AccountService;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.finwax.mangabuffjob.auth.MbAuth;
import org.openqa.selenium.support.ui.WebDriverWait;

@Component
@Slf4j
public class MangaBuffJobViewController implements Initializable {

    @FXML
    public CheckBox viewsCheckBox;
    @FXML
    public Button updateMangaListButton;
    @FXML
    public Button periodicReadingButton;
    @FXML
    public Button stopPeriodicReadingButton;
    @FXML
    private VBox accountsVBox;
    @FXML
    private Button refreshButton;
    @FXML
    private Button addAccountButton;
    @FXML
    private Button runBotButton;
    @FXML
    private Button stopBotButton;
    @FXML
    private ImageView supportImageView;
    @FXML
    private javafx.scene.control.TextField promoCodeInput;
    @FXML
    private Button applyPromoCodeButton;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private StackPane customSwitch;
    @FXML
    private Label accountCountLabel;

    private boolean isCustomSwitchOn = false;
    private Circle switchCircle;
    private Label switchLabel;

    private final UserCookieRepository userCookieRepository;
    private final MangaProgressRepository mangaProgressRepository;
    private final ApplicationContext applicationContext;
    private final AccountItemControllerFactory accountItemControllerFactory;
    private final ChatService chatService;
    private final ScanningProgress scanningProgress;
    private final MangaParserService mangaParserService;
    private final TaskExecutor taskExecutor;
    private Timeline loadingTimelineRefresh;
    private final MangaReadScheduler mangaReadScheduler;
    private final AccountService accountService;
    private final ru.finwax.mangabuffjob.service.PromoCodeService promoCodeService;
    private ScheduledExecutorService chatDiamondExecutor;
    private ScheduledFuture<?> chatDiamondFuture;
    private volatile boolean isChatDiamondActive = false;

    private final ObservableList<AccountProgress> accountData = FXCollections.observableArrayList();
    private final Set<Long> reloginRequiredAccounts = new HashSet<>();

    private Popup supportPopup;
    private Timeline hidePopupTimeline;


    public MangaBuffJobViewController(ChatService chatService,
            UserCookieRepository userCookieRepository,
            MangaProgressRepository mangaProgressRepository,
            ApplicationContext applicationContext,
            AccountItemControllerFactory accountItemControllerFactory,
            ScanningProgress scanningProgress,
            MangaParserService mangaParserService,
            TaskExecutor taskExecutor,
            MangaReadScheduler mangaReadScheduler,
            AccountService accountService,
            ru.finwax.mangabuffjob.service.PromoCodeService promoCodeService) {
        this.chatService = chatService;
        this.userCookieRepository = userCookieRepository;
        this.mangaProgressRepository = mangaProgressRepository;
        this.applicationContext = applicationContext;
        this.accountItemControllerFactory = accountItemControllerFactory;
        this.scanningProgress = scanningProgress;
        this.mangaParserService = mangaParserService;
        this.taskExecutor = taskExecutor;
        this.mangaReadScheduler = mangaReadScheduler;
        this.accountService = accountService;
        this.promoCodeService = promoCodeService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            log.info("Controller initialization...");
            
            // Проверяем инициализацию всех компонентов
            if (accountsVBox == null) {
                throw new IllegalStateException("accountsVBox not initialized");
            }
            if (addAccountButton == null) {
                throw new IllegalStateException("addAccountButton not initialized");
            }
            if (runBotButton == null) {
                throw new IllegalStateException("runBotButton not initialized");
            }
            if (stopBotButton == null) {
                throw new IllegalStateException("stopBotButton not initialized");
            }
            if (refreshButton == null) {
                throw new IllegalStateException("refreshButton not initialized");
            }
            if (updateMangaListButton == null) {
                throw new IllegalStateException("updateMangaListButton not initialized");
            }
            if (periodicReadingButton == null) {
                throw new IllegalStateException("periodicReadingButton not initialized");
            }
            if (stopPeriodicReadingButton == null) {
                throw new IllegalStateException("stopPeriodicReadingButton not initialized");
            }
            if (viewsCheckBox == null) {
                throw new IllegalStateException("viewsCheckBox not initialized");
            }
            if (promoCodeInput == null) {
                throw new IllegalStateException("promoCodeInput not initialized");
            }
            if (applyPromoCodeButton == null) {
                throw new IllegalStateException("applyPromoCodeButton not initialized");
            }
            if (rootPane == null) {
                throw new IllegalStateException("rootPane not initialized");
            }
            if (supportImageView == null) {
                throw new IllegalStateException("supportImageView not initialized");
            }
            if (customSwitch == null) {
                throw new IllegalStateException("customSwitch not initialized");
            }
            if (accountCountLabel == null) {
                throw new IllegalStateException("accountCountLabel not initialized");
            }

            log.info("All components initialized successfully");
            
            // Initialize support popup
            supportPopup = new Popup();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/SupportPopup.fxml"));
                VBox popupContent = loader.load();
                supportPopup.getContent().add(popupContent);
                
                // Add mouse event handlers to the popup content
                popupContent.setOnMouseEntered(event -> {
                    if (hidePopupTimeline != null) {
                        hidePopupTimeline.stop();
                        hidePopupTimeline = null;
                    }
                });
                popupContent.setOnMouseExited(event -> hideSupportPopupWithDelay());

                // Set mouse event handlers for the supportImageView
                supportImageView.setOnMouseEntered(event -> showSupportPopup());
                supportImageView.setOnMouseExited(event -> hideSupportPopupWithDelay());
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // Загрузка данных из базы данных
            loadAccountsFromDatabase();
            handleRefreshAccounts();
            killChromeDrivers();

            // Устанавливаем ссылку на этот контроллер в MangaReadScheduler
            mangaReadScheduler.setViewController(this);

            // Устанавливаем ссылку на этот контроллер в ScanningProgress
            scanningProgress.setMangaBuffJobViewController(this);

            // Настройка обработчиков событий для кнопок
            addAccountButton.setOnAction(event -> handleAddAccount());
            runBotButton.setOnAction(event -> handleRunBot());
            stopBotButton.setOnAction(event -> handleStopBot());
            refreshButton.setOnAction(event -> handleRefreshAccounts());
            updateMangaListButton.setOnAction(event -> handleUpdateMLB());
            periodicReadingButton.setOnAction(event -> handlePeriodicReading());
            stopPeriodicReadingButton.setOnAction(event -> handleStopPeriodicReading());
            applyPromoCodeButton.setOnAction(event -> handleApplyPromoCode());
            
            // Проверяем наличие данных в таблице манги
            if (mangaParserService.hasMangaData()) {
                setButtonState(updateMangaListButton, "green");
            }

            // --- Кастомный тумблер ---
            if (customSwitch != null) {
                switchCircle = new Circle(10, Color.WHITE);
                switchCircle.setStroke(Color.BLACK);
                switchLabel = new Label();
                switchLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #000000; -fx-font-family: 'Oswald', Arial, sans-serif;");
                updateCustomSwitchUI();
                customSwitch.setOnMouseClicked(event -> {
                    isCustomSwitchOn = !isCustomSwitchOn;
                    updateCustomSwitchUI();
                });
            }

            log.info("Controller initialization finished successfully");
        } catch (Exception e) {
            log.error("Controller initialization error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize MangaBuffJobViewController", e);
        }
    }

    private void handlePeriodicReading() {
        if (!mangaReadScheduler.isPeriodicReadingActive()) {
            // Скрываем кнопку запуска и показываем кнопку остановки
            periodicReadingButton.setVisible(false);
            periodicReadingButton.setManaged(false);
            stopPeriodicReadingButton.setVisible(true);
            stopPeriodicReadingButton.setManaged(true);

            mangaReadScheduler.startPeriodicReading(viewsCheckBox.isSelected());
            setButtonState(periodicReadingButton, "blue"); // Это будет применено к скрытой кнопке, но пока оставим для консистентности
            log.info("Запуск периодического чтения");
        } else {
            // Эта ветка теперь не должна достигаться через UI, так как будет отдельная кнопка остановки
            // Тем не менее, оставляем логику для безопасности или для внутреннего использования
            log.info("Периодическое чтение уже активно, игнорируем повторный запуск.");
        }
    }

    private void handleStopPeriodicReading() {
        log.info("Остановка периодического чтения...");

        // Визуально отключаем кнопку остановки и показываем "Загрузка"
        stopPeriodicReadingButton.setDisable(true);
        stopPeriodicReadingButton.getStyleClass().add("btn-loading");
        stopPeriodicReadingButton.setText("Остановка...");

        Task<Void> stopTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                mangaReadScheduler.stopPeriodicReading();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Возвращаем кнопки в исходное состояние
                    periodicReadingButton.setVisible(true);
                    periodicReadingButton.setManaged(true);
                    stopPeriodicReadingButton.setVisible(false);
                    stopPeriodicReadingButton.setManaged(false);
                    setButtonState(periodicReadingButton, "white"); // Возвращаем кнопку запуска в белое состояние
                    showNotification("Периодическое чтение остановлено.", "info");

                    // Сбрасываем состояние кнопки остановки
                    stopPeriodicReadingButton.getStyleClass().remove("btn-loading");
                    stopPeriodicReadingButton.setText("Остановить периодическое чтение");
                    stopPeriodicReadingButton.setDisable(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    log.error("Ошибка при остановке периодического чтения: " + getException().getMessage());
                    showNotification("Ошибка при остановке периодического чтения.", "error");

                    // Возвращаем кнопки в исходное состояние даже при ошибке
                    periodicReadingButton.setVisible(true);
                    periodicReadingButton.setManaged(true);
                    stopPeriodicReadingButton.setVisible(false);
                    stopPeriodicReadingButton.setManaged(false);
                    setButtonState(periodicReadingButton, "red"); // Возможно, показать красную кнопку, если произошла ошибка
                    
                    // Сбрасываем состояние кнопки остановки
                    stopPeriodicReadingButton.getStyleClass().remove("btn-loading");
                    stopPeriodicReadingButton.setText("Остановить периодическое чтение");
                    stopPeriodicReadingButton.setDisable(false);
                });
            }
        };

        new Thread(stopTask).start();
    }

    private void handleUpdateMLB() {
        setButtonState(updateMangaListButton, "blue");

        Task<Void> updateTask = new Task<>() {
            @Override
            protected Void call(){
                try {
                    InputStream is = getClass().getResourceAsStream("/static/manga_parsing_data.csv");
                    if (is != null) {
                        mangaParserService.importMangaFromCSV();
                        Platform.runLater(() -> setButtonState(updateMangaListButton, "green"));
                    } else {
                        mangaParserService.createMangaList();
                    }
                    Platform.runLater(() -> setButtonState(updateMangaListButton, "green"));
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(updateMangaListButton, "red"));
                    log.error("Error updating manga list: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };

        new Thread(updateTask).start();
    }

    private void startLoadingAnimationRefresh(Button button) {
        if (loadingTimelineRefresh != null) {
            loadingTimelineRefresh.stop();
        }
        loadingTimelineRefresh = new Timeline(
            new KeyFrame(Duration.ZERO, e -> button.setText("↻")),
            new KeyFrame(Duration.seconds(0.5), e -> button.setText("..")),
            new KeyFrame(Duration.seconds(1), e -> button.setText("↻"))
        );
        loadingTimelineRefresh.setCycleCount(Timeline.INDEFINITE);
        loadingTimelineRefresh.play();
        button.getStyleClass().add("btn-blue");
        button.setDisable(true);
    }

    private void stopLoadingAnimationRefresh(Button button) {
        if (loadingTimelineRefresh != null) {
            loadingTimelineRefresh.stop();
            loadingTimelineRefresh = null;
        }
        button.setText("↻");
        button.getStyleClass().remove("btn-blue");
        button.setDisable(false);
    }


    private void setButtonState(Button button, String state) {
        Platform.runLater(() -> {
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
                    if (button == periodicReadingButton) {
                        button.setText("Запускать периодическое чтение");
                    } else {
                        button.setText("начать");
                    }
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
        });
    }

    private void startLoadingAnimation(Button button) {
        if (loadingTimelineRefresh != null) {
            loadingTimelineRefresh.stop();
        }
        loadingTimelineRefresh = new Timeline(
            new KeyFrame(Duration.ZERO, e -> button.setText("Загрузка")),
            new KeyFrame(Duration.seconds(0.5), e -> button.setText("Загрузка.")),
            new KeyFrame(Duration.seconds(1), e -> button.setText("Загрузка..")),
            new KeyFrame(Duration.seconds(1.5), e -> button.setText("Загрузка..."))
        );
        loadingTimelineRefresh.setCycleCount(Timeline.INDEFINITE);
        loadingTimelineRefresh.play();
    }

    private void stopLoadingAnimation(Button button) {
        if (loadingTimelineRefresh != null) {
            loadingTimelineRefresh.stop();
            loadingTimelineRefresh = null;
        }
        button.setText("Обновить список манги");
    }

    public void loadAccountsFromDatabase() {
        try {
            accountData.clear();
            var accounts = userCookieRepository.findAll();

            accounts.forEach(userCookie -> {
                MangaProgress progress = mangaProgressRepository.findByUserId(userCookie.getId())
                    .orElseGet(() -> {
                        MangaProgress newProgress = MangaProgress.builder()
                            .userId(userCookie.getId())
                            .readerDone(0)
                            .commentDone(0)
                            .quizDone(false)
                            .mineHitsLeft(100)
                            .advDone(0)
                            .lastUpdated(LocalDate.now())
                            .build();
                        return mangaProgressRepository.save(newProgress);
                    });
                int mineHitsLeftForDisplay = progress.getMineHitsLeft() != null ? progress.getMineHitsLeft() : 100;

                // Получаем текущие данные о свитках из существующего аккаунта, если он есть
                Map<String, CountScroll> existingScrollCounts = null;
                for (AccountProgress existingAccount : accountData) {
                    if (existingAccount.getUserId().equals(userCookie.getId())) {
                        existingScrollCounts = existingAccount.getScrollCounts();
                        break;
                    }
                }

                // Если нет существующих данных, парсим новые
                if (existingScrollCounts == null) {
                    existingScrollCounts = scanningProgress.parseScrollCount(userCookie.getId());
                }

                accountData.add(new AccountProgress(
                    userCookie.getUsername(),
                    progress.getReaderDone() + "/" + progress.getTotalReaderChapters(),
                    progress.getCommentDone() + "/" + progress.getTotalCommentChapters(),
                    progress.getQuizDone(),
                    (100 - mineHitsLeftForDisplay) + "/100",
                    progress.getAdvDone() + "/3",
                    progress.getAdvDone(),
                    progress.getAvatarPath(),
                    progress.getAvatarAltText(),
                    userCookie.getId(),
                    progress.getTotalReaderChapters(),
                    progress.getTotalCommentChapters(),
                    mineHitsLeftForDisplay,
                    progress.getDiamond(),
                    reloginRequiredAccounts.contains(userCookie.getId()),
                    progress.isReaderEnabled(),
                    progress.isCommentEnabled(),
                    progress.isQuizEnabled(),
                    progress.isMineEnabled(),
                    progress.isAdvEnabled(),
                    existingScrollCounts,
                    progress.getMineCountCoin(),
                    progress.getMineLvl(),
                    progress.isAutoUpgradeEnabled(),
                    progress.isAutoExchangeEnabled()
                ));
            });
            displayAccounts();
        } catch (Exception e) {
            log.error("Error loading accounts from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayAccounts() {
        try {
            Platform.runLater(() -> {
                try {
                    accountsVBox.getChildren().clear();

                    // Add headers if there are accounts
                    if (!accountData.isEmpty()) {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/AccountHeaders.fxml"));
                            loader.setControllerFactory(applicationContext::getBean);
                            AnchorPane headers = loader.load();
                            AccountHeadersController headersController = loader.getController();
                            headersController.setParentController(this);
                            accountsVBox.getChildren().add(headers);
                        } catch (IOException e) {
                            System.err.println("Error loading headers FXML: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    for (AccountProgress account : accountData) {
                        try {
                            AccountItemController controller = accountItemControllerFactory.createController(this);

                            controller.setAccount(account);
                            controller.setViewsCheckBox(viewsCheckBox);

                            HBox accountItem = controller.getAccountItem();
                            accountItem.setUserData(controller);
                            accountsVBox.getChildren().add(accountItem);
                        } catch (IOException e) {
                            System.err.println("Error loading FXML for account " + account.getUsername() + ": " + e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            System.err.println("Unexpected error processing account " + account.getUsername() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    // Обновляем счётчик аккаунтов (без header'а)
                    if (accountCountLabel != null) {
                        int count = accountsVBox.getChildren().size();
                        if (count > 0 && accountsVBox.getChildren().get(0) instanceof AnchorPane) {
                            count = count - 1;
                        }
                        accountCountLabel.setText(String.valueOf(count));
                    }
                } catch (Exception e) {
                    log.error("Critical error displaying accounts: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            log.error("Critical error in displayAccounts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAddAccount() {
        log.info("handleAddAccount called");
        try {
            // Загрузка FXML для диалогового окна
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/LoginDialog.fxml"));
            log.info("Loading FXML for LoginDialog.fxml");
            
            // Устанавливаем фабрику контроллеров для LoginDialog.fxml
            loader.setControllerFactory(applicationContext::getBean);
            
            Scene scene = new Scene(loader.load());

            // Получение контроллера и установка сцены
            LoginDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Вход в аккаунт");
            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);

            log.info("Showing login dialog...");
            // Показ диалога и ожидание закрытия
            dialogStage.showAndWait();

            // Если пользователь нажал "Войти", обновляем список аккаунтов
            if (controller.isLoginClicked()) {
                log.info("User logged in, updating account list");
                loadAccountsFromDatabase();
            } else {
                log.info("User cancelled login");
            }
        } catch (IOException e) {
            log.error("Error loading LoginDialog.fxml");
            e.printStackTrace();
        }
    }

    private void setUIEnabled(boolean enabled) {
        runBotButton.setDisable(!enabled);
        addAccountButton.setDisable(!enabled);
        refreshButton.setDisable(!enabled);
        updateMangaListButton.setDisable(!enabled);
    }

    private List<MangaTask> getPendingTasks(AccountProgress account) {
        List<MangaTask> tasks = new ArrayList<>();

        // ADV tasks
        if (account.isAdvEnabled() && account.getAdvDone() < 3) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.ADV, 3 - account.getAdvDone(), false, false));
        }

        // MINE tasks
        if (account.isMineEnabled() && account.getMineHitsLeft() > 0) {
            tasks.add(new MangaTask(
                account.getUserId(),
                TaskType.MINE,
                account.getMineHitsLeft(),
                account.isAutoUpgradeEnabled(),
                account.isAutoExchangeEnabled()
            ));
        }

        // QUIZ tasks
        if (account.isQuizEnabled() && account.getQuizDone() != null && !account.getQuizDone()) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.QUIZ, 1, false, false));
        }

        // COMMENT tasks
        String[] commentProgress = account.getCommentProgress().split("/");
        int commentDone = Integer.parseInt(commentProgress[0]);
        int totalComments = Integer.parseInt(commentProgress[1]);
        if (account.isCommentEnabled() && commentDone < totalComments) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.COMMENT, totalComments - commentDone, false, false));
        }

        // READER tasks
        String[] readerProgress = account.getReaderProgress().split("/");
        int readerDone = Integer.parseInt(readerProgress[0]);
        int totalReader = Integer.parseInt(readerProgress[1]);
        if (account.isReaderEnabled() && readerDone < totalReader) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.READER, totalReader - readerDone, false, false));
        }

        return tasks;
    }

    private void updateTaskStatus(MangaTask task) {
        Platform.runLater(() -> {
            // Находим соответствующий контроллер аккаунта
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller.getAccount().getUserId().equals(task.getUserId())) {
                        // Обновляем статус кнопки
                        Button button = null;
                        switch (task.getType()) {
                            case ADV:
                                button = controller.getStartAdvButton();
                                break;
                            case MINE:
                                button = controller.getStartMiningButton();
                                break;
                            case QUIZ:
                                button = controller.getStartQuizButton();
                                break;
                            case COMMENT:
                                button = controller.getStartCommentsButton();
                                break;
                            case READER:
                                button = controller.getStartChaptersButton();
                                break;
                        }
                        if (button != null) {
                            switch (task.getStatus()) {
                                case RUNNING:
                                    controller.setButtonState(button, "blue");
                                    break;
                                case COMPLETED:
                                    controller.setButtonState(button, "green");
                                    break;
                                case ERROR:
                                    controller.setButtonState(button, "red");
                                    break;
                            }
                        }
                        
                        // Обновляем статус иконки
                        String iconStatus = "grey"; // Default
                        switch (task.getStatus()) {
                            case RUNNING:
                                iconStatus = "blue";
                                break;
                            case COMPLETED:
                                iconStatus = "green"; // Устанавливаем статус иконки в green
                                // Сканируем аккаунт для обновления прогресса и тултипа
                                Platform.runLater(() -> scanAccount(task.getUserId()));
                                break;
                            case ERROR:
                                iconStatus = "red";
                                break;
                        }
                        controller.updateTaskStatusIcon(task.getType(), iconStatus);

                        break; // Found the controller, no need to continue the loop
                    }
                }
            }
        });
    }

    private void handleRunBot() {
        // Блокируем UI
        setUIEnabled(false);
        
        // Показываем кнопку остановки и скрываем кнопку запуска
        runBotButton.setVisible(false);
        runBotButton.setManaged(false);
        stopBotButton.setVisible(true);
        stopBotButton.setManaged(true);
        
        // Устанавливаем состояние checkViews
        taskExecutor.getTaskExecutionService().setCheckViews(viewsCheckBox.isSelected());
        
        // Собираем все задачи
        List<MangaTask> allTasks = new ArrayList<>();
        for (AccountProgress account : accountData) {
            List<MangaTask> pendingTasks = getPendingTasks(account);
            allTasks.addAll(pendingTasks);
        }

        // Если нет задач для выполнения, сразу разблокируем UI и возвращаем кнопки
        if (allTasks.isEmpty()) {
            Platform.runLater(() -> {
                setUIEnabled(true);
                runBotButton.setVisible(true);
                runBotButton.setManaged(true);
                stopBotButton.setVisible(false);
                stopBotButton.setManaged(false);
                showNotification("Все выбранные задачи выполнены!", "info");
            });
            return; // Выходим из метода, так как нет задач для запуска
        }

        // Запускаем выполнение задач
        taskExecutor.executeTasks(allTasks, task -> {
            updateTaskStatus(task);
            // Если это последняя задача, разблокируем UI
            if (taskExecutor.getRunningTasks().isEmpty()) {
                Platform.runLater(() -> {
                    setUIEnabled(true);
                    // Возвращаем кнопки в исходное состояние
                    runBotButton.setVisible(true);
                    runBotButton.setManaged(true);
                    stopBotButton.setVisible(false);
                    stopBotButton.setManaged(false);
                });
            }
        });
    }

    private void handleStopBot() {
        try {
            log.info("Остановка бота...");
            
            // Останавливаем выполнение задач
            taskExecutor.stopAllTasks();
            
            // Закрываем все драйверы
            killChromeDrivers();
            
            // Сбрасываем состояние кнопок
            Platform.runLater(() -> {
                runBotButton.setVisible(true);
                runBotButton.setManaged(true);
                stopBotButton.setVisible(false);
                stopBotButton.setManaged(false);
                
                // Разблокируем UI
                setUIEnabled(true);
                
                // Сбрасываем состояние всех кнопок задач на белый
                accountsVBox.getChildren().forEach(node -> {
                    if (node instanceof HBox accountItem) {
                        Object controller = accountItem.getUserData();
                        if (controller instanceof AccountItemController accountController) {
                            accountController.resetRedButtonsToWhite();
                            accountController.updateButtonStates();
                        }
                    }
                });
            });
            
            log.info("Бот успешно остановлен");
        } catch (Exception e) {
            log.error("Ошибка при остановке бота: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void scanAccount(Long userId) {
        try {
            scanningProgress.sendGetRequestWithCookies(userId);
            reloginRequiredAccounts.remove(userId);
            Platform.runLater(() -> {
                for (javafx.scene.Node node : accountsVBox.getChildren()) {
                    if (node instanceof HBox accountItem) {
                        AccountItemController controller = (AccountItemController) accountItem.getUserData();
                        if (controller.getAccount().getUserId().equals(userId)) {
                            mangaProgressRepository.findByUserId(userId).ifPresent(progress -> {
                                UserCookie userCookie = userCookieRepository.findById(userId).orElse(null);
                                String username = userCookie != null ? userCookie.getUsername() : "";
                                AccountProgress updatedAccount = new AccountProgress(
                                    username,
                                    progress.getReaderDone() + "/" + progress.getTotalReaderChapters(),
                                    progress.getCommentDone() + "/" + progress.getTotalCommentChapters(),
                                    progress.getQuizDone(),
                                    (100 - progress.getMineHitsLeft()) + "/100",
                                    progress.getAdvDone() + "/3",
                                    progress.getAdvDone(),
                                    progress.getAvatarPath(),
                                    progress.getAvatarAltText(),
                                    progress.getUserId(),
                                    progress.getTotalReaderChapters(),
                                    progress.getTotalCommentChapters(),
                                    progress.getMineHitsLeft(),
                                    progress.getDiamond(),
                                    false,
                                    progress.isReaderEnabled(),
                                    progress.isCommentEnabled(),
                                    progress.isQuizEnabled(),
                                    progress.isMineEnabled(),
                                    progress.isAdvEnabled(),
                                    scanningProgress.parseScrollCount(progress.getUserId()),
                                    progress.getMineCountCoin(),
                                    progress.getMineLvl(),
                                    progress.isAutoUpgradeEnabled(),
                                    progress.isAutoExchangeEnabled()
                                );

                                controller.setAccount(updatedAccount);
                            });
                        }
                    }
                }
            });
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            reloginRequiredAccounts.add(userId);
            Platform.runLater(() -> {
                for (javafx.scene.Node node : accountsVBox.getChildren()) {
                    if (node instanceof HBox accountItem) {
                        AccountItemController controller = (AccountItemController) accountItem.getUserData();
                        if (controller.getAccount().getUserId().equals(userId)) {
                            controller.showReloginRequiredState();
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error scanning account " + userId + ": " + e.getMessage());
        }
    }

    public void updateGiftCountForAccount(Long userId) {
        Platform.runLater(() -> {
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller.getAccount().getUserId().equals(userId)) {
                        controller.updateGiftCount();
                        break;
                    }
                }
            }
        });
    }

    private void handleRefreshAccounts() {
        startLoadingAnimationRefresh(refreshButton);
        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    accountsVBox.getChildren().forEach(node -> {
                        if (node instanceof HBox accountItem) {
                            Object controller = accountItem.getUserData();
                            if (controller instanceof AccountItemController accountController) {
                                accountController.resetRedButtonsToWhite();
                                accountController.updateButtonStates();
                            }
                        }
                    });
                });
                var accounts = userCookieRepository.findAll();
                for (UserCookie userCookie : accounts) {
                    try {
                        scanningProgress.sendGetRequestWithCookies(userCookie.getId());
                        reloginRequiredAccounts.remove(userCookie.getId());
                        Thread.sleep(1000);
                    } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
                        reloginRequiredAccounts.add(userCookie.getId());
                        Platform.runLater(() -> {
                            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                                if (node instanceof HBox accountItem) {
                                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                                    if (controller.getAccount().getUserId().equals(userCookie.getId())) {
                                        controller.showReloginRequiredState();
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Error refreshing account " + userCookie.getId() + ": " + e.getMessage());
                    }
                }
                return null;
            }
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    stopLoadingAnimationRefresh(refreshButton);
                    loadAccountsFromDatabase();

                    // После загрузки данных из базы и обновления UI, проверяем запущенные задачи и обновляем иконки
                    List<MangaTask> runningTasks = taskExecutor.getRunningTasks();
                    if (!runningTasks.isEmpty()) {
                        for (javafx.scene.Node node : accountsVBox.getChildren()) {
                            if (node instanceof HBox accountItem) {
                                AccountItemController controller = (AccountItemController) accountItem.getUserData();
                                Long accountUserId = controller.getAccount().getUserId();
                                runningTasks.stream()
                                            .filter(task -> task.getUserId().equals(accountUserId))
                                            .forEach(runningTask -> {
                                                // Обновляем иконку статуса на синий для запущенных задач
                                                controller.updateTaskStatusIcon(runningTask.getType(), "blue");
                                            });
                            }
                        }
                    }
                });
            }
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    stopLoadingAnimationRefresh(refreshButton);
                    System.err.println("Error refreshing accounts: " + getException().getMessage());
                });
            }
        };
        new Thread(refreshTask).start();
    }

    public static void killChromeDrivers() {
        try {
            Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM chrome.exe /T");
            log.info("Killed chrome drivers");
        } catch (IOException e) {
            log.info("Failed to kill chrome processes");
        }
    }

    private void handleApplyPromoCode() {
        String promoCode = promoCodeInput.getText();
        if (promoCode == null || promoCode.trim().isEmpty()) {
            // Show toast notification for empty field
            log.info("Promo code is empty!");
            showNotification("Промокод не введен!", "error");
            return;
        }

        // Disable button and input while processing
        applyPromoCodeButton.setDisable(true);
        promoCodeInput.setDisable(true);

        Task<Void> applyPromoTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    promoCodeService.applyPromoCodeToAllAccounts(promoCode, MangaBuffJobViewController.this::showNotification);
                } catch (Exception e) {
                    System.err.println("Error applying promo code: " + e.getMessage());
                    // Show error toast notification
                    Platform.runLater(() -> showNotification("Ошибка при применении промокода: " + e.getMessage(), "error"));
                }
                return null;
            }

            @Override
            protected void succeeded() {
                // Re-enable button and input
                applyPromoCodeButton.setDisable(false);
                promoCodeInput.setDisable(false);
                // Final success notification will be handled by PromoCodeService
            }

            @Override
            protected void failed() {
                // Re-enable button and input
                applyPromoCodeButton.setDisable(false);
                promoCodeInput.setDisable(false);
                System.err.println("Promo code application failed.");
                Platform.runLater(() -> showNotification("Применение промокода завершилось с ошибкой.", "error"));
            }
        };

        new Thread(applyPromoTask).start();
    }

    // Method to show toast notifications
    public void showNotification(String message, String type) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/ToastNotification.fxml"));
                HBox toast = loader.load();
                ToastNotificationController controller = loader.getController();
                controller.showMessage(message, type, rootPane); // Pass rootPane to the controller

                // Position the toast notification (top right)
                if (rootPane != null) {
                    rootPane.getChildren().add(toast);

                    // Position at top right
                    AnchorPane.setTopAnchor(toast, 10.0);
                    AnchorPane.setRightAnchor(toast, 10.0);

                    // Adjust vertical position based on existing toasts
                    double offset = 10; // Initial offset from top
                    for (javafx.scene.Node existingToast : rootPane.getChildren()) {
                        if (existingToast != toast && existingToast instanceof HBox && existingToast.getStyleClass().contains("toast-notification")) {
                            // Calculate the new offset based on the bottom of the last toast + spacing
                            double bottom = existingToast.getBoundsInParent().getMinY() + existingToast.getBoundsInParent().getHeight();
                            offset = Math.max(offset, bottom + 10);
                        }
                    }
                    AnchorPane.setTopAnchor(toast, offset);

                    // Start the fade out animation after a delay
                    controller.startFadeOut(Duration.seconds(4), Duration.seconds(2)); // Fade out after 2 seconds over 0.5 seconds
                }

            } catch (IOException e) {
                System.err.println("Error loading ToastNotification.fxml: " + e.getMessage()); // Keep for debugging during dev
                e.printStackTrace();
            }
        });
    }

    private void showSupportPopup() {
        try {
            if (!supportPopup.isShowing()) {
                supportPopup.show(supportImageView.getScene().getWindow(),
                    supportImageView.localToScreen(supportImageView.getBoundsInLocal()).getMinX(),
                    supportImageView.localToScreen(supportImageView.getBoundsInLocal()).getMaxY());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при показе окна поддержки: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void hideSupportPopupWithDelay() {
        if (hidePopupTimeline != null) {
            hidePopupTimeline.stop();
        }
        hidePopupTimeline = new Timeline(new KeyFrame(Duration.millis(200), event -> {
            if (supportPopup != null && supportPopup.isShowing()) {
                supportPopup.hide();
            }
        }));
        hidePopupTimeline.play();
    }

    private void hideSupportPopup() {
        if (supportPopup != null && supportPopup.isShowing()) {
            supportPopup.hide();
        }
    }

    public VBox getAccountsVBox() {
        return accountsVBox;
    }

    public void toggleAllReaderCheckboxes() {
        Platform.runLater(() -> {
            boolean newState = !isAnyReaderCheckboxSelected();
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller != null) {
                        controller.getReaderCheckBox().setSelected(newState);
                    }
                }
            }
        });
    }

    public void toggleAllCommentCheckboxes() {
        Platform.runLater(() -> {
            boolean newState = !isAnyCommentCheckboxSelected();
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller != null) {
                        controller.getCommentCheckBox().setSelected(newState);
                    }
                }
            }
        });
    }

    public void toggleAllQuizCheckboxes() {
        Platform.runLater(() -> {
            boolean newState = !isAnyQuizCheckboxSelected();
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller != null) {
                        controller.getQuizCheckBox().setSelected(newState);
                    }
                }
            }
        });
    }

    public void toggleAllMinerCheckboxes() {
        Platform.runLater(() -> {
            boolean newState = !isAnyMinerCheckboxSelected();
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller != null) {
                        controller.getMineCheckBox().setSelected(newState);
                    }
                }
            }
        });
    }

    public void toggleAllAdvCheckboxes() {
        Platform.runLater(() -> {
            boolean newState = !isAnyAdvCheckboxSelected();
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof HBox accountItem) {
                    AccountItemController controller = (AccountItemController) accountItem.getUserData();
                    if (controller != null) {
                        controller.getAdvCheckBox().setSelected(newState);
                    }
                }
            }
        });
    }

    private boolean isAnyReaderCheckboxSelected() {
        for (javafx.scene.Node node : accountsVBox.getChildren()) {
            if (node instanceof HBox accountItem) {
                AccountItemController controller = (AccountItemController) accountItem.getUserData();
                if (controller != null && controller.getReaderCheckBox().isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAnyCommentCheckboxSelected() {
        for (javafx.scene.Node node : accountsVBox.getChildren()) {
            if (node instanceof HBox accountItem) {
                AccountItemController controller = (AccountItemController) accountItem.getUserData();
                if (controller != null && controller.getCommentCheckBox().isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAnyQuizCheckboxSelected() {
        for (javafx.scene.Node node : accountsVBox.getChildren()) {
            if (node instanceof HBox accountItem) {
                AccountItemController controller = (AccountItemController) accountItem.getUserData();
                if (controller != null && controller.getQuizCheckBox().isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAnyMinerCheckboxSelected() {
        for (javafx.scene.Node node : accountsVBox.getChildren()) {
            if (node instanceof HBox accountItem) {
                AccountItemController controller = (AccountItemController) accountItem.getUserData();
                if (controller != null && controller.getMineCheckBox().isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAnyAdvCheckboxSelected() {
        for (javafx.scene.Node node : accountsVBox.getChildren()) {
            if (node instanceof HBox accountItem) {
                AccountItemController controller = (AccountItemController) accountItem.getUserData();
                if (controller != null && controller.getAdvCheckBox().isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    public MangaProgressRepository getMangaProgressRepository() {
        return mangaProgressRepository;
    }

    private void updateCustomSwitchUI() {
        customSwitch.getChildren().clear();
        if (isCustomSwitchOn) {
            customSwitch.setStyle("-fx-background-color: #5DFF70; -fx-border-color: #0C0C0C; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
            switchLabel.setText("ON");
            switchLabel.setTranslateX(-11);
            switchCircle.setTranslateX(11);
            showNotification("Успешный запуск сбор алмазов из чата", "success");
            log.info("Тумблер в положении ON, включился");
            // Запуск фоновой задачи
            if (chatDiamondExecutor == null || chatDiamondExecutor.isShutdown()) {
                chatDiamondExecutor = Executors.newSingleThreadScheduledExecutor();
            }
            isChatDiamondActive = true;
            if (chatDiamondFuture == null || chatDiamondFuture.isCancelled() || chatDiamondFuture.isDone()) {
                // 15 минут 30 секунд
                int PERIOD_TAKIN_DAIMOND_IN_CHAT = 930;
                chatDiamondFuture = chatDiamondExecutor.scheduleAtFixedRate(() -> {
                    if (!isChatDiamondActive) return;
                    try {
                        MbAuth mbAuth = applicationContext.getBean(MbAuth.class);
                        List<UserCookie> accounts = userCookieRepository.findAll();
                        int total = accounts.size();
                        final int[] success = {0};
                        for (UserCookie account : accounts) {
                            if (!isChatDiamondActive) break;
                            try {
                                ChromeDriver driver = mbAuth.getActualDriver(account.getId(), "chat", false); // true = не headless
                                driver.get("https://mangabuff.ru/chat");
                                WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(15));
                                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.chat-arena__get-coins-btn")));
                                Thread.sleep(2000);
                                btn.click();
                                Thread.sleep(1000);
                                btn.click();
                                Thread.sleep(1000);
                                driver.quit();
                                success[0]++;
                            } catch (Exception e) {
                                log.error("Ошибка при сборе алмазов для аккаунта {}: {}", account.getUsername(), e.getMessage());
                            }
                        }
                        Platform.runLater(() -> showNotification("Сбор алмазов завершён: " + success[0] + "/" + total, "success"));
                    } catch (Exception e) {
                        log.error("Ошибка фоновой задачи сбора алмазов: {}", e.getMessage());
                        Platform.runLater(() -> showNotification("Ошибка фоновой задачи сбора алмазов: " + e.getMessage(), "error"));
                    }
                }, 0, PERIOD_TAKIN_DAIMOND_IN_CHAT, TimeUnit.SECONDS);
            }
        } else {
            customSwitch.setStyle("-fx-background-color: #C0C0C0; -fx-border-color: #0C0C0C; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
            switchLabel.setText("OFF");
            switchLabel.setTranslateX(11);
            switchCircle.setTranslateX(-11);
            showNotification("Сбор алмазов остановленн", "success");
            // Остановка фоновой задачи
            isChatDiamondActive = false;
            if (chatDiamondFuture != null && !chatDiamondFuture.isCancelled()) {
                chatDiamondFuture.cancel(true);
            }
            if (chatDiamondExecutor != null && !chatDiamondExecutor.isShutdown()) {
                chatDiamondExecutor.shutdownNow();
            }
        }
        customSwitch.getChildren().addAll(switchCircle, switchLabel);
    }
} 