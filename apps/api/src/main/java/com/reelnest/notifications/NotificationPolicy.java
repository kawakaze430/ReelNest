package com.reelnest.notifications;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Pure validation for private, bounded notification delivery. */
public final class NotificationPolicy {
    private NotificationPolicy() {}
    public static final Set<Integer> EXPIRY_THRESHOLDS = Set.of(30, 7, 1);
    public static String expiryKey(UUID rightsId, LocalDate expiration, int days) {
        if (rightsId == null || expiration == null || !EXPIRY_THRESHOLDS.contains(days))
            throw new IllegalArgumentException("invalid rights expiry notification");
        return "rights:" + rightsId + ":" + expiration + ":" + days;
    }
    public static String bounded(String text, int max) {
        if (text == null || text.isBlank() || text.length() > max || text.chars().anyMatch(c -> c < 32))
            throw new IllegalArgumentException("invalid notification content");
        return text;
    }
}
