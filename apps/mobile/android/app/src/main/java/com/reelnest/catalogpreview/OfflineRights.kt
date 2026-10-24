package com.reelnest.catalogpreview

import java.io.File

/**
 * Private-store clean-up and deny-by-default license boundary.
 * There is no DRM license, downloaded content, or offline entitlement in this
 * preview. Nothing may play merely because bytes exist in app storage.
 */
object OfflineRights {
    fun canPlayOffline(
        verifiedLicense: Boolean, rightsExpiryMillis: Long,
        licenseExpiryMillis: Long, nowMillis: Long
    ): Boolean {
        // Turn on ONLY after a certified CDM supports securely persisted
        // licenses and rechecks revocation. Ordinary local timestamps are not proof.
        return false
    }
    fun expired(nowMillis: Long, licenseUntil: Long, rightsUntil: Long): Boolean =
        nowMillis >= licenseUntil || nowMillis >= rightsUntil ||
            nowMillis < 0 || licenseUntil <= 0 || rightsUntil <= 0

    fun purgePrivatePreviewFiles(root: File): Int {
        // Never follow a symlink or accept user-selected paths for deletion.
        if (!root.isDirectory || java.nio.file.Files.isSymbolicLink(root.toPath())) return 0
        var removed=0
        root.listFiles()?.forEach { file ->
            if (file.isFile && !java.nio.file.Files.isSymbolicLink(file.toPath()) &&
                file.name.matches(Regex("[0-9a-fA-F-]{36}\\.offline-preview"))) {
                if(file.delete())removed++
            }
        }
        return removed
    }
}
