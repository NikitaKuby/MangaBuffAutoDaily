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
import ru.finwax.mangabuffjob.service.ScanningProgress;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class MangaBuffJobViewController implements Initializable {

    @FXML
    public CheckBox viewsCheckBox;
    @FXML
    private VBox accountsVBox;

    @FXML
    private Button refreshButton;
    @FXML
    private Button addAccountButton;


    @FXML
    private CheckBox periodicReadingCheckBox;
    @FXML
    private Button runBotButton;

    private final UserCookieRepository userCookieRepository;
    private final MangaProgressRepository mangaProgressRepository;
    private final ApplicationContext applicationContext;
    private final AccountItemControllerFactory accountItemControllerFactory;
    private final ScanningProgress scanningProgress;
    private Timeline loadingTimelineRefresh;

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

            // Настройка обработчиков событий для кнопок
            addAccountButton.setOnAction(event -> handleAddAccount());
            runBotButton.setOnAction(event -> handleRunBot());
            refreshButton.setOnAction(event -> handleRefreshAccounts());
            
            System.out.println("Controller initialization finished successfully");
        } catch (Exception e) {
            System.err.println("Controller initialization error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void loadAccountsFromDatabase() {
        try {
            accountData.clear();
            System.out.println("Loading accounts from database...");
            var accounts = userCookieRepository.findAll();
            System.out.println("Accounts found in database: " + accounts.size());

            accounts.forEach(userCookie -> {
                System.out.println("Processing account: " + userCookie.getUsername());
                // Получаем прогресс из базы данных
                MangaProgress progress = mangaProgressRepository.findByUserId(userCookie.getId())
                    .orElseGet(() -> {
                        System.out.println("Creating new progress for account: " + userCookie.getUsername());
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
                System.out.println("mineHitsLeft value from DB for user " + userCookie.getId() + ": " + progress.getMineHitsLeft());
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
                    mineHitsLeftForDisplay
                ));
            });
            System.out.println("Accounts loaded to UI: " + accountData.size());
            displayAccounts();
        } catch (Exception e) {
            System.err.println("Error loading accounts from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayAccounts() {
        try {
            System.out.println("Start displaying accounts...");
            Platform.runLater(() -> {
                try {
                    accountsVBox.getChildren().clear();

                    for (AccountProgress account : accountData) {
                        try {
                            System.out.println("Loading FXML for account: " + account.getUsername());
                            AccountItemController controller = accountItemControllerFactory.createController(this);
                            System.out.println("Controller created successfully");

                            controller.setAccount(account);
                            controller.setViewsCheckBox(viewsCheckBox);
                            System.out.println("Account set to controller");

                            HBox accountItem = controller.getAccountItem();
                            accountItem.setUserData(controller);
                            accountsVBox.getChildren().add(accountItem);
                            System.out.println("Account successfully added to UI: " + account.getUsername());
                        } catch (IOException e) {
                            System.err.println("Error loading FXML for account " + account.getUsername() + ": " + e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            System.err.println("Unexpected error processing account " + account.getUsername() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Accounts display finished. Total displayed: " + accountsVBox.getChildren().size());
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

    private void handleRunBot() {
        // Логика запуска бота
        System.out.println("Run Bot button clicked!");
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

    private void startLoadingAnimation(Button button) {
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

    private void stopLoadingAnimation(Button button) {
        if (loadingTimelineRefresh != null) {
            loadingTimelineRefresh.stop();
            loadingTimelineRefresh = null;
        }
        button.setText("↻");
        button.getStyleClass().remove("btn-blue");
        button.setDisable(false);
    }
    private void handleRefreshAccounts() {
        startLoadingAnimation(refreshButton);

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

                    Platform.runLater(() -> {
                        loadAccountsFromDatabase();
                    });
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> stopLoadingAnimation(refreshButton));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    stopLoadingAnimation(refreshButton);
                    System.err.println("Ошибка при обновлении аккаунтов: " + getException().getMessage());
                });
            }
        };

        new Thread(refreshTask).start();
    }


} 