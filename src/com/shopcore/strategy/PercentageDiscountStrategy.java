package com.shopcore.strategy;

import com.shopcore.model.Coupon;
import com.shopcore.util.MoneyUtil;

public class PercentageDiscountStrategy implements DiscountStrategy {
    @Override
    public double calculate(double subtotal, Coupon coupon) {
        double discount = subtotal * coupon.getDiscountValue() / 100.0;
        if (coupon.getMaximumDiscount() > 0) discount = Math.min(discount, coupon.getMaximumDiscount());
        return MoneyUtil.round2(Math.min(discount, subtotal));
    }
}
