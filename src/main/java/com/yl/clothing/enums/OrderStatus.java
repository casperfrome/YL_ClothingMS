package com.yl.clothing.enums;

import java.util.random.RandomGenerator;

public enum OrderStatus {
    PENDING_PAYMENT, CANCELLED, PENDING_SHIPMENT, SHIPPED, REFUNDED, COMPLETED;

    public OrderStatus next(RandomGenerator random) {
        return switch (this) {
            case PENDING_PAYMENT -> random.nextInt(10) < 9 ? PENDING_SHIPMENT : CANCELLED;
            case PENDING_SHIPMENT -> SHIPPED;
            case SHIPPED -> random.nextInt(10) < 9 ? COMPLETED : REFUNDED;
            default -> this;
        };
    }
}
