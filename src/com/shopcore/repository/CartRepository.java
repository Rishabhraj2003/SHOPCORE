package com.shopcore.repository;

import com.shopcore.model.Cart;

/** Keyed by userId: every user has exactly one cart. */
public class CartRepository extends FileRepository<Cart, String> {
    public CartRepository() { super("carts.dat", Cart::getUserId); }
}
