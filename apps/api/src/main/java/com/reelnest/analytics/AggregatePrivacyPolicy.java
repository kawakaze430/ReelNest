package com.reelnest.analytics;
/** Prevent low-cardinality aggregates from exposing individual viewers. */
public final class AggregatePrivacyPolicy {
    private AggregatePrivacyPolicy(){}
    public static final int MIN_PROFILES=5;
    public static boolean safe(long distinctProfiles,long totalEvents) {
        return distinctProfiles>=MIN_PROFILES && totalEvents>=MIN_PROFILES;
    }
    public static int days(int days) {
        if(days<1||days>90)throw new IllegalArgumentException("days must be 1-90");
        return days;
    }
}
