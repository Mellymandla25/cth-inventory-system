package com.cth.pos;

import com.cth.database.DatabaseConnection;
import com.cth.reporting.InvoiceGenerator;
import com.cth.security.SessionManager;
import com.cth.utils.AuditLogger;
import java.io.File;

import javax.swing.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaleProcessor {
    private String lastInvoiceNumber;
    private boolean saleProcessed = false;

    public boolean processSale(int userId, String customerName, CartManager cart, String paymentMethod) {
        System.out.println("========================================");
        System.out.println("🔴 SALE PROCESSOR STARTED!");
        System.out.println("========================================");
        System.out.println("User ID: " + userId);
        System.out.println("Customer: " + customerName);
        System.out.println("Payment: " + paymentMethod);
        System.out.println("Items in cart: " + cart.getItemCount());

        if (cart.getItemCount() == 0) {
            System.out.println("❌ Cart is empty!");
            showMessage("Cart is empty!\n\nPlease add items before completing a sale.", "Empty Cart");
            return false;
        }

        Connection conn = null;
        try {
            // STEP 1: Connect to database
            System.out.println("📌 Step 1: Connecting to database...");
            conn = DatabaseConnection.getConnection();
            System.out.println("✅ Database connected!");

            // STEP 2: Generate invoice number
            System.out.println("📌 Step 2: Generating invoice number...");
            String invoiceNumber = generateInvoiceNumber();
            lastInvoiceNumber = invoiceNumber;
            System.out.println("✅ Invoice #: " + invoiceNumber);

            // STEP 3: Calculate totals
            System.out.println("📌 Step 3: Calculating totals...");
            double subtotal = cart.getSubtotal();
            double vatAmount = cart.getVatAmount();
            double totalAmount = cart.getTotal();
            System.out.println("   Subtotal: R" + subtotal);
            System.out.println("   VAT: R" + vatAmount);
            System.out.println("   Total: R" + totalAmount);

            // STEP 4: Insert sale into database
            System.out.println("📌 Step 4: Inserting sale into database...");
            String insertSaleSQL = "INSERT INTO sales (invoice_number, user_id, customer_name, subtotal, vat_amount, total_amount, payment_method, sale_date) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            int saleId = -1;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSaleSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, invoiceNumber);
                pstmt.setInt(2, userId);
                pstmt.setString(3, customerName != null ? customerName : "");
                pstmt.setDouble(4, subtotal);
                pstmt.setDouble(5, vatAmount);
                pstmt.setDouble(6, totalAmount);
                pstmt.setString(7, paymentMethod);

                int rowsAffected = pstmt.executeUpdate();
                System.out.println("   Rows affected: " + rowsAffected);

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        System.out.println("✅ Sale ID: " + saleId);
                    }
                }
            }

            if (saleId <= 0) {
                System.out.println("❌ Failed to insert sale!");
                showMessage("Failed to insert sale record.", "Database Error");
                return false;
            }

            // STEP 5: Insert sale items
            System.out.println("📌 Step 5: Inserting sale items...");
            String insertItemSQL = "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";

            for (CartManager.CartItem item : cart.getItems()) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertItemSQL)) {
                    pstmt.setInt(1, saleId);
                    pstmt.setInt(2, item.getProductId());
                    pstmt.setInt(3, item.getQuantity());
                    pstmt.setDouble(4, item.getUnitPrice());
                    pstmt.setDouble(5, item.getSubtotal());
                    pstmt.executeUpdate();
                    System.out.println("   ✅ Added: " + item.getProductName() + " x " + item.getQuantity());
                }
            }

            // STEP 6: Update stock
            System.out.println("📌 Step 6: Updating stock...");
            String updateStockSQL = "UPDATE products SET quantity = quantity - ? WHERE id = ?";

            for (CartManager.CartItem item : cart.getItems()) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateStockSQL)) {
                    pstmt.setInt(1, item.getQuantity());
                    pstmt.setInt(2, item.getProductId());
                    int updated = pstmt.executeUpdate();
                    System.out.println("   ✅ Stock updated for product ID " + item.getProductId() + ": " + updated + " rows");
                }
            }

            // STEP 7: Generate PDF invoice
            System.out.println("📌 Step 7: Generating PDF invoice...");
            String invoicePath = InvoiceGenerator.generateInvoice(
                saleId, invoiceNumber, customerName, cart, totalAmount, paymentMethod
            );

            if (invoicePath != null) {
                System.out.println("✅ PDF generated: " + invoicePath);
            } else {
                System.out.println("❌ PDF generation failed!");
            }

            // STEP 8: Log the sale
            AuditLogger.log(userId, SessionManager.getInstance().getUsername(),
                "Sale Completed",
                "Invoice: " + invoiceNumber + " Amount: R" + totalAmount);

            saleProcessed = true;

            System.out.println("========================================");
            System.out.println("✅ SALE COMPLETED SUCCESSFULLY!");
            System.out.println("📄 Invoice #: " + invoiceNumber);
            System.out.println("💰 Total: R" + totalAmount);
            System.out.println("========================================");

            showMessage(
                "✅ SALE COMPLETED SUCCESSFULLY!\n\n" +
                "Invoice #: " + invoiceNumber + "\n" +
                "Total: R" + totalAmount + "\n\n" +
                "Sale has been saved to the database.\n" +
                "Stock has been updated.",
                "Success"
            );

            return true;

        } catch (SQLException e) {
            System.out.println("❌ SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            showMessage(
                "❌ Database Error:\n\n" + e.getMessage(),
                "Database Error"
            );
            return false;
        } catch (Exception e) {
            System.out.println("❌ GENERAL ERROR: " + e.getMessage());
            e.printStackTrace();
            showMessage(
                "❌ Error:\n\n" + e.getMessage(),
                "Error"
            );
            return false;
        }
    }

    private String generateInvoiceNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datePrefix = sdf.format(new Date());
        return "CTH-" + datePrefix + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    public void viewLastInvoice() {
        System.out.println("🔍 viewLastInvoice() called!");
        System.out.println("📄 lastInvoiceNumber: " + lastInvoiceNumber);

        if (lastInvoiceNumber != null) {
            String invoicePath = InvoiceGenerator.getInvoicePath(lastInvoiceNumber);
            System.out.println("📄 invoicePath: " + invoicePath);

            if (invoicePath != null) {
                try {
                    File pdfFile = new File(invoicePath);
                    if (pdfFile.exists()) {
                        System.out.println("✅ Opening PDF: " + invoicePath);
                        java.awt.Desktop.getDesktop().open(pdfFile);
                    } else {
                        System.out.println("❌ PDF file does not exist: " + invoicePath);
                        JOptionPane.showMessageDialog(null,
                            "PDF file does not exist!\n\n" +
                            "Invoice #: " + lastInvoiceNumber + "\n" +
                            "Expected location: " + invoicePath,
                            "File Not Found",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    System.out.println("❌ Failed to open PDF: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                        "Failed to open PDF:\n\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.out.println("❌ Invoice path is null!");
                JOptionPane.showMessageDialog(null,
                    "Invoice PDF not found.\n\n" +
                    "Invoice #: " + lastInvoiceNumber + "\n" +
                    "Check the invoices folder.",
                    "Not Found",
                    JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("❌ No invoice number stored!");
            JOptionPane.showMessageDialog(null,
                "No invoice has been generated yet.",
                "No Invoice",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showMessage(String message, String title) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        });
    }
}