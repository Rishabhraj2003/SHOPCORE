package com.shopcore.service;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.AuditAction;
import com.shopcore.exception.OutOfStockException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Cart;
import com.shopcore.model.CartItem;
import com.shopcore.model.Product;
import com.shopcore.repository.CartRepository;
import com.shopcore.util.IdGenerator;

public class CartService {
    private final CartRepository cartRepo;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final AuditLogger audit = AuditLogger.getInstance();

    public CartService(CartRepository cartRepo, ProductService productService, InventoryService inventoryService) {
        this.cartRepo = cartRepo;
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    public synchronized Cart getCart(String userId) {
        return cartRepo.findById(userId).orElseGet(() -> cartRepo.save(new Cart(IdGenerator.next("CRT"), userId)));
    }

    public Cart addItem(String userId, String productId, int quantity) {
        if (quantity <= 0) throw new ShopCoreException("Quantity must be at least 1");
        Product product = productService.getById(productId);
        if (!product.isActive()) throw new ShopCoreException("This product is no longer available");

        Cart cart = getCart(userId);
        CartItem existing = cart.getItem(productId);
        int newQty = (existing == null ? 0 : existing.getQuantity()) + quantity;
        checkLimits(product, newQty);

        if (existing == null) {
            cart.putItem(new CartItem(productId, newQty, product.getDiscountedPrice()));
        } else {
            existing.setQuantity(newQty);
            existing.setPriceAtAddition(product.getDiscountedPrice());
        }
        cartRepo.save(cart);
        audit.log(userId, AuditAction.CART_UPDATED, productId + " x" + newQty);
        return cart;
    }

    /** quantity 0 removes the item. */
    public Cart updateQuantity(String userId, String productId, int quantity) {
        Cart cart = getCart(userId);
        CartItem item = cart.getItem(productId);
        if (item == null) throw new ShopCoreException("That product is not in your cart");
        if (quantity <= 0) {
            cart.removeItem(productId);
        } else {
            checkLimits(productService.getById(productId), quantity);
            item.setQuantity(quantity);
        }
        cartRepo.save(cart);
        audit.log(userId, AuditAction.CART_UPDATED, productId + " x" + Math.max(quantity, 0));
        return cart;
    }

    public Cart removeItem(String userId, String productId) { return updateQuantity(userId, productId, 0); }

    public void clear(String userId) {
        Cart cart = getCart(userId);
        cart.clear();
        cartRepo.save(cart);
    }

    private void checkLimits(Product product, int quantity) {
        if (quantity > AppConfig.MAX_QTY_PER_ITEM)
            throw new ShopCoreException("You can buy at most " + AppConfig.MAX_QTY_PER_ITEM + " units of one product");
        if (!inventoryService.checkStock(product.getProductId(), quantity))
            throw new OutOfStockException(product.getName(), quantity, inventoryService.getAvailable(product.getProductId()));
    }
}
