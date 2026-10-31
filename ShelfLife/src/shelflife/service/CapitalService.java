package shelflife.service;

import shelflife.model.CapitalTransaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for Owner Capital and Investment Management.
 * Manages capital contributions, owner withdrawals, and maintains an auditable capital ledger.
 */
public interface CapitalService {

    /**
     * Records an owner capital injection.
     */
    CapitalTransaction recordContribution(BigDecimal amount, String investorName,
                                          String paymentMethod, String destinationAccount,
                                          String description, String createdBy) throws Exception;

    /**
     * Records an owner capital withdrawal (strictly separated from operating expenses).
     */
    CapitalTransaction recordWithdrawal(BigDecimal amount, String investorName,
                                        String paymentMethod, String sourceAccount,
                                        String description, String createdBy) throws Exception;

    /**
     * Total capital contributed to date (contributions only).
     */
    BigDecimal getTotalCapitalContributed();

    /**
     * Total capital withdrawn by owner to date.
     */
    BigDecimal getTotalOwnerWithdrawals();

    /**
     * Net owner capital introduced = Total Contributions - Total Withdrawals.
     */
    BigDecimal getNetOwnerCapital();

    /**
     * Returns all historical capital ledger entries.
     */
    List<CapitalTransaction> getAllTransactions();

    void reload();
}
