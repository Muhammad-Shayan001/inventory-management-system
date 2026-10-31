package shelflife.service;

import shelflife.model.SupplierPayable;
import shelflife.model.SupplierPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for supplier credit and payables management.
 */
public interface SupplierPayableService {

    List<SupplierPayable> getAllPayables();

    SupplierPayable getPayable(String payableId);

    void addPayable(SupplierPayable payable) throws Exception;

    default SupplierPayable recordPayable(String supplierId, String purchaseRef, BigDecimal amount,
                                          LocalDate dueDate, String notes) throws Exception {
        String id = "PAY-" + System.currentTimeMillis();
        SupplierPayable sp = new SupplierPayable(id, supplierId, purchaseRef, LocalDate.now(), dueDate, amount, BigDecimal.ZERO, amount, "UNPAID", notes);
        addPayable(sp);
        return sp;
    }

    SupplierPayment recordPayment(String payableId, BigDecimal amount, String method,
                                  String referenceNumber, String notes, String recordedBy) throws Exception;

    List<SupplierPayment> getPaymentsForPayable(String payableId);

    List<SupplierPayable> getOverduePayables();

    void reload();
}
