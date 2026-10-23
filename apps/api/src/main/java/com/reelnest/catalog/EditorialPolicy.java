package com.reelnest.catalog;

import java.time.Instant;

/** Standalone editorial rules, deliberately independent of media entitlement. */
public final class EditorialPolicy {
 private EditorialPolicy() {}
 public static boolean validSlug(String slug) {
   return slug != null && slug.length() <= 90 && slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*");
 }
 public static boolean active(String state, Instant from, Instant until, Instant now) {
   return "PUBLISHED".equals(state) && (from==null || !now.isBefore(from)) &&
       (until==null || now.isBefore(until));
 }
 public static boolean validDates(Instant from, Instant to) { return to==null || from==null || to.isAfter(from); }
}
