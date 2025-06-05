package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import org.springframework.stereotype.Component;

@Component
public class AccountHeadersController {
    @FXML
    private AnchorPane headersPane;

    public AnchorPane getHeadersPane() {
        return headersPane;
    }
} 