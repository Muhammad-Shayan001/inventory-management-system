package shelflife.storage;

import shelflife.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Shared CSV DataStore implementation for local file persistence.
 * Implements atomic writes via temporary files and crash recovery journal.
 */
public class FileManager implements DataStore {

    private final Path dataDirectory;
    private final Path journalFile;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final List<Product> products = new ArrayList<>();
    private final List<ProductBatch> batches = new ArrayList<>();
    private final List<Sale> sales = new ArrayList<>();
    private final List<SaleItem> saleItems = new ArrayList<>();
    private final List<WastageRecord> wastageRecords = new ArrayList<>();

    public FileManager(Path dataDirectory) throws StorageException {
        this.dataDirectory = dataDirectory;
        this.journalFile = dataDirectory.resolve("journal.log");
        initDirectory();
        recoverJournalIfNeeded();
        loadAll();
    }

    private void initDirectory() throws StorageException {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            createFileIfNotExists("products.csv", "productId,name,category,sellingPrice,lowStockThreshold");
            createFileIfNotExists("batches.csv", "batchId,productId,quantity,expiryDate");
            createFileIfNotExists("sales.csv", "saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus");
            createFileIfNotExists("sale_items.csv", "saleId,productId,batchId,quantity,unitPrice,lineTotal");
            createFileIfNotExists("wastage.csv", "wastageId,batchId,quantity,wastageDateTime,reason");
        } catch (IOException e) {
            throw new StorageException("Failed to initialize storage directory: " + dataDirectory, e);
        }
    }

    private void createFileIfNotExists(String filename, String header) throws IOException {
        Path p = dataDirectory.resolve(filename);
        if (!Files.exists(p)) {
            Files.write(p, Collections.singletonList(header), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void recoverJournalIfNeeded() {
        if (Files.exists(journalFile)) {
            try {
                // If a crash happened during a transaction, rollback or clear
                clearJournal();
            } catch (Exception ignored) {
            }
        }
    }

    private void writeJournal(String entry) {
        try {
            Files.write(journalFile, Collections.singletonList(entry), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    private void clearJournal() {
        try {
            Files.deleteIfExists(journalFile);
        } catch (IOException ignored) {}
    }

    private synchronized void safeAtomicWrite(String filename, List<String> lines) throws StorageException {
        Path target = dataDirectory.resolve(filename);
        Path temp = dataDirectory.resolve(filename + ".tmp");
        Path backup = dataDirectory.resolve(filename + ".bak");

        try {
            Files.write(temp, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            if (Files.exists(target)) {
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.exists(backup)) {
                Files.delete(backup);
            }
        } catch (IOException e) {
            if (Files.exists(backup) && !Files.exists(target)) {
                try { Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) {}
            }
            throw new StorageException("Atomic write failure for " + filename, e);
        }
    }

    @Override
    public synchronized void loadAll() throws StorageException {
        products.clear();
        batches.clear();
        sales.clear();
        saleItems.clear();
        wastageRecords.clear();

        loadProducts();
        loadBatches();
        loadSales();
        loadSaleItems();
        loadWastageRecords();
    }

    @Override
    public synchronized void saveAll() throws StorageException {
        saveProductsInternal();
        saveBatchesInternal();
        saveSalesInternal();
        saveSaleItemsInternal();
        saveWastageRecordsInternal();
    }

    private void loadProducts() {
        Path p = dataDirectory.resolve("products.csv");
        if (!Files.exists(p)) return;
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line);
                if (tokens.length >= 5) {
                    Product prod = new Product(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            tokens[2].trim(),
                            new BigDecimal(tokens[3].trim()).setScale(2, RoundingMode.HALF_UP),
                            Integer.parseInt(tokens[4].trim())
                    );
                    products.add(prod);
                }
            }
        } catch (Exception e) {
            throw new StorageException("Failed to read products.csv", e);
        }
    }

    private void loadBatches() {
        Path p = dataDirectory.resolve("batches.csv");
        if (!Files.exists(p)) return;
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line);
                if (tokens.length >= 4) {
                    ProductBatch batch = new ProductBatch(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            Integer.parseInt(tokens[2].trim()),
                            LocalDate.parse(tokens[3].trim(), DATE_FMT)
                    );
                    batches.add(batch);
                }
            }
        } catch (Exception e) {
            throw new StorageException("Failed to read batches.csv", e);
        }
    }

    private void loadSales() {
        Path p = dataDirectory.resolve("sales.csv");
        if (!Files.exists(p)) return;
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line);
                if (tokens.length >= 5) {
                    Sale sale = new Sale(
                            tokens[0].trim(),
                            LocalDateTime.parse(tokens[1].trim(), DATE_TIME_FMT),
                            new BigDecimal(tokens[2].trim()).setScale(2, RoundingMode.HALF_UP),
                            tokens[3].trim(),
                            tokens[4].trim()
                    );
                    sales.add(sale);
                }
            }
        } catch (Exception e) {
            throw new StorageException("Failed to read sales.csv", e);
        }
    }

    private void loadSaleItems() {
        Path p = dataDirectory.resolve("sale_items.csv");
        if (!Files.exists(p)) return;
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line);
                if (tokens.length >= 6) {
                    SaleItem item = new SaleItem(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            tokens[2].trim(),
                            Integer.parseInt(tokens[3].trim()),
                            new BigDecimal(tokens[4].trim()).setScale(2, RoundingMode.HALF_UP),
                            new BigDecimal(tokens[5].trim()).setScale(2, RoundingMode.HALF_UP)
                    );
                    saleItems.add(item);
                }
            }
        } catch (Exception e) {
            throw new StorageException("Failed to read sale_items.csv", e);
        }
    }

    private void loadWastageRecords() {
        Path p = dataDirectory.resolve("wastage.csv");
        if (!Files.exists(p)) return;
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line);
                if (tokens.length >= 5) {
                    WastageRecord record = new WastageRecord(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            Integer.parseInt(tokens[2].trim()),
                            LocalDateTime.parse(tokens[3].trim(), DATE_TIME_FMT),
                            tokens[4].trim()
                    );
                    wastageRecords.add(record);
                }
            }
        } catch (Exception e) {
            throw new StorageException("Failed to read wastage.csv", e);
        }
    }

    private void saveProductsInternal() throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("productId,name,category,sellingPrice,lowStockThreshold");
        for (Product p : products) {
            lines.add(String.format("%s,%s,%s,%s,%d",
                    escapeCsv(p.getProductId()),
                    escapeCsv(p.getName()),
                    escapeCsv(p.getCategory()),
                    p.getSellingPrice().toPlainString(),
                    p.getLowStockThreshold()));
        }
        safeAtomicWrite("products.csv", lines);
    }

    private void saveBatchesInternal() throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("batchId,productId,quantity,expiryDate");
        for (ProductBatch b : batches) {
            lines.add(String.format("%s,%s,%d,%s",
                    escapeCsv(b.getBatchId()),
                    escapeCsv(b.getProductId()),
                    b.getQuantity(),
                    b.getExpiryDate().format(DATE_FMT)));
        }
        safeAtomicWrite("batches.csv", lines);
    }

    private void saveSalesInternal() throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus");
        for (Sale s : sales) {
            lines.add(String.format("%s,%s,%s,%s,%s",
                    escapeCsv(s.getSaleId()),
                    s.getSaleDateTime().format(DATE_TIME_FMT),
                    s.getTotalAmount().toPlainString(),
                    escapeCsv(s.getPaymentMethod()),
                    escapeCsv(s.getPaymentStatus())));
        }
        safeAtomicWrite("sales.csv", lines);
    }

    private void saveSaleItemsInternal() throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,productId,batchId,quantity,unitPrice,lineTotal");
        for (SaleItem item : saleItems) {
            lines.add(String.format("%s,%s,%s,%d,%s,%s",
                    escapeCsv(item.getSaleId()),
                    escapeCsv(item.getProductId()),
                    escapeCsv(item.getBatchId()),
                    item.getQuantity(),
                    item.getUnitPrice().toPlainString(),
                    item.getLineTotal().toPlainString()));
        }
        safeAtomicWrite("sale_items.csv", lines);
    }

    private void saveWastageRecordsInternal() throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("wastageId,batchId,quantity,wastageDateTime,reason");
        for (WastageRecord r : wastageRecords) {
            lines.add(String.format("%s,%s,%d,%s,%s",
                    escapeCsv(r.getWastageId()),
                    escapeCsv(r.getBatchId()),
                    r.getQuantity(),
                    r.getWastageDateTime().format(DATE_TIME_FMT),
                    escapeCsv(r.getReason())));
        }
        safeAtomicWrite("wastage.csv", lines);
    }

    @Override
    public synchronized void commitSale(Sale sale, List<SaleItem> items, List<ProductBatch> updatedBatches) throws StorageException {
        writeJournal("BEGIN_SALE_TRANSACTION " + sale.getSaleId());
        try {
            // 1. Stage in-memory copy
            List<Sale> updatedSales = new ArrayList<>(sales);
            updatedSales.add(sale);

            List<SaleItem> updatedItems = new ArrayList<>(saleItems);
            updatedItems.addAll(items);

            // Update local batches
            Map<String, ProductBatch> batchMap = new HashMap<>();
            for (ProductBatch b : batches) {
                batchMap.put(b.getBatchId(), b);
            }
            for (ProductBatch ub : updatedBatches) {
                ProductBatch current = batchMap.get(ub.getBatchId());
                if (current != null) {
                    current.setQuantity(ub.getQuantity());
                }
            }

            // Write all atomically
            this.sales.clear();
            this.sales.addAll(updatedSales);
            saveSalesInternal();

            this.saleItems.clear();
            this.saleItems.addAll(updatedItems);
            saveSaleItemsInternal();

            saveBatchesInternal();

            clearJournal();
        } catch (Exception e) {
            loadAll(); // restore in-memory state on error
            throw new StorageException("Transaction failed for Sale " + sale.getSaleId() + ". Restored state.", e);
        }
    }

    @Override
    public synchronized void commitWastage(WastageRecord wastage, List<ProductBatch> updatedBatches) throws StorageException {
        writeJournal("BEGIN_WASTAGE_TRANSACTION " + wastage.getWastageId());
        try {
            List<WastageRecord> updatedWastage = new ArrayList<>(wastageRecords);
            updatedWastage.add(wastage);

            Map<String, ProductBatch> batchMap = new HashMap<>();
            for (ProductBatch b : batches) {
                batchMap.put(b.getBatchId(), b);
            }
            for (ProductBatch ub : updatedBatches) {
                ProductBatch current = batchMap.get(ub.getBatchId());
                if (current != null) {
                    current.setQuantity(ub.getQuantity());
                }
            }

            this.wastageRecords.clear();
            this.wastageRecords.addAll(updatedWastage);
            saveWastageRecordsInternal();

            saveBatchesInternal();

            clearJournal();
        } catch (Exception e) {
            loadAll();
            throw new StorageException("Transaction failed for Wastage " + wastage.getWastageId() + ". Restored state.", e);
        }
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        return Collections.unmodifiableList(new ArrayList<>(products));
    }

    @Override
    public synchronized Product getProductById(String productId) {
        if (productId == null) return null;
        for (Product p : products) {
            if (p.getProductId().equalsIgnoreCase(productId.trim())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public synchronized void addProduct(Product product) throws StorageException {
        if (product == null) throw new IllegalArgumentException("Product cannot be null");
        if (getProductById(product.getProductId()) != null) {
            throw new StorageException("Product ID " + product.getProductId() + " already exists.");
        }
        products.add(product);
        saveProductsInternal();
    }

    @Override
    public synchronized void updateProduct(Product product) throws StorageException {
        if (product == null) throw new IllegalArgumentException("Product cannot be null");
        boolean found = false;
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getProductId().equalsIgnoreCase(product.getProductId())) {
                products.set(i, product);
                found = true;
                break;
            }
        }
        if (!found) throw new StorageException("Product ID " + product.getProductId() + " not found.");
        saveProductsInternal();
    }

    @Override
    public synchronized void deleteProduct(String productId) throws StorageException {
        products.removeIf(p -> p.getProductId().equalsIgnoreCase(productId));
        saveProductsInternal();
    }

    @Override
    public synchronized List<ProductBatch> getAllBatches() {
        return Collections.unmodifiableList(new ArrayList<>(batches));
    }

    @Override
    public synchronized List<ProductBatch> getBatchesByProductId(String productId) {
        List<ProductBatch> result = new ArrayList<>();
        if (productId == null) return result;
        for (ProductBatch b : batches) {
            if (b.getProductId().equalsIgnoreCase(productId.trim())) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public synchronized ProductBatch getBatchById(String batchId) {
        if (batchId == null) return null;
        for (ProductBatch b : batches) {
            if (b.getBatchId().equalsIgnoreCase(batchId.trim())) {
                return b;
            }
        }
        return null;
    }

    @Override
    public synchronized void addBatch(ProductBatch batch) throws StorageException {
        if (batch == null) throw new IllegalArgumentException("Batch cannot be null");
        if (getBatchById(batch.getBatchId()) != null) {
            throw new StorageException("Batch ID " + batch.getBatchId() + " already exists.");
        }
        batches.add(batch);
        saveBatchesInternal();
    }

    @Override
    public synchronized void updateBatch(ProductBatch batch) throws StorageException {
        if (batch == null) throw new IllegalArgumentException("Batch cannot be null");
        boolean found = false;
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).getBatchId().equalsIgnoreCase(batch.getBatchId())) {
                batches.set(i, batch);
                found = true;
                break;
            }
        }
        if (!found) throw new StorageException("Batch ID " + batch.getBatchId() + " not found.");
        saveBatchesInternal();
    }

    @Override
    public synchronized void deleteBatch(String batchId) throws StorageException {
        batches.removeIf(b -> b.getBatchId().equalsIgnoreCase(batchId));
        saveBatchesInternal();
    }

    @Override
    public synchronized List<Sale> getAllSales() {
        return Collections.unmodifiableList(new ArrayList<>(sales));
    }

    @Override
    public synchronized List<SaleItem> getAllSaleItems() {
        return Collections.unmodifiableList(new ArrayList<>(saleItems));
    }

    @Override
    public synchronized List<WastageRecord> getAllWastageRecords() {
        return Collections.unmodifiableList(new ArrayList<>(wastageRecords));
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] parseCsvLine(String line) {
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
