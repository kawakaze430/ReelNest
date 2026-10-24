package com.reelnest.catalogpreview

import java.net.URI
import java.util.UUID

/**
 * Catalog deep links are NEVER identity callbacks, payment callbacks, or media
 * authorization. OAuth PKCE/deep link redirects require separate verified App Links.
 */
object SafeCatalogLink {
    fun titleId(raw: String?): UUID? {
        if (raw == null || raw.length > 180) return null
        return try {
            val uri=URI(raw)
            if (uri.scheme != "reelnest-preview" || uri.host != "catalog" ||
                uri.userInfo != null || uri.port != -1 || uri.rawQuery != null ||
                uri.rawFragment != null) return null
            val path=uri.rawPath ?: return null
            if (!Regex("^/title/[0-9a-fA-F-]{36}$").matches(path)) return null
            val uuid=UUID.fromString(path.removePrefix("/title/"))
            if (!uuid.toString().equals(path.removePrefix("/title/"),ignoreCase=true)) return null
            uuid
        } catch (_: Exception) {null}
    }
}
