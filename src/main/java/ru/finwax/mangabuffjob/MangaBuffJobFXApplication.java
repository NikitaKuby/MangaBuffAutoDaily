package ru.finwax.mangabuffjob;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

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

        // Установка иконки приложения (favicon)
        try {
            // Можно использовать favicon.ico или png (32x32)
            Image icon = new Image(getClass().getResourceAsStream("/static/favicon_io/favicon-32x32.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Не удалось загрузить иконку приложения: " + e.getMessage());
        }

        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 