package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import java.io.File;
import java.util.List;

public class GiftImagesPopupController {

    @FXML
    private FlowPane imageFlowPane;

    public void setGiftImages(List<String> imagePaths) {
        imageFlowPane.getChildren().clear();
        for (String imagePath : imagePaths) {
            File file = new File(imagePath);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);
                imageFlowPane.getChildren().add(imageView);
            }
        }
    }
} 