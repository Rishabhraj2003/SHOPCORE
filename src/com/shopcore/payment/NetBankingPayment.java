package com.shopcore.payment;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.PaymentStatus;

public class NetBankingPayment implements PaymentProcessor {
    @Override
    public PaymentStatus process(double amount) {
        return amount <= AppConfig.NET_BANKING_LIMIT ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}
