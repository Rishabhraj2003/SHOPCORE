package com.shopcore.model;

import com.shopcore.enums.DiscountType;

import java.io.Serializable;
import java.time.LocalDate;

public class Coupon implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String code;
    private final DiscountType discountType;
    private final double discountValue;        // percent or flat rupees depending on type
    private final double minimumOrderValue;
    private final double maximumDiscount;      // cap for percentage coupons, 0 = no cap
    private final LocalDate expiryDate;
    private final int usageLimit;
    private int usedCount;
    private boolean active;

    public Coupon(String code, DiscountType discountType, double discountValue, double minimumOrderValue,
                  double maximumDiscount, LocalDate expiryDate, int usageLimit) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderValue = minimumOrderValue;
        this.maximumDiscount = maximumDiscount;
        this.expiryDate = expiryDate;
        this.usageLimit = usageLimit;
        this.usedCount = 0;
        this.active = true;
    }

    public boolean isExpired() { return expiryDate.isBefore(LocalDate.now()); }
    public boolean hasUsesLeft() { return usedCount < usageLimit; }

    public String getCode() { return code; }
    public DiscountType getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getMinimumOrderValue() { return minimumOrderValue; }
    public double getMaximumDiscount() { return maximumDiscount; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public int getUsageLimit() { return usageLimit; }
    public int getUsedCount() { return usedCount; }
    public boolean isActive() { return active; }

    public void incrementUsed() { usedCount++; }
    public void decrementUsed() { if (usedCount > 0) usedCount--; }
    public void setActive(boolean active) { this.active = active; }
}
