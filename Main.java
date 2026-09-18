package com.dealerlink;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.ui.LoginScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create tables / seed demo data on first run (runs quickly, fine on UI thread at startup).
        DatabaseManager.initializeDatabase();

        primaryStage.setTitle("DealerLink - B2B Shop-Dealer Platform");
        Scene scene = new Scene(new LoginScreen(primaryStage), 1000, 680);

        // Global theme: color palette + fonts + component styles for every screen.
        // Because Login/Register/Dashboards only swap the scene's ROOT (see each
        // screen's "back"/"logout" actions), this one stylesheet automatically
        // applies everywhere without needing to be re-added per screen.
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Any global cleanup (thread pools, connections) can go here.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
