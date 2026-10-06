package com.shopcore.model;

import com.shopcore.enums.OrderStatus;
import com.shopcore.enums.PaymentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final String userId;
    private final List<OrderItem> items;
    private final double subtotal;
    private final double discount;
    private final double tax;
    private final double finalAmount;
    private final String couponCode;          // null when no coupon was used
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private final String shippingAddress;
    private final LocalDateTime createdAt;

    public Order(String orderId, String userId, List<OrderItem> items, double subtotal, double discount,
                 double tax, double finalAmount, String couponCode, PaymentStatus paymentStatus,
                 OrderStatus orderStatus, String shippingAddress) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.finalAmount = finalAmount;
        this.couponCode = couponCode;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.shippingAddress = shippingAddress;
        this.createdAt = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getTax() { return tax; }
    public double getFinalAmount() { return finalAmount; }
    public String getCouponCode() { return couponCode; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public OrderStatus getOrderStatus() { return orderStatus; }
    public String getShippingAddress() { return shippingAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }
}
