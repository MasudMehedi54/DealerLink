package com.dealerlink.service;

import com.dealerlink.dao.DeliveryDAO;
import com.dealerlink.dao.OrderDAO;
import com.dealerlink.model.Order;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs periodic background jobs (order/delivery status monitoring, low-inventory
 * notifications) on a separate thread pool so the JavaFX UI thread never blocks.
 * UI updates are always marshalled back via Platform.runLater(...).
 */
public class BackgroundTaskService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();

    public interface NotificationListener {
        void onNotification(String message);
    }

    private NotificationListener listener;

    public void setNotificationListener(NotificationListener listener) {
        this.listener = listener;
    }

    /** Polls order/delivery status for a shop every N seconds, off the UI thread. */
    public void startOrderMonitoring(int shopId, int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Order> orders = orderDAO.getOrdersForShop(shopId);
                for (Order o : orders) {
                    var delivery = deliveryDAO.getByOrderId(o.getId());
                    if (delivery != null && "IN_TRANSIT".equals(delivery.getStatus())) {
                        notify("Order #" + o.getId() + " (" + o.getProductName() +
                                ") is in transit, currently at " + delivery.getCurrentLocation());
                    }
                }
            } catch (Exception ex) {
                notify("Background monitoring error: " + ex.getMessage());
            }
        }, 5, intervalSeconds, TimeUnit.SECONDS);
    }

    /** Runs an arbitrary background job (e.g. inventory check) off the UI thread. */
    public void runAsync(Runnable job) {
        scheduler.execute(job);
    }

    private void notify(String message) {
        if (listener != null) {
            Platform.runLater(() -> listener.onNotification(message));
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
