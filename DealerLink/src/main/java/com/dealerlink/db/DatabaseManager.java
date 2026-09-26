package com.dealerlink.db;

import java.sql.Connection;
import java.sql.DriverManager;
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

    /** Creates all tables if they do not already exist, and seeds a couple of demo rows. */
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
            seedDemoData(st);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private static void seedDemoData(Statement st) throws SQLException {
        // Only seed once (check if users table is empty)
        var rs = st.executeQuery("SELECT COUNT(*) AS c FROM users");
        int count = rs.next() ? rs.getInt("c") : 0;
        if (count > 0) return;

        st.execute("""
            INSERT INTO users (username, password, role, full_name, location, latitude, longitude) VALUES
            ('shop1', '1234', 'SHOP', 'City Grocery Mart', 'Khulna', 22.8456, 89.5403),
            ('dealer1', '1234', 'DEALER', 'Northern Distributors', 'Khulna', 22.8100, 89.5600),
            ('dealer2', '1234', 'DEALER', 'Sundarban Traders', 'Jashore', 23.1667, 89.2083)
            """);

        st.execute("""
            INSERT INTO products (name, category, unit) VALUES
            ('Rice (Miniket) 25kg', 'Grocery', 'bag'),
            ('Cooking Oil 5L', 'Grocery', 'can'),
            ('Paracetamol 500mg', 'Pharmacy', 'box'),
            ('LED Bulb 9W', 'Electronics', 'pcs')
            """);

        st.execute("""
            INSERT INTO inventory (dealer_id, product_id, quantity, unit_price) VALUES
            (2, 1, 100, 1450.00),
            (2, 2, 60, 850.00),
            (3, 1, 40, 1420.00),
            (3, 3, 200, 45.00),
            (3, 4, 500, 120.00)
            """);
    }
}
