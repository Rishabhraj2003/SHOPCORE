package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.enums.Category;
import com.shopcore.exception.ProductNotFoundException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Product;
import com.shopcore.repository.ProductRepository;
import com.shopcore.util.IdGenerator;
import com.shopcore.util.InputValidator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {
    private final ProductRepository productRepo;
    private final InventoryService inventoryService;
    private final AuditLogger audit = AuditLogger.getInstance();

    public ProductService(ProductRepository productRepo, InventoryService inventoryService) {
        this.productRepo = productRepo;
        this.inventoryService = inventoryService;
    }

    public Product addProduct(String adminId, String name, String description, Category category,
                              String brand, double price, double discount, int initialStock) {
        validate(name, price, discount);
        Product product = new Product(IdGenerator.next("P"), name.trim(),
                description == null ? "" : description.trim(), category,
                InputValidator.isNotBlank(brand) ? brand.trim() : "Generic", price, discount);
        productRepo.save(product);
        inventoryService.createStock(product.getProductId(), initialStock);
        audit.log(adminId, AuditAction.PRODUCT_CREATED, product.getProductId());
        return product;
    }

    /** Any null argument means "leave that field unchanged". */
    public Product updateProduct(String adminId, String productId, String name, String description,
                                 String brand, Category category, Double price, Double discount, Double rating) {
        Product p = getById(productId);
        if (InputValidator.isNotBlank(name)) p.setName(name.trim());
        if (InputValidator.isNotBlank(description)) p.setDescription(description.trim());
        if (InputValidator.isNotBlank(brand)) p.setBrand(brand.trim());
        if (category != null) p.setCategory(category);
        double newPrice = price != null ? price : p.getPrice();
        double newDiscount = discount != null ? discount : p.getDiscount();
        validate(p.getName(), newPrice, newDiscount);
        p.setPrice(newPrice);
        p.setDiscount(newDiscount);
        if (rating != null) {
            if (rating < 0 || rating > 5) throw new ShopCoreException("Rating must be between 0 and 5");
            p.setRating(rating);
        }
        productRepo.save(p);
        audit.log(adminId, AuditAction.PRODUCT_UPDATED, productId);
        return p;
    }

    /** Soft delete: the product disappears from the shop but old orders keep working. */
    public void deactivateProduct(String adminId, String productId) {
        Product p = getById(productId);
        p.setActive(false);
        productRepo.save(p);
        audit.log(adminId, AuditAction.PRODUCT_DELETED, productId);
    }

    public Product getById(String productId) {
        return productRepo.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    public List<Product> listActive() {
        return productRepo.findAll().stream().filter(Product::isActive)
                .sorted(Comparator.comparing(Product::getProductId)).collect(Collectors.toList());
    }

    public List<Product> listAll() {
        return productRepo.findAll().stream()
                .sorted(Comparator.comparing(Product::getProductId)).collect(Collectors.toList());
    }

    public List<Product> listByCategory(Category category) {
        return listActive().stream().filter(p -> p.getCategory() == category).collect(Collectors.toList());
    }

    private void validate(String name, double price, double discount) {
        if (!InputValidator.isNotBlank(name)) throw new ShopCoreException("Product name is required");
        if (price <= 0) throw new ShopCoreException("Price must be greater than zero");
        if (discount < 0 || discount > 100) throw new ShopCoreException("Discount must be between 0 and 100");
    }
}
