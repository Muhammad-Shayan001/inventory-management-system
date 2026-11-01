package shelflife.service;

import shelflife.model.CapitalTransaction;
import shelflife.storage.DataStore;
import shelflife.util.CurrencyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Standard implementation of CapitalService.
 * Thread-safe with atomic multi-file transactional persistence.
 */
public class CapitalServiceImpl implements CapitalService {

    private final DataStore dataStore;
    private final List<CapitalTransaction> transactions = new ArrayList<>();

    public CapitalServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        transactions.clear();
        try {
            List<CapitalTransaction> loaded = dataStore.loadCapitalTransactions();
            if (loaded != null) {
                transactions.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load capital transactions: " + e.getMessage());
        }
    }

    @Override
    public synchronized CapitalTransaction recordContribution(BigDecimal amount, String investorName,
                                                              String paymentMethod, String destinationAccount,
                                                              String description, String createdBy) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Capital contribution amount must be greater than zero.");
        }

        String id = "CAP-IN-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 900 + 100);
        CapitalTransaction tx = new CapitalTransaction(
                id,
                CapitalTransaction.Type.CONTRIBUTION,
                amount,
                LocalDateTime.now(),
                investorName != null ? investorName : "Owner",
                paymentMethod != null ? paymentMethod : "CASH",
                destinationAccount != null ? destinationAccount : "MAIN_DRAWER",
                description != null ? description : "Capital Contribution",
                createdBy != null ? createdBy : "admin"
        );

        dataStore.executeTransaction(() -> {
            List<CapitalTransaction> list = dataStore.loadCapitalTransactions();
            list.add(tx);
            dataStore.saveCapitalTransactions(list);
        });

        reload();
        return tx;
    }

    @Override
    public synchronized CapitalTransaction recordWithdrawal(BigDecimal amount, String investorName,
                                                            String paymentMethod, String sourceAccount,
                                                            String description, String createdBy) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Owner withdrawal amount must be greater than zero.");
        }

        String id = "CAP-OUT-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 900 + 100);
        CapitalTransaction tx = new CapitalTransaction(
                id,
                CapitalTransaction.Type.WITHDRAWAL,
                amount,
                LocalDateTime.now(),
                investorName != null ? investorName : "Owner",
                paymentMethod != null ? paymentMethod : "CASH",
                sourceAccount != null ? sourceAccount : "MAIN_DRAWER",
                description != null ? description : "Owner Personal Withdrawal",
                createdBy != null ? createdBy : "admin"
        );

        dataStore.executeTransaction(() -> {
            List<CapitalTransaction> list = dataStore.loadCapitalTransactions();
            list.add(tx);
            dataStore.saveCapitalTransactions(list);
        });

        reload();
        return tx;
    }

    @Override
    public synchronized BigDecimal getTotalCapitalContributed() {
        BigDecimal total = BigDecimal.ZERO;
        for (CapitalTransaction t : transactions) {
            if (t.getType() == CapitalTransaction.Type.CONTRIBUTION) {
                total = total.add(t.getAmount());
            }
        }
        return CurrencyUtil.roundMoney(total);
    }

    @Override
    public synchronized BigDecimal getTotalOwnerWithdrawals() {
        BigDecimal total = BigDecimal.ZERO;
        for (CapitalTransaction t : transactions) {
            if (t.getType() == CapitalTransaction.Type.WITHDRAWAL) {
                total = total.add(t.getAmount());
            }
        }
        return CurrencyUtil.roundMoney(total);
    }

    @Override
    public synchronized BigDecimal getNetOwnerCapital() {
        return getTotalCapitalContributed().subtract(getTotalOwnerWithdrawals());
    }

    @Override
    public synchronized List<CapitalTransaction> getAllTransactions() {
        List<CapitalTransaction> copy = new ArrayList<>(transactions);
        copy.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
        return Collections.unmodifiableList(copy);
    }
}
