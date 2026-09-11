package com.dealerlink.ui;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LoginScreen extends VBox {

    private final UserDAO userDAO = new UserDAO();
    private final Stage stage;

    public LoginScreen(Stage stage) {
        this.stage = stage;
        setSpacing(15);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("DealerLink");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        Label subtitle = new Label("Connecting shop owners with product dealers");
        subtitle.setStyle("-fx-text-fill: gray;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");

        Button loginBtn = new Button("Login");
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> {
            User user = userDAO.login(usernameField.getText().trim(), passwordField.getText());
            if (user == null) {
                statusLabel.setText("Invalid username or password.");
                return;
            }
            openDashboard(user);
        });

        Button registerBtn = new Button("Create a new account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> stage.getScene().setRoot(new RegisterScreen(stage)));

        Label demoHint = new Label(
                "Demo accounts -> Shop: shop1 / 1234   |   Dealer: dealer1 / 1234 or dealer2 / 1234");
        demoHint.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        form.setMaxWidth(320);
        form.addRow(0, new Label("Username:"), usernameField);
        form.addRow(1, new Label("Password:"), passwordField);

        VBox card = new VBox(12, form, loginBtn, registerBtn, statusLabel, demoHint);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(360);

        getChildren().addAll(title, subtitle, card);
    }

    private void openDashboard(User user) {
        if (user.isShop()) {
            stage.getScene().setRoot(new ShopDashboard(stage, user));
        } else {
            stage.getScene().setRoot(new DealerDashboard(stage, user));
        }
    }
}
