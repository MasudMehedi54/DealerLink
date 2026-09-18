package com.dealerlink.dao;

import com.dealerlink.db.DatabaseManager;
import com.dealerlink.model.Inventory;
import com.dealerlink.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Product(rs.getInt("id"), rs.getString("name"),
                        rs.getString("category"), rs.getString("unit")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /** Search products by name/category (used by shop owners). */
    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE name LIKE ? OR category LIKE ? ORDER BY name";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Product(rs.getInt("id"), rs.getString("name"),
                            rs.getString("category"), rs.getString("unit")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /** All dealer inventory rows offering a given product (used to compare dealers). */
    public List<Inventory> getInventoryForProduct(int productId) {
        List<Inventory> list = new ArrayList<>();
        String sql = """
            SELECT i.*, p.name AS product_name FROM inventory i
            JOIN products p ON p.id = i.product_id
            WHERE i.product_id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Inventory> getInventoryForDealer(int dealerId) {
        List<Inventory> list = new ArrayList<>();
        String sql = """
            SELECT i.*, p.name AS product_name FROM inventory i
            JOIN products p ON p.id = i.product_id
            WHERE i.dealer_id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public boolean updateInventoryQuantity(int inventoryId, int newQuantity) {
        String sql = "UPDATE inventory SET quantity = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setInt(2, inventoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addOrUpdateInventory(int dealerId, int productId, int quantity, double price) {
        String check = "SELECT id FROM inventory WHERE dealer_id = ? AND product_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(check)) {
            ps.setInt(1, dealerId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String upd = "UPDATE inventory SET quantity = ?, unit_price = ? WHERE id = ?";
                    try (PreparedStatement ups = DatabaseManager.getConnection().prepareStatement(upd)) {
                        ups.setInt(1, quantity);
                        ups.setDouble(2, price);
                        ups.setInt(3, rs.getInt("id"));
                        return ups.executeUpdate() > 0;
                    }
                } else {
                    String ins = "INSERT INTO inventory (dealer_id, product_id, quantity, unit_price) VALUES (?,?,?,?)";
                    try (PreparedStatement ips = DatabaseManager.getConnection().prepareStatement(ins)) {
                        ips.setInt(1, dealerId);
                        ips.setInt(2, productId);
                        ips.setInt(3, quantity);
                        ips.setDouble(4, price);
                        return ips.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Inventory mapInventory(ResultSet rs) throws SQLException {
        return new Inventory(rs.getInt("id"), rs.getInt("dealer_id"), rs.getInt("product_id"),
                rs.getString("product_name"), rs.getInt("quantity"), rs.getDouble("unit_price"));
    }
}
