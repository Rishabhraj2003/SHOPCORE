package com.shopcore.controller;

import com.shopcore.enums.Category;
import com.shopcore.model.Product;
import com.shopcore.security.SessionManager;
import com.shopcore.service.CartService;
import com.shopcore.service.InventoryService;
import com.shopcore.service.ProductService;
import com.shopcore.service.SearchService;
import com.shopcore.service.SearchService.SearchCriteria;
import com.shopcore.service.SearchService.SortBy;
import com.shopcore.service.WishlistService;
import com.shopcore.util.ConsoleUtil;

import java.util.List;

public class ProductController {
    private final ProductService productService;
    private final SearchService searchService;
    private final InventoryService inventoryService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final SessionManager session = SessionManager.getInstance();

    public ProductController(ProductService productService, SearchService searchService,
                             InventoryService inventoryService, CartService cartService,
                             WishlistService wishlistService) {
        this.productService = productService;
        this.searchService = searchService;
        this.inventoryService = inventoryService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
    }

    public void browse() {
        while (true) {
            ConsoleUtil.header("PRODUCTS");
            System.out.println("1. View all products");
            System.out.println("2. Browse by category");
            System.out.println("3. Open a product (by ID)");
            System.out.println("4. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 4);
            if (choice == 4) return;
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    ViewHelper.printProducts(productService.listActive(), inventoryService);
                } else if (choice == 2) {
                    Category category = ConsoleUtil.chooseEnum("Category", Category.values());
                    ViewHelper.printProducts(productService.listByCategory(category), inventoryService);
                } else {
                    openProduct(ConsoleUtil.readRequired("Product ID").toUpperCase());
                }
            });
        }
    }

    public void search() {
        ConsoleUtil.safely(() -> {
            ConsoleUtil.header("SEARCH & FILTER (press Enter to skip any filter)");
            SearchCriteria criteria = new SearchCriteria()
                    .keyword(ConsoleUtil.readLine("Keyword"))
                    .category(ConsoleUtil.chooseEnumOptional("Category", Category.values()))
                    .brand(ConsoleUtil.readLine("Brand"))
                    .minPrice(ConsoleUtil.readOptionalDouble("Minimum price"))
                    .maxPrice(ConsoleUtil.readOptionalDouble("Maximum price"))
                    .minRating(ConsoleUtil.readOptionalDouble("Minimum rating (0-5)"))
                    .inStockOnly(ConsoleUtil.confirm("Show only in-stock items?"))
                    .sortBy(ConsoleUtil.chooseEnum("Sort by", SortBy.values()));
            List<Product> results = searchService.search(criteria);
            ConsoleUtil.info(results.size() + " product(s) found");
            ViewHelper.printProducts(results, inventoryService);
            if (!results.isEmpty() && ConsoleUtil.confirm("Open one of these products?")) {
                openProduct(ConsoleUtil.readRequired("Product ID").toUpperCase());
            }
        });
    }

    private void openProduct(String productId) {
        while (true) {
            Product product = productService.getById(productId);
            ViewHelper.printProductDetails(product, inventoryService.getAvailable(productId));
            System.out.println();
            System.out.println("1. Add to cart");
            System.out.println("2. Add to wishlist");
            System.out.println("3. Back");
            int choice = ConsoleUtil.readInt("Choose", 1, 3);
            if (choice == 3) return;
            String userId = session.getCurrentUser().getUserId();
            ConsoleUtil.safely(() -> {
                if (choice == 1) {
                    int qty = ConsoleUtil.readInt("Quantity", 1, 100);
                    cartService.addItem(userId, productId, qty);
                    ConsoleUtil.success(qty + " x " + product.getName() + " added to cart.");
                } else {
                    boolean added = wishlistService.add(userId, productId);
                    ConsoleUtil.success(added ? "Added to wishlist." : "Already in your wishlist.");
                }
            });
        }
    }
}
