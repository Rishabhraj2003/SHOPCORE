package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.exception.OutOfStockException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Inventory;
import com.shopcore.repository.InventoryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Owns all stock numbers. Each product has its own lock, so two buyers of the SAME product are
 * handled one after the other (no overselling) while buyers of DIFFERENT products never wait for each other.
 */
public class InventoryService {
    private final InventoryRepository repo;
    private final AuditLogger audit = AuditLogger.getInstance();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public InventoryService(InventoryRepository repo) { this.repo = repo; }

    private Object lockFor(String productId) { return locks.computeIfAbsent(productId, k -> new Object()); }

    private Inventory find(String productId) {
        return repo.findById(productId).orElseThrow(() -> new ShopCoreException("No inventory record for " + productId));
    }

    public void createStock(String productId, int quantity) {
        if (quantity < 0) throw new ShopCoreException("Stock cannot be negative");
        repo.save(new Inventory(productId, quantity));
    }

    public int getAvailable(String productId) {
        return repo.findById(productId).map(Inventory::getAvailableQty).orElse(0);
    }

    public boolean checkStock(String productId, int quantity) {
        return getAvailable(productId) >= quantity;
    }

    /** Moves units from available to reserved (held while payment is processed). */
    public void reserveStock(String productId, int quantity) {
        synchronized (lockFor(productId)) {
            Inventory inv = find(productId);
            if (inv.getAvailableQty() < quantity)
                throw new OutOfStockException(productId, quantity, inv.getAvailableQty());
            inv.setAvailableQty(inv.getAvailableQty() - quantity);
            inv.setReservedQty(inv.getReservedQty() + quantity);
            repo.save(inv);
        }
    }

    /** Payment failed: give the reserved units back. */
    public void releaseStock(String productId, int quantity) {
        synchronized (lockFor(productId)) {
            Inventory inv = find(productId);
            inv.setReservedQty(Math.max(0, inv.getReservedQty() - quantity));
            inv.setAvailableQty(inv.getAvailableQty() + quantity);
            repo.save(inv);
        }
    }

    /** Order confirmed: reserved units permanently leave the warehouse. */
    public void reduceStock(String productId, int quantity) {
        synchronized (lockFor(productId)) {
            Inventory inv = find(productId);
            inv.setReservedQty(Math.max(0, inv.getReservedQty() - quantity));
            repo.save(inv);
        }
    }

    /** Order cancelled after confirmation: units come back into sellable stock. */
    public void restoreStock(String productId, int quantity) {
        synchronized (lockFor(productId)) {
            Inventory inv = find(productId);
            inv.setAvailableQty(inv.getAvailableQty() + quantity);
            repo.save(inv);
        }
    }

    public void addStock(String adminId, String productId, int quantity) {
        if (quantity <= 0) throw new ShopCoreException("Quantity must be positive");
        synchronized (lockFor(productId)) {
            Inventory inv = find(productId);
            inv.setAvailableQty(inv.getAvailableQty() + quantity);
            repo.save(inv);
        }
        audit.log(adminId, AuditAction.INVENTORY_UPDATED, productId + " +" + quantity);
    }

    public void setStock(String adminId, String productId, int quantity) {
        if (quantity < 0) throw new ShopCoreException("Stock cannot be negative");
        synchronized (lockFor(productId)) {
            Inventory inv = find(productId);
            inv.setAvailableQty(quantity);
            repo.save(inv);
        }
        audit.log(adminId, AuditAction.INVENTORY_UPDATED, productId + " =" + quantity);
    }

    public List<Inventory> getAll() {
        return repo.findAll().stream().sorted(Comparator.comparing(Inventory::getProductId)).collect(Collectors.toList());
    }

    public List<Inventory> getLowStock(int threshold) {
        return getAll().stream().filter(i -> i.getAvailableQty() <= threshold).collect(Collectors.toList());
    }
}
