package com.shopcore.controller;

import com.shopcore.enums.PaymentMethod;
import com.shopcore.exception.InvalidCouponException;
import com.shopcore.model.Cart;
import com.shopcore.model.Order;
import com.shopcore.model.User;
import com.shopcore.security.SessionManager;
import com.shopcore.service.CartService;
import com.shopcore.service.CouponService;
import com.shopcore.service.OrderService;
import com.shopcore.service.ProductService;
import com.shopcore.util.ConsoleUtil;
import com.shopcore.util.MoneyUtil;

public class OrderController {
    private final OrderService orderService;
    private final CartService cartService;
    private final CouponService couponService;
    private final ProductService productService;
    private final SessionManager session = SessionManager.getInstance();

    public OrderController(OrderService orderService, CartService cartService,
                           CouponService couponService, ProductService productService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.couponService = couponService;
        this.productService = productService;
    }

    public void orderMenu() {
        while (true) {
            ConsoleUtil.header("MY ORDERS");
            System.out.println("1. Order history");
            System.out.println("2. View order details");
            System.out.println("3. Cancel an order");
            System.out.println("4. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 4);
            if (choice == 4) return;
            String userId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printOrders(orderService.getOrderHistory(userId));
                } else if (choice == 2) {
                    Order order = orderService.getById(ConsoleUtil.readRequired("Order ID"));
                    if (!order.getUserId().equals(userId)) throw new com.shopcore.exception.UnauthorizedException("That is not your order");
                    ViewHelper.printOrderDetails(order);
                } else {
                    ViewHelper.printOrders(orderService.getOrderHistory(userId));
                    String id = ConsoleUtil.readRequired("Order ID to cancel");
                    if (ConsoleUtil.confirm("Cancel order " + id.toUpperCase() + "?")) {
                        orderService.cancelOrder(userId, false, id);
                        ConsoleUtil.success("Order cancelled. Stock restored and refund initiated.");
                    }
                }
            });
        }
    }

    public void checkout() {
        User user = session.getCurrentUser();
        Cart cart = cartService.getCart(user.getUserId());
        if (cart.isEmpty()) { ConsoleUtil.info("Your cart is empty."); return; }

        ViewHelper.printCart(cart, productService);

        String couponCode = null;
        String entered = ConsoleUtil.readLine("Coupon code (Enter to skip)");
        if (!entered.isEmpty()) {
            try {
                double discount = couponService.previewDiscount(entered, cart.getTotalAmount());
                ConsoleUtil.success("Coupon accepted. You save about " + MoneyUtil.format(discount));
                couponCode = entered;
            } catch (InvalidCouponException e) {
                ConsoleUtil.error(e.getMessage());
                if (!ConsoleUtil.confirm("Continue without a coupon?")) return;
            }
        }

        String address = ConsoleUtil.readLine("Shipping address (Enter to use: " + user.getAddress() + ")");
        if (address.isEmpty()) address = user.getAddress();

        PaymentMethod method = ConsoleUtil.chooseEnum("Payment method", PaymentMethod.values());
        if (!ConsoleUtil.confirm("Place order now?")) { ConsoleUtil.info("Checkout cancelled."); return; }

        Order order = orderService.placeOrder(user.getUserId(), address, couponCode, method);
        ConsoleUtil.success("Order placed!");
        ViewHelper.printOrderDetails(order);
    }
}
