package com.dealerlink.ui.controller;

import com.dealerlink.dao.*;
import com.dealerlink.json.JsonDataService;
import com.dealerlink.model.*;
import com.dealerlink.service.BackgroundTaskService;
import com.dealerlink.ui.Navigator;
import com.dealerlink.ui.Responsive;
import com.dealerlink.ui.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

/** Controller for fxml/dealer-dashboard.fxml (dealer). */
public class DealerDashboardController {

    @FXML private BorderPane root;
    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label roleChip;
    @FXML private Label notificationBar;

    // Tab 1: inventory
    @FXML private TableView<Inventory> inventoryTable;
    @FXML private TableColumn<Inventory, String> invProductCol;
    @FXML private TableColumn<Inventory, Number> invQtyCol;
    @FXML private TableColumn<Inventory, Number> invPriceCol;
    @FXML private ComboBox<Product> productPicker;
    @FXML private Spinner<Integer> stockQtySpinner;
    @FXML private TextField stockPriceField;
    @FXML private Label inventoryStatus;

    // Tab 2: open requests
    @FXML private TableView<ProductRequest> openRequestTable;
    @FXML private TableColumn<ProductRequest, Number> openIdCol;
    @FXML private TableColumn<ProductRequest, String> openProductCol;
    @FXML private TableColumn<ProductRequest, Number> openQtyCol;
    @FXML private TableColumn<ProductRequest, String> openBidCol;
    @FXML private TextField bidPriceField;
    @FXML private Spinner<Integer> bidDaysSpinner;
    @FXML private Button bidButton;
    @FXML private Label bidStatus;

    // Tab 3: orders
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Number> orderIdCol;
    @FXML private TableColumn<Order, String> orderProductCol;
    @FXML private TableColumn<Order, Number> orderPriceCol;
    @FXML private TableColumn<Order, String> orderStatusCol;
    @FXML private ComboBox<String> orderStatusPicker;
    @FXML private ComboBox<String> deliveryStatusPicker;
    @FXML private TextField locationField;
    @FXML private TextField etaField;
    @FXML private Label orderStatusLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final BackgroundTaskService bgService = new BackgroundTaskService();

    /** From "settings.lowStockThreshold" in data/dealerlink-data.json. */
    private final int lowStockThreshold = JsonDataService.settings().getLowStockThreshold();

    private User currentUser;

    @FXML
    private void initialize() {
        setupTables();
        stockQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 50));
        bidDaysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 3));
        orderStatusPicker.setItems(FXCollections.observableArrayList("CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"));
        deliveryStatusPicker.setItems(FXCollections.observableArrayList("PREPARING", "IN_TRANSIT", "DELIVERED"));
        productPicker.setItems(FXCollections.observableArrayList(productDAO.getAllProducts()));

        // Pre-fill the bid form with this dealer's existing bid ("edit your bid").
        openRequestTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            bidPriceField.getStyleClass().remove("input-error");
            if (sel == null || currentUser == null) return;
            Quotation mine = quotationDAO.getMyQuotation(sel.getId(), currentUser.getId());
            if (mine != null) {
                bidPriceField.setText(String.valueOf(mine.getPrice()));
                bidDaysSpinner.getValueFactory().setValue(mine.getDeliveryDays());
                bidButton.setText("🔄  Update Your Bid");
            } else {
                bidPriceField.clear();
                bidDaysSpinner.getValueFactory().setValue(3);
                bidButton.setText("📨  Submit Quotation");
            }
        });

        applyResponsiveLayout();
    }

    /** Called by LoginController after loading the FXML. */
    public void init(User user) {
        this.currentUser = user;
        welcomeLabel.setText("🚚  Welcome back, " + user.getFullName());
        refreshInventory();
        refreshOpenRequests();
        refreshOrders();

        bgService.setNotificationListener(this::notifyUser);
        bgService.runAsync(this::checkLowStock);
        int every = JsonDataService.settings().getInventoryCheckIntervalSeconds();
        bgService.schedule(this::checkLowStock, every, every);
    }

    // =====================================================================
    // Layout responsiveness: property bindings relative to window size
    // =====================================================================
    private void applyResponsiveLayout() {
        ObservableDoubleValue w = root.widthProperty();
        ObservableDoubleValue h = root.heightProperty();

        Responsive.bindFontSize(welcomeLabel, w, 0.0155, 16, 22);
        Responsive.showWhenAtLeast(subtitleLabel, w, 980);
        Responsive.showWhenAtLeast(roleChip, w, 760);

        Responsive.bindMinHeight(inventoryTable, h, 0.30, 150, 460);
        Responsive.bindMinHeight(openRequestTable, h, 0.30, 150, 460);
        Responsive.bindMinHeight(orderTable, h, 0.22, 120, 380);

        Responsive.bindPrefWidth(productPicker, w, 0.20, 190, 320);
        Responsive.bindPrefWidth(stockPriceField, w, 0.12, 110, 200);
        Responsive.bindPrefWidth(bidPriceField, w, 0.12, 110, 200);
        Responsive.bindPrefWidth(locationField, w, 0.17, 150, 280);
        Responsive.bindPrefWidth(etaField, w, 0.14, 140, 240);
        Responsive.bindPrefWidth(orderStatusPicker, w, 0.16, 170, 260);
        Responsive.bindPrefWidth(deliveryStatusPicker, w, 0.14, 160, 240);
    }

    private void setupTables() {
        for (TableView<?> t : new TableView<?>[]{inventoryTable, openRequestTable, orderTable}) {
            UiUtils.polishTable(t);
        }
        inventoryTable.setPlaceholder(new Label("You haven't added any stock yet. Use the form below."));
        invProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        invQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        invPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        UiUtils.renderAsCurrency(invPriceCol);
        inventoryTable.setRowFactory(UiUtils.highlightRowFactory(
                inv -> inv.getQuantity() < lowStockThreshold, "low-stock-row"));

        openRequestTable.setPlaceholder(new Label("No open requests from shop owners right now."));
        openIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        openProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        openQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        openBidCol.setCellValueFactory(d -> {
            if (currentUser == null) return new SimpleStringProperty("");
            Quotation mine = quotationDAO.getMyQuotation(d.getValue().getId(), currentUser.getId());
            return new SimpleStringProperty(mine == null ? "Not bid yet"
                    : UiUtils.currency(mine.getPrice()) + "  (" + mine.getStatus() + ")");
        });

        orderTable.setPlaceholder(new Label("No orders yet."));
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        orderPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        UiUtils.renderAsCurrency(orderPriceCol);
        orderStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiUtils.renderAsBadge(orderStatusCol);
    }

    // =====================================================================
    // Background low-stock notification
    // =====================================================================
    private void checkLowStock() {
        try {
            for (Inventory inv : productDAO.getInventoryForDealer(currentUser.getId())) {
                if (inv.getQuantity() < lowStockThreshold) {
                    Platform.runLater(() -> notifyUser(
                            "Low stock: " + inv.getProductName() + " (" + inv.getQuantity() + " left)"));
                    break;
                }
            }
        } catch (Exception ignored) {
            // dealer may not have inventory yet
        }
    }

    private void notifyUser(String msg) {
        notificationBar.setText("🔔  " + msg);
        notificationBar.setVisible(true);
        notificationBar.setManaged(true);
    }

    // =====================================================================
    // Tab 1: inventory
    // =====================================================================
    @FXML
    private void refreshInventory() {
        inventoryTable.setItems(FXCollections.observableArrayList(productDAO.getInventoryForDealer(currentUser.getId())));
    }

    @FXML
    private void handleSaveStock() {
        stockPriceField.getStyleClass().remove("input-error");
        Product p = productPicker.getValue();
        if (p == null) {
            UiUtils.setStatus(inventoryStatus, false, "Choose a product first.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(stockPriceField.getText().trim());
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            stockPriceField.getStyleClass().add("input-error");
            UiUtils.setStatus(inventoryStatus, false, "Enter a valid, non-negative price.");
            return;
        }
        boolean ok = productDAO.addOrUpdateInventory(currentUser.getId(), p.getId(), stockQtySpinner.getValue(), price);
        UiUtils.setStatus(inventoryStatus, ok, ok ? "✔ Inventory updated for " + p.getName() + "." : "Update failed.");
        refreshInventory();
    }

    // =====================================================================
    // Tab 2: open requests -> bid
    // =====================================================================
    @FXML
    private void refreshOpenRequests() {
        openRequestTable.setItems(FXCollections.observableArrayList(requestDAO.getOpenRequests()));
    }

    @FXML
    private void handleSubmitBid() {
        bidPriceField.getStyleClass().remove("input-error");
        ProductRequest r = openRequestTable.getSelectionModel().getSelectedItem();
        if (r == null) {
            UiUtils.setStatus(bidStatus, false, "Select an open request first.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(bidPriceField.getText().trim());
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            bidPriceField.getStyleClass().add("input-error");
            UiUtils.setStatus(bidStatus, false, "Enter a valid price greater than zero.");
            return;
        }
        boolean wasUpdate = quotationDAO.getMyQuotation(r.getId(), currentUser.getId()) != null;
        int qid = quotationDAO.submitQuotation(r.getId(), currentUser.getId(), price, bidDaysSpinner.getValue());
        if (qid > 0) {
            UiUtils.setStatus(bidStatus, true, wasUpdate
                    ? "✔ Your bid was updated. It's visible only to the shop owner."
                    : "✔ Bid #" + qid + " submitted. The shop owner will compare it with other dealers' bids.");
            refreshOpenRequests();
        } else {
            UiUtils.setStatus(bidStatus, false, "Failed to submit quotation.");
        }
    }

    // =====================================================================
    // Tab 3: orders & delivery
    // =====================================================================
    @FXML
    private void refreshOrders() {
        orderTable.setItems(FXCollections.observableArrayList(orderDAO.getOrdersForDealer(currentUser.getId())));
    }

    @FXML
    private void handleUpdateOrderStatus() {
        Order o = orderTable.getSelectionModel().getSelectedItem();
        String newStatus = orderStatusPicker.getValue();
        if (o == null || newStatus == null) {
            UiUtils.setStatus(orderStatusLabel, false, "Select an order and a status first.");
            return;
        }
        boolean ok = orderDAO.updateStatus(o.getId(), newStatus);
        UiUtils.setStatus(orderStatusLabel, ok, ok ? "✔ Order #" + o.getId() + " updated to " + newStatus : "Update failed.");
        refreshOrders();
    }

    @FXML
    private void handleUpdateDelivery() {
        Order o = orderTable.getSelectionModel().getSelectedItem();
        String dStatus = deliveryStatusPicker.getValue();
        if (o == null || dStatus == null) {
            UiUtils.setStatus(orderStatusLabel, false, "Select an order and a delivery status first.");
            return;
        }
        boolean ok = deliveryDAO.updateDelivery(o.getId(), dStatus, etaField.getText().trim(), locationField.getText().trim());
        UiUtils.setStatus(orderStatusLabel, ok, ok ? "✔ Delivery info updated for Order #" + o.getId() : "Update failed.");
    }

    @FXML
    private void handleExportOrders() {
        UiUtils.exportJson(root.getScene().getWindow(), "dealer-orders.json", orderTable.getItems(), orderStatusLabel);
    }

    @FXML
    private void handleLogout() {
        bgService.shutdown();
        Navigator.show(Navigator.LOGIN);
    }
}
