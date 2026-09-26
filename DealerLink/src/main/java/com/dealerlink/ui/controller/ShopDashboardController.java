package com.dealerlink.ui.controller;

import com.dealerlink.dao.*;
import com.dealerlink.json.JsonDataService;
import com.dealerlink.model.*;
import com.dealerlink.service.BackgroundTaskService;
import com.dealerlink.service.WeatherService;
import com.dealerlink.ui.Navigator;
import com.dealerlink.ui.Responsive;
import com.dealerlink.ui.UiUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/** Controller for fxml/shop-dashboard.fxml (shop owner). */
public class ShopDashboardController {

    // ---------- FXML-injected nodes ----------
    @FXML private BorderPane root;
    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label roleChip;
    @FXML private Label notificationBar;

    // Tab 1: search & request
    @FXML private TextField searchField;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productNameCol;
    @FXML private TableColumn<Product, String> productCategoryCol;
    @FXML private TableColumn<Product, String> productUnitCol;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private Button requestButton;
    @FXML private Label searchStatus;

    // Tab 2: requests & quotations
    @FXML private SplitPane requestsSplit;
    @FXML private TableView<ProductRequest> requestTable;
    @FXML private TableColumn<ProductRequest, Number> reqIdCol;
    @FXML private TableColumn<ProductRequest, String> reqProductCol;
    @FXML private TableColumn<ProductRequest, Number> reqQtyCol;
    @FXML private TableColumn<ProductRequest, String> reqStatusCol;
    @FXML private TableColumn<ProductRequest, Number> reqQuotesCol;
    @FXML private TableView<Quotation> quoteTable;
    @FXML private TableColumn<Quotation, String> quoteDealerCol;
    @FXML private TableColumn<Quotation, Number> quotePriceCol;
    @FXML private TableColumn<Quotation, Number> quoteDaysCol;
    @FXML private TableColumn<Quotation, String> quoteStatusCol;
    @FXML private Button weatherButton;
    @FXML private Label weatherLabel;
    @FXML private Label requestsStatus;

    // Tab 3: orders
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Number> orderIdCol;
    @FXML private TableColumn<Order, String> orderProductCol;
    @FXML private TableColumn<Order, String> orderDealerCol;
    @FXML private TableColumn<Order, Number> orderPriceCol;
    @FXML private TableColumn<Order, String> orderStatusCol;
    @FXML private Label exportStatus;
    @FXML private Label deliveryInfo;

    // ---------- Data access / services ----------
    private final ProductDAO productDAO = new ProductDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final UserDAO userDAO = new UserDAO();
    private final BackgroundTaskService bgService = new BackgroundTaskService();

    private User currentUser;

    /** Runs right after the FXML is loaded: wire columns, renderers, listeners, responsiveness. */
    @FXML
    private void initialize() {
        setupTables();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 10));

        productTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> requestButton.setDisable(sel == null));
        requestTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> showQuotations(sel));
        orderTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> showDelivery(sel));

        // Weather box is only visible when it has something to say.
        weatherLabel.visibleProperty().bind(weatherLabel.textProperty().isNotEmpty());
        weatherLabel.managedProperty().bind(weatherLabel.visibleProperty());

        applyResponsiveLayout();
    }

    /** Called by LoginController after loading the FXML - gives this screen its user. */
    public void init(User user) {
        this.currentUser = user;
        welcomeLabel.setText("🏬  Welcome back, " + user.getFullName());
        productTable.setItems(FXCollections.observableArrayList(productDAO.getAllProducts()));
        refreshRequests();
        refreshOrders();

        bgService.setNotificationListener(msg -> {
            notificationBar.setText("🔔  " + msg);
            notificationBar.setVisible(true);
            notificationBar.setManaged(true);
        });
        bgService.startOrderMonitoring(user.getId(), JsonDataService.settings().getOrderMonitorIntervalSeconds());
    }

    // =====================================================================
    // Layout responsiveness: property bindings relative to window size
    // =====================================================================
    private void applyResponsiveLayout() {
        ObservableDoubleValue w = root.widthProperty();   // == window width
        ObservableDoubleValue h = root.heightProperty();  // == window height

        // Header: title scales with width; secondary items drop out on narrow windows.
        Responsive.bindFontSize(welcomeLabel, w, 0.0155, 16, 22);
        Responsive.showWhenAtLeast(subtitleLabel, w, 980);
        Responsive.showWhenAtLeast(roleChip, w, 760);

        // Requests & quotations: side by side on wide windows, stacked on narrow ones.
        requestsSplit.orientationProperty().bind(
                Bindings.when(Bindings.greaterThanOrEqual(w, 1250))
                        .then(Orientation.HORIZONTAL)
                        .otherwise(Orientation.VERTICAL));

        // Tables: minimum height is a fraction of the window height.
        Responsive.bindMinHeight(productTable, h, 0.28, 150, 420);
        Responsive.bindMinHeight(requestTable, h, 0.16, 110, 300);
        Responsive.bindMinHeight(quoteTable, h, 0.16, 110, 300);
        Responsive.bindMinHeight(orderTable, h, 0.30, 150, 460);

        // Inputs: widths are a fraction of the window width.
        Responsive.bindPrefWidth(qtySpinner, w, 0.11, 110, 170);
    }

    // =====================================================================
    // Table setup
    // =====================================================================
    private void setupTables() {
        for (TableView<?> t : new TableView<?>[]{productTable, requestTable, quoteTable, orderTable}) {
            UiUtils.polishTable(t);
        }
        productTable.setPlaceholder(new Label("No products found. Try a different search term."));
        productNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        productCategoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        productUnitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));

        requestTable.setPlaceholder(new Label("You haven't submitted any requests yet."));
        reqIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        reqProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        reqQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        reqStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(reqStatusCol);
        reqQuotesCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(quotationDAO.countForRequest(d.getValue().getId())));

        quoteTable.setPlaceholder(new Label("Select a request to see dealer quotations."));
        quoteDealerCol.setCellValueFactory(new PropertyValueFactory<>("dealerName"));
        quotePriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        UiUtils.renderAsCurrency(quotePriceCol);
        quoteDaysCol.setCellValueFactory(new PropertyValueFactory<>("deliveryDays"));
        quoteStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(quoteStatusCol);
        // Cheapest offer highlighted so comparing dealers is at-a-glance.
        quoteTable.setRowFactory(UiUtils.highlightRowFactory(q -> {
            ObservableList<Quotation> items = quoteTable.getItems();
            if (items == null || items.isEmpty()) return false;
            double min = items.stream().mapToDouble(Quotation::getPrice).min().orElse(Double.MAX_VALUE);
            return q.getPrice() == min;
        }, "best-price-row"));

        orderTable.setPlaceholder(new Label("You have no orders yet."));
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        orderDealerCol.setCellValueFactory(new PropertyValueFactory<>("dealerName"));
        orderPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        UiUtils.renderAsCurrency(orderPriceCol);
        orderStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(orderStatusCol);
    }

    // =====================================================================
    // Tab 1: search & request
    // =====================================================================
    @FXML
    private void handleSearch() {
        productTable.setItems(FXCollections.observableArrayList(productDAO.search(searchField.getText().trim())));
    }

    @FXML
    private void handleSubmitRequest() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.setStatus(searchStatus, false, "Select a product first.");
            return;
        }
        int reqId = requestDAO.createRequest(currentUser.getId(), selected.getId(), qtySpinner.getValue());
        if (reqId > 0) {
            UiUtils.setStatus(searchStatus, true, "✔ Request #" + reqId
                    + " submitted. Check 'Requests & Quotations' for dealer offers.");
            refreshRequests();
        } else {
            UiUtils.setStatus(searchStatus, false, "Failed to submit request.");
        }
    }

    // =====================================================================
    // Tab 2: requests & quotations
    // =====================================================================
    @FXML
    private void refreshRequests() {
        requestTable.setItems(FXCollections.observableArrayList(requestDAO.getRequestsByShop(currentUser.getId())));
        quoteTable.getItems().clear();
    }

    private void showQuotations(ProductRequest request) {
        if (request == null) return;
        ObservableList<Quotation> quotes =
                FXCollections.observableArrayList(quotationDAO.getQuotationsForRequest(request.getId()));
        quotes.sort(Comparator.comparingDouble(Quotation::getPrice));
        quoteTable.setItems(quotes);
    }

    @FXML
    private void handleAcceptQuotation() {
        Quotation q = quoteTable.getSelectionModel().getSelectedItem();
        ProductRequest r = requestTable.getSelectionModel().getSelectedItem();
        if (q == null || r == null) {
            UiUtils.setStatus(requestsStatus, false, "Select a request and one of its quotations first.");
            return;
        }
        if (!"OPEN".equalsIgnoreCase(r.getStatus())) {
            UiUtils.setStatus(requestsStatus, false, "Request #" + r.getId() + " is already " + r.getStatus() + ".");
            return;
        }
        quotationDAO.updateStatus(q.getId(), "ACCEPTED");
        quotationDAO.rejectOtherQuotations(r.getId(), q.getId());
        requestDAO.updateStatus(r.getId(), "ORDERED");
        int orderId = orderDAO.placeOrder(r.getId(), q.getId());
        if (orderId > 0) {
            UiUtils.setStatus(requestsStatus, true, "✔ Order #" + orderId + " placed with "
                    + q.getDealerName() + "! Track it under 'Orders & Delivery'.");
            refreshRequests();
            refreshOrders();
        } else {
            UiUtils.setStatus(requestsStatus, false, "Failed to place order.");
        }
    }

    /** Weather at both ends of the delivery route; runs off the UI thread. */
    @FXML
    private void handleCheckWeather() {
        Quotation q = quoteTable.getSelectionModel().getSelectedItem();
        if (q == null) {
            weatherLabel.setText("Select a quotation first to compare weather at your shop and the dealer.");
            return;
        }
        weatherLabel.setText("Fetching weather…");
        weatherButton.setDisable(true);
        User dealer = userDAO.getById(q.getDealerId());
        CompletableFuture<String> shopWeather = WeatherService.getWeatherForUserAsync(currentUser);
        CompletableFuture<String> dealerWeather = dealer == null
                ? CompletableFuture.completedFuture("dealer not found")
                : WeatherService.getWeatherForUserAsync(dealer);
        shopWeather.thenCombine(dealerWeather, (s, d) ->
                        "☁  Your shop (" + currentUser.getLocation() + "): " + s
                        + "\n🚚  " + q.getDealerName() + " (" + (dealer == null ? "?" : dealer.getLocation()) + "): " + d)
                .whenComplete((text, err) -> Platform.runLater(() -> {
                    weatherButton.setDisable(false);
                    weatherLabel.setText(err != null ? "Weather unavailable: " + err.getMessage() : text);
                }));
    }

    // =====================================================================
    // Tab 3: orders
    // =====================================================================
    @FXML
    private void refreshOrders() {
        orderTable.setItems(FXCollections.observableArrayList(orderDAO.getOrdersForShop(currentUser.getId())));
    }

    private void showDelivery(Order order) {
        if (order == null) return;
        Delivery d = deliveryDAO.getByOrderId(order.getId());
        if (d == null) return;
        deliveryInfo.setText(String.format("🚚  %s   ·   📍 %s   ·   ETA: %s   ·   Last updated %s",
                d.getStatus().replace("_", " "), d.getCurrentLocation(),
                d.getEta() == null || d.getEta().isBlank() ? "TBD" : d.getEta(), d.getUpdatedAt()));
    }

    @FXML
    private void handleExportOrders() {
        UiUtils.exportJson(root.getScene().getWindow(), "my-orders.json", orderTable.getItems(), exportStatus);
    }

    // =====================================================================
    @FXML
    private void handleLogout() {
        bgService.shutdown();
        Navigator.show(Navigator.LOGIN);
    }
}
