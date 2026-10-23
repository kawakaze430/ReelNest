package com.reelnest.creators;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure arithmetic for future contracts; NEVER authorizes payouts or infers entitlements. */
public final class RoyaltyEstimatePolicy {
    private RoyaltyEstimatePolicy(){}
    public static long shareMinorUnits(long licensedRevenueMinorUnits,BigDecimal contractedPercent) {
        if(licensedRevenueMinorUnits<0 || contractedPercent==null || contractedPercent.signum()<0 ||
           contractedPercent.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Invalid royalty basis");
        return BigDecimal.valueOf(licensedRevenueMinorUnits)
            .multiply(contractedPercent).divide(new BigDecimal("100"),0,RoundingMode.HALF_EVEN)
            .longValueExact();
    }
}
