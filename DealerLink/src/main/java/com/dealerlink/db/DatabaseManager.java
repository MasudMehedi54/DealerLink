package com.dealerlink.db;

import com.dealerlink.json.InventorySeed;
import com.dealerlink.json.JsonDataService;
import com.dealerlink.json.SeedData;
import com.dealerlink.model.Product;
import com.dealerlink.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central place for the SQLite connection and schema creation.
 * A single physical file "dealerlink.db" is created next to the jar / project root.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:dealerlink.db";

    // One shared connection is fine for a desktop SQLite app.
    private static Connection connection;

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(DB_URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON;");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to connect to SQLite database", e);
        }
        return connection;
    }

    /** Creates all tables if they do not already exist, then seeds data from the external JSON file. */
    public static void initializeDatabase() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('SHOP','DEALER')),
                full_name TEXT,
                location TEXT,
                latitude REAL,
                longitude REAL
            );
            """;

        String products = """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT,
                unit TEXT DEFAULT 'pcs'
            );
            """;

        String inventory = """
            CREATE TABLE IF NOT EXISTS inventory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dealer_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 0,
                unit_price REAL NOT NULL DEFAULT 0,
                FOREIGN KEY(dealer_id) REFERENCES users(id),
                FOREIGN KEY(product_id) REFERENCES products(id)
            );
            """;

        String requests = """
            CREATE TABLE IF NOT EXISTS requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                shop_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'OPEN',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(shop_id) REFERENCES users(id),
                FOREIGN KEY(product_id) REFERENCES products(id)
            );
            """;

        String quotations = """
            CREATE TABLE IF NOT EXISTS quotations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                request_id INTEGER NOT NULL,
                dealer_id INTEGER NOT NULL,
                price REAL NOT NULL,
                delivery_days INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(request_id) REFERENCES requests(id),
                FOREIGN KEY(dealer_id) REFERENCES users(id)
            );
            """;

        String orders = """
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                request_id INTEGER NOT NULL,
                quotation_id INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'PLACED',
                ordered_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(request_id) REFERENCES requests(id),
                FOREIGN KEY(quotation_id) REFERENCES quotations(id)
            );
            """;

        String deliveries = """
            CREATE TABLE IF NOT EXISTS deliveries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'PREPARING',
                eta TEXT,
                current_location TEXT,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(order_id) REFERENCES orders(id)
            );
            """;

        try (Statement st = getConnection().createStatement()) {
            st.execute(users);
            st.execute(products);
            st.execute(inventory);
            st.execute(requests);
            st.execute(quotations);
            st.execute(orders);
            st.execute(deliveries);
            seedFromJson();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Seeds the database from the EXTERNAL JSON file (data/dealerlink-data.json),
     * parsed by Jackson in {@link JsonDataService}. Instead of hard-coded SQL with
     * fixed ids, every record is inserted only if it doesn't exist yet - so you can
     * add a new product or dealer to the JSON file and it appears on next start,
     * without wiping the existing database.
     */
    private static void seedFromJson() throws SQLException {
        SeedData data = JsonDataService.load();
        Connection c = getConnection();

        for (User u : data.getUsers()) {
            if (idOf(c, "SELECT id FROM users WHERE username = ?", u.getUsername()) > 0) continue;
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO users (username, password, role, full_name, location, latitude, longitude)
                    VALUES (?,?,?,?,?,?,?)""")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getPassword());
                ps.setString(3, u.getRole() == null ? "SHOP" : u.getRole().toUpperCase());
                ps.setString(4, u.getFullName());
                ps.setString(5, u.getLocation());
                ps.setDouble(6, u.getLatitude());
                ps.setDouble(7, u.getLongitude());
                ps.executeUpdate();
            }
        }

        for (Product p : data.getProducts()) {
            if (idOf(c, "SELECT id FROM products WHERE name = ?", p.getName()) > 0) continue;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO products (name, category, unit) VALUES (?,?,?)")) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getCategory());
                ps.setString(3, p.getUnit() == null ? "pcs" : p.getUnit());
                ps.executeUpdate();
            }
        }

        for (InventorySeed inv : data.getInventory()) {
            int dealerId = idOf(c, "SELECT id FROM users WHERE username = ? AND role = 'DEALER'", inv.getDealerUsername());
            int productId = idOf(c, "SELECT id FROM products WHERE name = ?", inv.getProductName());
            if (dealerId <= 0 || productId <= 0) {
                System.err.println("[DealerLink] Skipping inventory row: unknown dealer '" + inv.getDealerUsername()
                        + "' or product '" + inv.getProductName() + "'");
                continue;
            }
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT id FROM inventory WHERE dealer_id = ? AND product_id = ?")) {
                check.setInt(1, dealerId);
                check.setInt(2, productId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) continue; // never overwrite stock the dealer has edited in the app
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO inventory (dealer_id, product_id, quantity, unit_price) VALUES (?,?,?,?)")) {
                ps.setInt(1, dealerId);
                ps.setInt(2, productId);
                ps.setInt(3, inv.getQuantity());
                ps.setDouble(4, inv.getUnitPrice());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Id of the row just inserted on the shared connection. Used instead of
     * Statement.getGeneratedKeys(), which newer sqlite-jdbc versions no longer
     * support (it throws SQLFeatureNotSupportedException). Callers must be
     * synchronized on the connection so no other insert sneaks in between.
     */
    public static int lastInsertId() throws SQLException {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /** Returns the id from a single-parameter lookup query, or -1 if no row matches. */
    private static int idOf(Connection c, String sql, String param) throws SQLException {
        if (param == null) return -1;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }
}
