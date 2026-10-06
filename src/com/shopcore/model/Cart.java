package com.shopcore.model;

import com.shopcore.util.MoneyUtil;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String cartId;
    private final String userId;
    // Thread-safe map: other threads may save the cart repository while this cart is being edited
    private final Map<String, CartItem> items = new ConcurrentHashMap<>();

    public Cart(String cartId, String userId) {
        this.cartId = cartId;
        this.userId = userId;
    }

    public String getCartId() { return cartId; }
    public String getUserId() { return userId; }

    public List<CartItem> getItems() {
        return items.values().stream()
                .sorted(Comparator.comparing(CartItem::getProductId))
                .collect(Collectors.toList());
    }

    public CartItem getItem(String productId) { return items.get(productId); }
    public void putItem(CartItem item) { items.put(item.getProductId(), item); }
    public void removeItem(String productId) { items.remove(productId); }
    public void clear() { items.clear(); }
    public boolean isEmpty() { return items.isEmpty(); }

    public double getTotalAmount() {
        return MoneyUtil.round2(items.values().stream().mapToDouble(CartItem::getLineTotal).sum());
    }
}
