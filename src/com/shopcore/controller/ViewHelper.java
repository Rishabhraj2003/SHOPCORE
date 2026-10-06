package com.shopcore.controller;

import com.shopcore.config.AppConfig;
import com.shopcore.exception.ProductNotFoundException;
import com.shopcore.model.Cart;
import com.shopcore.model.CartItem;
import com.shopcore.model.Coupon;
import com.shopcore.model.Inventory;
import com.shopcore.model.Order;
import com.shopcore.model.OrderItem;
import com.shopcore.model.Product;
import com.shopcore.model.User;
import com.shopcore.service.InventoryService;
import com.shopcore.service.ProductService;
import com.shopcore.util.ConsoleUtil;
import com.shopcore.util.DateUtil;
import com.shopcore.util.MoneyUtil;

import java.util.List;

/** All table/receipt printing in one place so controllers only deal with menu flow. */
public final class ViewHelper {
    private ViewHelper() {}

    public static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    public static void printProducts(List<Product> products, InventoryService inventory) {
        if (products.isEmpty()) { ConsoleUtil.info("No products found."); return; }
        System.out.printf("%-8s %-28s %-12s %-13s %11s %5s %11s %6s %6s%n",
                "ID", "Name", "Brand", "Category", "MRP", "Disc", "You Pay", "Rating", "Stock");
        ConsoleUtil.line();
        for (Product p : products) {
            int stock = inventory.getAvailable(p.getProductId());
            System.out.printf("%-8s %-28s %-12s %-13s %11s %4.0f%% %11s %6.1f %6s%s%n",
                    p.getProductId(), cut(p.getName(), 28), cut(p.getBrand(), 12), p.getCategory(),
                    MoneyUtil.format(p.getPrice()), p.getDiscount(), MoneyUtil.format(p.getDiscountedPrice()),
                    p.getRating(), stock == 0 ? "OUT" : String.valueOf(stock), p.isActive() ? "" : "  (inactive)");
        }
    }

    public static void printProductDetails(Product p, int stock) {
        ConsoleUtil.header(p.getName());
        System.out.println("ID          : " + p.getProductId());
        System.out.println("Brand       : " + p.getBrand());
        System.out.println("Category    : " + p.getCategory());
        System.out.println("Description : " + p.getDescription());
        System.out.println("MRP         : " + MoneyUtil.format(p.getPrice()));
        System.out.println("Discount    : " + (int) p.getDiscount() + "%");
        System.out.println("You pay     : " + MoneyUtil.format(p.getDiscountedPrice()));
        System.out.println("Rating      : " + p.getRating() + " / 5");
        System.out.println("Availability: " + (stock == 0 ? "OUT OF STOCK" : stock + " in stock"
                + (stock <= AppConfig.LOW_STOCK_THRESHOLD ? " (hurry, low stock!)" : "")));
    }

    public static void printCart(Cart cart, ProductService productService) {
        ConsoleUtil.header("YOUR CART");
        if (cart.isEmpty()) { ConsoleUtil.info("Your cart is empty."); return; }
        System.out.printf("%-8s %-28s %5s %12s %13s%n", "ID", "Name", "Qty", "Unit price", "Line total");
        ConsoleUtil.line();
        for (CartItem item : cart.getItems()) {
            String name = "(removed product)";
            String note = "";
            try {
                Product p = productService.getById(item.getProductId());
                name = p.getName();
                if (Math.abs(p.getDiscountedPrice() - item.getPriceAtAddition()) > 0.001)
                    note = "  * price is now " + MoneyUtil.format(p.getDiscountedPrice());
            } catch (ProductNotFoundException ignored) { }
            System.out.printf("%-8s %-28s %5d %12s %13s%s%n", item.getProductId(), cut(name, 28),
                    item.getQuantity(), MoneyUtil.format(item.getPriceAtAddition()),
                    MoneyUtil.format(item.getLineTotal()), note);
        }
        ConsoleUtil.line();
        System.out.println("Cart total: " + MoneyUtil.format(cart.getTotalAmount()));
    }

    public static void printOrders(List<Order> orders) {
        if (orders.isEmpty()) { ConsoleUtil.info("No orders found."); return; }
        System.out.printf("%-9s %-8s %-19s %6s %13s %-10s %-10s%n", "Order", "User", "Date", "Items", "Total", "Payment", "Status");
        ConsoleUtil.line();
        for (Order o : orders) {
            int units = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
            System.out.printf("%-9s %-8s %-19s %6d %13s %-10s %-10s%n", o.getOrderId(), o.getUserId(),
                    DateUtil.format(o.getCreatedAt()), units, MoneyUtil.format(o.getFinalAmount()),
                    o.getPaymentStatus(), o.getOrderStatus());
        }
    }

    public static void printOrderDetails(Order o) {
        ConsoleUtil.header("ORDER " + o.getOrderId());
        System.out.println("Placed on     : " + DateUtil.format(o.getCreatedAt()));
        System.out.println("Customer      : " + o.getUserId());
        System.out.println("Order status  : " + o.getOrderStatus());
        System.out.println("Payment status: " + o.getPaymentStatus());
        System.out.println("Ship to       : " + o.getShippingAddress());
        ConsoleUtil.line();
        System.out.printf("%-8s %-28s %5s %12s %13s%n", "ID", "Name", "Qty", "Unit price", "Line total");
        for (OrderItem i : o.getItems()) {
            System.out.printf("%-8s %-28s %5d %12s %13s%n", i.getProductId(), cut(i.getProductName(), 28),
                    i.getQuantity(), MoneyUtil.format(i.getUnitPrice()), MoneyUtil.format(i.getLineTotal()));
        }
        ConsoleUtil.line();
        System.out.printf("%-30s %s%n", "Subtotal", MoneyUtil.format(o.getSubtotal()));
        System.out.printf("%-30s -%s%n", "Discount" + (o.getCouponCode() != null ? " (" + o.getCouponCode() + ")" : ""),
                MoneyUtil.format(o.getDiscount()));
        System.out.printf("%-30s %s%n", "Tax (" + (int) (AppConfig.TAX_RATE * 100) + "%)", MoneyUtil.format(o.getTax()));
        System.out.printf("%-30s %s%n", "TOTAL", MoneyUtil.format(o.getFinalAmount()));
    }

    public static void printInventory(List<Inventory> list, ProductService productService) {
        if (list.isEmpty()) { ConsoleUtil.info("Nothing to show."); return; }
        System.out.printf("%-8s %-30s %10s %10s%n", "ID", "Name", "Available", "Reserved");
        ConsoleUtil.line();
        for (Inventory inv : list) {
            String name = "(unknown)";
            try { name = productService.getById(inv.getProductId()).getName(); } catch (ProductNotFoundException ignored) { }
            System.out.printf("%-8s %-30s %10d %10d%n", inv.getProductId(), cut(name, 30), inv.getAvailableQty(), inv.getReservedQty());
        }
    }

    public static void printUsers(List<User> users) {
        System.out.printf("%-7s %-20s %-28s %-6s %-8s%n", "ID", "Name", "Email", "Role", "Status");
        ConsoleUtil.line();
        for (User u : users) {
            System.out.printf("%-7s %-20s %-28s %-6s %-8s%n", u.getUserId(), cut(u.getName(), 20),
                    cut(u.getEmail(), 28), u.getRole(), u.getStatus());
        }
    }

    public static void printCoupons(List<Coupon> coupons) {
        if (coupons.isEmpty()) { ConsoleUtil.info("No coupons found."); return; }
        System.out.printf("%-12s %-11s %8s %10s %10s %-11s %9s %-6s%n", "Code", "Type", "Value", "Min order", "Max disc", "Expires", "Used", "Active");
        ConsoleUtil.line();
        for (Coupon c : coupons) {
            System.out.printf("%-12s %-11s %8.0f %10.0f %10.0f %-11s %4d/%-4d %-6s%n", c.getCode(), c.getDiscountType(),
                    c.getDiscountValue(), c.getMinimumOrderValue(), c.getMaximumDiscount(), c.getExpiryDate(),
                    c.getUsedCount(), c.getUsageLimit(), c.isActive() ? "yes" : "no");
        }
    }
}
