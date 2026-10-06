package com.cth.database;

import com.cth.security.PasswordUtil;
import java.sql.*;

public class DatabaseInitializer {
    
    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Check if database already has tables
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "users", null);
            
            if (tables.next()) {
                // Database already exists - check if it has data
                ResultSet countCheck = stmt.executeQuery("SELECT COUNT(*) FROM products");
                int productCount = countCheck.getInt(1);
                
                if (productCount == 0) {
                    // Products table is empty, insert default data
                    System.out.println("📦 Products table is empty. Inserting default data...");
                    insertDefaultProducts(conn);
                }
                
                System.out.println("✅ Database already initialized.");
                return;
            }
            
            // First time setup - create all tables
            System.out.println("🔄 Creating database tables...");
            createTables(stmt);
            insertDefaultProducts(conn);
            
            System.out.println("✅ Database initialized successfully.");
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }
    
    private static void createTables(Statement stmt) throws SQLException {
        // Users table
        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "username TEXT UNIQUE NOT NULL," +
                     "password_hash TEXT NOT NULL," +
                     "full_name TEXT NOT NULL," +
                     "role TEXT CHECK(role IN ('admin','cashier')) NOT NULL," +
                     "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                     "last_login DATETIME," +
                     "is_active INTEGER DEFAULT 1," +
                     "failed_attempts INTEGER DEFAULT 0," +
                     "locked_until DATETIME)");
        
        // Products table
        stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "barcode TEXT UNIQUE NOT NULL," +
                     "name TEXT NOT NULL," +
                     "category TEXT NOT NULL," +
                     "cost_price DECIMAL(10,2) NOT NULL," +
                     "selling_price DECIMAL(10,2) NOT NULL," +
                     "quantity INTEGER DEFAULT 0," +
                     "low_stock_threshold INTEGER DEFAULT 5," +
                     "supplier TEXT," +
                     "is_active INTEGER DEFAULT 1," +
                     "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                     "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        
        // Sales table
        stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "invoice_number TEXT UNIQUE NOT NULL," +
                     "user_id INTEGER NOT NULL," +
                     "customer_name TEXT," +
                     "customer_phone TEXT," +
                     "subtotal DECIMAL(10,2) NOT NULL," +
                     "vat_amount DECIMAL(10,2) NOT NULL," +
                     "discount DECIMAL(10,2) DEFAULT 0," +
                     "total_amount DECIMAL(10,2) NOT NULL," +
                     "payment_method TEXT CHECK(payment_method IN ('cash','card','eft')) NOT NULL," +
                     "sale_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                     "voided INTEGER DEFAULT 0," +
                     "voided_by INTEGER," +
                     "voided_at DATETIME," +
                     "FOREIGN KEY(user_id) REFERENCES users(id)," +
                     "FOREIGN KEY(voided_by) REFERENCES users(id))");
        
        // Sale items table
        stmt.execute("CREATE TABLE IF NOT EXISTS sale_items (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "sale_id INTEGER NOT NULL," +
                     "product_id INTEGER NOT NULL," +
                     "quantity INTEGER NOT NULL," +
                     "unit_price DECIMAL(10,2) NOT NULL," +
                     "subtotal DECIMAL(10,2) NOT NULL," +
                     "FOREIGN KEY(sale_id) REFERENCES sales(id) ON DELETE CASCADE," +
                     "FOREIGN KEY(product_id) REFERENCES products(id))");
        
        // Audit log table
        stmt.execute("CREATE TABLE IF NOT EXISTS audit_log (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "user_id INTEGER," +
                     "username TEXT," +
                     "action TEXT NOT NULL," +
                     "details TEXT," +
                     "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                     "ip_address TEXT," +
                     "FOREIGN KEY(user_id) REFERENCES users(id))");
        
        // Create indexes for performance
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_category ON products(category)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_invoice ON sales(invoice_number)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp)");
        
        // Create default admin user
        String adminPasswordHash = PasswordUtil.hashPassword("admin123");
        stmt.execute("INSERT INTO users (username, password_hash, full_name, role) VALUES " +
                     "('admin', '" + adminPasswordHash + "', 'System Administrator', 'admin')");
        
        // Create default cashier
        String cashierPasswordHash = PasswordUtil.hashPassword("cashier123");
        stmt.execute("INSERT INTO users (username, password_hash, full_name, role) VALUES " +
                     "('cashier', '" + cashierPasswordHash + "', 'Store Cashier', 'cashier')");
    }
    
    private static void insertDefaultProducts(Connection conn) throws SQLException {
        String insertSQL = "INSERT OR IGNORE INTO products (barcode, name, category, cost_price, selling_price, quantity, supplier) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            // Define products as a 2D array
            String[][] products = {
                {"1001", "Arc Welder 200A", "Welding", "4500.00", "6800.00", "10", "Default Supplier"},
                {"1002", "Welding Helmet", "Welding", "350.00", "550.00", "15", "Default Supplier"},
                {"1003", "Safety Boots", "PPE", "450.00", "750.00", "20", "Default Supplier"},
                {"1004", "Safety Gloves", "PPE", "85.00", "150.00", "30", "Default Supplier"},
                {"1005", "Angle Grinder 115mm", "Power Tools", "650.00", "950.00", "8", "Default Supplier"},
                {"1006", "Electric Drill", "Power Tools", "750.00", "1200.00", "5", "Default Supplier"},
                {"1007", "Tool Set 50pc", "Hand Tools", "550.00", "850.00", "12", "Default Supplier"},
                {"1008", "M10 Bolt and Nut Set", "Fasteners", "25.00", "45.00", "100", "Default Supplier"},
                {"1009", "White Paint 5L", "Paint", "350.00", "550.00", "25", "Default Supplier"},
                {"1010", "Pipe 15mm x 6m", "Plumbing", "120.00", "195.00", "40", "Default Supplier"}
            };
            
            for (String[] product : products) {
                pstmt.setString(1, product[0]);  // barcode
                pstmt.setString(2, product[1]);  // name
                pstmt.setString(3, product[2]);  // category
                pstmt.setDouble(4, Double.parseDouble(product[3]));  // cost_price
                pstmt.setDouble(5, Double.parseDouble(product[4]));  // selling_price
                pstmt.setInt(6, Integer.parseInt(product[5]));       // quantity
                pstmt.setString(7, product[6]);  // supplier
                pstmt.executeUpdate();
            }
            
            System.out.println("✅ Inserted " + products.length + " default products.");
            
            // Log initialization
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO audit_log (username, action, details) VALUES " +
                             "('system', 'Database Initialized', 'Default data and admin user created')");
            }
        }
    }
}