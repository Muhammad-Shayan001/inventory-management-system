package shelflife.storage;

import shelflife.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            throw new StorageException("Failed to initialize storage directory.", e);
        }
    }

    private void createFileIfNotExists(String filename, String header) throws IOException {
        Path p = dataDirectory.resolve(filename);
        if (!Files.exists(p)) {
            List<String> lines = Collections.singletonList(header);
            Files.write(p, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

    private void loadProducts() throws StorageException {
        Path p = dataDirectory.resolve("products.csv");
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<String> tokens = Csv.parseLine(line);
                if (tokens.size() < 5) continue;
                try {
                    Product pr = new Product(
                            tokens.get(0),
                            tokens.get(1),
                            tokens.get(2),
                            new BigDecimal(tokens.get(3)),
                            Integer.parseInt(tokens.get(4))
                    );
                    products.add(pr);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            throw new StorageException("Error reading products.csv", e);
        }
    }

    private void loadBatches() throws StorageException {
        Path p = dataDirectory.resolve("batches.csv");
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<String> tokens = Csv.parseLine(line);
                if (tokens.size() < 4) continue;
                try {
                    ProductBatch pb = new ProductBatch(
                            tokens.get(0),
                            tokens.get(1),
                            Integer.parseInt(tokens.get(2)),
                            LocalDate.parse(tokens.get(3), DATE_FMT)
                    );
                    batches.add(pb);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            throw new StorageException("Error reading batches.csv", e);
        }
    }

    private void loadSales() throws StorageException {
        Path p = dataDirectory.resolve("sales.csv");
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<String> tokens = Csv.parseLine(line);
                if (tokens.size() < 5) continue;
                try {
                    Sale s = new Sale(
                            tokens.get(0),
                            LocalDateTime.parse(tokens.get(1), DATE_TIME_FMT),
                            new BigDecimal(tokens.get(2)),
                            tokens.get(3),
                            tokens.get(4)
                    );
                    sales.add(s);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            throw new StorageException("Error reading sales.csv", e);
        }
    }

    private void loadSaleItems() throws StorageException {
        Path p = dataDirectory.resolve("sale_items.csv");
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<String> tokens = Csv.parseLine(line);
                if (tokens.size() < 6) continue;
                try {
                    SaleItem si = new SaleItem(
                            tokens.get(0),
                            tokens.get(1),
                            tokens.get(2),
                            Integer.parseInt(tokens.get(3)),
                            new BigDecimal(tokens.get(4)),
                            new BigDecimal(tokens.get(5))
                    );
                    saleItems.add(si);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            throw new StorageException("Error reading sale_items.csv", e);
        }
    }

    private void loadWastageRecords() throws StorageException {
        Path p = dataDirectory.resolve("wastage.csv");
        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<String> tokens = Csv.parseLine(line);
                if (tokens.size() < 5) continue;
                try {
                    WastageRecord wr = new WastageRecord(
                            tokens.get(0),
                            tokens.get(1),
                            Integer.parseInt(tokens.get(2)),
                            LocalDateTime.parse(tokens.get(3), DATE_TIME_FMT),
                            tokens.get(4)
                    );
                    wastageRecords.add(wr);
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            throw new StorageException("Error reading wastage.csv", e);
        }
    }

    @Override public synchronized List<Product> getProducts() { return new ArrayList<>(products); }
    @Override public synchronized List<ProductBatch> getBatches() { return new ArrayList<>(batches); }
    @Override public synchronized List<Sale> getSales() { return new ArrayList<>(sales); }
    @Override public synchronized List<SaleItem> getSaleItems() { return new ArrayList<>(saleItems); }
    @Override public synchronized List<WastageRecord> getWastageRecords() { return new ArrayList<>(wastageRecords); }

    private void safeAtomicWrite(String filename, List<String> lines) throws StorageException {
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
    public synchronized void saveProducts(List<Product> newProducts) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("productId,name,category,sellingPrice,lowStockThreshold");
        for (Product p : newProducts) {
            lines.add(String.format("%s,%s,%s,%s,%d",
                    Csv.escape(p.getProductId()),
                    Csv.escape(p.getName()),
                    Csv.escape(p.getCategory()),
                    p.getSellingPrice().toPlainString(),
                    p.getLowStockThreshold()));
        }
        safeAtomicWrite("products.csv", lines);
        this.products.clear();
        this.products.addAll(newProducts);
    }

    @Override
    public synchronized void saveBatches(List<ProductBatch> newBatches) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("batchId,productId,quantity,expiryDate");
        for (ProductBatch b : newBatches) {
            lines.add(String.format("%s,%s,%d,%s",
                    Csv.escape(b.getBatchId()),
                    Csv.escape(b.getProductId()),
                    b.getQuantity(),
                    b.getExpiryDate().format(DATE_FMT)));
        }
        safeAtomicWrite("batches.csv", lines);
        this.batches.clear();
        this.batches.addAll(newBatches);
    }

    @Override
    public synchronized void saveSales(List<Sale> newSales) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus");
        for (Sale s : newSales) {
            lines.add(String.format("%s,%s,%s,%s,%s",
                    Csv.escape(s.getSaleId()),
                    s.getSaleDateTime().format(DATE_TIME_FMT),
                    s.getTotalAmount().toPlainString(),
                    Csv.escape(s.getPaymentMethod()),
                    Csv.escape(s.getPaymentStatus())));
        }
        safeAtomicWrite("sales.csv", lines);
        this.sales.clear();
        this.sales.addAll(newSales);
    }

    @Override
    public synchronized void saveSaleItems(List<SaleItem> newItems) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,productId,batchId,quantity,unitPrice,lineTotal");
        for (SaleItem item : newItems) {
            lines.add(String.format("%s,%s,%s,%d,%s,%s",
                    Csv.escape(item.getSaleId()),
                    Csv.escape(item.getProductId()),
                    Csv.escape(item.getBatchId()),
                    item.getQuantity(),
                    item.getUnitPrice().toPlainString(),
                    item.getLineTotal().toPlainString()));
        }
        safeAtomicWrite("sale_items.csv", lines);
        this.saleItems.clear();
        this.saleItems.addAll(newItems);
    }

    @Override
    public synchronized void saveWastageRecords(List<WastageRecord> newRecords) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("wastageId,batchId,quantity,wastageDateTime,reason");
        for (WastageRecord r : newRecords) {
            lines.add(String.format("%s,%s,%d,%s,%s",
                    Csv.escape(r.getWastageId()),
                    Csv.escape(r.getBatchId()),
                    r.getQuantity(),
                    r.getWastageDateTime().format(DATE_TIME_FMT),
                    Csv.escape(r.getReason())));
        }
        safeAtomicWrite("wastage.csv", lines);
        this.wastageRecords.clear();
        this.wastageRecords.addAll(newRecords);
    }

    @Override
    public synchronized void commitSale(Sale sale, List<SaleItem> items, List<ProductBatch> updatedBatches) throws StorageException {
        writeJournal("BEGIN_SALE_TRANSACTION " + sale.getSaleId());
        try {
            List<Sale> updatedSales = new ArrayList<>(sales);
            updatedSales.add(sale);

            List<SaleItem> updatedItems = new ArrayList<>(saleItems);
            updatedItems.addAll(items);

            saveSales(updatedSales);
            saveSaleItems(updatedItems);
            saveBatches(updatedBatches);

            clearJournal();
        } catch (StorageException e) {
            loadAll();
            throw new StorageException("Transaction failed for Sale " + sale.getSaleId() + ". Restored state.", e);
        }
    }

    @Override
    public synchronized void commitWastage(WastageRecord wastage, List<ProductBatch> updatedBatches) throws StorageException {
        writeJournal("BEGIN_WASTAGE_TRANSACTION " + wastage.getWastageId());
        try {
            List<WastageRecord> updatedWastage = new ArrayList<>(wastageRecords);
            updatedWastage.add(wastage);

            saveWastageRecords(updatedWastage);
            saveBatches(updatedBatches);

            clearJournal();
        } catch (StorageException e) {
            loadAll();
            throw new StorageException("Transaction failed for Wastage " + wastage.getWastageId() + ". Restored state.", e);
        }
    }

    private void writeJournal(String entry) {
        try {
            Files.write(journalFile, Collections.singletonList(entry), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    private void clearJournal() {
        try { Files.deleteIfExists(journalFile); } catch (IOException ignored) {}
    }

    private void recoverJournalIfNeeded() {
        if (Files.exists(journalFile)) {
            try {
                Files.deleteIfExists(journalFile);
            } catch (IOException ignored) {}
        }
    }
}
