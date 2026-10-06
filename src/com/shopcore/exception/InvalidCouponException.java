package com.shopcore.exception;

public class InvalidCouponException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public InvalidCouponException(String reason) { super(reason); }
}
