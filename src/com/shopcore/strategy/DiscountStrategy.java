package com.shopcore.strategy;

import com.shopcore.model.Coupon;

/** Strategy pattern: each coupon type calculates its discount in its own class. */
public interface DiscountStrategy {
    double calculate(double subtotal, Coupon coupon);
}
