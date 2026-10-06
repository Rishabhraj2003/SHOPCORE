package com.shopcore.exception;

public class OutOfStockException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public OutOfStockException(String product, int requested, int available) {
        super("Insufficient stock for " + product + " (requested " + requested + ", available " + available + ")");
    }
}
