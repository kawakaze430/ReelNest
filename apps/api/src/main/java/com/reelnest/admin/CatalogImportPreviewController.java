package com.reelnest.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Explicitly read-only, administrator-scoped preview. Never alters catalog state. */
@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogImportPreviewController {
    @PostMapping(value="/import/preview",consumes="text/csv")
    public CatalogImportPreview.Preview preview(@RequestBody String csv) {
        CatalogImportPreview.Preview result=CatalogImportPreview.parse(csv);
        return result;
    }
}
