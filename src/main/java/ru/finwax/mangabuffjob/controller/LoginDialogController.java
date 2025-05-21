package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.service.ScanningProgress;
import ru.finwax.mangabuffjob.service.CookieService;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;
import javafx.scene.control.Alert;

@Component
@RequiredArgsConstructor
public class LoginDialogController {
    @FXML
    private TextField loginField;
    @FXML
    private Button loginButton;

    private final MangaBuffAuth mangaBuffAuth;
    private final ScanningProgress scanningProgress;
    private final CookieService cookieService;
    private final UserCookieRepository userCookieRepository;

    private Stage dialogStage;
    @Getter
    private boolean loginClicked = false;

    private UserCookie authenticatedUserCookie;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleLogin() {
        if (isInputValid()) {
            loginButton.setDisable(true);
            try {
                authenticatedUserCookie = mangaBuffAuth.authenticate(loginField.getText());

                scanningProgress.sendGetRequestWithCookies(authenticatedUserCookie.getId());

                loginClicked = true;
                dialogStage.close();
            } catch (HttpClientErrorException.Unauthorized e) {
                System.err.println("Ошибка аутентификации: " + e.getMessage());
                // Показываем сообщение пользователю
                showErrorMessage("Что-то пошло не так при входе. Пожалуйста, попробуйте еще раз.");
                loginButton.setDisable(false);
            } catch (Exception e) {
                e.printStackTrace();
                loginButton.setDisable(false);
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (loginField.getText() == null || loginField.getText().length() == 0) {
            errorMessage += "Логин не может быть пустым!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            return false;
        }
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 