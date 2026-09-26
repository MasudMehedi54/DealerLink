package com.dealerlink.ui.controller;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.json.JsonDataService;
import com.dealerlink.model.User;
import com.dealerlink.ui.Navigator;
import com.dealerlink.ui.Responsive;
import javafx.beans.value.ObservableDoubleValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Controller for fxml/login.fxml. */
public class LoginController {

    @FXML private HBox root;
    @FXML private VBox brandPanel;
    @FXML private Label brandTitle;
    @FXML private Label brandTagline;
    @FXML private Label brandLogo;
    @FXML private VBox card;
    @FXML private Label cardTitle;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private CheckBox showPassword;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    /** Called automatically by FXMLLoader after all @FXML fields are injected. */
    @FXML
    private void initialize() {
        brandTitle.setText(JsonDataService.settings().getAppName());

        // Show/hide password: both fields share the same text; the checkbox picks which is visible.
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        passwordVisible.visibleProperty().bind(showPassword.selectedProperty());
        passwordVisible.managedProperty().bind(showPassword.selectedProperty());
        passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
        passwordField.managedProperty().bind(showPassword.selectedProperty().not());

        applyResponsiveLayout();
    }

    /**
     * Layout responsiveness - every size below is a property binding relative to the
     * window's width (w) or height (h). root fills the whole scene, so root.width == window width.
     */
    private void applyResponsiveLayout() {
        ObservableDoubleValue w = root.widthProperty();
        ObservableDoubleValue h = root.heightProperty();

        Responsive.bindPrefWidth(brandPanel, w, 0.42, 360, 640);   // brand panel = 42% of width
        Responsive.showWhenAtLeast(brandPanel, w, 960);             // hidden on narrow windows
        Responsive.bindWidth(card, w, 0.34, 340, 440);              // card = 34% of width
        Responsive.bindFontSize(brandTitle, w, 0.036, 30, 54);      // title grows with width
        Responsive.bindFontSize(brandLogo, w, 0.04, 34, 60);
        Responsive.bindFontSize(brandTagline, w, 0.0125, 14, 18);
        Responsive.bindFontSize(cardTitle, w, 0.02, 22, 28);
        Responsive.bindSpacing(card, h, 0.018, 8, 16);              // tighter on short windows
        Responsive.bindSpacing(brandPanel, h, 0.025, 10, 22);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        clearError(usernameField, passwordField, passwordVisible);

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            if (username.isEmpty()) markError(usernameField);
            if (password.isEmpty()) { markError(passwordField); markError(passwordVisible); }
            return;
        }
        User user = userDAO.login(username, password);
        if (user == null) {
            statusLabel.setText("Invalid username or password. Please try again.");
            markError(usernameField);
            markError(passwordField);
            markError(passwordVisible);
            return;
        }
        statusLabel.setText("");
        if (user.isShop()) {
            ShopDashboardController c = Navigator.show(Navigator.SHOP_DASHBOARD);
            c.init(user);
        } else {
            DealerDashboardController c = Navigator.show(Navigator.DEALER_DASHBOARD);
            c.init(user);
        }
    }

    @FXML
    private void goToRegister() {
        Navigator.show(Navigator.REGISTER);
    }

    private void markError(TextField f) {
        if (!f.getStyleClass().contains("input-error")) f.getStyleClass().add("input-error");
    }

    private void clearError(TextField... fields) {
        for (TextField f : fields) f.getStyleClass().remove("input-error");
    }
}
