package ru.finwax.mangabuffjob.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ResourceBundle;

public class ToastNotificationController implements Initializable {

    @FXML
    private HBox toastBox;
    @FXML
    private Label messageLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial setup i needed
    }

    public void showMessage(String message, String type, Pane parentContainer) {
        messageLabel.setText(message);

        // Apply styles based on type
        toastBox.getStyleClass().removeAll("toast-error", "toast-success");
        if ("error".equals(type)) {
            toastBox.getStyleClass().add("toast-error");
        } else {
            toastBox.getStyleClass().add("toast-success");
        }

        // We will manage fade out and removal in the main controller
        // The main controller will add this toast to a container and manage its lifecycle.
        // This controller is mainly for setting message and style.
    }

    // Method to start fade out animation and remove from parent
    public void startFadeOut(Duration delay, Duration duration) {
        FadeTransition fadeOut = new FadeTransition(duration, toastBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(delay);
        fadeOut.setOnFinished(event -> {
            if (toastBox.getParent() instanceof Pane parentPane) {
                parentPane.getChildren().remove(toastBox);
            }
        });
        fadeOut.play();
    }

    public HBox getToastBox() {
        return toastBox;
    }
} 