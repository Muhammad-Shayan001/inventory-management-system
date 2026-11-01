package shelflife.service;

import shelflife.model.ProductBatch;
import shelflife.model.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for managing vendor/supplier directories and stock-in purchase intake.
 */
public interface SupplierService {

    List<Supplier> getAllSuppliers();

    List<Supplier> getActiveSuppliers();

    Supplier getSupplierById(String supplierId);

    void addSupplier(Supplier supplier) throws Exception;

    void updateSupplier(Supplier supplier) throws Exception;

    void deactivateSupplier(String supplierId) throws Exception;

    ProductBatch receiveStock(String productId, String batchId, int quantity, LocalDate expiryDate, BigDecimal costPrice, String supplierId) throws Exception;

    java.util.Map<String, Object> getSupplierPerformance(String supplierId);

    List<java.util.Map<String, Object>> getAllSupplierPerformanceReports();

    void reload();
}
