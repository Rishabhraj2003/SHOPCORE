package com.shopcore.config;

import com.shopcore.notification.ConsoleNotification;
import com.shopcore.notification.FileNotification;
import com.shopcore.repository.*;
import com.shopcore.service.*;

/**
 * Composition root: the ONLY place where objects are created and wired together
 * (manual dependency injection - what Spring would do for you automatically).
 */
public class AppContext {
    public final UserRepository userRepo = new UserRepository();
    public final ProductRepository productRepo = new ProductRepository();
    public final OrderRepository orderRepo = new OrderRepository();
    public final CartRepository cartRepo = new CartRepository();
    public final WishlistRepository wishlistRepo = new WishlistRepository();
    public final CouponRepository couponRepo = new CouponRepository();
    public final InventoryRepository inventoryRepo = new InventoryRepository();
    public final PaymentRepository paymentRepo = new PaymentRepository();

    public final NotificationService notificationService = new NotificationService();
    public final InventoryService inventoryService = new InventoryService(inventoryRepo);
    public final ProductService productService = new ProductService(productRepo, inventoryService);
    public final SearchService searchService = new SearchService(productRepo, inventoryService);
    public final AuthService authService = new AuthService(userRepo);
    public final UserService userService = new UserService(userRepo);
    public final CartService cartService = new CartService(cartRepo, productService, inventoryService);
    public final WishlistService wishlistService = new WishlistService(wishlistRepo, productService);
    public final CouponService couponService = new CouponService(couponRepo);
    public final PaymentService paymentService = new PaymentService(paymentRepo);
    public final OrderService orderService = new OrderService(orderRepo, cartService, productService,
            inventoryService, couponService, paymentService, notificationService);
    public final AdminService adminService = new AdminService(orderRepo, productRepo, userRepo, inventoryService);

    public AppContext(boolean consoleNotifications) {
        if (consoleNotifications) notificationService.register(new ConsoleNotification());
        notificationService.register(new FileNotification());
    }
}
