package shelflife.util;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Supplier;
import shelflife.service.InventoryService;
import shelflife.service.SupplierService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Utility for seeding realistic Pakistani retail catalog data on first run
 * if the inventory database is empty.
 */
public final class SeedDataUtil {

    private SeedDataUtil() {}

    /**
     * Seeds initial suppliers, products, and batches if no products exist.
     *
     * @param inv InventoryService instance
     * @param sup SupplierService instance
     */
    public static void seedIfEmpty(InventoryService inv, SupplierService sup) {
        if (inv == null) return;
        List<Product> existingProducts = inv.getAllProducts();
        if (existingProducts != null && !existingProducts.isEmpty()) {
            return;
        }

        // 1. Add 3 realistic Pakistani Retail Suppliers
        if (sup != null) {
            Supplier[] suppliers = new Supplier[]{
                new Supplier("SUP-METRO", "Metro Cash & Carry", "Tariq Mahmood", "+92 42 111 638 760", "orders@metro.pk", "Airport Road, Lahore", true),
                new Supplier("SUP-ALFATAH", "Al-Fatah Department Store", "Salman Sheikh", "+92 42 35755866", "wholesale@alfatah.pk", "Gulberg III, Lahore", true),
                new Supplier("SUP-NESTLE", "Nestle Pakistan Ltd", "Imran Farooq", "+92 42 111 637 853", "distribution@nestle.com.pk", "Upper Mall, Lahore", true)
            };
            for (Supplier s : suppliers) {
                try {
                    sup.addSupplier(s);
                } catch (Exception ignored) {}
            }
        }

        // 2. Add 20 realistic Pakistani retail products across categories:
        // Beverages, Dairy, Snacks, Groceries, Cleaning, Personal Care
        LocalDate sixMonthsAhead = LocalDate.now().plusMonths(6);

        Object[][] productData = new Object[][]{
            // id, name, category, sellingPrice, costPrice, threshold, barcode, sku, supplierId
            {"PROD-01", "Olper's Full Cream Milk 1L", "Dairy", "320.00", "270.00", 10, "896400010101", "DAI-OLP-01", "SUP-NESTLE"},
            {"PROD-02", "Nestle MilkPak 1L", "Dairy", "310.00", "265.00", 10, "896400010102", "DAI-MPK-02", "SUP-NESTLE"},
            {"PROD-03", "Tarang Tea Whitener 1L", "Dairy", "280.00", "240.00", 10, "896400010103", "DAI-TRG-03", "SUP-NESTLE"},
            {"PROD-04", "Nurpur Butter 200g", "Dairy", "450.00", "380.00", 5, "896400010104", "DAI-NUR-04", "SUP-METRO"},
            {"PROD-05", "Rooh Afza 800ml", "Beverages", "480.00", "410.00", 8, "896400020101", "BEV-ROO-05", "SUP-ALFATAH"},
            {"PROD-06", "Tapal Danedar Tea 450g", "Beverages", "750.00", "640.00", 10, "896400020102", "BEV-TAP-06", "SUP-ALFATAH"},
            {"PROD-07", "Lipton Yellow Label Tea 400g", "Beverages", "790.00", "680.00", 10, "896400020103", "BEV-LIP-07", "SUP-ALFATAH"},
            {"PROD-08", "Pakola Ice Cream Soda 1.5L", "Beverages", "160.00", "135.00", 12, "896400020104", "BEV-PAK-08", "SUP-METRO"},
            {"PROD-09", "Dalda Cooking Oil 1L Pouch", "Groceries", "520.00", "460.00", 15, "896400030101", "GRO-DAL-09", "SUP-METRO"},
            {"PROD-10", "Sufi Banaspati Ghee 1kg", "Groceries", "510.00", "450.00", 15, "896400030102", "GRO-SUF-10", "SUP-METRO"},
            {"PROD-11", "National Basmati Rice 5kg", "Groceries", "1850.00", "1600.00", 8, "896400030103", "GRO-RIC-11", "SUP-METRO"},
            {"PROD-12", "National Chili Garlic Sauce 500g", "Groceries", "340.00", "290.00", 10, "896400030104", "GRO-SAU-12", "SUP-METRO"},
            {"PROD-13", "Lays Masala Chips 65g", "Snacks", "100.00", "82.00", 20, "896400040101", "SNK-LAY-13", "SUP-ALFATAH"},
            {"PROD-14", "Kurkure Chutney Chaska 60g", "Snacks", "80.00", "65.00", 20, "896400040102", "SNK-KUR-14", "SUP-ALFATAH"},
            {"PROD-15", "Super Biscuit Half Roll", "Snacks", "50.00", "40.00", 25, "896400040103", "SNK-SUP-15", "SUP-ALFATAH"},
            {"PROD-16", "Sooper Biscuit Family Pack", "Snacks", "140.00", "115.00", 15, "896400040104", "SNK-SOP-16", "SUP-ALFATAH"},
            {"PROD-17", "Surf Excel Washing Powder 1kg", "Cleaning", "620.00", "530.00", 10, "896400050101", "CLN-SRF-17", "SUP-METRO"},
            {"PROD-18", "Vim Dishwash Bar 200g", "Cleaning", "120.00", "95.00", 15, "896400050102", "CLN-VIM-18", "SUP-METRO"},
            {"PROD-19", "Lifebuoy Total Soap 130g", "Personal Care", "140.00", "115.00", 20, "896400060101", "PC-LFB-19", "SUP-ALFATAH"},
            {"PROD-20", "Sunsilk Black Shine Shampoo 185ml", "Personal Care", "420.00", "350.00", 10, "896400060102", "PC-SNS-20", "SUP-ALFATAH"}
        };

        for (Object[] row : productData) {
            String pId = (String) row[0];
            String name = (String) row[1];
            String category = (String) row[2];
            BigDecimal sellPrice = new BigDecimal((String) row[3]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costPrice = new BigDecimal((String) row[4]).setScale(2, RoundingMode.HALF_UP);
            int threshold = (Integer) row[5];
            String barcode = (String) row[6];
            String sku = (String) row[7];
            String supplierId = (String) row[8];

            try {
                Product p = new Product(pId, name, category, sellPrice, threshold, barcode, sku, true);
                inv.addProduct(p);
            } catch (Exception ignored) {}

            try {
                String batchId = "BATCH-" + pId.replace("PROD-", "");
                ProductBatch batch = new ProductBatch(batchId, pId, 50, sixMonthsAhead, costPrice, supplierId);
                inv.addBatch(batch);
            } catch (Exception ignored) {}
        }
    }
}
