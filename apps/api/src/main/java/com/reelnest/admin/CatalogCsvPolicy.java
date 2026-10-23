package com.reelnest.admin;

/** Pure, testable CSV cell encoding for operator-downloaded catalog metadata.
 * A dangerous spreadsheet formula is made literal even after leading whitespace is trimmed.
 * This does not authorize CSV uploads or bulk catalog changes.
 */
public final class CatalogCsvPolicy {
    private CatalogCsvPolicy() {}

    public static String cell(Object source) {
        if (source == null) return "\"\"";
        String value = source.toString().replace('\r', ' ').replace('\n', ' ');
        String trimmed = value.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            // Prepend apostrophe before the *original* value, making formulas literal in spreadsheets.
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
