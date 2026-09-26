package com.dealerlink;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.json.JsonDataService;
import com.dealerlink.ui.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point. The UI is defined in FXML files (src/main/resources/fxml/*.fxml),
 * each with its own controller in com.dealerlink.ui.controller - open them in
 * Scene Builder to edit the layout visually.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create tables, then seed users/products/inventory from the external
        // JSON file data/dealerlink-data.json (parsed with Jackson).
        DatabaseManager.initializeDatabase();

        primaryStage.setTitle(JsonDataService.settings().getAppName() + " - B2B Shop-Dealer Platform");
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(600);

        Navigator.setStage(primaryStage);
        Navigator.show(Navigator.LOGIN);   // window size = 80% x 85% of the screen (see Navigator)
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Background services use daemon threads, so the JVM exits cleanly on close.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
