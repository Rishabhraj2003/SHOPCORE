package com.shopcore.repository;

import com.shopcore.model.Coupon;

public class CouponRepository extends FileRepository<Coupon, String> {
    public CouponRepository() { super("coupons.dat", Coupon::getCode); }
}
