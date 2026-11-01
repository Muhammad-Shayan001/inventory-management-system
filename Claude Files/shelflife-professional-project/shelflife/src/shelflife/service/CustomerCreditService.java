package shelflife.service;

import shelflife.model.Customer;
import shelflife.model.CustomerLedgerEntry;
import shelflife.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for customer credit management (Khata / Udhaar).
 */
public interface CustomerCreditService {

    List<Customer> getAllCustomers();

    Customer getCustomer(String customerId);

    void addCustomer(Customer customer) throws Exception;

    void updateCustomer(Customer customer) throws Exception;

    List<Customer> searchCustomers(String query);

    void recordCreditSale(String customerId, String saleId, BigDecimal amount, String cashierUsername) throws Exception;

    CustomerLedgerEntry recordPayment(String customerId, BigDecimal amount, PaymentMethod method, String referenceNumber, String notes, String cashierUsername) throws Exception;

    List<CustomerLedgerEntry> getCustomerLedger(String customerId);

    boolean hasSufficientCredit(String customerId, BigDecimal amount);

    void reload();
}
