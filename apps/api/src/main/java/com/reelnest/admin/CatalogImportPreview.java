package com.reelnest.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Strict, dependency-free CSV preview. It never writes catalog records. */
public final class CatalogImportPreview {
    private CatalogImportPreview() {}
    public record Row(int line, UUID id, String name, String genre, String language, String ageRating) {}
    public record Preview(List<Row> rows, List<String> errors) {
        public boolean valid() { return errors.isEmpty() && !rows.isEmpty(); }
    }
    public static Preview parse(String raw) {
        if (raw == null || raw.isBlank()) return new Preview(List.of(), List.of("CSV file is empty"));
        if (raw.length() > 100_000) return new Preview(List.of(), List.of("CSV file exceeds 100 KB limit"));
        if (raw.indexOf('\0') >= 0) return new Preview(List.of(), List.of("NUL bytes are not allowed"));
        List<String> errors = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        List<List<String>> records = tokenize(raw, errors);
        if (!errors.isEmpty() || records.isEmpty()) return new Preview(List.of(), List.copyOf(errors));
        List<String> header = records.get(0);
        if (!header.equals(List.of("id", "name", "genre", "language", "age_rating")))
            return new Preview(List.of(), List.of("Header must be id,name,genre,language,age_rating"));
        if (records.size() > 501) return new Preview(List.of(), List.of("Maximum 500 metadata rows per preview"));
        Set<UUID> seen = new java.util.HashSet<>();
        for (int i=1;i<records.size();i++) {
            List<String> r = records.get(i);
            if (r.size()==1 && r.get(0).isBlank()) continue;
            if (r.size()!=5) {errors.add("Row "+(i+1)+": expected 5 cells"); continue;}
            try {
                UUID id = UUID.fromString(r.get(0).strip());
                if (!seen.add(id)) throw new IllegalArgumentException("Duplicate title ID");
                String name = r.get(1).strip();
                String genre = r.get(2).strip();
                String lang = r.get(3).strip();
                String rating = r.get(4).strip();
                if (name.isBlank() || name.length()>255) throw new IllegalArgumentException("Title length must be 1–255");
                if (genre.isBlank() || genre.length()>80) throw new IllegalArgumentException("Genre length must be 1–80");
                if (!lang.matches("[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?")) throw new IllegalArgumentException("Unsupported language syntax");
                if (!List.of("U", "UA7", "UA13", "UA16", "A").contains(rating))
                    throw new IllegalArgumentException("Invalid age rating");
                rows.add(new Row(i+1,id,name,genre,lang,rating));
            } catch (IllegalArgumentException e) {errors.add("Row "+(i+1)+": "+e.getMessage());}
        }
        return new Preview(List.copyOf(rows), List.copyOf(errors));
    }
    private static List<List<String>> tokenize(String s, List<String> errors) {
        List<List<String>> records = new ArrayList<>(); List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder(); boolean quoted=false, closed=false, atStart=true;
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (quoted) {
                if (c=='"' && i+1<s.length() && s.charAt(i+1)=='"') { value.append('"'); i++; }
                else if (c=='"') { quoted=false; closed=true; }
                else if (c=='\r' || c=='\n') { errors.add("Embedded newlines in CSV cells are not supported"); return List.of(); }
                else value.append(c);
            } else if (c=='"' && atStart) {quoted=true;atStart=false;}
            else if (c==',') {fields.add(value.toString()); value.setLength(0); atStart=true;closed=false;}
            else if (c=='\r' || c=='\n') {
                if (c=='\r' && i+1<s.length() && s.charAt(i+1)=='\n') i++;
                fields.add(value.toString()); records.add(List.copyOf(fields)); fields.clear();
                value.setLength(0);atStart=true;closed=false;
            } else if (closed || c=='"') {errors.add("Invalid quoting in CSV");return List.of();}
            else {value.append(c);atStart=false;}
        }
        if (quoted) {errors.add("Unclosed CSV quote");return List.of();}
        if (value.length()>0 || !fields.isEmpty()) {fields.add(value.toString());records.add(List.copyOf(fields));}
        return records;
    }
}
