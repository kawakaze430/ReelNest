package com.reelnest.admin;
import java.time.Instant;
public final class PublicationSchedulePolicy {
 private PublicationSchedulePolicy(){}
 public static boolean canSchedule(String titleStatus,Instant publishAt,Instant now){
   return "IN_REVIEW".equals(titleStatus) && publishAt!=null && now!=null
      && publishAt.isAfter(now.plusSeconds(60)) && publishAt.isBefore(now.plusSeconds(365L*24*3600));
 }
 public static boolean canPublish(String titleStatus,boolean ready,boolean rights,boolean creator){
   return "IN_REVIEW".equals(titleStatus) && ready && rights && creator;
 }
 public static boolean expired(java.time.LocalDate lastLicensed,java.time.LocalDate today){
   return lastLicensed!=null && lastLicensed.isBefore(today);
 }
}
