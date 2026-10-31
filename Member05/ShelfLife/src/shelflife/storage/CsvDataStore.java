package shelflife.storage;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;
import shelflife.service.TransactionOperation;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Shared local CSV file storage implementation conforming to DataStore.
 * Provides atomic file-level transactions using rollback backup files.
 * CSV column orders strictly follow the team contract:
 *   products.csv:       productId,name,category,sellingPrice,lowStockThreshold
 *   batches.csv:        batchId,productId,quantity,expiryDate
 *   sales.csv:          saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus
 *   sale_items.csv:     saleId,productId,batchId,quantity,unitPrice,lineTotal
 *   wastage.csv:        wastageId,batchId,quantity,wastageDateTime,reason
 * Date format: yyyy-MM-dd for LocalDate, ISO-8601 for LocalDateTime.
 * Uses splitCsvLine for proper CSV parsing (handles quoted fields).
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class CsvDataStore implements shelflife.service.DataStore {
    private final File dataDir;
    private final File productsFile;
    private final File batchesFile;
    private final File salesFile;
    private final File saleItemsFile;
    private final File wastageFile;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public CsvDataStore(File dataDir) {
        this.dataDir = dataDir;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.productsFile = new File(dataDir, "products.csv");
        this.batchesFile = new File(dataDir, "batches.csv");
        this.salesFile = new File(dataDir, "sales.csv");
        this.saleItemsFile = new File(dataDir, "sale_items.csv");
        this.wastageFile = new File(dataDir, "wastage.csv");
        initFiles();
    }

    private void initFiles() {
        try {
            if (!productsFile.exists()) {
                writeLines(productsFile, CollectionsSingle("productId,name,category,sellingPrice,lowStockThreshold"));
            }
            if (!batchesFile.exists()) {
                writeLines(batchesFile, CollectionsSingle("batchId,productId,quantity,expiryDate"));
            }
            if (!salesFile.exists()) {
                writeLines(salesFile, CollectionsSingle("saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus"));
            }
            if (!saleItemsFile.exists()) {
                writeLines(saleItemsFile, CollectionsSingle("saleId,productId,batchId,quantity,unitPrice,lineTotal"));
            }
            if (!wastageFile.exists()) {
                writeLines(wastageFile, CollectionsSingle("wastageId,batchId,quantity,wastageDateTime,reason"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV storage files in " + dataDir.getAbsolutePath(), e);
        }
    }

    private List<String> CollectionsSingle(String line) {
        List<String> list = new ArrayList<>();
        list.add(line);
        return list;
    }

    @Override
    public synchronized List<Product> loadProducts() throws Exception {
        List<Product> products = new ArrayList<>();
        List<String> lines = readLines(productsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] tokens = splitCsvLine(line);
            if (tokens.length >= 5) {
                Product p = new Product(tokens[0], tokens[1], tokens[2], new BigDecimal(tokens[3]), Integer.parseInt(tokens[4]));
                products.add(p);
            }
        }
        return products;
    }

    @Override
    public synchronized void saveProducts(List<Product> products) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("productId,name,category,sellingPrice,lowStockThreshold");
        for (Product p : products) {
            lines.add(p.getProductId() + "," +
                    escapeCsv(p.getName()) + "," +
                    escapeCsv(p.getCategory()) + "," +
                    p.getSellingPrice().toPlainString() + "," +
                    p.getLowStockThreshold());
        }
        writeLines(productsFile, lines);
    }

    @Override
    public synchronized List<ProductBatch> loadBatches() throws Exception {
        List<ProductBatch> batches = new ArrayList<>();
        List<String> lines = readLines(batchesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] tokens = splitCsvLine(line);
            if (tokens.length >= 4) {
                ProductBatch b = new ProductBatch(tokens[0], tokens[1], Integer.parseInt(tokens[2]), LocalDate.parse(tokens[3], DATE_FORMATTER));
                batches.add(b);
            }
        }
        return batches;
    }

    @Override
    public synchronized void saveBatches(List<ProductBatch> batches) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("batchId,productId,quantity,expiryDate");
        for (ProductBatch b : batches) {
            lines.add(b.getBatchId() + "," +
                    b.getProductId() + "," +
                    b.getQuantity() + "," +
                    b.getExpiryDate().format(DATE_FORMATTER));
        }
        writeLines(batchesFile, lines);
    }

    @Override
    public synchronized List<Sale> loadSales() throws Exception {
        List<Sale> sales = new ArrayList<>();
        List<SaleItem> allItems = loadSaleItems();
        List<String> lines = readLines(salesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] tokens = splitCsvLine(line);
            if (tokens.length >= 5) {
                Sale s = new Sale();
                s.setSaleId(tokens[0]);
                s.setSaleDateTime(LocalDateTime.parse(tokens[1], DATE_TIME_FORMATTER));
                s.setSubtotal(new BigDecimal(tokens[2]));
                s.setPaymentMethod(tokens[3]);
                s.setPaymentStatus(tokens[4]);
                // Attach corresponding items
                for (SaleItem item : allItems) {
                    if (s.getSaleId().equals(item.getSaleId())) {
                        s.addItem(item);
                    }
                }
                sales.add(s);
            }
        }
        return sales;
    }

    @Override
    public synchronized void saveSales(List<Sale> sales) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus");
        for (Sale s : sales) {
            lines.add(s.getSaleId() + "," +
                    s.getSaleDateTime().format(DATE_TIME_FORMATTER) + "," +
                    s.getFinalTotal().toPlainString() + "," +
                    s.getPaymentMethod() + "," +
                    s.getPaymentStatus());
        }
        writeLines(salesFile, lines);
    }

    @Override
    public synchronized List<SaleItem> loadSaleItems() throws Exception {
        List<SaleItem> items = new ArrayList<>();
        List<String> lines = readLines(saleItemsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] tokens = splitCsvLine(line);
            if (tokens.length >= 6) {
                SaleItem item = new SaleItem();
                item.setSaleItemId(tokens[0]);
                item.setSaleId(tokens[1]);
                item.setProductId(tokens[2]);
                item.setBatchId(tokens[3]);
                item.setQuantity(Integer.parseInt(tokens[4]));
                item.setUnitPrice(new BigDecimal(tokens[5]));
                // lineTotal is at tokens[5] for contract format, but sale_items has 6 columns:
                // saleId,productId,batchId,quantity,unitPrice,lineTotal
                // Actually tokens[5] is lineTotal, but we need unitPrice at index 4 and lineTotal at index 5
                // Let me re-read: tokens[4] = quantity, tokens[5] = unitPrice... wait.
                // Header: saleId,productId,batchId,quantity,unitPrice,lineTotal
                // So: tokens[0]=saleId, tokens[1]=productId, tokens[2]=batchId, tokens[3]=quantity, tokens[4]=unitPrice, tokens[5]=lineTotal
                // But the SaleItem constructor sets unitPrice and then calculates lineTotal.
                // Let me set them directly.
                item.setQuantity(Integer.parseInt(tokens[3]));
                item.setUnitPrice(new BigDecimal(tokens[4]));
                item.setLineTotal(new BigDecimal(tokens[5]));
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public synchronized void saveSaleItems(List<SaleItem> saleItems) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,productId,batchId,quantity,unitPrice,lineTotal");
        for (SaleItem item : saleItems) {
            lines.add(item.getSaleId() + "," +
                    item.getProductId() + "," +
                    item.getBatchId() + "," +
                    item.getQuantity() + "," +
                    item.getUnitPrice().toPlainString() + "," +
                    item.getLineTotal().toPlainString());
        }
        writeLines(saleItemsFile, lines);
    }

    @Override
    public synchronized List<WastageRecord> loadWastageRecords() throws Exception {
        List<WastageRecord> records = new ArrayList<>();
        List<String> lines = readLines(wastageFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] tokens = splitCsvLine(line);
            if (tokens.length >= 5) {
                WastageRecord r = new WastageRecord(tokens[0], tokens[1], Integer.parseInt(tokens[2]),
                        LocalDateTime.parse(tokens[3], DATE_TIME_FORMATTER), tokens[4]);
                records.add(r);
            }
        }
        return records;
    }

    @Override
    public synchronized void saveWastageRecords(List<WastageRecord> wastageRecords) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("wastageId,batchId,quantity,wastageDateTime,reason");
        for (WastageRecord r : wastageRecords) {
            lines.add(r.getWastageId() + "," +
                    r.getBatchId() + "," +
                    r.getQuantity() + "," +
                    r.getWastageDateTime().format(DATE_TIME_FORMATTER) + "," +
                    escapeCsv(r.getReason()));
        }
        writeLines(wastageFile, lines);
    }

    @Override
    public synchronized void executeTransaction(TransactionOperation operation) throws Exception {
        File prodBak = new File(dataDir, "products.csv.bak");
        File batchBak = new File(dataDir, "batches.csv.bak");
        File salesBak = new File(dataDir, "sales.csv.bak");
        File itemsBak = new File(dataDir, "sale_items.csv.bak");
        File wastageBak = new File(dataDir, "wastage.csv.bak");

        copyFile(productsFile, prodBak);
        copyFile(batchesFile, batchBak);
        copyFile(salesFile, salesBak);
        copyFile(saleItemsFile, itemsBak);
        copyFile(wastageFile, wastageBak);

        try {
            operation.execute();
            // Success: remove backups
            prodBak.delete();
            batchBak.delete();
            salesBak.delete();
            itemsBak.delete();
            wastageBak.delete();
        } catch (Exception ex) {
            // Failure: atomic rollback to backup state
            copyFile(prodBak, productsFile);
            copyFile(batchBak, batchesFile);
            copyFile(salesBak, salesFile);
            copyFile(itemsBak, saleItemsFile);
            copyFile(wastageBak, wastageFile);

            prodBak.delete();
            batchBak.delete();
            salesBak.delete();
            itemsBak.delete();
            wastageBak.delete();
            throw ex;
        }
    }

    private void copyFile(File src, File dest) throws IOException {
        if (!src.exists()) return;
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!file.exists()) return lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void writeLines(File file, List<String> lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}