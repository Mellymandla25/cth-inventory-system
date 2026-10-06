package com.cth.reporting;

import com.cth.config.ConfigManager;
import com.cth.pos.CartManager;
import com.cth.utils.PathUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvoiceGenerator {
    
    public static String generateInvoice(int saleId, String invoiceNumber,  String customerName, CartManager cart,double totalAmount, String paymentMethod) {
        
        try {
            // Create invoices directory
            File invoiceDir = new File(PathUtils.getInvoicePath());
            if (!invoiceDir.exists()) {
                invoiceDir.mkdirs();
                System.out.println("📁 Created invoices folder: " + invoiceDir.getAbsolutePath());
            }
            
            String filePath = PathUtils.getInvoicePath() + File.separator + invoiceNumber + ".pdf";
            System.out.println("📄 Creating PDF at: " + filePath);
            
            // Create document
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            // ============================================
            // FONTS
            // ============================================
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font totalFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            
            // ============================================
            // HEADER WITH LOGO
            // ============================================
            
            // Try to load the logo
            try {
                String logoPath = "resources/logo.png";
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    Image logo = Image.getInstance(logoPath);
                    logo.scaleToFit(200, 80);
                    logo.setAlignment(Image.ALIGN_CENTER);
                    document.add(logo);
                    document.add(new Paragraph(" "));
                } else {
                    System.out.println("⚠️ Logo not found at: " + logoPath);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not load logo: " + e.getMessage());
            }
            
            // Title
            Paragraph title = new Paragraph("CTH TOOLS HARDWARE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph address = new Paragraph("Carletonville, South Africa", normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
            
            Paragraph phone = new Paragraph("Phone: +27 82 123 4567", normalFont);
            phone.setAlignment(Element.ALIGN_CENTER);
            document.add(phone);
            
            document.add(new Paragraph(" "));
            
            // ============================================
            // INVOICE HEADER
            // ============================================
            Paragraph invoiceTitle = new Paragraph("TAX INVOICE", titleFont);
            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(invoiceTitle);
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Invoice #: " + invoiceNumber));
            document.add(new Paragraph("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
            if (customerName != null && !customerName.isEmpty()) {
                document.add(new Paragraph("Customer: " + customerName));
            }
            document.add(new Paragraph(" "));
            
            // ============================================
            // TABLE
            // ============================================
            Table table = new Table(4);
            table.setWidths(new float[]{1, 3, 2, 2});
            table.setWidth(100);
            table.setBorder(Table.NO_BORDER);
            
            // Headers
            Cell h1 = new Cell(new Paragraph("Qty", boldFont));
            h1.setBackgroundColor(new Color(200, 200, 200));
            h1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(h1);
            
            Cell h2 = new Cell(new Paragraph("Product", boldFont));
            h2.setBackgroundColor(new Color(200, 200, 200));
            h2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(h2);
            
            Cell h3 = new Cell(new Paragraph("Unit Price", boldFont));
            h3.setBackgroundColor(new Color(200, 200, 200));
            h3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(h3);
            
            Cell h4 = new Cell(new Paragraph("Subtotal", boldFont));
            h4.setBackgroundColor(new Color(200, 200, 200));
            h4.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(h4);
            
            // Items
            for (CartManager.CartItem item : cart.getItems()) {
                Cell c1 = new Cell(new Paragraph(String.valueOf(item.getQuantity()), normalFont));
                c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c1);
                
                Cell c2 = new Cell(new Paragraph(item.getProductName(), normalFont));
                c2.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(c2);
                
                Cell c3 = new Cell(new Paragraph("R " + String.format("%.2f", item.getUnitPrice()), normalFont));
                c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c3);
                
                Cell c4 = new Cell(new Paragraph("R " + String.format("%.2f", item.getSubtotal()), normalFont));
                c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c4);
            }
            
            document.add(table);
            document.add(new Paragraph(" "));
            
            // ============================================
            // TOTALS
            // ============================================
            Paragraph subtotal = new Paragraph("Subtotal: R " + String.format("%.2f", cart.getSubtotal()), normalFont);
            subtotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(subtotal);
            
            Paragraph vat = new Paragraph("VAT (15%): R " + String.format("%.2f", cart.getVatAmount()), normalFont);
            vat.setAlignment(Element.ALIGN_RIGHT);
            document.add(vat);
            
            Paragraph total = new Paragraph("TOTAL: R " + String.format("%.2f", totalAmount), totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Payment Method: " + paymentMethod, normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            // ============================================
            // BANKING DETAILS
            // ============================================
            document.add(new Paragraph("Banking Details for EFT Payments", boldFont));
            document.add(new Paragraph("Bank: First National Bank", normalFont));
            document.add(new Paragraph("Account Holder: CTH Tools", normalFont));
            document.add(new Paragraph("Account Number: 1234567890", normalFont));
            document.add(new Paragraph("Branch Code: 123456", normalFont));
            
            document.add(new Paragraph(" "));
            
            // ============================================
            // FOOTER
            // ============================================
            Paragraph thankYou = new Paragraph("Thank you for your business!", boldFont);
            thankYou.setAlignment(Element.ALIGN_CENTER);
            document.add(thankYou);
            
            document.close();
            
            System.out.println("✅ PDF SAVED SUCCESSFULLY!");
            System.out.println("📄 File: " + filePath);
            
            return filePath;
            
        } catch (Exception e) {
            System.out.println("❌ PDF generation error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getInvoicePath(String invoiceNumber) {
        String path = PathUtils.getInvoicePath() + File.separator + invoiceNumber + ".pdf";
        File file = new File(path);
        return file.exists() ? path : null;
    }
}