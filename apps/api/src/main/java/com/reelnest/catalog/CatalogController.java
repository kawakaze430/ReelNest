package com.reelnest.catalog;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final TitleRepository titles;

    public CatalogController(TitleRepository titles) { this.titles = titles; }

    @GetMapping
    public Page<TitleResponse> list(
        @RequestParam(required = false) String genre,
        @RequestParam(required = false) String language,
        @RequestParam(required = false, name = "q") String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        if (page < 0 || size < 1 || size > 48) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0; size must be 1-48");
        }
        return titles.search(normalize(genre), normalize(language), normalize(query),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "featured", "createdAt")))
            .map(TitleResponse::from);
    }

    @GetMapping("/by-slug/{slug}")
    public TitleResponse bySlug(@PathVariable String slug){
        if(slug.length()>255 || !slug.matches("[a-z0-9][a-z0-9-]*"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid slug");
        return titles.findBySlugAndStatus(slug,"PUBLISHED").map(TitleResponse::from)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
    }

    @GetMapping("/{id}")
    public TitleResponse get(@PathVariable UUID id) {
        return titles.findByIdAndStatus(id, "PUBLISHED")
            .map(TitleResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search filter exceeds 120 characters");
        }
        return trimmed;
    }
}
