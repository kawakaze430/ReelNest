package com.reelnest.catalog;

/** Normalize the Creator Studio form's short labels to catalog/parental-control labels. */
public final class AgeRating {
    private AgeRating() {}
    public static String canonical(String value) {
        if (value == null) throw new IllegalArgumentException("Age classification is required");
        return switch(value) {
            case "U" -> "U";
            case "UA7", "U/A 7+" -> "U/A 7+";
            case "UA13", "U/A 13+" -> "U/A 13+";
            case "UA16", "U/A 16+" -> "U/A 16+";
            case "A" -> "A";
            default -> throw new IllegalArgumentException("Unsupported age classification");
        };
    }
}
