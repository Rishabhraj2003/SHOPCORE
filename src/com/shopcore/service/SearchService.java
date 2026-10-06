package com.shopcore.service;

import com.shopcore.enums.Category;
import com.shopcore.model.Product;
import com.shopcore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Search + filter + sort, written entirely with Streams, Predicates and Comparators. */
public class SearchService {
    public enum SortBy { RELEVANCE, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, RATING, NEWEST }

    /** Fluent (builder-style) holder for all optional filters. */
    public static class SearchCriteria {
        private String keyword;
        private Category category;
        private String brand;
        private Double minPrice;
        private Double maxPrice;
        private Double minRating;
        private boolean inStockOnly;
        private SortBy sortBy = SortBy.RELEVANCE;

        public SearchCriteria keyword(String v) { this.keyword = v; return this; }
        public SearchCriteria category(Category v) { this.category = v; return this; }
        public SearchCriteria brand(String v) { this.brand = v; return this; }
        public SearchCriteria minPrice(Double v) { this.minPrice = v; return this; }
        public SearchCriteria maxPrice(Double v) { this.maxPrice = v; return this; }
        public SearchCriteria minRating(Double v) { this.minRating = v; return this; }
        public SearchCriteria inStockOnly(boolean v) { this.inStockOnly = v; return this; }
        public SearchCriteria sortBy(SortBy v) { this.sortBy = v; return this; }
    }

    private final ProductRepository productRepo;
    private final InventoryService inventoryService;

    public SearchService(ProductRepository productRepo, InventoryService inventoryService) {
        this.productRepo = productRepo;
        this.inventoryService = inventoryService;
    }

    public List<Product> search(SearchCriteria c) {
        List<Predicate<Product>> filters = new ArrayList<>();
        filters.add(Product::isActive);

        if (c.keyword != null && !c.keyword.isBlank()) {
            String[] words = c.keyword.toLowerCase().trim().split("\\s+");
            filters.add(p -> {
                String haystack = (p.getName() + " " + p.getBrand() + " " + p.getCategory() + " " + p.getDescription()).toLowerCase();
                for (String w : words) if (!haystack.contains(w)) return false;   // every word must match
                return true;
            });
        }
        if (c.category != null) filters.add(p -> p.getCategory() == c.category);
        if (c.brand != null && !c.brand.isBlank()) filters.add(p -> p.getBrand().equalsIgnoreCase(c.brand.trim()));
        if (c.minPrice != null) filters.add(p -> p.getDiscountedPrice() >= c.minPrice);
        if (c.maxPrice != null) filters.add(p -> p.getDiscountedPrice() <= c.maxPrice);
        if (c.minRating != null) filters.add(p -> p.getRating() >= c.minRating);
        if (c.inStockOnly) filters.add(p -> inventoryService.getAvailable(p.getProductId()) > 0);

        Predicate<Product> all = filters.stream().reduce(p -> true, Predicate::and);   // combine every filter

        return productRepo.findAll().stream()
                .filter(all)
                .sorted(comparatorFor(c.sortBy))
                .collect(Collectors.toList());
    }

    private Comparator<Product> comparatorFor(SortBy sortBy) {
        switch (sortBy) {
            case PRICE_LOW_TO_HIGH: return Comparator.comparingDouble(Product::getDiscountedPrice);
            case PRICE_HIGH_TO_LOW: return Comparator.comparingDouble(Product::getDiscountedPrice).reversed();
            case RATING:            return Comparator.comparingDouble(Product::getRating).reversed();
            case NEWEST:            return Comparator.comparing(Product::getCreatedAt).reversed();
            default:                return Comparator.comparing(Product::getProductId);
        }
    }
}
