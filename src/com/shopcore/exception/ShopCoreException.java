package com.shopcore.exception;

/** Parent of every business error. Unchecked, so services stay clean; controllers catch it once. */
public class ShopCoreException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShopCoreException(String message) { super(message); }
    public ShopCoreException(String message, Throwable cause) { super(message, cause); }
}
