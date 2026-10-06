package com.shopcore.payment;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.PaymentStatus;

public class UpiPayment implements PaymentProcessor {
    @Override
    public PaymentStatus process(double amount) {
        return amount <= AppConfig.UPI_LIMIT ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}
