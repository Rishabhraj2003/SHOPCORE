package com.shopcore.repository;

import com.shopcore.model.Order;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OrderRepository extends FileRepository<Order, String> {
    public OrderRepository() { super("orders.dat", Order::getOrderId); }

    public List<Order> findByUserId(String userId) {
        return store.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
}
