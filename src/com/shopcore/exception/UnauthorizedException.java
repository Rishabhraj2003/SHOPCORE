package com.shopcore.exception;

public class UnauthorizedException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public UnauthorizedException(String message) { super(message); }
}
