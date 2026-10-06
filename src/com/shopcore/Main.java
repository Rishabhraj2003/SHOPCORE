package com.shopcore;

import com.shopcore.config.AppConfig;
import com.shopcore.config.AppContext;
import com.shopcore.config.DataSeeder;
import com.shopcore.controller.AdminController;
import com.shopcore.controller.AuthController;
import com.shopcore.controller.CartController;
import com.shopcore.controller.OrderController;
import com.shopcore.controller.ProductController;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.model.User;
import com.shopcore.security.SessionManager;
import com.shopcore.util.ConsoleUtil;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));   // so the rupee sign prints

        AppContext ctx = new AppContext(true);
        boolean seeded = new DataSeeder(ctx).seedIfEmpty();

        AuthController authController = new AuthController(ctx.authService, ctx.userService);
        ProductController productController = new ProductController(ctx.productService, ctx.searchService,
                ctx.inventoryService, ctx.cartService, ctx.wishlistService);
        OrderController orderController = new OrderController(ctx.orderService, ctx.cartService,
                ctx.couponService, ctx.productService);
        CartController cartController = new CartController(ctx.cartService, ctx.wishlistService,
                ctx.productService, ctx.inventoryService, orderController);
        AdminController adminController = new AdminController(ctx.productService, ctx.inventoryService,
                ctx.userService, ctx.authService, ctx.orderService, ctx.couponService, ctx.adminService, authController);

        ConsoleUtil.header(AppConfig.APP_NAME + " - Core Java E-Commerce Backend");
        if (!ctx.authService.adminExists()) setupFirstAdmin(ctx);
        if (seeded) ConsoleUtil.info("Sample products and coupons (WELCOME10, FLAT200, BIGSAVE) were loaded.");

        SessionManager session = SessionManager.getInstance();
        boolean running = true;
        while (running) {
            if (!session.isLoggedIn()) {
                running = authController.showAuthMenu();
            } else if (session.isAdmin()) {
                adminController.showAdminPanel();
            } else {
                showUserDashboard(session, authController, productController, cartController, orderController);
            }
        }
        System.out.println("Thank you for using ShopCore. Goodbye!");
    }

    /**
     * No admin credentials exist in the source code. The first time the app runs, the owner chooses them
     * (or supplies SHOPCORE_ADMIN_EMAIL / SHOPCORE_ADMIN_PASSWORD as environment variables).
     * Only a salted hash of the password is stored, in data/users.dat on this computer.
     */
    private static void setupFirstAdmin(AppContext ctx) {
        String envEmail = System.getenv("SHOPCORE_ADMIN_EMAIL");
        String envPassword = System.getenv("SHOPCORE_ADMIN_PASSWORD");
        if (envEmail != null && !envEmail.isBlank() && envPassword != null && !envPassword.isBlank()) {
            try {
                ctx.authService.registerAdmin("Shop Owner", envEmail, envPassword, "9000000000", "ShopCore HQ");
                ConsoleUtil.info("Admin account created from environment variables.");
                return;
            } catch (ShopCoreException e) {
                ConsoleUtil.error("Environment admin settings rejected: " + e.getMessage());
            }
        }
        ConsoleUtil.header("FIRST-TIME SETUP: CREATE THE ADMIN ACCOUNT");
        ConsoleUtil.info("Choose your own admin login. It is stored only on this computer (password is hashed).");
        while (true) {
            try {
                String name = ConsoleUtil.readRequired("Admin name");
                String email = ConsoleUtil.readRequired("Admin email");
                String password = ConsoleUtil.readPassword("Admin password (min 8 chars, letter + digit)");
                String confirm = ConsoleUtil.readPassword("Confirm password");
                if (!password.equals(confirm)) {
                    ConsoleUtil.error("Passwords do not match. Try again.");
                    continue;
                }
                ctx.authService.registerAdmin(name, email, password, "9000000000", "ShopCore HQ");
                ConsoleUtil.success("Admin account created. Use it to login from the main menu.");
                return;
            } catch (ShopCoreException e) {
                ConsoleUtil.error(e.getMessage());
            }
        }
    }

    private static void showUserDashboard(SessionManager session, AuthController auth, ProductController products,
                                          CartController cart, OrderController orders) {
        User user = session.getCurrentUser();
        ConsoleUtil.header(AppConfig.APP_NAME);
        System.out.println("Welcome, " + user.getName());
        System.out.println();
        System.out.println("1. Products");
        System.out.println("2. Search");
        System.out.println("3. Cart");
        System.out.println("4. Wishlist");
        System.out.println("5. Orders");
        System.out.println("6. Profile");
        System.out.println("7. Logout");
        switch (ConsoleUtil.readInt("Choose", 1, 7)) {
            case 1: products.browse(); break;
            case 2: products.search(); break;
            case 3: cart.cartMenu(); break;
            case 4: cart.wishlistMenu(); break;
            case 5: orders.orderMenu(); break;
            case 6: auth.profileMenu(); break;
            default: auth.logout();
        }
    }
}
