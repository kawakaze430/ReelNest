package com.reelnest.catalog;

import java.util.UUID;

public record TitleResponse(
    UUID id, String slug, String name, String synopsis, String type,
    String genre, String language, String ageRating,
    Integer runtimeMinutes, Integer releaseYear,
    String artTone, boolean featured, String accessTier
) {
    public static TitleResponse from(TitleEntity t) {
        return new TitleResponse(t.getId(), t.getSlug(), t.getName(), t.getSynopsis(),
            t.getType(), t.getGenre(), t.getLanguage(), t.getAgeRating(),
            t.getRuntimeMinutes(), t.getReleaseYear(), t.getArtTone(), t.isFeatured(), t.getAccessTier());
    }
}
