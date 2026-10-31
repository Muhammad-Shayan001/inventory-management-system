package shelflife.storage;

import shelflife.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared local CSV file storage implementation conforming to DataStore.
 * Provides atomic file-level transactions using rollback backup files.
 */
public class CsvDataStore implements DataStore {
    private final File dataDir;
    private final File productsFile;
    private final File batchesFile;
    private final File salesFile;
    private final File saleItemsFile;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public CsvDataStore(File dataDir) {
        this.dataDir = dataDir;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.productsFile = new File(dataDir, "products.csv");
        this.batchesFile = new File(dataDir, "batches.csv");
        this.salesFile = new File(dataDir, "sales.csv");
        this.saleItemsFile = new File(dataDir, "sale_items.csv");
        initFiles();
    }

    private void initFiles() {
        try {
            if (!productsFile.exists()) {
                writeLines(productsFile, CollectionsSingle("productId,name,category,unitPrice"));
            }
            if (!batchesFile.exists()) {
                writeLines(batchesFile, CollectionsSingle("batchId,productId,quantity,expiryDate,receivedDate"));
            }
            if (!salesFile.exists()) {
                writeLines(salesFile, CollectionsSingle("saleId,saleDateTime,subtotal,tax,finalTotal,paymentMethod,paymentStatus,notes"));
            }
            if (!saleItemsFile.exists()) {
                writeLines(saleItemsFile, CollectionsSingle("saleItemId,saleId,productId,productName,batchId,quantity,unitPrice,lineTotal"));
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
            if (tokens.length >= 4) {
                Product p = new Product(tokens[0], tokens[1], tokens[2], new BigDecimal(tokens[3]));
                products.add(p);
            }
        }
        return products;
    }

    @Override
    public synchronized void saveProducts(List<Product> products) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("productId,name,category,unitPrice");
        for (Product p : products) {
            lines.add(escapeCsv(p.getProductId()) + "," +
                    escapeCsv(p.getName()) + "," +
                    escapeCsv(p.getCategory()) + "," +
                    p.getUnitPrice().toPlainString());
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
            if (tokens.length >= 5) {
                ProductBatch b = new ProductBatch(
                        tokens[0],
                        tokens[1],
                        Integer.parseInt(tokens[2]),
                        LocalDate.parse(tokens[3], DATE_FORMATTER),
                        LocalDate.parse(tokens[4], DATE_FORMATTER)
                );
                batches.add(b);
            }
        }
        return batches;
    }

    @Override
    public synchronized void saveBatches(List<ProductBatch> batches) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("batchId,productId,quantity,expiryDate,receivedDate");
        for (ProductBatch b : batches) {
            lines.add(escapeCsv(b.getBatchId()) + "," +
                    escapeCsv(b.getProductId()) + "," +
                    b.getQuantity() + "," +
                    b.getExpiryDate().format(DATE_FORMATTER) + "," +
                    (b.getReceivedDate() != null ? b.getReceivedDate().format(DATE_FORMATTER) : ""));
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
            if (tokens.length >= 7) {
                Sale s = new Sale();
                s.setSaleId(tokens[0]);
                s.setSaleDateTime(LocalDateTime.parse(tokens[1], DATE_TIME_FORMATTER));
                s.setSubtotal(new BigDecimal(tokens[2]));
                s.setTax(new BigDecimal(tokens[3]));
                s.setFinalTotal(new BigDecimal(tokens[4]));
                s.setPaymentMethod(PaymentMethod.valueOf(tokens[5]));
                s.setPaymentStatus(PaymentStatus.valueOf(tokens[6]));
                if (tokens.length > 7) {
                    s.setNotes(tokens[7]);
                }
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
        lines.add("saleId,saleDateTime,subtotal,tax,finalTotal,paymentMethod,paymentStatus,notes");
        for (Sale s : sales) {
            lines.add(escapeCsv(s.getSaleId()) + "," +
                    s.getSaleDateTime().format(DATE_TIME_FORMATTER) + "," +
                    s.getSubtotal().toPlainString() + "," +
                    s.getTax().toPlainString() + "," +
                    s.getFinalTotal().toPlainString() + "," +
                    s.getPaymentMethod().name() + "," +
                    s.getPaymentStatus().name() + "," +
                    escapeCsv(s.getNotes() != null ? s.getNotes() : ""));
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
            if (tokens.length >= 8) {
                SaleItem item = new SaleItem();
                item.setSaleItemId(tokens[0]);
                item.setSaleId(tokens[1]);
                item.setProductId(tokens[2]);
                item.setProductName(tokens[3]);
                item.setBatchId(tokens[4]);
                item.setQuantity(Integer.parseInt(tokens[5]));
                item.setUnitPrice(new BigDecimal(tokens[6]));
                item.setLineTotal(new BigDecimal(tokens[7]));
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public synchronized void saveSaleItems(List<SaleItem> saleItems) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("saleItemId,saleId,productId,productName,batchId,quantity,unitPrice,lineTotal");
        for (SaleItem item : saleItems) {
            lines.add(escapeCsv(item.getSaleItemId()) + "," +
                    escapeCsv(item.getSaleId()) + "," +
                    escapeCsv(item.getProductId()) + "," +
                    escapeCsv(item.getProductName()) + "," +
                    escapeCsv(item.getBatchId()) + "," +
                    item.getQuantity() + "," +
                    item.getUnitPrice().toPlainString() + "," +
                    item.getLineTotal().toPlainString());
        }
        writeLines(saleItemsFile, lines);
    }

    @Override
    public synchronized void executeTransaction(TransactionOperation operation) throws Exception {
        File prodBak = new File(dataDir, "products.csv.bak");
        File batchBak = new File(dataDir, "batches.csv.bak");
        File salesBak = new File(dataDir, "sales.csv.bak");
        File itemsBak = new File(dataDir, "sale_items.csv.bak");

        copyFile(productsFile, prodBak);
        copyFile(batchesFile, batchBak);
        copyFile(salesFile, salesBak);
        copyFile(saleItemsFile, itemsBak);

        try {
            operation.execute();
            // Success: remove backups
            prodBak.delete();
            batchBak.delete();
            salesBak.delete();
            itemsBak.delete();
        } catch (Exception ex) {
            // Failure: atomic rollback to backup state
            copyFile(prodBak, productsFile);
            copyFile(batchBak, batchesFile);
            copyFile(salesBak, salesFile);
            copyFile(itemsBak, saleItemsFile);

            prodBak.delete();
            batchBak.delete();
            salesBak.delete();
            itemsBak.delete();
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
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
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
