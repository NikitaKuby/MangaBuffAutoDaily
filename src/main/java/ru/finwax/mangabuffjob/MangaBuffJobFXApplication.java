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