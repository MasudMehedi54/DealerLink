package com.dealerlink.dao;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.model.Delivery;

import java.sql.*;

public class DeliveryDAO {

    public Delivery getByOrderId(int orderId) {
        String sql = "SELECT * FROM deliveries WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean updateDelivery(int orderId, String status, String eta, String currentLocation) {
        String sql = """
            UPDATE deliveries SET status = ?, eta = ?, current_location = ?, updated_at = CURRENT_TIMESTAMP
            WHERE order_id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, eta);
            ps.setString(3, currentLocation);
            ps.setInt(4, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Delivery map(ResultSet rs) throws SQLException {
        return new Delivery(rs.getInt("id"), rs.getInt("order_id"), rs.getString("status"),
                rs.getString("eta"), rs.getString("current_location"), rs.getString("updated_at"));
    }
}
