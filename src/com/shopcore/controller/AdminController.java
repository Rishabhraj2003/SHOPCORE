package com.shopcore.controller;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.Category;
import com.shopcore.enums.DiscountType;
import com.shopcore.enums.OrderStatus;
import com.shopcore.enums.UserStatus;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Order;
import com.shopcore.model.Product;
import com.shopcore.security.SessionManager;
import com.shopcore.service.AdminService;
import com.shopcore.service.AuthService;
import com.shopcore.service.CouponService;
import com.shopcore.service.InventoryService;
import com.shopcore.service.OrderService;
import com.shopcore.service.ProductService;
import com.shopcore.service.UserService;
import com.shopcore.util.ConsoleUtil;
import com.shopcore.util.DateUtil;
import com.shopcore.util.MoneyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminController {
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final AuthService authService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final AdminService adminService;
    private final AuthController authController;
    private final SessionManager session = SessionManager.getInstance();
    private final AuditLogger audit = AuditLogger.getInstance();

    public AdminController(ProductService productService, InventoryService inventoryService, UserService userService,
                           AuthService authService, OrderService orderService, CouponService couponService,
                           AdminService adminService, AuthController authController) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.userService = userService;
        this.authService = authService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.adminService = adminService;
        this.authController = authController;
    }

    /** Shows the admin panel once; Main keeps calling it until the admin logs out. */
    public void showAdminPanel() {
        session.requireAdmin();                         // every entry into the panel is re-checked
        ConsoleUtil.header("ADMIN PANEL");
        System.out.println("Logged in as " + session.getCurrentUser().getName());
        System.out.println("1. Manage Products");
        System.out.println("2. Manage Inventory");
        System.out.println("3. Manage Users");
        System.out.println("4. Manage Orders");
        System.out.println("5. Manage Coupons");
        System.out.println("6. Reports");
        System.out.println("7. Audit Logs");
        System.out.println("8. Logout");
        switch (ConsoleUtil.readInt("Choose", 1, 8)) {
            case 1: manageProducts(); break;
            case 2: manageInventory(); break;
            case 3: manageUsers(); break;
            case 4: manageOrders(); break;
            case 5: manageCoupons(); break;
            case 6: ConsoleUtil.safely(this::reports); break;
            case 7: auditLogs(); break;
            default: authController.logout();
        }
    }

    // ------------------------------------------------------------------ products
    private void manageProducts() {
        while (true) {
            ConsoleUtil.header("MANAGE PRODUCTS");
            System.out.println("1. List all products (including inactive)");
            System.out.println("2. Add product");
            System.out.println("3. Update product");
            System.out.println("4. Remove product (soft delete)");
            System.out.println("5. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 5);
            if (choice == 5) return;
            String adminId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printProducts(productService.listAll(), inventoryService);
                } else if (choice == 2) {
                    String name = ConsoleUtil.readRequired("Name");
                    String desc = ConsoleUtil.readLine("Description");
                    Category category = ConsoleUtil.chooseEnum("Category", Category.values());
                    String brand = ConsoleUtil.readLine("Brand");
                    double price = ConsoleUtil.readDouble("Price", 0.01);
                    double discount = ConsoleUtil.readDouble("Discount % (0-100)", 0);
                    int stock = ConsoleUtil.readInt("Initial stock", 0, 1_000_000);
                    Product p = productService.addProduct(adminId, name, desc, category, brand, price, discount, stock);
                    ConsoleUtil.success("Product created with ID " + p.getProductId());
                } else if (choice == 3) {
                    String id = ConsoleUtil.readRequired("Product ID").toUpperCase();
                    ViewHelper.printProductDetails(productService.getById(id), inventoryService.getAvailable(id));
                    ConsoleUtil.info("Press Enter to keep the current value of any field.");
                    String name = ConsoleUtil.readLine("New name");
                    String desc = ConsoleUtil.readLine("New description");
                    String brand = ConsoleUtil.readLine("New brand");
                    Category category = ConsoleUtil.chooseEnumOptional("New category", Category.values());
                    Double price = ConsoleUtil.readOptionalDouble("New price");
                    Double discount = ConsoleUtil.readOptionalDouble("New discount %");
                    Double rating = ConsoleUtil.readOptionalDouble("New rating (0-5)");
                    productService.updateProduct(adminId, id, name, desc, brand, category, price, discount, rating);
                    ConsoleUtil.success("Product updated.");
                } else {
                    String id = ConsoleUtil.readRequired("Product ID").toUpperCase();
                    if (ConsoleUtil.confirm("Remove " + id + " from the shop?")) {
                        productService.deactivateProduct(adminId, id);
                        ConsoleUtil.success("Product removed from the catalogue.");
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ inventory
    private void manageInventory() {
        while (true) {
            ConsoleUtil.header("MANAGE INVENTORY");
            System.out.println("1. View all stock");
            System.out.println("2. View low-stock items (<= " + AppConfig.LOW_STOCK_THRESHOLD + ")");
            System.out.println("3. Add stock");
            System.out.println("4. Set exact stock");
            System.out.println("5. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 5);
            if (choice == 5) return;
            String adminId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printInventory(inventoryService.getAll(), productService);
                } else if (choice == 2) {
                    ViewHelper.printInventory(adminService.lowStock(), productService);
                } else if (choice == 3) {
                    String id = ConsoleUtil.readRequired("Product ID").toUpperCase();
                    productService.getById(id);
                    inventoryService.addStock(adminId, id, ConsoleUtil.readInt("Units to add", 1, 1_000_000));
                    ConsoleUtil.success("Stock updated. Available now: " + inventoryService.getAvailable(id));
                } else {
                    String id = ConsoleUtil.readRequired("Product ID").toUpperCase();
                    productService.getById(id);
                    inventoryService.setStock(adminId, id, ConsoleUtil.readInt("New stock quantity", 0, 1_000_000));
                    ConsoleUtil.success("Stock set to " + inventoryService.getAvailable(id));
                }
            });
        }
    }

    // ------------------------------------------------------------------ users
    private void manageUsers() {
        while (true) {
            ConsoleUtil.header("MANAGE USERS");
            System.out.println("1. List all users");
            System.out.println("2. Block a user");
            System.out.println("3. Unblock a user");
            System.out.println("4. Create another admin");
            System.out.println("5. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 5);
            if (choice == 5) return;
            String adminId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printUsers(userService.getAllUsers());
                } else if (choice == 2 || choice == 3) {
                    String id = ConsoleUtil.readRequired("User ID").toUpperCase();
                    userService.setStatus(adminId, id, choice == 2 ? UserStatus.BLOCKED : UserStatus.ACTIVE);
                    ConsoleUtil.success(choice == 2 ? "User blocked." : "User unblocked.");
                } else {
                    String name = ConsoleUtil.readRequired("Full name");
                    String email = ConsoleUtil.readRequired("Email");
                    String phone = ConsoleUtil.readRequired("Phone (10 digits)");
                    String address = ConsoleUtil.readRequired("Address");
                    String password = ConsoleUtil.readPassword("Password");
                    authService.registerAdmin(name, email, password, phone, address);
                    ConsoleUtil.success("Admin account created.");
                }
            });
        }
    }

    // ------------------------------------------------------------------ orders
    private void manageOrders() {
        while (true) {
            ConsoleUtil.header("MANAGE ORDERS");
            System.out.println("1. List all orders");
            System.out.println("2. View order details");
            System.out.println("3. Update order status");
            System.out.println("4. Cancel an order");
            System.out.println("5. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 5);
            if (choice == 5) return;
            String adminId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printOrders(orderService.getAllOrders());
                } else if (choice == 2) {
                    ViewHelper.printOrderDetails(orderService.getById(ConsoleUtil.readRequired("Order ID")));
                } else if (choice == 3) {
                    Order order = orderService.getById(ConsoleUtil.readRequired("Order ID"));
                    List<OrderStatus> next = new ArrayList<>();
                    for (OrderStatus s : OrderStatus.values()) if (order.getOrderStatus().canMoveTo(s)) next.add(s);
                    if (next.isEmpty()) {
                        ConsoleUtil.info("Order is " + order.getOrderStatus() + " and cannot move forward.");
                        return;
                    }
                    OrderStatus chosen = ConsoleUtil.chooseEnum("Move " + order.getOrderId() + " from "
                            + order.getOrderStatus() + " to", next.toArray(new OrderStatus[0]));
                    orderService.updateStatus(adminId, order.getOrderId(), chosen);
                    ConsoleUtil.success("Order is now " + chosen);
                } else {
                    String id = ConsoleUtil.readRequired("Order ID");
                    if (ConsoleUtil.confirm("Cancel order " + id.toUpperCase() + "?")) {
                        orderService.cancelOrder(adminId, true, id);
                        ConsoleUtil.success("Order cancelled, stock restored, refund initiated.");
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ coupons
    private void manageCoupons() {
        while (true) {
            ConsoleUtil.header("MANAGE COUPONS");
            System.out.println("1. List coupons");
            System.out.println("2. Create coupon");
            System.out.println("3. Deactivate coupon");
            System.out.println("4. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 4);
            if (choice == 4) return;
            String adminId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printCoupons(couponService.listAll());
                } else if (choice == 2) {
                    String code = ConsoleUtil.readRequired("Coupon code");
                    DiscountType type = ConsoleUtil.chooseEnum("Discount type", DiscountType.values());
                    double value = ConsoleUtil.readDouble(type == DiscountType.PERCENTAGE ? "Percent off" : "Flat amount off", 0.01);
                    double min = ConsoleUtil.readDouble("Minimum order value", 0);
                    double max = type == DiscountType.PERCENTAGE ? ConsoleUtil.readDouble("Maximum discount (0 = no cap)", 0) : 0;
                    String expiry = ConsoleUtil.readRequired("Expiry date (yyyy-MM-dd)");
                    int limit = ConsoleUtil.readInt("Total usage limit", 1, 1_000_000);
                    couponService.createCoupon(adminId, code, type, value, min, max, DateUtil.parseDate(expiry), limit);
                    ConsoleUtil.success("Coupon created.");
                } else {
                    couponService.deactivate(adminId, ConsoleUtil.readRequired("Coupon code"));
                    ConsoleUtil.success("Coupon deactivated.");
                }
            });
        }
    }

    // ------------------------------------------------------------------ reports + audit
    private void reports() {
        ConsoleUtil.header("REPORTS");
        System.out.println("Customers          : " + adminService.customerCount());
        System.out.println("Admins             : " + adminService.adminCount());
        System.out.println("Total orders       : " + adminService.totalOrders());
        System.out.println("Revenue (no cancel): " + MoneyUtil.format(adminService.totalRevenue()));
        System.out.println("Average order value: " + MoneyUtil.format(adminService.averageOrderValue()));

        System.out.println("\nOrders by status:");
        adminService.ordersByStatus().forEach((status, count) -> System.out.printf("  %-10s %d%n", status, count));

        System.out.println("\nTop selling products (units):");
        Map<String, Integer> top = adminService.topSellingProducts(5);
        if (top.isEmpty()) System.out.println("  (no sales yet)");
        top.forEach((name, units) -> System.out.printf("  %-30s %d%n", ViewHelper.cut(name, 30), units));

        System.out.println("\nRevenue by category:");
        Map<Category, Double> byCategory = adminService.revenueByCategory();
        if (byCategory.isEmpty()) System.out.println("  (no sales yet)");
        byCategory.forEach((cat, amount) -> System.out.printf("  %-14s %s%n", cat, MoneyUtil.format(amount)));

        System.out.println("\nLow stock (<= " + AppConfig.LOW_STOCK_THRESHOLD + "):");
        ViewHelper.printInventory(adminService.lowStock(), productService);
    }

    private void auditLogs() {
        ConsoleUtil.header("AUDIT LOGS");
        System.out.println("1. Last 20 events");
        System.out.println("2. Last 50 events");
        System.out.println("3. Events of one user");
        System.out.println("4. Back");
        int choice = ConsoleUtil.readInt("Choose", 1, 4);
        if (choice == 4) return;
        ConsoleUtil.safely(() -> {
            List<String> lines;
            if (choice == 1) lines = audit.readLast(20, null);
            else if (choice == 2) lines = audit.readLast(50, null);
            else lines = audit.readLast(50, ConsoleUtil.readRequired("User ID").toUpperCase());
            if (lines.isEmpty()) ConsoleUtil.info("No audit events found.");
            lines.forEach(System.out::println);
        });
    }
}
