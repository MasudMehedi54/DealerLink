package com.dealerlink.dao;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.model.ProductRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    public int createRequest(int shopId, int productId, int quantity) {
        String sql = "INSERT INTO requests (shop_id, product_id, quantity, status) VALUES (?,?,?,'OPEN')";
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, shopId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public List<ProductRequest> getRequestsByShop(int shopId) {
        String sql = """
            SELECT r.*, p.name AS product_name FROM requests r
            JOIN products p ON p.id = r.product_id
            WHERE r.shop_id = ? ORDER BY r.created_at DESC
            """;
        return query(sql, shopId);
    }

    /** All OPEN requests visible to dealers, so they can submit quotations. */
    public List<ProductRequest> getOpenRequests() {
        String sql = """
            SELECT r.*, p.name AS product_name FROM requests r
            JOIN products p ON p.id = r.product_id
            WHERE r.status = 'OPEN' ORDER BY r.created_at DESC
            """;
        return query(sql, null);
    }

    public boolean updateStatus(int requestId, String status) {
        String sql = "UPDATE requests SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ProductRequest> query(String sql, Integer param) {
        List<ProductRequest> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProductRequest(
                            rs.getInt("id"), rs.getInt("shop_id"), rs.getInt("product_id"),
                            rs.getString("product_name"), rs.getInt("quantity"),
                            rs.getString("status"), rs.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
