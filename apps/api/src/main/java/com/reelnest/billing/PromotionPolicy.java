package com.reelnest.billing;

import java.time.Instant;
import java.util.regex.Pattern;
/** Quotation-only arithmetic; not a promise of final checkout amount. */
public final class PromotionPolicy {
    private PromotionPolicy(){}
    private static final Pattern VALID=Pattern.compile("[A-Z0-9]{6,16}");
    public static String checkedCode(String raw){
        if(raw==null || !VALID.matcher(raw).matches())throw new IllegalArgumentException("Invalid promotion code");
        return raw;
    }
    public static boolean active(Instant now,Instant start,Instant expiry){
        return now!=null && start!=null && expiry!=null && !now.isBefore(start) && now.isBefore(expiry);
    }
    public static int quoteMinor(int original,int percent){
        if(original<0 || percent<0 || percent>50)throw new IllegalArgumentException("Invalid promotion");
        return (int)(original-((long)original*percent+50L)/100L);
    }
}
