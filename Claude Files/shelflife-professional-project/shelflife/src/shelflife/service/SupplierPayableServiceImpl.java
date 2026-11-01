package shelflife.service;

import shelflife.model.SupplierPayable;
import shelflife.model.SupplierPayment;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementation of SupplierPayableService.
 */
public class SupplierPayableServiceImpl implements SupplierPayableService {

    private final DataStore dataStore;
    private final List<SupplierPayable> payables = new ArrayList<>();
    private final List<SupplierPayment> payments = new ArrayList<>();

    public SupplierPayableServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            payables.clear();
            payables.addAll(dataStore.loadSupplierPayables());
            payments.clear();
            payments.addAll(dataStore.loadSupplierPayments());
        } catch (Exception e) {
            System.err.println("Warning: Failed to load supplier payables: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<SupplierPayable> getAllPayables() {
        return Collections.unmodifiableList(new ArrayList<>(payables));
    }

    @Override
    public synchronized SupplierPayable getPayable(String payableId) {
        if (payableId == null) return null;
        for (SupplierPayable p : payables) {
            if (p.getPayableId().equalsIgnoreCase(payableId.trim())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public synchronized void addPayable(SupplierPayable payable) throws Exception {
        if (payable == null) {
            throw new IllegalArgumentException("Payable cannot be null.");
        }
        if (getPayable(payable.getPayableId()) != null) {
            throw new IllegalArgumentException("Payable with ID '" + payable.getPayableId() + "' already exists.");
        }

        dataStore.executeTransaction(() -> {
            payables.add(payable);
            dataStore.saveSupplierPayables(payables);
        });
    }

    @Override
    public synchronized SupplierPayment recordPayment(String payableId, BigDecimal amount, String method,
                                                      String referenceNumber, String notes, String recordedBy) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        SupplierPayable p = getPayable(payableId);
        if (p == null) {
            throw new IllegalArgumentException("Payable not found: " + payableId);
        }

        if (amount.compareTo(p.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment amount (Rs. " + amount + ") exceeds balance due (Rs. " + p.getBalanceDue() + ").");
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String paymentId = "SPAY-" + stamp + "-" + (int)(Math.random() * 900 + 100);

        SupplierPayment sp = new SupplierPayment(paymentId, payableId, p.getSupplierId(),
                LocalDate.now(), amount, method, referenceNumber, notes, recordedBy);

        dataStore.executeTransaction(() -> {
            p.setPaidAmount(p.getPaidAmount().add(amount));
            payments.add(sp);
            dataStore.saveSupplierPayables(payables);
            dataStore.saveSupplierPayments(payments);
        });

        return sp;
    }

    @Override
    public synchronized List<SupplierPayment> getPaymentsForPayable(String payableId) {
        List<SupplierPayment> list = new ArrayList<>();
        if (payableId == null) return list;
        for (SupplierPayment sp : payments) {
            if (sp.getPayableId().equalsIgnoreCase(payableId.trim())) {
                list.add(sp);
            }
        }
        return list;
    }

    @Override
    public synchronized List<SupplierPayable> getOverduePayables() {
        List<SupplierPayable> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (SupplierPayable p : payables) {
            if (p.getBalanceDue().compareTo(BigDecimal.ZERO) > 0 && p.getDueDate() != null && p.getDueDate().isBefore(today)) {
                list.add(p);
            }
        }
        return list;
    }
}
