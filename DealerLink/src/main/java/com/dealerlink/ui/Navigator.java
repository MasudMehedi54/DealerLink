package com.dealerlink.ui;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/**
 * Loads the .fxml screens (src/main/resources/fxml/) and swaps them into the one
 * window. Each FXML file declares its own controller (fx:controller) and its own
 * stylesheet (stylesheets="@../css/theme.css"), so the same files open with full
 * styling in Scene Builder.
 */
public final class Navigator {

    public static final String LOGIN = "login.fxml";
    public static final String REGISTER = "register.fxml";
    public static final String SHOP_DASHBOARD = "shop-dashboard.fxml";
    public static final String DEALER_DASHBOARD = "dealer-dashboard.fxml";

    private static Stage stage;

    private Navigator() {}

    public static void setStage(Stage primaryStage) { stage = primaryStage; }

    public static Stage stage() { return stage; }

    /** Loads an FXML screen, shows it, and returns its controller (e.g. to pass the logged-in user). */
    public static <C> C show(String fxmlName) {
        FXMLLoader loader = loader(fxmlName);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load /fxml/" + fxmlName, e);
        }
        if (stage.getScene() == null) {
            // First window size is RELATIVE to the user's screen: 80% x 85%, within sane limits.
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double w = clamp(screen.getWidth() * 0.80, 920, 1500);
            double h = clamp(screen.getHeight() * 0.85, 640, 960);
            stage.setScene(new Scene(root, w, h));
        } else {
            stage.getScene().setRoot(root);
        }
        return loader.getController();
    }

    public static FXMLLoader loader(String fxmlName) {
        URL url = Navigator.class.getResource("/fxml/" + fxmlName);
        if (url == null) throw new IllegalStateException("Missing FXML: /fxml/" + fxmlName);
        return new FXMLLoader(url);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
