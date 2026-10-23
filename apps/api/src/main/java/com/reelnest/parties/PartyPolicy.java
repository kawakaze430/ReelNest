package com.reelnest.parties;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
public final class PartyPolicy {
 private static final SecureRandom RANDOM=new SecureRandom();
 private PartyPolicy(){}
 public static String invitation(){byte[] b=new byte[24];RANDOM.nextBytes(b);return HexFormat.of().formatHex(b);}
 public static String digest(String value){
  if(value==null||!value.matches("[0-9a-f]{48}"))throw new IllegalArgumentException("Invalid invitation");
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)));}
  catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
 }
 public static boolean active(String state,Instant expires,Instant now){
  return "ACTIVE".equals(state) && expires!=null && expires.isAfter(now);
 }
 public static int boundedPosition(int pos){if(pos<0||pos>86400)throw new IllegalArgumentException("Bad position");return pos;}
 public static boolean saneExpiry(Instant created,Instant until){
  return created!=null&&until!=null&&until.isAfter(created)&&Duration.between(created,until).toMinutes()<=240;
 }
}
