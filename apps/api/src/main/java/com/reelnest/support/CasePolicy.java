package com.reelnest.support;

/** Deliberately dependency-free policy for deterministic Java assertion tests. */
public final class CasePolicy {
    private CasePolicy() {}
    public static boolean allowed(String current, String next) {
        if (current == null || next == null || current.equals(next)) return false;
        return switch (current) {
            case "OPEN" -> next.equals("IN_REVIEW") || next.equals("RESOLVED") || next.equals("REJECTED");
            case "IN_REVIEW" -> next.equals("RESOLVED") || next.equals("REJECTED");
            default -> false;
        };
    }
    public static boolean canAppeal(String category, String status, boolean owned, boolean appealExists) {
        return owned && !appealExists && !"APPEAL".equals(category)
            && ("RESOLVED".equals(status) || "REJECTED".equals(status));
    }
    public static boolean mayUnpublish(String category,String current, String next,boolean titlePresent) {
        return titlePresent && "RIGHTS_COMPLAINT".equals(category)
            && ("OPEN".equals(current) || "IN_REVIEW".equals(current)) && "RESOLVED".equals(next);
    }
}
