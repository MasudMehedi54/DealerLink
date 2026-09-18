package com.dealerlink.ui;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegisterScreen extends VBox {

    private final UserDAO userDAO = new UserDAO();

    public RegisterScreen(Stage stage) {
        setSpacing(20);
        setPadding(new Insets(50));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: linear-gradient(to bottom right, #EFF6FF, #F1F5F9);");

        Label title = new Label("📝 Create Your Account");
        title.getStyleClass().add("app-title");
        title.setStyle("-fx-font-size: 24px;");
        Label subtitle = new Label("Join DealerLink as a shop owner or a dealer");
        subtitle.getStyleClass().add("app-subtitle");
        VBox headerBox = new VBox(6, title, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // ---------- Role toggle: pill-style buttons instead of a plain combo box ----------
        ToggleGroup roleGroup = new ToggleGroup();
        ToggleButton shopToggle = new ToggleButton("🏬  Shop Owner");
        ToggleButton dealerToggle = new ToggleButton("🚚  Dealer");
        shopToggle.getStyleClass().add("toggle-pill");
        dealerToggle.getStyleClass().add("toggle-pill");
        shopToggle.setToggleGroup(roleGroup);
        dealerToggle.setToggleGroup(roleGroup);
        shopToggle.setSelected(true);
        shopToggle.setMaxWidth(Double.MAX_VALUE);
        dealerToggle.setMaxWidth(Double.MAX_VALUE);
        HBox roleBox = new HBox(10, shopToggle, dealerToggle);
        HBox.setHgrow(shopToggle, Priority.ALWAYS);
        HBox.setHgrow(dealerToggle, Priority.ALWAYS);

        TextField username = new TextField();
        username.setPromptText("Choose a username");
        PasswordField password = new PasswordField();
        password.setPromptText("Choose a password (min 4 characters)");
        TextField fullName = new TextField();
        fullName.setPromptText("Your name or shop/business name");
        TextField location = new TextField();
        location.setPromptText("City, e.g. Khulna");

        Label status = UiUtils.errorText("");

        GridPane form = new GridPane();
        form.setVgap(14);
        form.setHgap(12);
        int row = 0;
        form.add(new Label("I am registering as:"), 0, row++);
        form.add(roleBox, 0, row++);
        form.add(new Label("👤 Username"), 0, row++);
        form.add(username, 0, row++);
        form.add(new Label("🔒 Password"), 0, row++);
        form.add(password, 0, row++);
        form.add(new Label("🏷 Full Name / Shop Name"), 0, row++);
        form.add(fullName, 0, row++);
        form.add(new Label("📍 Location"), 0, row++);
        form.add(location, 0, row);
        form.setMaxWidth(340);
        for (var field : new TextField[]{username, password, fullName, location}) {
            field.setMaxWidth(Double.MAX_VALUE);
        }

        Button createBtn = UiUtils.primaryButton("Create Account");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setDefaultButton(true);
        createBtn.setOnAction(e -> {
            clearErrors(username, password, fullName, location);
            String u = username.getText().trim();
            String p = password.getText();
            String name = fullName.getText().trim();
            String loc = location.getText().trim();

            StringBuilder problems = new StringBuilder();
            if (u.length() < 3) { problems.append("Username must be at least 3 characters. "); markError(username); }
            if (p.length() < 4) { problems.append("Password must be at least 4 characters. "); markError(password); }
            if (name.isEmpty()) { problems.append("Full name / shop name is required. "); markError(fullName); }
            if (loc.isEmpty()) { problems.append("Location is required. "); markError(location); }

            if (!problems.isEmpty()) {
                status.setText(problems.toString().trim());
                return;
            }

            String role = shopToggle.isSelected() ? "SHOP" : "DEALER";
            // Latitude/longitude default to 0 here; a real deployment would geocode
            // the typed location via a REST API (see WeatherService for the HTTP pattern).
            User newUser = new User(0, u, p, role, name, loc, 0, 0);
            boolean ok = userDAO.register(newUser);
            if (ok) {
                stage.getScene().setRoot(new LoginScreen(stage));
            } else {
                status.setText("Registration failed - that username is already taken.");
                markError(username);
            }
        });

        Button backBtn = UiUtils.ghostButton("← Back to Login");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> stage.getScene().setRoot(new LoginScreen(stage)));

        VBox card = UiUtils.card(form, createBtn, backBtn, status);
        card.setMaxWidth(400);

        getChildren().addAll(headerBox, card);
    }

    private void markError(TextField field) {
        if (!field.getStyleClass().contains("input-error")) field.getStyleClass().add("input-error");
    }

    private void clearErrors(TextField... fields) {
        for (TextField f : fields) f.getStyleClass().remove("input-error");
    }
}
