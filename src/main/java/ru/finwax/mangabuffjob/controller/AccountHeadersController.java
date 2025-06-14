package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Tooltip;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AccountHeadersController {
    @FXML
    private AnchorPane headersPane;
    
    @FXML
    private ImageView readerImageView;
    
    @FXML
    private ImageView commentImageView;
    
    @FXML
    private ImageView quizImageView;
    
    @FXML
    private ImageView minerImageView;
    
    @FXML
    private ImageView advImageView;

    private MangaBuffJobViewController parentController;

    @FXML
    public void initialize() {
        // Инициализация Tooltip'ов
        Tooltip readerTooltip = new Tooltip("Включить/Отключить все чекбоксы");
        readerTooltip.setShowDelay(javafx.util.Duration.millis(50));
        Tooltip.install(readerImageView, readerTooltip);

        Tooltip commentTooltip = new Tooltip("Включить/Отключить все чекбоксы");
        commentTooltip.setShowDelay(javafx.util.Duration.millis(50));
        Tooltip.install(commentImageView, commentTooltip);

        Tooltip quizTooltip = new Tooltip("Включить/Отключить все чекбоксы");
        quizTooltip.setShowDelay(javafx.util.Duration.millis(50));
        Tooltip.install(quizImageView, quizTooltip);

        Tooltip minerTooltip = new Tooltip("Включить/Отключить все чекбоксы");
        minerTooltip.setShowDelay(javafx.util.Duration.millis(50));
        Tooltip.install(minerImageView, minerTooltip);

        Tooltip advTooltip = new Tooltip("Включить/Отключить все чекбоксы");
        advTooltip.setShowDelay(javafx.util.Duration.millis(50));
        Tooltip.install(advImageView, advTooltip);
    }

    public void setParentController(MangaBuffJobViewController parentController) {
        this.parentController = parentController;
    }

    public AnchorPane getHeadersPane() {
        return headersPane;
    }

    @FXML
    private void handleReaderIconClick() {
        if (parentController != null) {
            parentController.toggleAllReaderCheckboxes();
        }
    }

    @FXML
    private void handleCommentIconClick() {
        if (parentController != null) {
            parentController.toggleAllCommentCheckboxes();
        }
    }

    @FXML
    private void handleQuizIconClick() {
        if (parentController != null) {
            parentController.toggleAllQuizCheckboxes();
        }
    }

    @FXML
    private void handleMinerIconClick() {
        if (parentController != null) {
            parentController.toggleAllMinerCheckboxes();
        }
    }

    @FXML
    private void handleAdvIconClick() {
        if (parentController != null) {
            parentController.toggleAllAdvCheckboxes();
        }
    }
} 