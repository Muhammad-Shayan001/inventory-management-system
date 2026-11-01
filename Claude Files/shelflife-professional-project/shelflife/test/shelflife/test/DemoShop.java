package shelflife.test;

import shelflife.model.PaymentMethod;
import shelflife.model.PaymentStatus;
import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;
import shelflife.service.InventoryService;
import shelflife.service.InventoryServiceImpl;
import shelflife.service.SupplierService;
import shelflife.service.SupplierServiceImpl;
import shelflife.storage.CsvDataStore;
import shelflife.storage.DataStore;
import shelflife.util.SeedDataUtil;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a believable demo shop (catalogue, 90 days of sales, some wastage and
 * a few expiry / low-stock problems) in an empty data folder. Used for the
 * README screenshots and for manual testing. The random seed is fixed so the
 * result is always the same.
 */
public final class DemoShop {

    private static final int DAYS = 90;
    private static final int[] HOUR_WEIGHT = {
            0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 5, 7, 8, 7, 5, 4, 5, 7, 9, 9, 7, 4, 1, 0};

    private DemoShop() {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "demo-data");
        build(dir);
        System.out.println("Demo shop written to " + dir.getAbsolutePath());
    }

    public static void build(File dir) throws Exception {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create " + dir);
        }
        DataStore ds = new CsvDataStore(dir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SupplierService sup = new SupplierServiceImpl(inv, ds);
        SeedDataUtil.seedIfEmpty(inv, sup);

        Random rnd = new Random(42);
        List<Product> products = inv.getAllProducts();
        List<ProductBatch> batches = inv.getAllBatches();
        List<Sale> sales = new ArrayList<>();
        List<SaleItem> items = new ArrayList<>();

        int saleNo = 1;
        for (int d = DAYS; d >= 0; d--) {
            LocalDate day = LocalDate.now().minusDays(d);
            boolean weekend = day.getDayOfWeek().getValue() >= 5;
            int count = (weekend ? 26 : 16) + rnd.nextInt(10) + (DAYS - d) / 12;
            if (d == 0) {
                count = Math.min(count, 14);
            }
            for (int n = 0; n < count; n++) {
                LocalDateTime when = LocalDateTime.of(day, LocalTime.of(pickHour(rnd), rnd.nextInt(60)));
                if (when.isAfter(LocalDateTime.now())) {
                    continue;
                }
                String saleId = String.format("SALE-%s-%04d", day.toString().replace("-", ""), saleNo++);
                BigDecimal total = BigDecimal.ZERO;
                int lines = 1 + rnd.nextInt(4);
                for (int l = 0; l < lines; l++) {
                    Product p = products.get(popular(rnd, products.size()));
                    ProductBatch b = batchOf(batches, p.getProductId());
                    int qty = 1 + rnd.nextInt(3);
                    BigDecimal lineTotal = p.getSellingPrice().multiply(BigDecimal.valueOf(qty));
                    SaleItem it = new SaleItem(saleId, p.getProductId(), b.getBatchId(), qty,
                            p.getSellingPrice(), lineTotal);
                    it.setProductName(p.getName());
                    it.setCostPrice(b.getCostPrice());
                    items.add(it);
                    total = total.add(lineTotal);
                }
                PaymentStatus status = rnd.nextInt(100) < 96 ? PaymentStatus.PAID : PaymentStatus.CANCELLED;
                sales.add(new Sale(saleId, when, total.setScale(2, RoundingMode.HALF_UP), payment(rnd), status));
            }
        }
        ds.saveSales(sales);
        ds.saveSaleItems(items);

        // a few stock problems so the risk tables are not empty
        List<ProductBatch> all = ds.loadBatches();
        tweak(all, "BATCH-01", 6, LocalDate.now().plusDays(3));
        tweak(all, "BATCH-04", 4, LocalDate.now().plusDays(5));
        tweak(all, "BATCH-13", 9, LocalDate.now().plusMonths(5));
        tweak(all, "BATCH-14", 7, LocalDate.now().plusMonths(5));
        tweak(all, "BATCH-17", 22, LocalDate.now().minusDays(4));
        tweak(all, "BATCH-09", 3, LocalDate.now().plusMonths(4));
        ds.saveBatches(all);

        List<WastageRecord> waste = new ArrayList<>();
        String[] reasons = {"Expired", "Damaged", "Expired", "Spoiled", "Damaged", "Expired"};
        for (int i = 0; i < 14; i++) {
            ProductBatch b = all.get(rnd.nextInt(all.size()));
            waste.add(new WastageRecord("WST-" + (i + 1), b.getBatchId(), 1 + rnd.nextInt(4),
                    LocalDateTime.now().minusDays(rnd.nextInt(28)).withHour(9), reasons[rnd.nextInt(reasons.length)]));
        }
        ds.saveWastageRecords(waste);
    }

    private static void tweak(List<ProductBatch> all, String batchId, int qty, LocalDate expiry) {
        for (ProductBatch b : all) {
            if (b.getBatchId().equals(batchId)) {
                b.setQuantity(qty);
                b.setExpiryDate(expiry);
            }
        }
    }

    private static int pickHour(Random rnd) {
        int total = 0;
        for (int w : HOUR_WEIGHT) {
            total += w;
        }
        int r = rnd.nextInt(total);
        for (int h = 0; h < HOUR_WEIGHT.length; h++) {
            r -= HOUR_WEIGHT[h];
            if (r < 0) {
                return h;
            }
        }
        return 12;
    }

    /** Lower indexes sell more often. */
    private static int popular(Random rnd, int size) {
        double x = Math.pow(rnd.nextDouble(), 1.7);
        return Math.min(size - 1, (int) (x * size));
    }

    private static ProductBatch batchOf(List<ProductBatch> batches, String productId) {
        for (ProductBatch b : batches) {
            if (b.getProductId().equals(productId)) {
                return b;
            }
        }
        throw new IllegalStateException("No batch for " + productId);
    }

    private static PaymentMethod payment(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 55) {
            return PaymentMethod.CASH;
        }
        if (r < 75) {
            return PaymentMethod.EASYPAISA;
        }
        if (r < 87) {
            return PaymentMethod.JAZZCASH;
        }
        if (r < 95) {
            return PaymentMethod.DEBIT_CARD;
        }
        return PaymentMethod.STORE_CREDIT;
    }
}
