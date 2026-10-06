package com.shopcore.payment;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.PaymentStatus;

/** COD is accepted but stays PENDING until the order is delivered. */
public class CashOnDeliveryPayment implements PaymentProcessor {
    @Override
    public PaymentStatus process(double amount) {
        return amount <= AppConfig.COD_LIMIT ? PaymentStatus.PENDING : PaymentStatus.FAILED;
    }
}
