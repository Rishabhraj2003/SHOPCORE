package com.shopcore.model;

import com.shopcore.enums.Category;
import com.shopcore.util.MoneyUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Catalogue item. Stock is NOT stored here - Inventory is the single source of truth for quantity. */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String productId;
    private String name;
    private String description;
    private Category category;
    private String brand;
    private double price;
    private double discount;      // percentage 0-100
    private double rating;        // 0-5
    private boolean active;       // false = soft deleted (kept so old orders still make sense)
    private final LocalDateTime createdAt;

    public Product(String productId, String name, String description, Category category,
                   String brand, double price, double discount) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.price = price;
        this.discount = discount;
        this.rating = 0;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public double getDiscountedPrice() {
        return MoneyUtil.round2(price * (1 - discount / 100.0));
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public String getBrand() { return brand; }
    public double getPrice() { return price; }
    public double getDiscount() { return discount; }
    public double getRating() { return rating; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(Category category) { this.category = category; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setPrice(double price) { this.price = price; }
    public void setDiscount(double discount) { this.discount = discount; }
    public void setRating(double rating) { this.rating = rating; }
    public void setActive(boolean active) { this.active = active; }
}
