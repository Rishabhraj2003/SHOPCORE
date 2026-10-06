package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.exception.ProductNotFoundException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Product;
import com.shopcore.model.Wishlist;
import com.shopcore.repository.WishlistRepository;

import java.util.ArrayList;
import java.util.List;

public class WishlistService {
    private final WishlistRepository wishlistRepo;
    private final ProductService productService;
    private final AuditLogger audit = AuditLogger.getInstance();

    public WishlistService(WishlistRepository wishlistRepo, ProductService productService) {
        this.wishlistRepo = wishlistRepo;
        this.productService = productService;
    }

    private synchronized Wishlist getWishlist(String userId) {
        return wishlistRepo.findById(userId).orElseGet(() -> wishlistRepo.save(new Wishlist(userId)));
    }

    public boolean add(String userId, String productId) {
        productService.getById(productId);                 // throws if the product does not exist
        Wishlist wishlist = getWishlist(userId);
        boolean added = wishlist.add(productId);
        wishlistRepo.save(wishlist);
        if (added) audit.log(userId, AuditAction.WISHLIST_UPDATED, "+" + productId);
        return added;
    }

    public boolean remove(String userId, String productId) {
        Wishlist wishlist = getWishlist(userId);
        boolean removed = wishlist.remove(productId);
        wishlistRepo.save(wishlist);
        if (removed) audit.log(userId, AuditAction.WISHLIST_UPDATED, "-" + productId);
        return removed;
    }

    public List<Product> getProducts(String userId) {
        List<Product> products = new ArrayList<>();
        for (String id : getWishlist(userId).getProductIds()) {
            try {
                Product p = productService.getById(id);
                if (p.isActive()) products.add(p);
            } catch (ProductNotFoundException ignored) {
                // product was removed from the catalogue - skip it
            }
        }
        products.sort((a, b) -> a.getProductId().compareTo(b.getProductId()));
        return products;
    }
}
