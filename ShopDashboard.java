package com.dealerlink.ui;

import com.dealerlink.dao.DeliveryDAO;
import com.dealerlink.dao.OrderDAO;
import com.dealerlink.dao.ProductDAO;
import com.dealerlink.dao.QuotationDAO;
import com.dealerlink.dao.RequestDAO;
import com.dealerlink.model.*;
import com.dealerlink.service.BackgroundTaskService;
import com.dealerlink.service.WeatherService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.util.Comparator;

public class ShopDashboard extends BorderPane {

    private final ProductDAO productDAO = new ProductDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final BackgroundTaskService bgService = new BackgroundTaskService();

    private final User currentUser;
    private final Label notificationBar = new Label();

    public ShopDashboard(Stage stage, User user) {
        this.currentUser = user;
        setStyle("-fx-background-color: #F1F5F9;");

        setTop(buildHeader(stage));

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("🔍  Search & Request", buildSearchTab()));
        tabs.getTabs().add(new Tab("📦  Requests & Quotations", buildRequestsTab()));
        tabs.getTabs().add(new Tab("🚚  Orders & Delivery", buildOrdersTab()));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        BorderPane.setMargin(tabs, new Insets(16, 20, 16, 20));
        setCenter(tabs);

        notificationBar.getStyleClass().add("notification-bar");
        notificationBar.setVisible(false);
        notificationBar.setManaged(false);
        BorderPane.setMargin(notificationBar, new Insets(0, 20, 16, 20));
        setBottom(notificationBar);

        // Background monitoring: polls order/delivery status every 10s off the UI thread,
        // and posts updates back via Platform.runLater inside BackgroundTaskService.
        bgService.setNotificationListener(msg -> {
            notificationBar.setText("🔔  " + msg);
            notificationBar.setVisible(true);
            notificationBar.setManaged(true);
        });
        bgService.startOrderMonitoring(user.getId(), 10);
    }

    private HBox buildHeader(Stage stage) {
        Label welcome = new Label("🏬  Welcome back, " + currentUser.getFullName());
        welcome.getStyleClass().add("header-title");
        Label roleChip = new Label("SHOP OWNER");
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

    // ---------- Tab 1: Search products and submit a request ----------
    private VBox buildSearchTab() {
        TextField searchField = new TextField();
        searchField.setPromptText("🔎  Search product name or category (e.g. rice, pharmacy)");
        Button searchBtn = UiUtils.primaryButton("Search");

        TableView<Product> table = new TableView<>();
        TableColumn<Product, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Product, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        table.getColumns().addAll(nameCol, catCol, unitCol);
        table.getStyleClass().add("table-view");
        table.setPlaceholder(new Label("No products found. Try a different search term."));
        table.setItems(FXCollections.observableArrayList(productDAO.getAllProducts()));

        Runnable doSearch = () ->
                table.setItems(FXCollections.observableArrayList(productDAO.search(searchField.getText().trim())));
        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run()); // Enter key also searches

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100000, 10);
        qtySpinner.setEditable(true);
        Button requestBtn = UiUtils.primaryButton("✅  Submit Request to Dealers");
        requestBtn.setDisable(true);
        Label status = new Label();

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> requestBtn.setDisable(sel == null));

        requestBtn.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.getStyleClass().setAll("error-text");
                status.setText("Select a product first.");
                return;
            }
            int reqId = requestDAO.createRequest(currentUser.getId(), selected.getId(), qtySpinner.getValue());
            if (reqId > 0) {
                status.getStyleClass().setAll("success-text");
                status.setText("✔ Request #" + reqId + " submitted. Check 'Requests & Quotations' for dealer offers.");
            } else {
                status.getStyleClass().setAll("error-text");
                status.setText("Failed to submit request.");
            }
        });

        HBox searchBar = new HBox(10, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox requestBar = new HBox(12, new Label("Quantity needed:"), qtySpinner, requestBtn);
        requestBar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14, UiUtils.sectionTitle("Browse Products"), searchBar, table,
                new Separator(), UiUtils.sectionTitle("Request a Product"), requestBar, status);
        box.setPadding(new Insets(20));
        return box;
    }

    // ---------- Tab 2: view own requests and compare dealer quotations ----------
    private VBox buildRequestsTab() {
        TableView<ProductRequest> reqTable = new TableView<>();
        reqTable.getStyleClass().add("table-view");
        TableColumn<ProductRequest, Number> idCol = new TableColumn<>("Req#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<ProductRequest, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<ProductRequest, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<ProductRequest, String> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(statCol);
        reqTable.getColumns().addAll(idCol, prodCol, qtyCol, statCol);
        reqTable.setPlaceholder(new Label("You haven't submitted any requests yet."));
        reqTable.setItems(FXCollections.observableArrayList(requestDAO.getRequestsByShop(currentUser.getId())));

        Button refreshBtn = UiUtils.secondaryButton("🔄  Refresh");
        refreshBtn.setOnAction(e ->
                reqTable.setItems(FXCollections.observableArrayList(requestDAO.getRequestsByShop(currentUser.getId()))));

        TableView<Quotation> quoteTable = new TableView<>();
        quoteTable.getStyleClass().add("table-view");
        quoteTable.setPlaceholder(new Label("Select a request above to see dealer quotations."));
        TableColumn<Quotation, String> dealerCol = new TableColumn<>("Dealer");
        dealerCol.setCellValueFactory(new PropertyValueFactory<>("dealerName"));
        TableColumn<Quotation, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        UiUtils.renderAsCurrency(priceCol);
        TableColumn<Quotation, Number> daysCol = new TableColumn<>("Delivery (days)");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("deliveryDays"));
        TableColumn<Quotation, String> qStatCol = new TableColumn<>("Status");
        qStatCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(qStatCol);
        quoteTable.getColumns().addAll(dealerCol, priceCol, daysCol, qStatCol);

        // Visually highlight the cheapest offer so comparing dealers is at-a-glance, not manual.
        quoteTable.setRowFactory(UiUtils.highlightRowFactory(q -> {
            ObservableList<Quotation> items = quoteTable.getItems();
            if (items == null || items.isEmpty()) return false;
            double min = items.stream().mapToDouble(Quotation::getPrice).min().orElse(Double.MAX_VALUE);
            return q.getPrice() == min;
        }, "best-price-row"));

        Label weatherLabel = UiUtils.hint("");
        Button weatherBtn = UiUtils.secondaryButton("☀️  Check Area Weather");

        reqTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                ObservableList<Quotation> quotes =
                        FXCollections.observableArrayList(quotationDAO.getQuotationsForRequest(sel.getId()));
                quotes.sort(Comparator.comparingDouble(Quotation::getPrice));
                quoteTable.setItems(quotes);
            }
        });

        Button acceptBtn = UiUtils.primaryButton("🏆  Accept Selected Quotation & Place Order");
        Label status = new Label();
        acceptBtn.setOnAction(e -> {
            Quotation q = quoteTable.getSelectionModel().getSelectedItem();
            ProductRequest r = reqTable.getSelectionModel().getSelectedItem();
            if (q == null || r == null) {
                status.getStyleClass().setAll("error-text");
                status.setText("Select a request and one of its quotations first.");
                return;
            }
            quotationDAO.updateStatus(q.getId(), "ACCEPTED");
            requestDAO.updateStatus(r.getId(), "ORDERED");
            int orderId = orderDAO.placeOrder(r.getId(), q.getId());
            if (orderId > 0) {
                status.getStyleClass().setAll("success-text");
                status.setText("✔ Order #" + orderId + " placed! Track it under 'Orders & Delivery'.");
            } else {
                status.getStyleClass().setAll("error-text");
                status.setText("Failed to place order.");
            }
        });

        weatherBtn.setOnAction(e -> {
            Quotation q = quoteTable.getSelectionModel().getSelectedItem();
            if (q == null) {
                weatherLabel.setText("Select a quotation first.");
                return;
            }
            weatherLabel.setText("Fetching weather…");
            // Background REST call via CompletableFuture; UI updated on completion via Platform.runLater.
            WeatherService.getCurrentWeatherAsync(currentUser.getLatitude(), currentUser.getLongitude())
                    .thenAccept(result -> Platform.runLater(() ->
                            weatherLabel.setText("☁️  Weather near your shop: " + result)));
        });

        HBox actionBar = new HBox(12, acceptBtn, weatherBtn);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14,
                UiUtils.sectionTitle("Your Requests"), refreshBtn, reqTable,
                new Separator(),
                UiUtils.sectionTitle("Dealer Quotations (cheapest highlighted)"), quoteTable,
                actionBar, weatherLabel, status);
        box.setPadding(new Insets(20));
        return box;
    }

    // ---------- Tab 3: orders and delivery tracking ----------
    private VBox buildOrdersTab() {
        TableView<Order> orderTable = new TableView<>();
        orderTable.getStyleClass().add("table-view");
        orderTable.setPlaceholder(new Label("You have no orders yet."));
        TableColumn<Order, Number> idCol = new TableColumn<>("Order#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Order, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Order, String> dealerCol = new TableColumn<>("Dealer");
        dealerCol.setCellValueFactory(new PropertyValueFactory<>("dealerName"));
        TableColumn<Order, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        UiUtils.renderAsCurrency(priceCol);
        TableColumn<Order, String> statusCol = new TableColumn<>("Order Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(statusCol);
        orderTable.getColumns().addAll(idCol, prodCol, dealerCol, priceCol, statusCol);

        Button refreshBtn = UiUtils.secondaryButton("🔄  Refresh");
        Label deliveryInfo = UiUtils.hint("Select an order above to see its live delivery status.");
        deliveryInfo.setWrapText(true);

        Runnable refresh = () ->
                orderTable.setItems(FXCollections.observableArrayList(orderDAO.getOrdersForShop(currentUser.getId())));
        refreshBtn.setOnAction(e -> refresh.run());
        refresh.run();

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                var delivery = deliveryDAO.getByOrderId(sel.getId());
                if (delivery != null) {
                    deliveryInfo.getStyleClass().setAll("section-title");
                    deliveryInfo.setText(String.format(
                            "🚚  %s   ·   📍 %s   ·   ETA: %s   ·   Last updated %s",
                            delivery.getStatus(), delivery.getCurrentLocation(),
                            delivery.getEta() == null || delivery.getEta().isBlank() ? "TBD" : delivery.getEta(),
                            delivery.getUpdatedAt()));
                }
            }
        });

        VBox box = new VBox(14, UiUtils.sectionTitle("Your Orders"), refreshBtn, orderTable, deliveryInfo);
        box.setPadding(new Insets(20));
        return box;
    }
}
