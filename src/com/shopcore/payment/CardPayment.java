package com.shopcore.payment;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.PaymentStatus;

public class CardPayment implements PaymentProcessor {
    @Override
    public PaymentStatus process(double amount) {
        return amount <= AppConfig.CARD_LIMIT ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}
