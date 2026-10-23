package com.reelnest.catalog;
import java.time.Duration;
import java.time.Instant;
public final class ChannelSchedulePolicy {
 private ChannelSchedulePolicy() {}
 public static boolean validWindow(Instant from,Instant until) {
   return from!=null && until!=null && until.isAfter(from) && Duration.between(from,until).compareTo(Duration.ofHours(24))<=0;
 }
 public static boolean overlaps(Instant a,Instant b,Instant c,Instant d) {
   return a.isBefore(d) && c.isBefore(b);
 }
 public static boolean nowPlaying(Instant start,Instant end,Instant now) {
   return !now.isBefore(start) && now.isBefore(end);
 }
}
