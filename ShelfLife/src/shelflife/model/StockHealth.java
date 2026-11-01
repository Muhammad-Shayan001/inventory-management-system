package shelflife.model;

/**
 * Snapshot of on-hand stock split by condition. Quantities are units, not batches.
 * "Expired" also includes recalled batches because neither can be sold.
 */
public final class StockHealth {

    private final int goodQuantity;
    private final int nearExpiryQuantity;
    private final int expiredQuantity;

    public StockHealth(int goodQuantity, int nearExpiryQuantity, int expiredQuantity) {
        this.goodQuantity = goodQuantity;
        this.nearExpiryQuantity = nearExpiryQuantity;
        this.expiredQuantity = expiredQuantity;
    }

    public int getGoodQuantity() {
        return goodQuantity;
    }

    public int getNearExpiryQuantity() {
        return nearExpiryQuantity;
    }

    public int getExpiredQuantity() {
        return expiredQuantity;
    }

    public int getTotalQuantity() {
        return goodQuantity + nearExpiryQuantity + expiredQuantity;
    }
}
