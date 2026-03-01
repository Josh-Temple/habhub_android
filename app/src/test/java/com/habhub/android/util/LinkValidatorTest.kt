package com.habhub.android.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkValidatorTest {
    @Test
    fun validWebUrl() {
        assertTrue(LinkValidator.isValidWebUrl("https://example.com"))
        assertFalse(LinkValidator.isValidWebUrl("intent://example"))
    }

    @Test
    fun validAppLink() {
        assertTrue(LinkValidator.isValidAppLink("intent://someapp"))
        assertFalse(LinkValidator.isValidAppLink("https://example.com"))
    }

    @Test
    fun validTime() {
        assertTrue(LinkValidator.isValidTime("09:30"))
        assertFalse(LinkValidator.isValidTime("25:99"))
    }
}
