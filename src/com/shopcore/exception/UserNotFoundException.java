package com.shopcore.exception;

public class UserNotFoundException extends ShopCoreException {
    private static final long serialVersionUID = 1L;
    public UserNotFoundException(String userId) { super("User not found: " + userId); }
}
