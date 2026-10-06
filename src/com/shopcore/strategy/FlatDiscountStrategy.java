package com.shopcore.strategy;

import com.shopcore.model.Coupon;
import com.shopcore.util.MoneyUtil;

public class FlatDiscountStrategy implements DiscountStrategy {
    @Override
    public double calculate(double subtotal, Coupon coupon) {
        return MoneyUtil.round2(Math.min(coupon.getDiscountValue(), subtotal));
    }
}
