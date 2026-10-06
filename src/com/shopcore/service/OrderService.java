package com.shopcore.service;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.AuditAction;
import com.shopcore.enums.OrderStatus;
import com.shopcore.enums.PaymentMethod;
import com.shopcore.enums.PaymentStatus;
import com.shopcore.exception.OrderNotFoundException;
import com.shopcore.exception.OutOfStockException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.exception.UnauthorizedException;
import com.shopcore.logging.AppLogger;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Cart;
import com.shopcore.model.CartItem;
import com.shopcore.model.Order;
import com.shopcore.model.OrderItem;
import com.shopcore.model.Payment;
import com.shopcore.model.Product;
import com.shopcore.repository.OrderRepository;
import com.shopcore.util.IdGenerator;
import com.shopcore.util.MoneyUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** The orchestrator: coordinates cart, products, stock, coupon, payment and notifications. */
public class OrderService {
    private static final Logger LOG = AppLogger.get(OrderService.class);

    private final OrderRepository orderRepo;
    private final CartService cartService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final AuditLogger audit = AuditLogger.getInstance();

    public OrderService(OrderRepository orderRepo, CartService cartService, ProductService productService,
                        InventoryService inventoryService, CouponService couponService,
                        PaymentService paymentService, NotificationService notificationService) {
        this.orderRepo = orderRepo;
        this.cartService = cartService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public Order placeOrder(String userId, String shippingAddress, String couponCode, PaymentMethod method) {
        if (shippingAddress == null || shippingAddress.isBlank()) throw new ShopCoreException("Shipping address is required");

        // 1-3. Validate cart, products and stock (fast fail before touching anything)
        Cart cart = cartService.getCart(userId);
        if (cart.isEmpty()) throw new ShopCoreException("Your cart is empty");

        List<OrderItem> items = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = productService.getById(cartItem.getProductId());
            if (!product.isActive()) throw new ShopCoreException("'" + product.getName() + "' is no longer available");
            if (!inventoryService.checkStock(product.getProductId(), cartItem.getQuantity()))
                throw new OutOfStockException(product.getName(), cartItem.getQuantity(),
                        inventoryService.getAvailable(product.getProductId()));
            // the order always uses the CURRENT price, not the (possibly stale) price from when it was added
            items.add(new OrderItem(product.getProductId(), product.getName(), cartItem.getQuantity(), product.getDiscountedPrice()));
        }

        // 4. Subtotal
        double subtotal = MoneyUtil.round2(items.stream().mapToDouble(OrderItem::getLineTotal).sum());
        String orderId = IdGenerator.next("ORD");
        boolean couponUsed = couponCode != null && !couponCode.isBlank();
        String normalizedCoupon = couponUsed ? couponCode.trim().toUpperCase() : null;

        List<OrderItem> reserved = new ArrayList<>();
        Payment payment = null;
        Order order;
        double discount = 0;
        boolean couponRedeemed = false;

        try {
            // 5-6. Apply coupon (consumes one use) and calculate discount
            if (couponUsed) {
                discount = couponService.redeem(userId, normalizedCoupon, subtotal);
                couponRedeemed = true;
            }
            // 7. Final amount (tax is charged on the discounted amount)
            double taxable = subtotal - discount;
            double tax = MoneyUtil.round2(taxable * AppConfig.TAX_RATE);
            double finalAmount = MoneyUtil.round2(taxable + tax);

            // 8. Reserve inventory (this is the step that stops two buyers taking the last item)
            for (OrderItem item : items) {
                inventoryService.reserveStock(item.getProductId(), item.getQuantity());
                reserved.add(item);
            }

            // 9. Process payment (throws PaymentFailedException if declined)
            payment = paymentService.pay(orderId, userId, finalAmount, method);

            // 10. Create order
            order = new Order(orderId, userId, items, subtotal, discount, tax, finalAmount,
                    couponRedeemed ? normalizedCoupon : null, payment.getStatus(), OrderStatus.PLACED, shippingAddress.trim());
            orderRepo.save(order);

            // 11. Reduce inventory: reserved units are now permanently sold
            for (OrderItem item : items) inventoryService.reduceStock(item.getProductId(), item.getQuantity());
            reserved.clear();
        } catch (RuntimeException e) {
            // Compensation: undo whatever already happened, in reverse
            for (OrderItem item : reserved) {
                try { inventoryService.releaseStock(item.getProductId(), item.getQuantity()); }
                catch (RuntimeException ex) { LOG.log(Level.SEVERE, "Could not release stock for " + item.getProductId(), ex); }
            }
            if (couponRedeemed) couponService.releaseUsage(normalizedCoupon);
            if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) paymentService.refund(orderId);
            audit.log(userId, AuditAction.ORDER_PLACED, orderId, null, "FAILED: " + e.getMessage());
            throw e;
        }

        // 12-14. The order is committed. These steps must never undo it.
        try {
            cartService.clear(userId);
            notificationService.notifyUser(userId, "Order " + orderId + " placed successfully. Total " + MoneyUtil.format(order.getFinalAmount()));
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Post-order step failed for " + orderId, e);
        }
        audit.log(userId, AuditAction.ORDER_PLACED, orderId, order.getFinalAmount(), "SUCCESS");
        return order;
    }

    /** synchronized: a double-submitted cancel can never restock the same items twice. */
    public synchronized Order cancelOrder(String requesterId, boolean requesterIsAdmin, String orderId) {
        Order order = getById(orderId);
        if (!requesterIsAdmin && !order.getUserId().equals(requesterId))
            throw new UnauthorizedException("You can only cancel your own orders");
        if (!order.getOrderStatus().canBeCancelled())
            throw new ShopCoreException("Order " + orderId + " cannot be cancelled because it is " + order.getOrderStatus());

        order.setOrderStatus(OrderStatus.CANCELLED);
        for (OrderItem item : order.getItems()) inventoryService.restoreStock(item.getProductId(), item.getQuantity());
        Payment payment = paymentService.refund(orderId);
        order.setPaymentStatus(payment.getStatus());
        if (order.getCouponCode() != null) couponService.releaseUsage(order.getCouponCode());
        orderRepo.save(order);

        audit.log(requesterId, AuditAction.ORDER_CANCELLED, orderId, order.getFinalAmount(), "SUCCESS");
        notificationService.notifyUser(order.getUserId(), "Order " + orderId + " was cancelled. Payment status: " + payment.getStatus());
        return order;
    }

    public synchronized Order updateStatus(String adminId, String orderId, OrderStatus newStatus) {
        Order order = getById(orderId);
        if (!order.getOrderStatus().canMoveTo(newStatus))
            throw new ShopCoreException("Cannot move an order from " + order.getOrderStatus() + " to " + newStatus);
        order.setOrderStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setPaymentStatus(paymentService.markCodPaid(orderId).getStatus());   // COD cash is collected on delivery
        }
        orderRepo.save(order);
        audit.log(adminId, AuditAction.ORDER_STATUS_UPDATED, orderId + " -> " + newStatus);
        notificationService.notifyUser(order.getUserId(), "Order " + orderId + " is now " + newStatus);
        return order;
    }

    public Order getById(String orderId) {
        return orderRepo.findById(orderId == null ? "" : orderId.trim().toUpperCase())
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> getOrderHistory(String userId) { return orderRepo.findByUserId(userId); }

    public List<Order> getAllOrders() {
        return orderRepo.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed()).collect(Collectors.toList());
    }
}
