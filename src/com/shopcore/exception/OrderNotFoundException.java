package com.shopcore.exception;

public class OrderNotFoundException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public OrderNotFoundException(String orderId) { super("Order not found: " + orderId); }
}
