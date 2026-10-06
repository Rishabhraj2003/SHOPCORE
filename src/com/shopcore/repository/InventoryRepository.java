package com.shopcore.repository;

import com.shopcore.model.Inventory;

public class InventoryRepository extends FileRepository<Inventory, String> {
    public InventoryRepository() { super("inventory.dat", Inventory::getProductId); }
}
