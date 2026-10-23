package com.reelnest.catalog;
/** Escape SQL LIKE metacharacters (PostgreSQL ESCAPE '!') before adding wildcards. */
public final class SearchTextPolicy {
 private SearchTextPolicy(){}
 public static String likeLiteral(String input){
    if(input==null)throw new IllegalArgumentException("Missing query");
    return "%"+input.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
 }
 public static String foldLatin(String input){
    if(input==null)return "";
    return java.text.Normalizer.normalize(input,java.text.Normalizer.Form.NFKD)
     .replaceAll("\\p{M}+","").toLowerCase(java.util.Locale.ROOT).strip();
 }
}
