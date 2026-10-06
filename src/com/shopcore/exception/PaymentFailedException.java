package com.shopcore.exception;

public class PaymentFailedException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public PaymentFailedException(String reason) { super(reason); }
}
