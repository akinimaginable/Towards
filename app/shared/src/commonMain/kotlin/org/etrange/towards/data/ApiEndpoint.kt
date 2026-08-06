package org.etrange.towards.data

import io.ktor.http.Url

/** Trims whitespace and trailing slashes from an API base URL. */
fun normalizeApiEndpoint(raw: String): String = raw.trim().trimEnd('/')

/**
 * Returns a normalized `http`/`https` endpoint when [raw] is valid, otherwise `null`.
 * A valid endpoint must include an explicit scheme and a non-blank host.
 */
fun validateApiEndpoint(raw: String): String? {
    val normalized = normalizeApiEndpoint(raw)
    if (normalized.isEmpty()) return null

    val lower = normalized.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return null

    val url = runCatching { Url(normalized) }.getOrNull() ?: return null
    if (url.protocol.name !in setOf("http", "https")) return null
    if (url.host.isBlank()) return null

    return normalized
}
