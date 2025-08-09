package ru.finwax.mangabuffjob;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ru.finwax.mangabuffjob.service.DiamondCounterService;
import ru.finwax.mangabuffjob.service.DriverManager;
import ru.finwax.mangabuffjob.service.TaskExecutor;
import ru.finwax.mangabuffjob.service.ProcessManager;
import javax.sql.DataSource;

@Slf4j
public class MangaBuffJobFXApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        SpringApplication application = new SpringApplication(AppConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        springContext = application.run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/MangaBuffJobView.fxml"));

        // Устанавливаем фабрику контроллеров для получения контроллеров из Spring контекста
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();
        primaryStage.setTitle("MangaBuffJob Desktop");
        primaryStage.setScene(new Scene(root));

        // Добавляем обработчик закрытия окна
        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // Предотвращаем немедленное закрытие
            stop(); // Вызываем наш метод закрытия
            primaryStage.close();
        });

        // Установка иконки приложения (favicon)
        try {
            // Можно использовать favicon.ico или png (32x32)
            Image icon = new Image(getClass().getResourceAsStream("/static/favicon_io/favicon-32x32.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            log.error("Не удалось загрузить иконку приложения: " + e.getMessage());
        }

        primaryStage.show();
    }

    @Override
    public void stop() {
        try {
            log.info("Закрытие приложения...");
            if (springContext != null) {
                // Останавливаем все задачи
                try {
                    TaskExecutor taskExecutor = springContext.getBean(TaskExecutor.class);
                    taskExecutor.stopAllTasks();
                    log.info("Все задачи остановлены");
                } catch (Exception e) {
                    log.error("Ошибка при остановке задач: " + e.getMessage());
                }
                
                // Закрываем все драйверы
                try {
                    DriverManager driverManager = springContext.getBean(DriverManager.class);
                    driverManager.closeAllDrivers();
                    log.info("Все драйверы закрыты");
                } catch (Exception e) {
                    log.error("Ошибка при закрытии драйверов: " + e.getMessage());
                }

                
                // Очищаем Java процессы
                try {
                    ProcessManager processManager = springContext.getBean(ProcessManager.class);
                    processManager.logProcessInfo();
                    processManager.killJavaProcesses();
                    log.info("Java процессы очищены");
                } catch (Exception e) {
                    log.error("Ошибка при очистке Java процессов: " + e.getMessage());
                }
                
                // Останавливаем сервис счётчика алмазов
                try {
                    DiamondCounterService diamondCounterService = springContext.getBean(DiamondCounterService.class);
                    diamondCounterService.shutdown();
                    log.info("Сервис счётчика алмазов остановлен");
                } catch (Exception e) {
                    log.error("Ошибка при остановке сервиса счётчика алмазов: " + e.getMessage());
                }
                
                // Закрываем все соединения с базой данных
                try {
                    DataSource dataSource = springContext.getBean(DataSource.class);
                    if (dataSource instanceof DriverManagerDataSource) {
                        ((DriverManagerDataSource) dataSource).getConnection().close();
                    }
                } catch (Exception e) {
                    log.error("Ошибка при закрытии соединения с базой данных: " + e.getMessage());
                }
                
                springContext.close();
            }
            log.info("MangaBuffJob Desktop остановлен.");
        } catch (Exception e) {
            log.error("Ошибка при закрытии приложения: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 