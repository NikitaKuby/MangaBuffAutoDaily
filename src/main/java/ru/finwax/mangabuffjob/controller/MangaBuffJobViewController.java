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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;

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
            mangaReadScheduler.startPeriodicReading();
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
                    } else {
                        mangaParserService.createMangaList();
                    }
                    Platform.runLater(() -> setButtonState(updateMangaListButton, "green"));
                } catch (Exception e) {
                    Platform.runLater(() -> setButtonState(updateMangaListButton, "red"));
                    System.err.println("Ошибка при обновлении списка манги: " + e.getMessage());
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
            new KeyFrame(Duration.ZERO, e -> button.setText("↻.")),
            new KeyFrame(Duration.seconds(0.5), e -> button.setText("↻..")),
            new KeyFrame(Duration.seconds(1), e -> button.setText("↻..."))
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
            // Обновляем UI в FX потоке
            Platform.runLater(this::loadAccountsFromDatabase);
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
                // Получаем все дочерние элементы из accountsVBox
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
                    scanningProgress.sendGetRequestWithCookies(userCookie.getId());
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        loadAccountsFromDatabase();
                    });
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
                    System.err.println("Ошибка при обновлении аккаунтов: " + getException().getMessage());
                });
            }
        };

        new Thread(refreshTask).start();
    }

    public static void killChromeDrivers() {
        try {
            Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM chrome.exe /T");
            System.out.println("Убили драйвера");
        } catch (IOException e) {
            System.err.print("Failed to kill chrome processes");
        }
    }


} 