package com.shopcore.service;

import com.shopcore.enums.Category;
import com.shopcore.enums.OrderStatus;
import com.shopcore.model.Inventory;
import com.shopcore.model.Order;
import com.shopcore.model.OrderItem;
import com.shopcore.model.Product;
import com.shopcore.model.User;
import com.shopcore.repository.OrderRepository;
import com.shopcore.repository.ProductRepository;
import com.shopcore.repository.UserRepository;
import com.shopcore.config.AppConfig;
import com.shopcore.util.MoneyUtil;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-only business reports. Every report is a Stream pipeline over orders. */
public class AdminService {
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final InventoryService inventoryService;

    public AdminService(OrderRepository orderRepo, ProductRepository productRepo,
                        UserRepository userRepo, InventoryService inventoryService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.inventoryService = inventoryService;
    }

    private List<Order> activeOrders() {
        return orderRepo.findAll().stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED).collect(Collectors.toList());
    }

    public double totalRevenue() {
        return MoneyUtil.round2(activeOrders().stream().mapToDouble(Order::getFinalAmount).sum());
    }

    public double averageOrderValue() {
        return MoneyUtil.round2(activeOrders().stream().mapToDouble(Order::getFinalAmount).average().orElse(0));
    }

    public long totalOrders() { return orderRepo.findAll().size(); }

    public Map<OrderStatus, Long> ordersByStatus() {
        return orderRepo.findAll().stream().collect(Collectors.groupingBy(
                Order::getOrderStatus, () -> new EnumMap<>(OrderStatus.class), Collectors.counting()));
    }

    /** Product name -> units sold, best sellers first. */
    public Map<String, Integer> topSellingProducts(int limit) {
        Map<String, Integer> unitsSold = activeOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getProductName, Collectors.summingInt(OrderItem::getQuantity)));
        return unitsSold.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public Map<Category, Double> revenueByCategory() {
        return activeOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> productRepo.findById(i.getProductId()).isPresent())
                .collect(Collectors.groupingBy(
                        i -> productRepo.findById(i.getProductId()).map(Product::getCategory).orElse(Category.ELECTRONICS),
                        () -> new EnumMap<>(Category.class),
                        Collectors.summingDouble(OrderItem::getLineTotal)));
    }

    public List<Inventory> lowStock() { return inventoryService.getLowStock(AppConfig.LOW_STOCK_THRESHOLD); }

    public long customerCount() { return userRepo.findAll().stream().filter(u -> !u.isAdmin()).count(); }

    public long adminCount() { return userRepo.findAll().stream().filter(User::isAdmin).count(); }
}
