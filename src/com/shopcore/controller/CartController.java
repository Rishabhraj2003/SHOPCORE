package com.shopcore.controller;

import com.shopcore.model.Cart;
import com.shopcore.model.Product;
import com.shopcore.security.SessionManager;
import com.shopcore.service.CartService;
import com.shopcore.service.InventoryService;
import com.shopcore.service.ProductService;
import com.shopcore.service.WishlistService;
import com.shopcore.util.ConsoleUtil;

import java.util.List;

/** Handles both the cart and the wishlist (they are two sides of the same "basket" idea). */
public class CartController {
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderController orderController;
    private final SessionManager session = SessionManager.getInstance();

    public CartController(CartService cartService, WishlistService wishlistService, ProductService productService,
                          InventoryService inventoryService, OrderController orderController) {
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderController = orderController;
    }

    public void cartMenu() {
        while (true) {
            String userId = session.getCurrentUser().getUserId();
            Cart cart = cartService.getCart(userId);
            ViewHelper.printCart(cart, productService);
            System.out.println();
            System.out.println("1. Change quantity");
            System.out.println("2. Remove an item");
            System.out.println("3. Clear cart");
            System.out.println("4. Proceed to checkout");
            System.out.println("5. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 5);
            if (choice == 5) return;
            ConsoleUtil.safely(() -> {
                switch (choice) {
                    case 1:
                        String id = ConsoleUtil.readRequired("Product ID").toUpperCase();
                        int qty = ConsoleUtil.readInt("New quantity (0 removes)", 0, 100);
                        cartService.updateQuantity(userId, id, qty);
                        ConsoleUtil.success("Cart updated.");
                        break;
                    case 2:
                        cartService.removeItem(userId, ConsoleUtil.readRequired("Product ID").toUpperCase());
                        ConsoleUtil.success("Item removed.");
                        break;
                    case 3:
                        if (ConsoleUtil.confirm("Remove everything from your cart?")) {
                            cartService.clear(userId);
                            ConsoleUtil.success("Cart cleared.");
                        }
                        break;
                    default:
                        orderController.checkout();
                }
            });
        }
    }

    public void wishlistMenu() {
        while (true) {
            String userId = session.getCurrentUser().getUserId();
            ConsoleUtil.header("YOUR WISHLIST");
            List<Product> products = wishlistService.getProducts(userId);
            ViewHelper.printProducts(products, inventoryService);
            System.out.println();
            System.out.println("1. Move an item to cart");
            System.out.println("2. Remove an item");
            System.out.println("3. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 3);
            if (choice == 3) return;
            ConsoleUtil.safely(() -> {
                String id = ConsoleUtil.readRequired("Product ID").toUpperCase();
                if (choice == 1) {
                    cartService.addItem(userId, id, 1);
                    wishlistService.remove(userId, id);
                    ConsoleUtil.success("Moved to cart.");
                } else {
                    ConsoleUtil.success(wishlistService.remove(userId, id) ? "Removed." : "That product is not in your wishlist.");
                }
            });
        }
    }
}
