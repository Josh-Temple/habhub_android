package com.habhub.android.util

import android.net.Uri

object LinkValidator {
    fun isValidWebUrl(value: String): Boolean {
        val uri = Uri.parse(value)
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()
    }

    fun isValidAppLink(value: String): Boolean {
        val uri = Uri.parse(value)
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme !in listOf("http", "https")
    }

    fun isValidTime(value: String): Boolean {
        return Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)
    }
}
