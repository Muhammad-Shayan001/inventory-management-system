package shelflife.service;

import shelflife.model.Account;
import shelflife.model.AccountTransaction;
import shelflife.storage.DataStore;
import shelflife.util.CurrencyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Standard implementation of CashAccountService.
 */
public class CashAccountServiceImpl implements CashAccountService {

    private final DataStore dataStore;
    private final List<Account> accounts = new ArrayList<>();
    private final List<AccountTransaction> transactions = new ArrayList<>();

    public CashAccountServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
        seedDefaultAccountsIfEmpty();
    }

    private void seedDefaultAccountsIfEmpty() {
        if (accounts.isEmpty()) {
            try {
                Account main = new Account("ACC-01", "Main Cash Drawer", Account.Type.CASH, BigDecimal.ZERO, BigDecimal.ZERO, true);
                Account bank = new Account("ACC-02", "Business Bank Account", Account.Type.BANK, BigDecimal.ZERO, BigDecimal.ZERO, true);
                Account ep = new Account("ACC-03", "Easypaisa Account", Account.Type.MOBILE_WALLET, BigDecimal.ZERO, BigDecimal.ZERO, true);
                Account jz = new Account("ACC-04", "JazzCash Account", Account.Type.MOBILE_WALLET, BigDecimal.ZERO, BigDecimal.ZERO, true);

                List<Account> defaults = Arrays.asList(main, bank, ep, jz);
                dataStore.saveAccounts(defaults);
                accounts.addAll(defaults);
            } catch (Exception e) {
                System.err.println("Warning: Failed to seed default accounts: " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized void reload() {
        accounts.clear();
        transactions.clear();
        try {
            List<Account> loadedAccounts = dataStore.loadAccounts();
            if (loadedAccounts != null) {
                accounts.addAll(loadedAccounts);
            }
            List<AccountTransaction> loadedTxs = dataStore.loadAccountTransactions();
            if (loadedTxs != null) {
                transactions.addAll(loadedTxs);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to reload cash accounts: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<Account> getAllAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts));
    }

    @Override
    public synchronized Account getAccount(String accountId) {
        if (accountId == null) return null;
        for (Account a : accounts) {
            if (a.getAccountId().equalsIgnoreCase(accountId.trim())) {
                return a;
            }
        }
        return null;
    }

    @Override
    public synchronized void addAccount(Account account) throws Exception {
        if (account == null) throw new IllegalArgumentException("Account cannot be null.");
        dataStore.executeTransaction(() -> {
            List<Account> list = dataStore.loadAccounts();
            list.add(account);
            dataStore.saveAccounts(list);
        });
        reload();
    }

    @Override
    public synchronized void updateAccount(Account account) throws Exception {
        if (account == null) throw new IllegalArgumentException("Account cannot be null.");
        dataStore.executeTransaction(() -> {
            List<Account> list = dataStore.loadAccounts();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getAccountId().equalsIgnoreCase(account.getAccountId())) {
                    list.set(i, account);
                    break;
                }
            }
            dataStore.saveAccounts(list);
        });
        reload();
    }

    @Override
    public synchronized AccountTransaction recordInflow(String accountId, BigDecimal amount,
                                                        AccountTransaction.Type type, String referenceId,
                                                        String description, String createdBy) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Inflow amount must be greater than zero.");
        }
        Account acc = getAccount(accountId);
        if (acc == null) {
            // Fallback to first available cash drawer
            acc = !accounts.isEmpty() ? accounts.get(0) : null;
            if (acc == null) throw new IllegalArgumentException("Account not found: " + accountId);
        }

        BigDecimal newBalance = acc.getCurrentBalance().add(amount);
        acc.setCurrentBalance(newBalance);
        acc.setUpdatedAt(LocalDateTime.now());

        String txId = "TX-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 900 + 100);
        AccountTransaction tx = new AccountTransaction(
                txId,
                acc.getAccountId(),
                LocalDateTime.now(),
                type != null ? type : AccountTransaction.Type.SALES_COLLECTION,
                amount,
                BigDecimal.ZERO,
                newBalance,
                referenceId != null ? referenceId : "",
                description != null ? description : "",
                createdBy != null ? createdBy : "admin"
        );

        final Account targetAccount = acc;
        dataStore.executeTransaction(() -> {
            List<Account> accList = dataStore.loadAccounts();
            for (int i = 0; i < accList.size(); i++) {
                if (accList.get(i).getAccountId().equalsIgnoreCase(targetAccount.getAccountId())) {
                    accList.set(i, targetAccount);
                    break;
                }
            }
            dataStore.saveAccounts(accList);

            List<AccountTransaction> txList = dataStore.loadAccountTransactions();
            txList.add(tx);
            dataStore.saveAccountTransactions(txList);
        });

        reload();
        return tx;
    }

    @Override
    public synchronized AccountTransaction recordOutflow(String accountId, BigDecimal amount,
                                                         AccountTransaction.Type type, String referenceId,
                                                         String description, String createdBy) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Outflow amount must be greater than zero.");
        }
        Account acc = getAccount(accountId);
        if (acc == null) {
            acc = !accounts.isEmpty() ? accounts.get(0) : null;
            if (acc == null) throw new IllegalArgumentException("Account not found: " + accountId);
        }

        BigDecimal newBalance = acc.getCurrentBalance().subtract(amount);
        acc.setCurrentBalance(newBalance);
        acc.setUpdatedAt(LocalDateTime.now());

        String txId = "TX-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 900 + 100);
        AccountTransaction tx = new AccountTransaction(
                txId,
                acc.getAccountId(),
                LocalDateTime.now(),
                type != null ? type : AccountTransaction.Type.OPERATING_EXPENSE,
                BigDecimal.ZERO,
                amount,
                newBalance,
                referenceId != null ? referenceId : "",
                description != null ? description : "",
                createdBy != null ? createdBy : "admin"
        );

        final Account targetAccount = acc;
        dataStore.executeTransaction(() -> {
            List<Account> accList = dataStore.loadAccounts();
            for (int i = 0; i < accList.size(); i++) {
                if (accList.get(i).getAccountId().equalsIgnoreCase(targetAccount.getAccountId())) {
                    accList.set(i, targetAccount);
                    break;
                }
            }
            dataStore.saveAccounts(accList);

            List<AccountTransaction> txList = dataStore.loadAccountTransactions();
            txList.add(tx);
            dataStore.saveAccountTransactions(txList);
        });

        reload();
        return tx;
    }

    @Override
    public synchronized void transferFunds(String sourceAccountId, String destinationAccountId,
                                           BigDecimal amount, String description, String createdBy) throws Exception {
        if (sourceAccountId.equalsIgnoreCase(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
        recordOutflow(sourceAccountId, amount, AccountTransaction.Type.TRANSFER_OUT, destinationAccountId, "Transfer to " + destinationAccountId + ": " + description, createdBy);
        recordInflow(destinationAccountId, amount, AccountTransaction.Type.TRANSFER_IN, sourceAccountId, "Transfer from " + sourceAccountId + ": " + description, createdBy);
    }

    @Override
    public synchronized BigDecimal getTotalCashAndBankBalance() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Account a : accounts) {
            if (a.isActive()) {
                sum = sum.add(a.getCurrentBalance());
            }
        }
        return CurrencyUtil.roundMoney(sum);
    }

    @Override
    public synchronized List<AccountTransaction> getAccountTransactions(String accountId) {
        List<AccountTransaction> list = new ArrayList<>();
        if (accountId == null) return list;
        for (AccountTransaction t : transactions) {
            if (t.getAccountId().equalsIgnoreCase(accountId.trim())) {
                list.add(t);
            }
        }
        list.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
        return list;
    }

    @Override
    public synchronized List<AccountTransaction> getAllTransactions() {
        List<AccountTransaction> copy = new ArrayList<>(transactions);
        copy.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
        return Collections.unmodifiableList(copy);
    }
}
