package com.dealerlink.dao;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.model.Quotation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuotationDAO {

    /**
     * Submits a bid from a dealer for a request. If this dealer already has a
     * quotation on this request, it's updated in place (a re-bid) instead of
     * creating a duplicate row - so a dealer can revise their price/delivery
     * time any time before the shop owner accepts an offer.
     */
    public int submitQuotation(int requestId, int dealerId, double price, int deliveryDays) {
        Quotation existing = getMyQuotation(requestId, dealerId);
        if (existing != null) {
            String sql = """
                UPDATE quotations SET price = ?, delivery_days = ?, status = 'PENDING', created_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setDouble(1, price);
                ps.setInt(2, deliveryDays);
                ps.setInt(3, existing.getId());
                ps.executeUpdate();
                return existing.getId();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        String sql = """
            INSERT INTO quotations (request_id, dealer_id, price, delivery_days, status)
            VALUES (?,?,?,?,'PENDING')
            """;
        Connection c = DatabaseManager.getConnection();
        synchronized (c) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, requestId);
                ps.setInt(2, dealerId);
                ps.setDouble(3, price);
                ps.setInt(4, deliveryDays);
                ps.executeUpdate();
                return DatabaseManager.lastInsertId();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** The current dealer's own bid on a request, or null if they haven't bid yet. */
    public Quotation getMyQuotation(int requestId, int dealerId) {
        String sql = """
            SELECT q.*, u.full_name AS dealer_name FROM quotations q
            JOIN users u ON u.id = q.dealer_id
            WHERE q.request_id = ? AND q.dealer_id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /** How many dealers have bid on this request so far (shown to the shop owner). */
    public int countForRequest(int requestId) {
        String sql = "SELECT COUNT(*) AS c FROM quotations WHERE request_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /** Marks every other still-pending bid on this request as REJECTED once one is accepted. */
    public boolean rejectOtherQuotations(int requestId, int acceptedQuotationId) {
        String sql = "UPDATE quotations SET status = 'REJECTED' WHERE request_id = ? AND id != ? AND status = 'PENDING'";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, acceptedQuotationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
