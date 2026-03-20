package com.habhub.android.util

import java.net.URI

object LinkValidator {
    fun isValidWebUrl(value: String): Boolean {
        val uri = parseUri(value) ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()
    }

    fun isValidAppLink(value: String): Boolean {
        val uri = parseUri(value) ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme !in listOf("http", "https")
    }

    fun isValidTime(value: String): Boolean {
        return Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)
    }

    private fun parseUri(value: String): URI? {
        return runCatching { URI(value.trim()) }
            .getOrNull()
            ?.takeIf { !it.scheme.isNullOrBlank() }
    }
}
