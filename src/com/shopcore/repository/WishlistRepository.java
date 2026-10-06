package com.shopcore.repository;

import com.shopcore.model.Wishlist;

public class WishlistRepository extends FileRepository<Wishlist, String> {
    public WishlistRepository() { super("wishlists.dat", Wishlist::getUserId); }
}
