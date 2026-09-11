package com.dealerlink.ui;

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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ShopDashboard extends BorderPane {

    private final ProductDAO productDAO = new ProductDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final BackgroundTaskService bgService = new BackgroundTaskService();

    private final User currentUser;
    private final Label notificationBar = new Label();

    public ShopDashboard(Stage stage, User user) {
        this.currentUser = user;
        setPadding(new Insets(10));

        setTop(buildHeader(stage));

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Search & Request Products", buildSearchTab()));
        tabs.getTabs().add(new Tab("My Requests & Quotations", buildRequestsTab()));
        tabs.getTabs().add(new Tab("My Orders & Delivery", buildOrdersTab()));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        setCenter(tabs);

        setBottom(notificationBar);
        notificationBar.setStyle("-fx-text-fill: #2e7d32; -fx-padding: 6;");

        // Background monitoring: polls order/delivery status every 10s off the UI thread,
        // and posts updates back via Platform.runLater inside BackgroundTaskService.
        bgService.setNotificationListener(msg -> notificationBar.setText("🔔 " + msg));
        bgService.startOrderMonitoring(user.getId(), 10);
    }

    private HBox buildHeader(Stage stage) {
        Label welcome = new Label("Welcome, " + currentUser.getFullName() + " (Shop Owner)");
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

    // ---------- Tab 1: Search products and submit a request ----------
    private VBox buildSearchTab() {
        TextField searchField = new TextField();
        searchField.setPromptText("Search product name or category (e.g. rice, pharmacy)");
        Button searchBtn = new Button("Search");

        TableView<Product> table = new TableView<>();
        TableColumn<Product, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Product, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        table.getColumns().addAll(nameCol, catCol, unitCol);
        table.setItems(FXCollections.observableArrayList(productDAO.getAllProducts()));

        searchBtn.setOnAction(e ->
                table.setItems(FXCollections.observableArrayList(productDAO.search(searchField.getText().trim()))));

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100000, 10);
        qtySpinner.setEditable(true);
        Button requestBtn = new Button("Submit Request to Dealers");
        Label status = new Label();

        requestBtn.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.setText("Select a product first.");
                return;
            }
            int reqId = requestDAO.createRequest(currentUser.getId(), selected.getId(), qtySpinner.getValue());
            status.setText(reqId > 0
                    ? "Request submitted (#" + reqId + "). Check the 'My Requests' tab for dealer quotations."
                    : "Failed to submit request.");
        });

        HBox searchBar = new HBox(10, searchField, searchBtn);
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);
        HBox requestBar = new HBox(10, new Label("Quantity:"), qtySpinner, requestBtn);
        requestBar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, searchBar, table, requestBar, status);
        box.setPadding(new Insets(10));
        return box;
    }

    // ---------- Tab 2: view own requests and compare dealer quotations ----------
    private VBox buildRequestsTab() {
        TableView<ProductRequest> reqTable = new TableView<>();
        TableColumn<ProductRequest, Number> idCol = new TableColumn<>("Req#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<ProductRequest, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<ProductRequest, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<ProductRequest, String> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        reqTable.getColumns().addAll(idCol, prodCol, qtyCol, statCol);
        reqTable.setItems(FXCollections.observableArrayList(requestDAO.getRequestsByShop(currentUser.getId())));

        Button refreshBtn = new Button("Refresh My Requests");
        refreshBtn.setOnAction(e ->
                reqTable.setItems(FXCollections.observableArrayList(requestDAO.getRequestsByShop(currentUser.getId()))));

        TableView<Quotation> quoteTable = new TableView<>();
        TableColumn<Quotation, String> dealerCol = new TableColumn<>("Dealer");
        dealerCol.setCellValueFactory(new PropertyValueFactory<>("dealerName"));
        TableColumn<Quotation, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Quotation, Number> daysCol = new TableColumn<>("Delivery (days)");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("deliveryDays"));
        TableColumn<Quotation, String> qStatCol = new TableColumn<>("Status");
        qStatCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        quoteTable.getColumns().addAll(dealerCol, priceCol, daysCol, qStatCol);

        Label weatherLabel = new Label();
        Button weatherBtn = new Button("Check Dealer Area Weather");

        reqTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                ObservableList<Quotation> quotes =
                        FXCollections.observableArrayList(quotationDAO.getQuotationsForRequest(sel.getId()));
                quoteTable.setItems(quotes);
            }
        });

        Button acceptBtn = new Button("Accept Selected Quotation & Place Order");
        Label status = new Label();
        acceptBtn.setOnAction(e -> {
            Quotation q = quoteTable.getSelectionModel().getSelectedItem();
            ProductRequest r = reqTable.getSelectionModel().getSelectedItem();
            if (q == null || r == null) {
                status.setText("Select a request and one of its quotations first.");
                return;
            }
            quotationDAO.updateStatus(q.getId(), "ACCEPTED");
            requestDAO.updateStatus(r.getId(), "ORDERED");
            int orderId = orderDAO.placeOrder(r.getId(), q.getId());
            status.setText(orderId > 0
                    ? "Order #" + orderId + " placed! Track it in 'My Orders & Delivery'."
                    : "Failed to place order.");
        });

        weatherBtn.setOnAction(e -> {
            Quotation q = quoteTable.getSelectionModel().getSelectedItem();
            if (q == null) {
                weatherLabel.setText("Select a quotation first.");
                return;
            }
            weatherLabel.setText("Fetching weather...");
            // Background REST call via CompletableFuture; UI updated on completion (already on FX thread here).
            WeatherService.getCurrentWeatherAsync(currentUser.getLatitude(), currentUser.getLongitude())
                    .thenAccept(result -> Platform.runLater(() ->
                            weatherLabel.setText("Weather near your shop: " + result)));
        });

        VBox box = new VBox(10,
                new Label("Your Requests:"), refreshBtn, reqTable,
                new Label("Dealer Quotations for Selected Request:"), quoteTable,
                new HBox(10, acceptBtn, weatherBtn), weatherLabel, status);
        box.setPadding(new Insets(10));
        return box;
    }

    // ---------- Tab 3: orders and delivery tracking ----------
    private VBox buildOrdersTab() {
        TableView<Order> orderTable = new TableView<>();
        TableColumn<Order, Number> idCol = new TableColumn<>("Order#");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Order, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Order, String> dealerCol = new TableColumn<>("Dealer");
        dealerCol.setCellValueFactory(new PropertyValueFactory<>("dealerName"));
        TableColumn<Order, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Order, String> statusCol = new TableColumn<>("Order Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderTable.getColumns().addAll(idCol, prodCol, dealerCol, priceCol, statusCol);

        Button refreshBtn = new Button("Refresh My Orders");
        Label deliveryInfo = new Label("Select an order to see delivery status.");

        Runnable refresh = () ->
                orderTable.setItems(FXCollections.observableArrayList(orderDAO.getOrdersForShop(currentUser.getId())));
        refreshBtn.setOnAction(e -> refresh.run());
        refresh.run();

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                var delivery = new com.dealerlink.dao.DeliveryDAO().getByOrderId(sel.getId());
                if (delivery != null) {
                    deliveryInfo.setText(String.format(
                            "Delivery status: %s | Current location: %s | ETA: %s | Updated: %s",
                            delivery.getStatus(), delivery.getCurrentLocation(),
                            delivery.getEta() == null ? "TBD" : delivery.getEta(),
                            delivery.getUpdatedAt()));
                }
            }
        });

        VBox box = new VBox(10, refreshBtn, orderTable, deliveryInfo);
        box.setPadding(new Insets(10));
        return box;
    }
}
