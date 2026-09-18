package com.dealerlink.dao;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.model.Quotation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuotationDAO {

    public int submitQuotation(int requestId, int dealerId, double price, int deliveryDays) {
        String sql = """
            INSERT INTO quotations (request_id, dealer_id, price, delivery_days, status)
            VALUES (?,?,?,?,'PENDING')
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requestId);
            ps.setInt(2, dealerId);
            ps.setDouble(3, price);
            ps.setInt(4, deliveryDays);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    /** Used by the shop owner to compare all dealer offers for one request. */
    public List<Quotation> getQuotationsForRequest(int requestId) {
        String sql = """
            SELECT q.*, u.full_name AS dealer_name FROM quotations q
            JOIN users u ON u.id = q.dealer_id
            WHERE q.request_id = ? ORDER BY q.price ASC
            """;
        List<Quotation> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Quotation getById(int quotationId) {
        String sql = """
            SELECT q.*, u.full_name AS dealer_name FROM quotations q
            JOIN users u ON u.id = q.dealer_id
            WHERE q.id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, quotationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean updateStatus(int quotationId, String status) {
        String sql = "UPDATE quotations SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, quotationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Quotation map(ResultSet rs) throws SQLException {
        return new Quotation(rs.getInt("id"), rs.getInt("request_id"), rs.getInt("dealer_id"),
                rs.getString("dealer_name"), rs.getDouble("price"), rs.getInt("delivery_days"),
                rs.getString("status"), rs.getString("created_at"));
    }
}
