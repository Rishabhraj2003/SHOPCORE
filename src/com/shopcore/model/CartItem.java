package com.shopcore.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String productId;
    private int quantity;
    private double priceAtAddition;     // discounted unit price when the item was added

    public CartItem(String productId, int quantity, double priceAtAddition) {
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtAddition = priceAtAddition;
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPriceAtAddition() { return priceAtAddition; }
    public double getLineTotal() { return priceAtAddition * quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPriceAtAddition(double priceAtAddition) { this.priceAtAddition = priceAtAddition; }
}
