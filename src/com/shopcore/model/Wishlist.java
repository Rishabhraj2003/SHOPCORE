package com.shopcore.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Wishlist implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final Set<String> productIds = ConcurrentHashMap.newKeySet();

    public Wishlist(String userId) { this.userId = userId; }

    public String getUserId() { return userId; }
    public boolean add(String productId) { return productIds.add(productId); }
    public boolean remove(String productId) { return productIds.remove(productId); }
    public boolean contains(String productId) { return productIds.contains(productId); }
    public List<String> getProductIds() { return new ArrayList<>(productIds); }
}
