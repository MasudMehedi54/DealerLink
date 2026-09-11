package com.dealerlink.ui;

import com.dealerlink.dao.*;
import com.dealerlink.model.*;
import com.dealerlink.service.BackgroundTaskService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class DealerDashboard extends BorderPane {

    private final ProductDAO productDAO = new ProductDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final BackgroundTaskService bgService = new BackgroundTaskService();

    private final User currentUser;
    private final Label notificationBar = new Label();

    public DealerDashboard(Stage stage, User user) {
        this.currentUser = user;
        setPadding(new Insets(10));

        setTop(buildHeader(stage));

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("My Inventory", buildInventoryTab()));
        tabs.getTabs().add(new Tab("Open Shop Requests", buildOpenRequestsTab()));
        tabs.getTabs().add(new Tab("My Orders & Deliveries", buildOrdersTab()));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        setCenter(tabs);

        setBottom(notificationBar);
        notificationBar.setStyle("-fx-text-fill: #2e7d32; -fx-padding: 6;");

        // Background thread: periodically checks this dealer's inventory for low-stock
        // items without ever touching the JavaFX thread directly (Platform.runLater inside).
        bgService.setNotificationListener(msg -> notificationBar.setText("🔔 " + msg));
        startInventoryWatch();
    }

    private HBox buildHeader(Stage stage) {
        Label welcome = new Label("Welcome, " + currentUser.getFullName() + " (Dealer)");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 16));
        Button logout = new Button("Logout");
        logout.setOnAction(e -> {
            bgService.shutdown();
            stage.getScene().setRoot(new LoginScreen(stage));
        });
        HBox box = new HBox(20, welcome, logout);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 10, 0));
        HBox.setHgrow(welcome, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    /** Runs a low-stock scan on a background thread every 15s; UI is only touched via the listener. */
    private void startInventoryWatch() {
        bgService.runAsync(() -> {
            // one-off immediate scan at startup
            checkLowStock();
        });
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "inventory-watch");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::checkLowStock, 15, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void checkLowStock() {
        try {
            var items = productDAO.getInventoryForDealer(currentUser.getId());
            for (Inventory inv : items) {
                if (inv.getQuantity() < 10) {
                    javafx.application.Platform.runLater(() ->
                            notificationBar.setText("🔔 Low stock: " + inv.getProductName() + " (" + inv.getQuantity() + " left)"));
                    break;
                }
            }
        } catch (Exception ignored) {
            // dealer may not have inventory yet
        }
    }

    // ---------- Tab 1: manage inventory ----------
    private VBox buildInventoryTab() {
        TableView<Inventory> invTable = new TableView<>();
        TableColumn<Inventory, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Inventory, Number> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<Inventory, Number> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        invTable.getColumns().addAll(prodCol, qtyCol, priceCol);

        Runnable refreshInv = () ->
                invTable.setItems(FXCollections.observableArrayList(productDAO.getInventoryForDealer(currentUser.getId())));
        refreshInv.run();

        Button refreshBtn = new Button("Refresh Inventory");
        refreshBtn.setOnAction(e -> refreshInv.run());

        ComboBox<Product> productPicker = new ComboBox<>(FXCollections.observableArrayList(productDAO.getAllProducts()));
        productPicker.setPromptText("Select product");
        Spinner<Integer> qtySpinner = new Spinner<>(0, 1000000, 50);
        qtySpinner.setEditable(true);
        TextField priceField = new TextField();
        priceField.setPromptText("Unit price");
        Button saveBtn = new Button("Add / Update Stock");
        Label status = new Label();

        saveBtn.setOnAction(e -> {
            Product p = productPicker.getValue();
            if (p == null) {
                status.setText("Choose a product first.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                status.setText("Enter a valid price.");
                return;
            }
            boolean ok = productDAO.addOrUpdateInventory(currentUser.getId(), p.getId(), qtySpinner.getValue(), price);
            status.setText(ok ? "Inventory updated." : "Update failed.");
            refreshInv.run();
        });

        HBox form = new HBox(10, productPicker, new Label("Qty:"), qtySpinner,
                new Label("Price:"), priceField, saveBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, refreshBtn, invTable, new Separator(), new Label("Add or Update Stock:"), form, status);
        box.setPadding(new Insets(10));
        return box;
    }

    // ---------- Tab 2: open requests from shop owners -> submit quotation ----------
    private VBox buildOpenRequestsTab() {
        TableView<ProductRequest> reqTable = new TableView<>();
        TableColumn<ProductRequest, Number> idCol = new TableColumn<>("Req#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<ProductRequest, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<ProductRequest, Number> qtyCol = new TableColumn<>("Qty Needed");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<ProductRequest, String> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        reqTable.getColumns().addAll(idCol, prodCol, qtyCol, statCol);

        Runnable refreshReq = () ->
                reqTable.setItems(FXCollections.observableArrayList(requestDAO.getOpenRequests()));
        refreshReq.run();

        Button refreshBtn = new Button("Refresh Open Requests");
        refreshBtn.setOnAction(e -> refreshReq.run());

        TextField priceField = new TextField();
        priceField.setPromptText("Your price");
        Spinner<Integer> daysSpinner = new Spinner<>(1, 60, 3);
        daysSpinner.setEditable(true);
        Button quoteBtn = new Button("Submit Quotation");
        Label status = new Label();

        quoteBtn.setOnAction(e -> {
            ProductRequest r = reqTable.getSelectionModel().getSelectedItem();
            if (r == null) {
                status.setText("Select an open request first.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                status.setText("Enter a valid price.");
                return;
            }
            int qid = quotationDAO.submitQuotation(r.getId(), currentUser.getId(), price, daysSpinner.getValue());
            if (qid > 0) {
                requestDAO.updateStatus(r.getId(), "QUOTED");
                status.setText("Quotation #" + qid + " submitted to the shop owner.");
                refreshReq.run();
            } else {
                status.setText("Failed to submit quotation.");
            }
        });

        HBox form = new HBox(10, new Label("Price:"), priceField, new Label("Delivery days:"), daysSpinner, quoteBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, refreshBtn, reqTable, new Separator(), new Label("Quote This Request:"), form, status);
        box.setPadding(new Insets(10));
        return box;
    }

    // ---------- Tab 3: confirmed orders -> update delivery status ----------
    private VBox buildOrdersTab() {
        TableView<Order> orderTable = new TableView<>();
        TableColumn<Order, Number> idCol = new TableColumn<>("Order#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Order, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Order, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Order, String> statusCol = new TableColumn<>("Order Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderTable.getColumns().addAll(idCol, prodCol, priceCol, statusCol);

        Runnable refreshOrders = () ->
                orderTable.setItems(FXCollections.observableArrayList(orderDAO.getOrdersForDealer(currentUser.getId())));
        refreshOrders.run();

        Button refreshBtn = new Button("Refresh My Orders");
        refreshBtn.setOnAction(e -> refreshOrders.run());

        ComboBox<String> orderStatusPicker = new ComboBox<>(
                FXCollections.observableArrayList("CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"));
        orderStatusPicker.setPromptText("New order status");
        Button confirmBtn = new Button("Update Order Status");

        ComboBox<String> deliveryStatusPicker = new ComboBox<>(
                FXCollections.observableArrayList("PREPARING", "IN_TRANSIT", "DELIVERED"));
        deliveryStatusPicker.setPromptText("Delivery status");
        TextField locationField = new TextField();
        locationField.setPromptText("Current location");
        TextField etaField = new TextField();
        etaField.setPromptText("ETA (e.g. 2026-09-15)");
        Button updateDeliveryBtn = new Button("Update Delivery Info");

        Label status = new Label();

        confirmBtn.setOnAction(e -> {
            Order o = orderTable.getSelectionModel().getSelectedItem();
            String newStatus = orderStatusPicker.getValue();
            if (o == null || newStatus == null) {
                status.setText("Select an order and a status first.");
                return;
            }
            boolean ok = orderDAO.updateStatus(o.getId(), newStatus);
            status.setText(ok ? "Order #" + o.getId() + " updated to " + newStatus : "Update failed.");
            refreshOrders.run();
        });

        updateDeliveryBtn.setOnAction(e -> {
            Order o = orderTable.getSelectionModel().getSelectedItem();
            String dStatus = deliveryStatusPicker.getValue();
            if (o == null || dStatus == null) {
                status.setText("Select an order and a delivery status first.");
                return;
            }
            boolean ok = deliveryDAO.updateDelivery(o.getId(), dStatus, etaField.getText().trim(), locationField.getText().trim());
            status.setText(ok ? "Delivery info updated for Order #" + o.getId() : "Update failed.");
        });

        HBox orderForm = new HBox(10, orderStatusPicker, confirmBtn);
        orderForm.setAlignment(Pos.CENTER_LEFT);
        HBox deliveryForm = new HBox(10, deliveryStatusPicker, locationField, etaField, updateDeliveryBtn);
        deliveryForm.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, refreshBtn, orderTable,
                new Separator(), new Label("Confirm / Update Order:"), orderForm,
                new Separator(), new Label("Update Delivery / Shipment:"), deliveryForm,
                status);
        box.setPadding(new Insets(10));
        return box;
    }
}
