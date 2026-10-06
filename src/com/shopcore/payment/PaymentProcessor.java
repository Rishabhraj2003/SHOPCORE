package com.shopcore.payment;

import com.shopcore.enums.PaymentStatus;

/** One implementation per payment method. All of them are simulations (no real gateway). */
public interface PaymentProcessor {
    PaymentStatus process(double amount);
}
