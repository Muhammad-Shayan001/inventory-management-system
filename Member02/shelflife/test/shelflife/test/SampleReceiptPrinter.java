package shelflife.test;
import shelflife.model.*;
import shelflife.ui.billing.ReceiptDialog;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SampleReceiptPrinter {
    public static void main(String[] args) {
        Sale sale = new Sale("SALE-20261024-160530-1001", LocalDateTime.of(2026, 10, 24, 16, 5, 30), PaymentMethod.CASH, PaymentStatus.PAID);
        sale.addItem(new SaleItem("SI-1001-001", sale.getSaleId(), "P101", "Organic Whole Milk 1L", "B101-A", 5, new BigDecimal("3.50")));
        sale.addItem(new SaleItem("SI-1001-002", sale.getSaleId(), "P101", "Organic Whole Milk 1L", "B101-B", 2, new BigDecimal("3.50")));
        sale.addItem(new SaleItem("SI-1001-003", sale.getSaleId(), "P102", "Artisan Sourdough Loaf", "B102-A", 1, new BigDecimal("4.75")));
        System.out.println(ReceiptDialog.generateReceiptText(sale));
    }
}
