package shelflife.service;

import shelflife.model.Customer;
import shelflife.model.CustomerLedgerEntry;
import shelflife.model.PaymentMethod;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementation of CustomerCreditService managing customers, credit limits,
 * and immutable customer ledger transactions.
 */
public class CustomerCreditServiceImpl implements CustomerCreditService {

    private final DataStore dataStore;
    private final List<Customer> customers = new ArrayList<>();
    private final List<CustomerLedgerEntry> ledgerEntries = new ArrayList<>();

    public CustomerCreditServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            customers.clear();
            customers.addAll(dataStore.loadCustomers());
            ledgerEntries.clear();
            ledgerEntries.addAll(dataStore.loadCustomerLedger());
        } catch (Exception e) {
            System.err.println("Warning: Failed to load customer credit data: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<Customer> getAllCustomers() {
        return Collections.unmodifiableList(new ArrayList<>(customers));
    }

    @Override
    public synchronized Customer getCustomer(String customerId) {
        if (customerId == null) return null;
        for (Customer c : customers) {
            if (c.getCustomerId().equalsIgnoreCase(customerId.trim())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public synchronized void addCustomer(Customer customer) throws Exception {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }
        if (getCustomer(customer.getCustomerId()) != null) {
            throw new IllegalArgumentException("Customer with ID '" + customer.getCustomerId() + "' already exists.");
        }

        dataStore.executeTransaction(() -> {
            customers.add(customer);
            dataStore.saveCustomers(customers);
        });
    }

    @Override
    public synchronized void updateCustomer(Customer customer) throws Exception {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }
        Customer existing = getCustomer(customer.getCustomerId());
        if (existing == null) {
            throw new IllegalArgumentException("Customer not found: " + customer.getCustomerId());
        }

        dataStore.executeTransaction(() -> {
            existing.setFullName(customer.getFullName());
            existing.setPhoneNumber(customer.getPhoneNumber());
            existing.setAddress(customer.getAddress());
            existing.setCreditLimit(customer.getCreditLimit());
            existing.setActive(customer.isActive());
            dataStore.saveCustomers(customers);
        });
    }

    @Override
    public synchronized List<Customer> searchCustomers(String query) {
        List<Customer> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return getAllCustomers();
        }
        String q = query.trim().toLowerCase();
        for (Customer c : customers) {
            if (c.getCustomerId().toLowerCase().contains(q) ||
                c.getFullName().toLowerCase().contains(q) ||
                c.getPhoneNumber().toLowerCase().contains(q)) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public synchronized boolean hasSufficientCredit(String customerId, BigDecimal amount) {
        if (customerId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        Customer c = getCustomer(customerId);
        if (c == null || !c.isActive()) {
            return false;
        }
        BigDecimal newBalance = c.getCurrentBalance().add(amount);
        return newBalance.compareTo(c.getCreditLimit()) <= 0;
    }

    @Override
    public synchronized void recordCreditSale(String customerId, String saleId, BigDecimal amount, String cashierUsername) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit sale amount must be positive.");
        }
        Customer c = getCustomer(customerId);
        if (c == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        if (!c.isActive()) {
            throw new IllegalArgumentException("Customer account is deactivated: " + c.getFullName());
        }

        BigDecimal newBalance = c.getCurrentBalance().add(amount);
        if (newBalance.compareTo(c.getCreditLimit()) > 0) {
            throw new IllegalArgumentException("Credit limit exceeded for '" + c.getFullName() + 
                    "'. Limit: Rs. " + c.getCreditLimit() + ", Current: Rs. " + c.getCurrentBalance() + ", Requested: Rs. " + amount);
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String entryId = "LEDG-" + stamp + "-" + (int)(Math.random() * 900 + 100);

        CustomerLedgerEntry entry = new CustomerLedgerEntry(entryId, customerId, LocalDateTime.now(),
                "SALE_ON_CREDIT", saleId, amount, BigDecimal.ZERO, newBalance,
                "Credit purchase on POS checkout", cashierUsername);

        dataStore.executeTransaction(() -> {
            c.setCurrentBalance(newBalance);
            ledgerEntries.add(entry);
            dataStore.saveCustomers(customers);
            dataStore.saveCustomerLedger(ledgerEntries);
        });
    }

    @Override
    public synchronized CustomerLedgerEntry recordPayment(String customerId, BigDecimal amount, PaymentMethod method,
                                                          String referenceNumber, String notes, String cashierUsername) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        Customer c = getCustomer(customerId);
        if (c == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }

        BigDecimal newBalance = c.getCurrentBalance().subtract(amount);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String entryId = "LEDG-PAY-" + stamp + "-" + (int)(Math.random() * 900 + 100);

        String memo = "Payment received (" + (method != null ? method.getDisplayName() : "Cash") + 
                (referenceNumber != null && !referenceNumber.isEmpty() ? " Ref: " + referenceNumber : "") + 
                (notes != null && !notes.isEmpty() ? " - " + notes : "");

        CustomerLedgerEntry entry = new CustomerLedgerEntry(entryId, customerId, LocalDateTime.now(),
                "PAYMENT_RECEIVED", referenceNumber, BigDecimal.ZERO, amount, newBalance, memo, cashierUsername);

        dataStore.executeTransaction(() -> {
            c.setCurrentBalance(newBalance);
            ledgerEntries.add(entry);
            dataStore.saveCustomers(customers);
            dataStore.saveCustomerLedger(ledgerEntries);
        });

        return entry;
    }

    @Override
    public synchronized List<CustomerLedgerEntry> getCustomerLedger(String customerId) {
        List<CustomerLedgerEntry> list = new ArrayList<>();
        if (customerId == null) return list;
        for (CustomerLedgerEntry e : ledgerEntries) {
            if (e.getCustomerId().equalsIgnoreCase(customerId.trim())) {
                list.add(e);
            }
        }
        list.sort(Comparator.comparing(CustomerLedgerEntry::getTimestamp));
        return list;
    }
}
