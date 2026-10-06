package com.shopcore.demo;

import com.shopcore.config.AppConfig;
import com.shopcore.config.AppContext;
import com.shopcore.enums.Category;
import com.shopcore.enums.PaymentMethod;
import com.shopcore.exception.OutOfStockException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.model.Inventory;
import com.shopcore.model.Product;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stress test for the inventory: 50 customers fight for 10 units at the same instant.
 * Expected result: exactly 10 orders succeed, 40 get "out of stock", stock ends at 0 (never negative).
 * Run:  java -cp out com.shopcore.demo.ConcurrencyDemo
 */
public class ConcurrencyDemo {
    private static final int STOCK = 10;
    private static final int BUYERS = 50;

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("shopcore-demo");     // never touch real data
        AppConfig.setDataDir(tempDir);
        AppContext ctx = new AppContext(false);

        Product product = ctx.productService.addProduct("DEMO", "Limited Edition Sneakers", "Only 10 pairs exist",
                Category.FASHION, "ShopCore", 1999, 0, STOCK);
        System.out.println("Product " + product.getProductId() + " created with stock " + STOCK);

        List<String> userIds = new ArrayList<>();
        for (int i = 1; i <= BUYERS; i++) {
            userIds.add(ctx.authService.register("Buyer " + i, "buyer" + i + "@test.com", "Passw0rd" + i,
                    "9876543210", "Test Street " + i).getUserId());
        }
        System.out.println(BUYERS + " buyers registered. Starting the rush...");

        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (String userId : userIds) {
            pool.submit(() -> {
                try {
                    startSignal.await();                                   // everyone starts together
                    ctx.cartService.addItem(userId, product.getProductId(), 1);
                    ctx.orderService.placeOrder(userId, "Test Street", null, PaymentMethod.UPI);
                    success.incrementAndGet();
                } catch (OutOfStockException e) {
                    outOfStock.incrementAndGet();
                } catch (ShopCoreException | InterruptedException e) {
                    other.incrementAndGet();
                    System.out.println("Unexpected: " + e.getMessage());
                }
            });
        }
        startSignal.countDown();
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);

        Inventory inv = ctx.inventoryRepo.findById(product.getProductId()).orElseThrow(IllegalStateException::new);
        System.out.println("\nSuccessful orders : " + success.get());
        System.out.println("Out-of-stock      : " + outOfStock.get());
        System.out.println("Other errors      : " + other.get());
        System.out.println("Final stock       : available=" + inv.getAvailableQty() + ", reserved=" + inv.getReservedQty());
        System.out.println("Orders saved      : " + ctx.orderRepo.findAll().size());

        boolean pass = success.get() == STOCK && outOfStock.get() == BUYERS - STOCK && other.get() == 0
                && inv.getAvailableQty() == 0 && inv.getReservedQty() == 0
                && ctx.orderRepo.findAll().size() == STOCK;
        System.out.println(pass ? "\nRESULT: PASS - no overselling." : "\nRESULT: FAIL - race condition detected!");
        System.exit(pass ? 0 : 1);
    }
}
