package com.shopcore.enums;

/** Order life-cycle: PLACED -> CONFIRMED -> SHIPPED -> DELIVERED (or CANCELLED early on). */
public enum OrderStatus {
    PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    public boolean canBeCancelled() {
        return this == PLACED || this == CONFIRMED;
    }

    public boolean canMoveTo(OrderStatus next) {
        switch (this) {
            case PLACED:    return next == CONFIRMED;
            case CONFIRMED: return next == SHIPPED;
            case SHIPPED:   return next == DELIVERED;
            default:        return false;
        }
    }
}
