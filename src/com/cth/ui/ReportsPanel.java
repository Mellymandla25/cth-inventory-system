package com.cth.ui;

import com.cth.database.DatabaseConnection;
import com.cth.security.SessionManager;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ReportsPanel extends JPanel {
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    private DecimalFormat currencyFormat = new DecimalFormat("R #,##0.00");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    public ReportsPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initUI();
    }
    
    private void initUI() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel dateLabel = new JLabel("Date Range:");
        JSpinner startDate = new JSpinner(new SpinnerDateModel());
        JSpinner endDate = new JSpinner(new SpinnerDateModel());
        
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startDate, "yyyy-MM-dd");
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endDate, "yyyy-MM-dd");
        startDate.setEditor(startEditor);
        endDate.setEditor(endEditor);
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
        startDate.setValue(cal.getTime());
        endDate.setValue(new Date());
        
        JButton generateBtn = new JButton("Generate Report");
        JButton exportBtn = new JButton("Export to PDF");
        JButton refreshBtn = new JButton("Refresh");
        
        generateBtn.addActionListener(e -> generateReport(startDate.getValue(), endDate.getValue()));
        exportBtn.addActionListener(e -> exportToPDF());
        refreshBtn.addActionListener(e -> generateReport(startDate.getValue(), endDate.getValue()));
        
        controlPanel.add(dateLabel);
        controlPanel.add(new JLabel("From:"));
        controlPanel.add(startDate);
        controlPanel.add(new JLabel("To:"));
        controlPanel.add(endDate);
        controlPanel.add(generateBtn);
        controlPanel.add(exportBtn);
        controlPanel.add(refreshBtn);
        
        add(controlPanel, BorderLayout.NORTH);
        
        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(new Font("Arial", Font.BOLD, 14));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(summaryLabel, BorderLayout.SOUTH);
        
        String[] columns = {"Date", "Invoice #", "Customer", "Amount", "VAT", "Payment", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        reportTable = new JTable(tableModel);
        reportTable.setRowHeight(25);
        reportTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        JScrollPane scrollPane = new JScrollPane(reportTable);
        add(scrollPane, BorderLayout.CENTER);
        
        generateReport(startDate.getValue(), endDate.getValue());
        
        dateFormat.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));
    }
    
    private void generateReport(Object start, Object end) {
        System.out.println("📊 Generating report...");
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT s.sale_date, s.invoice_number, s.customer_name, " +
                 "s.total_amount, s.vat_amount, s.payment_method, s.voided, " +
                 "u.username as cashier " +
                 "FROM sales s " +
                 "LEFT JOIN users u ON s.user_id = u.id " +
                 "WHERE date(s.sale_date) BETWEEN date(?) AND date(?) " +
                 "ORDER BY s.sale_date DESC")) {
            
            // Convert dates to proper format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String startStr = sdf.format((Date)start);
            String endStr = sdf.format((Date)end);
            
            pstmt.setString(1, startStr);
            pstmt.setString(2, endStr);
            
            System.out.println("SQL: " + pstmt.toString());
            
            ResultSet rs = pstmt.executeQuery();
            tableModel.setRowCount(0);
            
            double totalRevenue = 0;
            double totalVAT = 0;
            int totalSales = 0;
            int voidedCount = 0;
            
            while (rs.next()) {
                boolean voided = rs.getInt("voided") == 1;
                totalRevenue += rs.getDouble("total_amount");
                totalVAT += rs.getDouble("vat_amount");
                totalSales++;
                if (voided) voidedCount++;
                
                tableModel.addRow(new Object[]{
                    dateFormat.format(rs.getTimestamp("sale_date")),
                    rs.getString("invoice_number"),
                    rs.getString("customer_name") != null ? rs.getString("customer_name") : "Walk-in",
                    currencyFormat.format(rs.getDouble("total_amount")),
                    currencyFormat.format(rs.getDouble("vat_amount")),
                    rs.getString("payment_method"),
                    voided ? "VOIDED" : "Completed"
                });
            }
            
            int completedSales = totalSales - voidedCount;
            summaryLabel.setText(String.format(
                "📊 Summary: %d Sales | %d Completed | %d Voided | Total Revenue: %s | Total VAT: %s",
                totalSales, completedSales, voidedCount,
                currencyFormat.format(totalRevenue),
                currencyFormat.format(totalVAT)
            ));
            
            System.out.println("✅ Report generated: " + tableModel.getRowCount() + " rows");
            
        } catch (SQLException e) {
            System.err.println("❌ Report error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error generating report: " + e.getMessage(),
                "Report Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            System.err.println("❌ General error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void exportToPDF() {
    if (tableModel.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this,
            "No data to export. Generate a report first.",
            "Export Error",
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Show file chooser with default filename
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save Report as PDF");
    fileChooser.setSelectedFile(new java.io.File("sales_report_" + 
        new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf"));
    
    // Set current directory to Desktop or Documents
    String userHome = System.getProperty("user.home");
    fileChooser.setCurrentDirectory(new java.io.File(userHome + "/Documents"));
    
    int result = fileChooser.showSaveDialog(this);
    
    if (result == JFileChooser.APPROVE_OPTION) {
        try {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            
            // Add .pdf extension if not present
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf";
            }
            
            System.out.println("📄 Exporting report to: " + filePath);
            
            // Create the PDF
            com.itextpdf.kernel.pdf.PdfWriter writer = 
                new com.itextpdf.kernel.pdf.PdfWriter(filePath);
            com.itextpdf.kernel.pdf.PdfDocument pdf = 
                new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = 
                new com.itextpdf.layout.Document(pdf);
            
            // Title
            document.add(new com.itextpdf.layout.element.Paragraph("CTH Tools - Sales Report")
                .setFontSize(18)
                .setBold());
            document.add(new com.itextpdf.layout.element.Paragraph("Generated: " + new Date()));
            document.add(new com.itextpdf.layout.element.Paragraph(" "));
            
            // Summary
            document.add(new com.itextpdf.layout.element.Paragraph(summaryLabel.getText()));
            document.add(new com.itextpdf.layout.element.Paragraph(" "));
            
            // Table
            int columnCount = tableModel.getColumnCount();
            float[] columnWidths = new float[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnWidths[i] = 1.0f;
            }
            
            com.itextpdf.layout.element.Table table = 
                new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(columnWidths));
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
            
            // Headers
            for (int i = 0; i < columnCount; i++) {
                table.addCell(new com.itextpdf.layout.element.Paragraph(
                    tableModel.getColumnName(i)).setBold());
            }
            
            // Data
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < columnCount; col++) {
                    Object value = tableModel.getValueAt(row, col);
                    table.addCell(new com.itextpdf.layout.element.Paragraph(
                        value != null ? value.toString() : ""));
                }
            }
            
            document.add(table);
            document.close();
            
            // ✅ Show success message
            JOptionPane.showMessageDialog(this,
                "✅ Report exported successfully!\n\n" +
                "File saved to:\n" + filePath,
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
            
            // ✅ Open the PDF automatically
            try {
                java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
            } catch (Exception e) {
                System.out.println("Could not auto-open PDF: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Export error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "❌ Error exporting report:\n\n" + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    } else {
        System.out.println("ℹ️ Export cancelled by user");
    }
}
}