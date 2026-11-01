package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * System settings entity storing shop configuration, operational thresholds,
 * and tax rules.
 */
public class AppSettings {
    private String shopName;
    private String currencyCode;
    private String currencySymbol;
    private int expiryWarningDays;
    private int defaultLowStockThreshold;
    private BigDecimal taxRatePercent;
    private BigDecimal adminDiscountThreshold;
    private boolean taxEnabled;
    private String storeAddress;
    private String storePhone;
    private String receiptFooter;

    public AppSettings() {
        this.shopName = "ShelfLife Retail POS";
        this.currencyCode = "PKR";
        this.currencySymbol = "Rs. ";
        this.expiryWarningDays = 14;
        this.defaultLowStockThreshold = 10;
        this.taxRatePercent = new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP);
        this.adminDiscountThreshold = new BigDecimal("5.00").setScale(2, RoundingMode.HALF_UP);
        this.taxEnabled = true;
        this.storeAddress = "Main Commercial Market, Rawalpindi";
        this.storePhone = "+92 300 1234567";
        this.receiptFooter = "Thank you for shopping with us! Returns accepted within 7 days with valid receipt.";
    }

    public AppSettings(String shopName, String currencyCode, String currencySymbol, int expiryWarningDays, int defaultLowStockThreshold, BigDecimal taxRatePercent) {
        setShopName(shopName);
        setCurrencyCode(currencyCode);
        setCurrencySymbol(currencySymbol);
        setExpiryWarningDays(expiryWarningDays);
        setDefaultLowStockThreshold(defaultLowStockThreshold);
        setTaxRatePercent(taxRatePercent);
        this.adminDiscountThreshold = new BigDecimal("5.00").setScale(2, RoundingMode.HALF_UP);
        this.taxEnabled = true;
        this.storeAddress = "Main Commercial Market, Rawalpindi";
        this.storePhone = "+92 300 1234567";
        this.receiptFooter = "Thank you for shopping with us! Returns accepted within 7 days with valid receipt.";
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = (shopName != null && !shopName.trim().isEmpty()) ? shopName.trim() : "ShelfLife Retail POS";
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = (currencyCode != null && !currencyCode.trim().isEmpty()) ? currencyCode.trim() : "PKR";
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = (currencySymbol != null) ? currencySymbol : "Rs. ";
    }

    public int getExpiryWarningDays() {
        return expiryWarningDays;
    }

    public void setExpiryWarningDays(int expiryWarningDays) {
        this.expiryWarningDays = Math.max(1, expiryWarningDays);
    }

    public int getDefaultLowStockThreshold() {
        return defaultLowStockThreshold;
    }

    public void setDefaultLowStockThreshold(int defaultLowStockThreshold) {
        this.defaultLowStockThreshold = Math.max(0, defaultLowStockThreshold);
    }

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
    }

    public void setTaxRatePercent(BigDecimal taxRatePercent) {
        if (taxRatePercent == null || taxRatePercent.compareTo(BigDecimal.ZERO) < 0) {
            this.taxRatePercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.taxRatePercent = taxRatePercent.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public String getStoreName() {
        return getShopName();
    }

    public void setStoreName(String storeName) {
        setShopName(storeName);
    }

    public BigDecimal getTaxRate() {
        return getTaxRatePercent();
    }

    public void setTaxRate(BigDecimal taxRate) {
        setTaxRatePercent(taxRate);
    }

    public BigDecimal getAdminDiscountThreshold() {
        return adminDiscountThreshold != null ? adminDiscountThreshold : new BigDecimal("5.00");
    }

    public void setAdminDiscountThreshold(BigDecimal threshold) {
        this.adminDiscountThreshold = threshold != null ? threshold.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public boolean isTaxEnabled() {
        return taxEnabled;
    }

    public void setTaxEnabled(boolean taxEnabled) {
        this.taxEnabled = taxEnabled;
    }

    public String getStoreAddress() {
        return storeAddress != null ? storeAddress : "";
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getStorePhone() {
        return storePhone != null ? storePhone : "";
    }

    public void setStorePhone(String storePhone) {
        this.storePhone = storePhone;
    }

    public String getReceiptFooter() {
        return receiptFooter != null ? receiptFooter : "";
    }

    public void setReceiptFooter(String receiptFooter) {
        this.receiptFooter = receiptFooter;
    }
}
