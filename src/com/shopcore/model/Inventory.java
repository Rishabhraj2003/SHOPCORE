package com.shopcore.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** availableQty can still be sold, reservedQty is held for orders whose payment is in progress. */
public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String productId;
    private int availableQty;
    private int reservedQty;
    private LocalDateTime lastUpdated;

    public Inventory(String productId, int availableQty) {
        this.productId = productId;
        this.availableQty = availableQty;
        this.reservedQty = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getProductId() { return productId; }
    public int getAvailableQty() { return availableQty; }
    public int getReservedQty() { return reservedQty; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; touch(); }
    public void setReservedQty(int reservedQty) { this.reservedQty = reservedQty; touch(); }
    private void touch() { this.lastUpdated = LocalDateTime.now(); }
}
