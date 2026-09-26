package com.dealerlink.ui.controller;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.model.User;
import com.dealerlink.ui.Navigator;
import com.dealerlink.ui.Responsive;
import javafx.beans.value.ObservableDoubleValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Controller for fxml/register.fxml. */
public class RegisterController {

    @FXML private HBox root;
    @FXML private VBox brandPanel;
    @FXML private Label brandTitle;
    @FXML private VBox card;
    @FXML private Label cardTitle;
    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton shopToggle;
    @FXML private ToggleButton dealerToggle;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private TextField locationField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void initialize() {
        // A pill toggle group must always have one role selected.
        roleGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == null && old != null) old.setSelected(true);
        });
        applyResponsiveLayout();
    }

    /** Sizes relative to the window width (w) and height (h) via property bindings. */
    private void applyResponsiveLayout() {
        ObservableDoubleValue w = root.widthProperty();
        ObservableDoubleValue h = root.heightProperty();

        Responsive.bindPrefWidth(brandPanel, w, 0.38, 340, 580);
        Responsive.showWhenAtLeast(brandPanel, w, 1000);
        Responsive.bindWidth(card, w, 0.36, 360, 460);
        Responsive.bindFontSize(brandTitle, w, 0.032, 28, 48);
        Responsive.bindFontSize(cardTitle, w, 0.02, 22, 28);
        Responsive.bindSpacing(card, h, 0.013, 6, 12);
    }

    @FXML
    private void handleRegister() {
        TextField[] all = {usernameField, passwordField, fullNameField, locationField};
        for (TextField f : all) f.getStyleClass().remove("input-error");

        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        String name = fullNameField.getText().trim();
        String loc = locationField.getText().trim();

        StringBuilder problems = new StringBuilder();
        if (u.length() < 3) { problems.append("Username must be at least 3 characters. "); markError(usernameField); }
        if (p.length() < 4) { problems.append("Password must be at least 4 characters. "); markError(passwordField); }
        if (name.isEmpty()) { problems.append("Full name / shop name is required. "); markError(fullNameField); }
        if (loc.isEmpty()) { problems.append("Location is required. "); markError(locationField); }
        if (!problems.isEmpty()) {
            statusLabel.setText(problems.toString().trim());
            return;
        }

        String role = shopToggle.isSelected() ? "SHOP" : "DEALER";
        // Latitude/longitude start at 0; GeocodingService resolves the city the first
        // time weather is needed and saves the coordinates for this account.
        User newUser = new User(0, u, p, role, name, loc, 0, 0);
        if (userDAO.register(newUser)) {
            Navigator.show(Navigator.LOGIN);
        } else {
            statusLabel.setText("Registration failed - that username is already taken.");
            markError(usernameField);
        }
    }

    @FXML
    private void goToLogin() {
        Navigator.show(Navigator.LOGIN);
    }

    private void markError(TextField f) {
        if (!f.getStyleClass().contains("input-error")) f.getStyleClass().add("input-error");
    }
}
