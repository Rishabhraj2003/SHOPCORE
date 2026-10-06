package com.shopcore.model;

import com.shopcore.enums.PaymentMethod;
import com.shopcore.enums.PaymentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String paymentId;
    private final String orderId;
    private final String userId;
    private final double amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private final String transactionRef;
    private final LocalDateTime createdAt;

    public Payment(String paymentId, String orderId, String userId, double amount,
                   PaymentMethod method, PaymentStatus status) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transactionRef = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.createdAt = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(PaymentStatus status) { this.status = status; }
}
