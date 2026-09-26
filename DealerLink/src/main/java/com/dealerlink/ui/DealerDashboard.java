package com.dealerlink.ui;

import com.dealerlink.dao.*;
import com.dealerlink.model.*;
import com.dealerlink.service.BackgroundTaskService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DealerDashboard extends BorderPane {

    private final ProductDAO productDAO = new ProductDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final BackgroundTaskService bgService = new BackgroundTaskService();

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final User currentUser;
    private final Label notificationBar = new Label();

    public DealerDashboard(Stage stage, User user) {
        this.currentUser = user;
        setStyle("-fx-background-color: #F1F5F9;");

        setTop(buildHeader(stage));

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("📦  My Inventory", buildInventoryTab()));
        tabs.getTabs().add(new Tab("📥  Open Shop Requests", buildOpenRequestsTab()));
        tabs.getTabs().add(new Tab("🚚  Orders & Deliveries", buildOrdersTab()));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        BorderPane.setMargin(tabs, new Insets(16, 20, 16, 20));
        setCenter(tabs);

        notificationBar.getStyleClass().add("notification-bar");
        notificationBar.setVisible(false);
        notificationBar.setManaged(false);
        BorderPane.setMargin(notificationBar, new Insets(0, 20, 16, 20));
        setBottom(notificationBar);

        // Background thread: periodically checks this dealer's inventory for low-stock
        // items without ever touching the JavaFX thread directly (Platform.runLater inside).
        bgService.setNotificationListener(msg -> {
            notificationBar.setText("🔔  " + msg);
            notificationBar.setVisible(true);
            notificationBar.setManaged(true);
        });
        startInventoryWatch();
    }

    private HBox buildHeader(Stage stage) {
        Label welcome = new Label("🚚  Welcome back, " + currentUser.getFullName());
        welcome.getStyleClass().add("header-title");
        Label roleChip = new Label("DEALER");
        roleChip.getStyleClass().add("header-role-chip");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logout = UiUtils.withTooltip(UiUtils.secondaryButton("Logout"), "Sign out of DealerLink");
        logout.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.5);");
        logout.setOnAction(e -> {
            bgService.shutdown();
            stage.getScene().setRoot(new LoginScreen(stage));
        });

        HBox box = new HBox(14, welcome, roleChip, spacer, logout);
        box.getStyleClass().add("header-bar");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Runs a low-stock scan on a background thread every 30s; UI is only touched via the listener. */
    private void startInventoryWatch() {
        bgService.runAsync(this::checkLowStock); // one-off immediate scan at startup
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "inventory-watch");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::checkLowStock, 15, 30, TimeUnit.SECONDS);
    }

    private void checkLowStock() {
        try {
            var items = productDAO.getInventoryForDealer(currentUser.getId());
            for (Inventory inv : items) {
                if (inv.getQuantity() < LOW_STOCK_THRESHOLD) {
                    Platform.runLater(() -> {
                        notificationBar.setText("🔔  Low stock: " + inv.getProductName() + " (" + inv.getQuantity() + " left)");
                        notificationBar.setVisible(true);
                        notificationBar.setManaged(true);
                    });
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
        invTable.getStyleClass().add("table-view");
        invTable.setPlaceholder(new Label("You haven't added any stock yet. Use the form below."));
        TableColumn<Inventory, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Inventory, Number> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<Inventory, Number> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        UiUtils.renderAsCurrency(priceCol);
        invTable.getColumns().addAll(prodCol, qtyCol, priceCol);

        // Low-stock rows get a soft red highlight so they stand out immediately.
        invTable.setRowFactory(UiUtils.highlightRowFactory(
                inv -> inv.getQuantity() < LOW_STOCK_THRESHOLD, "low-stock-row"));

        Runnable refreshInv = () ->
                invTable.setItems(FXCollections.observableArrayList(productDAO.getInventoryForDealer(currentUser.getId())));
        refreshInv.run();

        Button refreshBtn = UiUtils.secondaryButton("🔄  Refresh");
        refreshBtn.setOnAction(e -> refreshInv.run());

        ComboBox<Product> productPicker = new ComboBox<>(FXCollections.observableArrayList(productDAO.getAllProducts()));
        productPicker.setPromptText("Select product");
        Spinner<Integer> qtySpinner = new Spinner<>(0, 1000000, 50);
        qtySpinner.setEditable(true);
        TextField priceField = new TextField();
        priceField.setPromptText("Unit price");
        Button saveBtn = UiUtils.primaryButton("💾  Add / Update Stock");
        Label status = new Label();

        saveBtn.setOnAction(e -> {
            priceField.getStyleClass().remove("input-error");
            Product p = productPicker.getValue();
            if (p == null) {
                status.getStyleClass().setAll("error-text");
                status.setText("Choose a product first.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
                if (price < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                priceField.getStyleClass().add("input-error");
                status.getStyleClass().setAll("error-text");
                status.setText("Enter a valid, non-negative price.");
                return;
            }
            boolean ok = productDAO.addOrUpdateInventory(currentUser.getId(), p.getId(), qtySpinner.getValue(), price);
            status.getStyleClass().setAll(ok ? "success-text" : "error-text");
            status.setText(ok ? "✔ Inventory updated for " + p.getName() + "." : "Update failed.");
            refreshInv.run();
        });

        HBox form = new HBox(12, productPicker, new Label("Qty:"), qtySpinner,
                new Label("Price:"), priceField, saveBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14, UiUtils.sectionTitle("Your Stock (low stock highlighted in red)"),
                refreshBtn, invTable, new Separator(),
                UiUtils.sectionTitle("Add or Update Stock"), form, status);
        box.setPadding(new Insets(20));
        return box;
    }

    // ---------- Tab 2: open requests from shop owners -> submit / update quotation ----------
    // A request stays visible here to EVERY dealer (it does not disappear once someone
    // bids) so all dealers can compete on price until the shop owner accepts one offer.
    // Bids are private: a dealer only ever sees their OWN bid, never competitors' prices.
    private VBox buildOpenRequestsTab() {
        TableView<ProductRequest> reqTable = new TableView<>();
        reqTable.getStyleClass().add("table-view");
        reqTable.setPlaceholder(new Label("No open requests from shop owners right now."));
        TableColumn<ProductRequest, Number> idCol = new TableColumn<>("Req#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<ProductRequest, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<ProductRequest, Number> qtyCol = new TableColumn<>("Qty Needed");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<ProductRequest, String> yourBidCol = new TableColumn<>("Your Bid");
        yourBidCol.setCellValueFactory(data -> {
            Quotation mine = quotationDAO.getMyQuotation(data.getValue().getId(), currentUser.getId());
            String text = mine == null ? "Not bid yet" : UiUtils.currency(mine.getPrice()) + " (" + mine.getStatus() + ")";
            return new javafx.beans.property.SimpleStringProperty(text);
        });
        reqTable.getColumns().addAll(idCol, prodCol, qtyCol, yourBidCol);

        Runnable refreshReq = () ->
                reqTable.setItems(FXCollections.observableArrayList(requestDAO.getOpenRequests()));
        refreshReq.run();

        Button refreshBtn = UiUtils.secondaryButton("🔄  Refresh");
        refreshBtn.setOnAction(e -> refreshReq.run());

        TextField priceField = new TextField();
        priceField.setPromptText("Your price");
        Spinner<Integer> daysSpinner = new Spinner<>(1, 60, 3);
        daysSpinner.setEditable(true);
        Button quoteBtn = UiUtils.primaryButton("📨  Submit Quotation");
        Label status = new Label();

        // Pre-fill the form with this dealer's existing bid when they select a request
        // they've already quoted, so it reads as "edit your bid" rather than starting blank.
        reqTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            priceField.getStyleClass().remove("input-error");
            if (sel == null) return;
            Quotation mine = quotationDAO.getMyQuotation(sel.getId(), currentUser.getId());
            if (mine != null) {
                priceField.setText(String.valueOf(mine.getPrice()));
                daysSpinner.getValueFactory().setValue(mine.getDeliveryDays());
                quoteBtn.setText("🔄  Update Your Bid");
            } else {
                priceField.clear();
                daysSpinner.getValueFactory().setValue(3);
                quoteBtn.setText("📨  Submit Quotation");
            }
        });

        quoteBtn.setOnAction(e -> {
            priceField.getStyleClass().remove("input-error");
            ProductRequest r = reqTable.getSelectionModel().getSelectedItem();
            if (r == null) {
                status.getStyleClass().setAll("error-text");
                status.setText("Select an open request first.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                priceField.getStyleClass().add("input-error");
                status.getStyleClass().setAll("error-text");
                status.setText("Enter a valid price greater than zero.");
                return;
            }
            boolean wasUpdate = quotationDAO.getMyQuotation(r.getId(), currentUser.getId()) != null;
            int qid = quotationDAO.submitQuotation(r.getId(), currentUser.getId(), price, daysSpinner.getValue());
            if (qid > 0) {
                // Note: the request itself stays OPEN here - it is NOT marked "quoted" -
                // so every other dealer can still see and bid on it too.
                status.getStyleClass().setAll("success-text");
                status.setText(wasUpdate
                        ? "✔ Your bid was updated. It's visible only to the shop owner."
                        : "✔ Bid #" + qid + " submitted. The shop owner will compare it with other dealers' bids.");
                refreshReq.run();
            } else {
                status.getStyleClass().setAll("error-text");
                status.setText("Failed to submit quotation.");
            }
        });

        HBox form = new HBox(12, new Label("Price:"), priceField, new Label("Delivery days:"), daysSpinner, quoteBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14, UiUtils.sectionTitle("Open Requests From Shop Owners (visible to all dealers)"),
                refreshBtn, reqTable, new Separator(),
                UiUtils.sectionTitle("Place or Update Your Bid"), form, status);
        box.setPadding(new Insets(20));
        return box;
    }

    // ---------- Tab 3: confirmed orders -> update delivery status ----------
    private VBox buildOrdersTab() {
        TableView<Order> orderTable = new TableView<>();
        orderTable.getStyleClass().add("table-view");
        orderTable.setPlaceholder(new Label("No orders yet."));
        TableColumn<Order, Number> idCol = new TableColumn<>("Order#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Order, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Order, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        UiUtils.renderAsCurrency(priceCol);
        TableColumn<Order, String> statusCol = new TableColumn<>("Order Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(statusCol);
        orderTable.getColumns().addAll(idCol, prodCol, priceCol, statusCol);

        Runnable refreshOrders = () ->
                orderTable.setItems(FXCollections.observableArrayList(orderDAO.getOrdersForDealer(currentUser.getId())));
        refreshOrders.run();

        Button refreshBtn = UiUtils.secondaryButton("🔄  Refresh");
        refreshBtn.setOnAction(e -> refreshOrders.run());

        ComboBox<String> orderStatusPicker = new ComboBox<>(
                FXCollections.observableArrayList("CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"));
        orderStatusPicker.setPromptText("New order status");
        Button confirmBtn = UiUtils.primaryButton("✔  Update Order Status");

        ComboBox<String> deliveryStatusPicker = new ComboBox<>(
                FXCollections.observableArrayList("PREPARING", "IN_TRANSIT", "DELIVERED"));
        deliveryStatusPicker.setPromptText("Delivery status");
        TextField locationField = new TextField();
        locationField.setPromptText("Current location");
        TextField etaField = new TextField();
        etaField.setPromptText("ETA (e.g. 2026-09-15)");
        Button updateDeliveryBtn = UiUtils.primaryButton("🚚  Update Delivery Info");

        Label status = new Label();

        confirmBtn.setOnAction(e -> {
            Order o = orderTable.getSelectionModel().getSelectedItem();
            String newStatus = orderStatusPicker.getValue();
            if (o == null || newStatus == null) {
                status.getStyleClass().setAll("error-text");
                status.setText("Select an order and a status first.");
                return;
            }
            boolean ok = orderDAO.updateStatus(o.getId(), newStatus);
            status.getStyleClass().setAll(ok ? "success-text" : "error-text");
            status.setText(ok ? "✔ Order #" + o.getId() + " updated to " + newStatus : "Update failed.");
            refreshOrders.run();
        });

        updateDeliveryBtn.setOnAction(e -> {
            Order o = orderTable.getSelectionModel().getSelectedItem();
            String dStatus = deliveryStatusPicker.getValue();
            if (o == null || dStatus == null) {
                status.getStyleClass().setAll("error-text");
                status.setText("Select an order and a delivery status first.");
                return;
            }
            boolean ok = deliveryDAO.updateDelivery(o.getId(), dStatus, etaField.getText().trim(), locationField.getText().trim());
            status.getStyleClass().setAll(ok ? "success-text" : "error-text");
            status.setText(ok ? "✔ Delivery info updated for Order #" + o.getId() : "Update failed.");
        });

        HBox orderForm = new HBox(12, orderStatusPicker, confirmBtn);
        orderForm.setAlignment(Pos.CENTER_LEFT);
        HBox deliveryForm = new HBox(12, deliveryStatusPicker, locationField, etaField, updateDeliveryBtn);
        deliveryForm.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14, UiUtils.sectionTitle("Your Confirmed Orders"), refreshBtn, orderTable,
                new Separator(), UiUtils.sectionTitle("Confirm / Update Order Status"), orderForm,
                new Separator(), UiUtils.sectionTitle("Update Delivery / Shipment Info"), deliveryForm,
                status);
        box.setPadding(new Insets(20));
        return box;
    }
}
