package com.shopcore.config;

import com.shopcore.enums.Category;
import com.shopcore.enums.DiscountType;
import com.shopcore.model.Product;

import java.time.LocalDate;

/** Loads sample products and coupons on the very first run so the shop is not empty. */
public class DataSeeder {
    private static final String SYSTEM = "SYSTEM";
    private final AppContext ctx;

    public DataSeeder(AppContext ctx) { this.ctx = ctx; }

    public boolean seedIfEmpty() {
        if (!ctx.productService.listAll().isEmpty()) return false;

        add("boAt Rockerz 450 Headphones", "Wireless on-ear headphones, 15h battery", Category.ELECTRONICS, "boAt", 1499, 20, 40, 4.2);
        add("Redmi Note 13 5G", "6.67 inch AMOLED, 5G smartphone", Category.ELECTRONICS, "Xiaomi", 17999, 10, 25, 4.3);
        add("Samsung 55 inch Smart TV", "4K UHD LED smart television", Category.ELECTRONICS, "Samsung", 52990, 15, 8, 4.5);
        add("Logitech M331 Silent Mouse", "Wireless silent-click mouse", Category.ELECTRONICS, "Logitech", 899, 5, 60, 4.4);
        add("Levi's 511 Slim Jeans", "Slim fit stretch denim", Category.FASHION, "Levi's", 3299, 25, 35, 4.1);
        add("Puma Running Shoes", "Lightweight mesh running shoes", Category.FASHION, "Puma", 4599, 30, 20, 4.0);
        add("Clean Code", "A handbook of agile software craftsmanship", Category.BOOKS, "Pearson", 799, 10, 50, 4.7);
        add("Effective Java", "Best practices for the Java platform", Category.BOOKS, "Pearson", 899, 5, 30, 4.8);
        add("Atomic Habits", "Tiny changes, remarkable results", Category.BOOKS, "Penguin", 499, 10, 70, 4.9);
        add("Prestige Induction Cooktop", "1900W induction cooktop with push buttons", Category.HOME_KITCHEN, "Prestige", 2499, 18, 15, 4.1);
        add("Milton Thermosteel Bottle 1L", "24-hour hot and cold steel bottle", Category.HOME_KITCHEN, "Milton", 749, 0, 80, 4.3);
        add("Yonex Badminton Racquet", "Graphite racquet with full cover", Category.SPORTS, "Yonex", 1999, 12, 3, 4.4);   // low stock on purpose

        LocalDate nextYear = LocalDate.now().plusYears(1);
        ctx.couponService.createCoupon(SYSTEM, "WELCOME10", DiscountType.PERCENTAGE, 10, 500, 300, nextYear, 1000);
        ctx.couponService.createCoupon(SYSTEM, "FLAT200", DiscountType.FLAT, 200, 1500, 0, nextYear, 500);
        ctx.couponService.createCoupon(SYSTEM, "BIGSAVE", DiscountType.PERCENTAGE, 15, 10000, 5000, nextYear, 100);
        return true;
    }

    private void add(String name, String desc, Category cat, String brand, double price, double disc, int stock, double rating) {
        Product p = ctx.productService.addProduct(SYSTEM, name, desc, cat, brand, price, disc, stock);
        ctx.productService.updateProduct(SYSTEM, p.getProductId(), null, null, null, null, null, null, rating);
    }
}
