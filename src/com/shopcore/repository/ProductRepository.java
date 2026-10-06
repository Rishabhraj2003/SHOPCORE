package com.shopcore.repository;

import com.shopcore.model.Product;

public class ProductRepository extends FileRepository<Product, String> {
    public ProductRepository() { super("products.dat", Product::getProductId); }
}
