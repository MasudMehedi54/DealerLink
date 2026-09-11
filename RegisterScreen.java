package com.dealerlink.ui;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class RegisterScreen extends VBox {

    private final UserDAO userDAO = new UserDAO();

    public RegisterScreen(Stage stage) {
        setSpacing(15);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Create Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        TextField username = new TextField();
        PasswordField password = new PasswordField();
        TextField fullName = new TextField();
        TextField location = new TextField();
        location.setPromptText("City, e.g. Khulna");
        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("SHOP", "DEALER");
        role.setValue("SHOP");

        Label status = new Label();
        status.setStyle("-fx-text-fill: red;");

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        form.addRow(0, new Label("Username:"), username);
        form.addRow(1, new Label("Password:"), password);
        form.addRow(2, new Label("Full Name / Shop:"), fullName);
        form.addRow(3, new Label("Location:"), location);
        form.addRow(4, new Label("Role:"), role);

        Button createBtn = new Button("Register");
        createBtn.setOnAction(e -> {
            if (username.getText().isBlank() || password.getText().isBlank()) {
                status.setText("Username and password are required.");
                return;
            }
            // Latitude/longitude default to 0 here; a real deployment would geocode
            // the typed location via a REST API (see WeatherService for the HTTP pattern).
            User newUser = new User(0, username.getText().trim(), password.getText(),
                    role.getValue(), fullName.getText().trim(), location.getText().trim(), 0, 0);
            boolean ok = userDAO.register(newUser);
            if (ok) {
                stage.getScene().setRoot(new LoginScreen(stage));
            } else {
                status.setText("Registration failed - username may already exist.");
            }
        });

        Button backBtn = new Button("Back to Login");
        backBtn.setOnAction(e -> stage.getScene().setRoot(new LoginScreen(stage)));

        getChildren().addAll(title, form, createBtn, backBtn, status);
    }
}
