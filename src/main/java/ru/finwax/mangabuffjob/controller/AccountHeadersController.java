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

    @FXML
    private ImageView scrollImageView;

    private javafx.stage.Popup scrollStatsPopup;

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

        Tooltip scrollTooltip = new Tooltip("Общее количество свитков (по всем аккаунтам)");
        scrollTooltip.setShowDelay(javafx.util.Duration.millis(50));
        Tooltip.install(scrollImageView, scrollTooltip);

        scrollImageView.setOnMouseEntered(event -> showScrollStatsPopup());
        scrollImageView.setOnMouseExited(event -> hideScrollStatsPopupWithDelay());
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

    private void showScrollStatsPopup() {
        if (scrollStatsPopup == null) {
            scrollStatsPopup = new javafx.stage.Popup();
        } else if (scrollStatsPopup.isShowing()) {
            scrollStatsPopup.hide();
        }
        javafx.scene.layout.VBox popupContent = new javafx.scene.layout.VBox();
        popupContent.setStyle("-fx-background-color: #fff; -fx-padding: 12; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-color: #bdbdbd; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, #888, 6, 0.2, 0, 2);");
        popupContent.setSpacing(6);
        String[] ranks = {"X", "S", "A", "P", "G", "B", "C", "D", "E"};
        String iconBase = "/static/icon/scroll/";
        javafx.scene.text.Font font = javafx.scene.text.Font.font("Arial", 16);
        int[] totalCounts = new int[ranks.length];
        int[] blessedCounts = new int[ranks.length];
        if (parentController != null) {
            javafx.scene.layout.VBox accountsVBox = parentController.getAccountsVBox();
            for (javafx.scene.Node node : accountsVBox.getChildren()) {
                if (node instanceof javafx.scene.layout.HBox) {
                    Object userData = node.getUserData();
                    if (userData instanceof ru.finwax.mangabuffjob.controller.AccountItemController) {
                        ru.finwax.mangabuffjob.model.AccountProgress acc = ((ru.finwax.mangabuffjob.controller.AccountItemController) userData).getAccount();
                        if (acc != null && acc.getScrollCounts() != null) {
                            for (int i = 0; i < ranks.length; i++) {
                                ru.finwax.mangabuffjob.model.CountScroll cs = acc.getScrollCounts().get(ranks[i]);
                                if (cs != null) {
                                    totalCounts[i] += cs.getCount();
                                    blessedCounts[i] += cs.getBlessedCount();
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < ranks.length; i++) {
            javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream(iconBase + ranks[i] + ".png")));
            icon.setFitHeight(28);
            icon.setFitWidth(28);
            javafx.scene.control.Label label = new javafx.scene.control.Label(totalCounts[i] + " | " + blessedCounts[i]);
            label.setFont(font);
            label.setStyle("-fx-text-fill: #444; -fx-font-weight: bold;");
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8, icon, label);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            popupContent.getChildren().add(row);
        }
        scrollStatsPopup.getContent().clear();
        scrollStatsPopup.getContent().add(popupContent);
        javafx.geometry.Bounds bounds = scrollImageView.localToScreen(scrollImageView.getBoundsInLocal());
        scrollStatsPopup.show(scrollImageView.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 2);
    }

    private void hideScrollStatsPopupWithDelay() {
        if (scrollStatsPopup != null && scrollStatsPopup.isShowing()) {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            pause.setOnFinished(e -> scrollStatsPopup.hide());
            pause.play();
        }
    }

    private int getTotalScrolls() {
        if (parentController == null) return 0;
        javafx.scene.layout.VBox accountsVBox = parentController.getAccountsVBox();
        int total = 0;
        for (javafx.scene.Node node : accountsVBox.getChildren()) {
            if (node instanceof javafx.scene.layout.HBox) {
                Object userData = node.getUserData();
                if (userData instanceof ru.finwax.mangabuffjob.controller.AccountItemController) {
                    ru.finwax.mangabuffjob.controller.AccountItemController controller = (ru.finwax.mangabuffjob.controller.AccountItemController) userData;
                    ru.finwax.mangabuffjob.model.AccountProgress acc = controller.getAccount();
                    if (acc != null) {
                        total += acc.getTotalScrolls(); // предполагается, что есть такой метод
                    }
                }
            }
        }
        return total;
    }
} 