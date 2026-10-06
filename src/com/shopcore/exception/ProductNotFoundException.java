package com.shopcore.exception;

public class ProductNotFoundException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public ProductNotFoundException(String productId) { super("Product not found: " + productId); }
}
