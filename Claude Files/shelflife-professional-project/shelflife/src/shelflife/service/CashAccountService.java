package shelflife.service;

import shelflife.model.Account;
import shelflife.model.AccountTransaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for Cash & Bank Management.
 * Tracks multiple accounts (Cash Drawer, Bank, Easypaisa, JazzCash) and immutable ledger transactions.
 */
public interface CashAccountService {

    List<Account> getAllAccounts();

    Account getAccount(String accountId);

    void addAccount(Account account) throws Exception;

    void updateAccount(Account account) throws Exception;

    /**
     * Records an inflow of funds into an account.
     */
    AccountTransaction recordInflow(String accountId, BigDecimal amount, AccountTransaction.Type type,
                                    String referenceId, String description, String createdBy) throws Exception;

    /**
     * Records an outflow of funds from an account.
     */
    AccountTransaction recordOutflow(String accountId, BigDecimal amount, AccountTransaction.Type type,
                                     String referenceId, String description, String createdBy) throws Exception;

    /**
     * Transfers money between two accounts without affecting income or expense.
     */
    void transferFunds(String sourceAccountId, String destinationAccountId,
                       BigDecimal amount, String description, String createdBy) throws Exception;

    BigDecimal getTotalCashAndBankBalance();

    List<AccountTransaction> getAccountTransactions(String accountId);

    List<AccountTransaction> getAllTransactions();

    void reload();
}
