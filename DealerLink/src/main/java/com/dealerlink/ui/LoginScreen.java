package com.dealerlink.ui;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginScreen extends VBox {

    private final UserDAO userDAO = new UserDAO();
    private final Stage stage;

    public LoginScreen(Stage stage) {
        this.stage = stage;
        setSpacing(22);
        setPadding(new Insets(50));
        setAlignment(Pos.CENTER);
        // Soft gradient backdrop behind the card, distinct from the plain gray dashboards.
        setStyle("-fx-background-color: linear-gradient(to bottom right, #EFF6FF, #F1F5F9);");

        Label logo = new Label("🔗 DealerLink");
        logo.getStyleClass().add("app-title");
        Label subtitle = new Label("Connecting shop owners with product dealers, instantly");
        subtitle.getStyleClass().add("app-subtitle");
        VBox headerBox = new VBox(6, logo, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // ---------- Username ----------
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefColumnCount(18);

        // ---------- Password with show/hide toggle ----------
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        TextField passwordVisible = new TextField();
        passwordVisible.setPromptText("Enter your password");
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());

        CheckBox showPassword = new CheckBox("Show password");
        showPassword.getStyleClass().add("hint-text");
        passwordVisible.visibleProperty().bind(showPassword.selectedProperty());
        passwordVisible.managedProperty().bind(showPassword.selectedProperty());
        passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
        passwordField.managedProperty().bind(showPassword.selectedProperty().not());
        StackPane passwordStack = new StackPane(passwordField, passwordVisible);

        Label statusLabel = UiUtils.errorText("");

        Button loginBtn = UiUtils.primaryButton("Log In");
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        Runnable attemptLogin = () -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            clearError(usernameField, passwordField);

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter both username and password.");
                markError(username.isEmpty(), usernameField);
                markError(password.isEmpty(), passwordField);
                return;
            }
            User user = userDAO.login(username, password);
            if (user == null) {
                statusLabel.setText("Invalid username or password. Please try again.");
                markError(true, usernameField);
                markError(true, passwordField);
                return;
            }
            statusLabel.setText("");
            openDashboard(user);
        };
        loginBtn.setOnAction(e -> attemptLogin.run());
        // Enter key inside either field also submits the form.
        usernameField.setOnAction(e -> attemptLogin.run());
        passwordField.setOnAction(e -> attemptLogin.run());
        passwordVisible.setOnAction(e -> attemptLogin.run());

        Button registerBtn = UiUtils.secondaryButton("Create a New Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> stage.getScene().setRoot(new RegisterScreen(stage)));

        Label demoHint = UiUtils.hint(
                "Demo accounts  ·  Shop: shop1 / 1234  ·  Dealer: dealer1 / 1234 or dealer2 / 1234");

        GridPane form = new GridPane();
        form.setVgap(14);
        form.setHgap(12);
        Label userLabel = new Label("👤 Username");
        Label passLabel = new Label("🔒 Password");
        form.addRow(0, userLabel);
        form.add(usernameField, 0, 1);
        form.addRow(2, passLabel);
        form.add(passwordStack, 0, 3);
        form.add(showPassword, 0, 4);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordStack, Priority.ALWAYS);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        passwordStack.setMaxWidth(Double.MAX_VALUE);
        form.setMaxWidth(320);

        VBox card = UiUtils.card(form, loginBtn, registerBtn, statusLabel, demoHint);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(380);

        getChildren().addAll(headerBox, card);
    }

    private void markError(boolean isError, TextField field) {
        if (isError) field.getStyleClass().add("input-error");
    }

    private void clearError(TextField... fields) {
        for (TextField f : fields) f.getStyleClass().remove("input-error");
    }

    private void openDashboard(User user) {
        if (user.isShop()) {
            stage.getScene().setRoot(new ShopDashboard(stage, user));
        } else {
            stage.getScene().setRoot(new DealerDashboard(stage, user));
        }
    }
}
