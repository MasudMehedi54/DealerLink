package com.dealerlink.dao;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    /** Places an order from an accepted quotation, and creates the initial delivery record. */
    public int placeOrder(int requestId, int quotationId) {
        String sql = "INSERT INTO orders (request_id, quotation_id, status) VALUES (?,?,'PLACED')";
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requestId);
            ps.setInt(2, quotationId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int orderId = keys.getInt(1);
                    createInitialDelivery(orderId);
                    return orderId;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    private void createInitialDelivery(int orderId) throws SQLException {
        String sql = "INSERT INTO deliveries (order_id, status, current_location) VALUES (?, 'PREPARING', 'Dealer Warehouse')";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    public List<Order> getOrdersForShop(int shopId) {
        String sql = """
            SELECT o.*, p.name AS product_name, q.price AS price, u.full_name AS dealer_name
            FROM orders o
            JOIN requests r ON r.id = o.request_id
            JOIN products p ON p.id = r.product_id
            JOIN quotations q ON q.id = o.quotation_id
            JOIN users u ON u.id = q.dealer_id
            WHERE r.shop_id = ?
            ORDER BY o.ordered_at DESC
            """;
        return query(sql, shopId);
    }

    public List<Order> getOrdersForDealer(int dealerId) {
        String sql = """
            SELECT o.*, p.name AS product_name, q.price AS price, u.full_name AS dealer_name
            FROM orders o
            JOIN requests r ON r.id = o.request_id
            JOIN products p ON p.id = r.product_id
            JOIN quotations q ON q.id = o.quotation_id
            JOIN users u ON u.id = q.dealer_id
            WHERE q.dealer_id = ?
            ORDER BY o.ordered_at DESC
            """;
        return query(sql, dealerId);
    }

    public boolean updateStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Order> query(String sql, int param) {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order(rs.getInt("id"), rs.getInt("request_id"), rs.getInt("quotation_id"),
                            rs.getString("status"), rs.getString("ordered_at"));
                    o.setProductName(rs.getString("product_name"));
                    o.setPrice(rs.getDouble("price"));
                    o.setDealerName(rs.getString("dealer_name"));
                    list.add(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
