package com.example.shoppingmall.common;

public enum Grade {
    BRONZE(0, 0),
    SILVER(300_000, 3),
    GOLD(1_000_000, 5),
    PLATINUM(3_000_000, 8),
    VIP(10_000_000, 10);

    private final int minPurchaseAmount; // 등급 유지/승급 기준 금액
    private final int discountRate;      // 할인율(%)

    Grade(int minPurchaseAmount, int discountRate) {
        this.minPurchaseAmount = minPurchaseAmount;
        this.discountRate = discountRate;
    }

    public int getMinPurchaseAmount() { return minPurchaseAmount; }
    public int getDiscountRate() { return discountRate; }
}