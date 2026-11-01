package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit record of manual price modifications at the POS.
 */
public class PriceOverrideLog {
    private String overrideId;
    private LocalDateTime timestamp;
    private String cashierUsername;
    private String adminApprover;
    private String productId;
    private BigDecimal originalPrice;
    private BigDecimal overriddenPrice;
    private String reason;

    public PriceOverrideLog() {
        this.timestamp = LocalDateTime.now();
        this.originalPrice = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.overriddenPrice = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public PriceOverrideLog(String overrideId, LocalDateTime timestamp, String cashierUsername,
                            String adminApprover, String productId, BigDecimal originalPrice,
                            BigDecimal overriddenPrice, String reason) {
        this.overrideId = overrideId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.cashierUsername = cashierUsername != null ? cashierUsername : "system";
        this.adminApprover = adminApprover != null ? adminApprover : "admin";
        this.productId = productId;
        this.originalPrice = originalPrice != null ? originalPrice.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.overriddenPrice = overriddenPrice != null ? overriddenPrice.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.reason = reason != null ? reason : "";
    }

    public String getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(String overrideId) {
        this.overrideId = overrideId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }

    public String getAdminApprover() {
        return adminApprover;
    }

    public void setAdminApprover(String adminApprover) {
        this.adminApprover = adminApprover;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice != null ? originalPrice.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getOverriddenPrice() {
        return overriddenPrice;
    }

    public void setOverriddenPrice(BigDecimal overriddenPrice) {
        this.overriddenPrice = overriddenPrice != null ? overriddenPrice.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
