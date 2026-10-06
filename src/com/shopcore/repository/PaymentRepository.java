package com.shopcore.repository;

import com.shopcore.model.Payment;

import java.util.Optional;

public class PaymentRepository extends FileRepository<Payment, String> {
    public PaymentRepository() { super("payments.dat", Payment::getPaymentId); }

    public Optional<Payment> findByOrderId(String orderId) {
        return store.values().stream().filter(p -> p.getOrderId().equals(orderId)).findFirst();
    }
}
