package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.enums.DiscountType;
import com.shopcore.exception.InvalidCouponException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Coupon;
import com.shopcore.repository.CouponRepository;
import com.shopcore.strategy.DiscountStrategy;
import com.shopcore.strategy.FlatDiscountStrategy;
import com.shopcore.strategy.PercentageDiscountStrategy;
import com.shopcore.util.MoneyUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CouponService {
    private final CouponRepository couponRepo;
    private final AuditLogger audit = AuditLogger.getInstance();

    public CouponService(CouponRepository couponRepo) { this.couponRepo = couponRepo; }

    public Coupon createCoupon(String adminId, String code, DiscountType type, double value, double minOrder,
                               double maxDiscount, LocalDate expiry, int usageLimit) {
        String normalized = normalize(code);
        if (normalized.isEmpty()) throw new ShopCoreException("Coupon code is required");
        if (couponRepo.existsById(normalized)) throw new ShopCoreException("Coupon code already exists");
        if (value <= 0) throw new ShopCoreException("Discount value must be greater than zero");
        if (type == DiscountType.PERCENTAGE && value > 100) throw new ShopCoreException("Percentage cannot exceed 100");
        if (expiry.isBefore(LocalDate.now())) throw new ShopCoreException("Expiry date must be in the future");
        if (usageLimit <= 0) throw new ShopCoreException("Usage limit must be at least 1");

        Coupon coupon = new Coupon(normalized, type, value, minOrder, maxDiscount, expiry, usageLimit);
        couponRepo.save(coupon);
        audit.log(adminId, AuditAction.COUPON_CREATED, normalized);
        return coupon;
    }

    public void deactivate(String adminId, String code) {
        Coupon coupon = couponRepo.findById(normalize(code)).orElseThrow(() -> new InvalidCouponException("Coupon not found"));
        coupon.setActive(false);
        couponRepo.save(coupon);
        audit.log(adminId, AuditAction.COUPON_DEACTIVATED, coupon.getCode());
    }

    public List<Coupon> listAll() {
        return couponRepo.findAll().stream().sorted(Comparator.comparing(Coupon::getCode)).collect(Collectors.toList());
    }

    /** Checks the coupon and returns the discount WITHOUT using it up (used for the checkout preview). */
    public double previewDiscount(String code, double subtotal) {
        Coupon coupon = validate(code, subtotal);
        return strategyFor(coupon.getDiscountType()).calculate(subtotal, coupon);
    }

    /**
     * Validates AND consumes one use. Synchronized so two customers cannot both take the last use.
     * If the order later fails, call releaseUsage to give it back.
     */
    public synchronized double redeem(String userId, String code, double subtotal) {
        Coupon coupon = validate(code, subtotal);
        double discount = strategyFor(coupon.getDiscountType()).calculate(subtotal, coupon);
        coupon.incrementUsed();
        couponRepo.save(coupon);
        audit.log(userId, AuditAction.COUPON_APPLIED, coupon.getCode(), discount, "SUCCESS");
        return discount;
    }

    public synchronized void releaseUsage(String code) {
        couponRepo.findById(normalize(code)).ifPresent(c -> {
            c.decrementUsed();
            couponRepo.save(c);
        });
    }

    private Coupon validate(String code, double subtotal) {
        Coupon coupon = couponRepo.findById(normalize(code))
                .orElseThrow(() -> new InvalidCouponException("Coupon '" + code.trim() + "' does not exist"));
        if (!coupon.isActive()) throw new InvalidCouponException("This coupon is no longer active");
        if (coupon.isExpired()) throw new InvalidCouponException("This coupon has expired");
        if (!coupon.hasUsesLeft()) throw new InvalidCouponException("This coupon has reached its usage limit");
        if (subtotal < coupon.getMinimumOrderValue())
            throw new InvalidCouponException("Minimum order value for this coupon is " + MoneyUtil.format(coupon.getMinimumOrderValue()));
        return coupon;
    }

    private DiscountStrategy strategyFor(DiscountType type) {
        return type == DiscountType.PERCENTAGE ? new PercentageDiscountStrategy() : new FlatDiscountStrategy();
    }

    private String normalize(String code) { return code == null ? "" : code.trim().toUpperCase(); }
}
