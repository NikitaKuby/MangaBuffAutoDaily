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
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Entity.MangaProgress;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.model.AccountProgress;
import ru.finwax.mangabuffjob.repository.MangaProgressRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import ru.finwax.mangabuffjob.service.MangaParserService;
import ru.finwax.mangabuffjob.service.ScanningProgress;
import ru.finwax.mangabuffjob.model.TaskType;
import ru.finwax.mangabuffjob.service.TaskExecutor;
import ru.finwax.mangabuffjob.model.MangaTask;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.service.AccountService;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Pos;
import javafx.scene.Node;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Component
@RequiredArgsConstructor
public class MangaBuffJobViewController implements Initializable{

    @FXML
    public CheckBox viewsCheckBox;
    @FXML
    public Button updateMangaListButton;

    @FXML
    public Button periodicReadingButton;
    @FXML
    private VBox accountsVBox;

    @FXML
    private Button refreshButton;
    @FXML
    private Button addAccountButton;
    


    @FXML
    private Button runBotButton;

    @FXML
    private javafx.scene.control.TextField promoCodeInput;
    @FXML
    private Button applyPromoCodeButton;

    @FXML
    private AnchorPane rootPane;

    private final UserCookieRepository userCookieRepository;
    private final MangaProgressRepository mangaProgressRepository;
    private final ApplicationContext applicationContext;
    private final AccountItemControllerFactory accountItemControllerFactory;
    private final ScanningProgress scanningProgress;
    private final MangaParserService mangaParserService;
    private final TaskExecutor taskExecutor;
    private Timeline loadingTimelineRefresh;
    private final MangaReadScheduler mangaReadScheduler;
    private final AccountService accountService;
    private final ru.finwax.mangabuffjob.service.PromoCodeService promoCodeService;

    private ObservableList<AccountProgress> accountData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            System.out.println("Controller initialization...");
            
            // Проверяем инициализацию компонентов
            if (accountsVBox == null) {
                throw new IllegalStateException("accountsVBox not initialized");
            }
            if (addAccountButton == null) {
                throw new IllegalStateException("addAccountButton not initialized");
            }
            if (runBotButton == null) {
                throw new IllegalStateException("runBotButton not initialized");
            }
            
            System.out.println("All components initialized successfully");
            
            // Загрузка данных из базы данных
            loadAccountsFromDatabase();
            handleRefreshAccounts();
            killChromeDrivers();

            // Устанавливаем ссылку на этот контроллер в MangaReadScheduler
            mangaReadScheduler.setViewController(this);

            // Настройка обработчиков событий для кнопок
            addAccountButton.setOnAction(event -> handleAddAccount());
            runBotButton.setOnAction(event -> handleRunBot());
            refreshButton.setOnAction(event -> handleRefreshAccounts());
            updateMangaListButton.setOnAction(event -> handleUpdateMLB());
            periodicReadingButton.setOnAction(event -> handlePeriodicReading());

            // Add handler for promo code button
            applyPromoCodeButton.setOnAction(event -> handleApplyPromoCode());
            
            // Проверяем наличие данных в таблице манги
            if (mangaParserService.hasMangaData()) {
                setButtonState(updateMangaListButton, "green");
            }
            
            System.out.println("Controller initialization finished successfully");
        } catch (Exception e) {
            System.err.println("Controller initialization error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void handlePeriodicReading() {
        if (!mangaReadScheduler.isPeriodicReadingActive()) {
            mangaReadScheduler.startPeriodicReading(viewsCheckBox.isSelected());
            setButtonState(periodicReadingButton, "blue");
        } else {
            mangaReadScheduler.stopPeriodicReading();
            setButtonState(periodicReadingButton, "white");
        }
    }

    private void handleUpdateMLB() {
        setButtonState(updateMangaListButton, "blue");

        Task<Void> updateTask = new Task<>() {
            @Override
            protected Void call(){
                try {
                    File csvFile = new File("manga_parsing_data.csv");
                    if (csvFile.exists()) {
                        mangaParserService.importMangaFromCSV();
                        Platform.runLater(() -> setButtonState(updateMangaListButton, "green"));
                    } else {
                        mangaParserService.createMangaList();
                    }
                    Platform.runLater(() -> setButtonState(updateMangaListButton, "green"));
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(updateMangaListButton, "red"));
                    System.err.println("Error updating manga list: " + e.getMessage());
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
                // Получаем прогресс из базы данных
                MangaProgress progress = mangaProgressRepository.findByUserId(userCookie.getId())
                    .orElseGet(() -> {
                        // Создаем новый прогресс только если его нет в базе
                        MangaProgress newProgress = MangaProgress.builder()
                            .userId(userCookie.getId())
                            .readerDone(0)
                            .commentDone(0)
                            .quizDone(false)
                            .mineHitsLeft(100)
                            .advDone(0)
                            .lastUpdated(LocalDate.now())
                            .build();
                        // Сохраняем новый прогресс в базу
                        return mangaProgressRepository.save(newProgress);
                    });

                // Добавляем аккаунт в список только если он существует в базе
                Integer mineHitsLeftForDisplay = progress.getMineHitsLeft() != null ? progress.getMineHitsLeft() : 100;

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
                    progress.getDiamond()
                ));
            });

            // Обновляем отображение аккаунтов после загрузки данных
            displayAccounts();

        } catch (Exception e) {
            System.err.println("Error loading accounts from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayAccounts() {
        try {
            Platform.runLater(() -> {
                try {
                    accountsVBox.getChildren().clear();

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
                } catch (Exception e) {
                    System.err.println("Critical error displaying accounts: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Critical error in displayAccounts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAddAccount() {
        System.out.println("handleAddAccount called");
        try {
            // Загрузка FXML для диалогового окна
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/LoginDialog.fxml"));
            System.out.println("Loading FXML for LoginDialog.fxml");
            
            // Устанавливаем фабрику контроллеров для LoginDialog.fxml
            loader.setControllerFactory(applicationContext::getBean);
            
            Scene scene = new Scene(loader.load());

            // Получение контроллера и установка сцены
            LoginDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Вход в аккаунт");
            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);

            System.out.println("Showing login dialog...");
            // Показ диалога и ожидание закрытия
            dialogStage.showAndWait();

            // Если пользователь нажал "Войти", обновляем список аккаунтов
            if (controller.isLoginClicked()) {
                System.out.println("User logged in, updating account list");
                loadAccountsFromDatabase();
            } else {
                System.out.println("User cancelled login");
            }
        } catch (IOException e) {
            System.err.println("Error loading LoginDialog.fxml");
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
        if (account.getAdvDone() < 3) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.ADV, 3 - account.getAdvDone()));
        }
        
        // MINE tasks
        if (account.getMineHitsLeft() > 0) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.MINE, account.getMineHitsLeft()));
        }
        
        // QUIZ tasks
        if (account.getQuizDone() != null && !account.getQuizDone()) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.QUIZ, 1));
        }
        
        // COMMENT tasks
        String[] commentProgress = account.getCommentProgress().split("/");
        int commentDone = Integer.parseInt(commentProgress[0]);
        int totalComments = Integer.parseInt(commentProgress[1]);
        if (commentDone < totalComments) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.COMMENT, totalComments - commentDone));
        }
        
        // READER tasks
        String[] readerProgress = account.getReaderProgress().split("/");
        int readerDone = Integer.parseInt(readerProgress[0]);
        int totalReader = Integer.parseInt(readerProgress[1]);
        if (readerDone < 75) {
            tasks.add(new MangaTask(account.getUserId(), TaskType.READER, 75 - readerDone));
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
                        break;
                    }
                }
            }
        });
    }

    private void handleRunBot() {
        // Блокируем UI
        setUIEnabled(false);
        
//        // Рефрешим состояние всех аккаунтов
//        handleRefreshAccounts();
        
        // Устанавливаем состояние checkViews
        taskExecutor.getTaskExecutionService().setCheckViews(viewsCheckBox.isSelected());
        
        // Собираем все задачи
        List<MangaTask> allTasks = new ArrayList<>();
        for (AccountProgress account : accountData) {
            allTasks.addAll(getPendingTasks(account));
        }
        
        // Запускаем выполнение задач
        taskExecutor.executeTasks(allTasks, this::updateTaskStatus);
        
        // После завершения всех задач
        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Ждем завершения всех задач
                    Thread.sleep(1000); // Даем время на завершение
                    Platform.runLater(() -> {
                        handleRefreshAccounts();
                        setUIEnabled(true);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        };
        new Thread(refreshTask).start();
    }

    public void scanAccount(Long userId) {
        try {
            scanningProgress.sendGetRequestWithCookies(userId);
            Platform.runLater(this::loadAccountsFromDatabase);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            System.err.println("Error 401 Unauthorized for account " + userId);
            Platform.runLater(() -> {
                // Find the corresponding AccountItemController
                for (javafx.scene.Node node : accountsVBox.getChildren()) {
                    if (node instanceof HBox accountItem) {
                        AccountItemController controller = (AccountItemController) accountItem.getUserData();
                        if (controller.getAccount().getUserId().equals(userId)) {
                            controller.showReloginRequiredState();
                            break;
                        }
                    }
                }
            });
            // e.printStackTrace(); // You might want to log this error
        } catch (org.springframework.web.client.HttpServerErrorException.BadGateway e) {
            System.err.println("Error 502 Bad Gateway when scanning account " + userId + ": Site unavailable.");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Ошибка сканирования аккаунта");
                alert.setHeaderText("Сайт недоступен");
                alert.setContentText("Не удалось подключиться к сайту для сканирования аккаунта " + userId + ". Пожалуйста, попробуйте позже.");
                alert.showAndWait();
            });
            // e.printStackTrace(); // You might want to log this error
        } catch (Exception e) {
            e.printStackTrace();
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

                // Обновляем все аккаунты
                var accounts = userCookieRepository.findAll();
                for (UserCookie userCookie : accounts) {
                    scanAccount(userCookie.getId());
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> stopLoadingAnimationRefresh(refreshButton));
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
            System.out.println("Killed chrome drivers");
        } catch (IOException e) {
            System.err.print("Failed to kill chrome processes");
        }
    }

    private void handleApplyPromoCode() {
        String promoCode = promoCodeInput.getText();
        if (promoCode == null || promoCode.trim().isEmpty()) {
            // Show toast notification for empty field
            System.out.println("Promo code is empty!");
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

} 