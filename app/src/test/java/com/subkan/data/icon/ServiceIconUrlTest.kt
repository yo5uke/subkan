package com.subkan.data.icon

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceIconUrlTest {

    @Test
    fun `a known service resolves to its own domain`() {
        assertTrue(ServiceIconUrl.forServiceName("Netflix").encodesDomain("netflix.com"))
    }

    @Test
    fun `matching is case-insensitive and tolerates surrounding words`() {
        assertTrue(ServiceIconUrl.forServiceName("NETFLIX").encodesDomain("netflix.com"))
        assertTrue(
            ServiceIconUrl.forServiceName("Amazon Prime Video").encodesDomain("amazon.co.jp"),
        )
    }

    @Test
    fun `Google's own sub-brands win over the generic google entry`() {
        assertTrue(ServiceIconUrl.forServiceName("YouTube Premium").encodesDomain("youtube.com"))
        assertTrue(ServiceIconUrl.forServiceName("Gemini").encodesDomain("gemini.google.com"))
        assertTrue(ServiceIconUrl.forServiceName("Google One").encodesDomain("google.com"))
    }

    @Test
    fun `a domain-shaped name is used directly`() {
        assertTrue(ServiceIconUrl.forServiceName("example.com").encodesDomain("example.com"))
        assertTrue(ServiceIconUrl.forServiceName("My Site .com").encodesDomain("mysite.com"))
    }

    @Test
    fun `an unrecognisable name resolves to nothing, so the caller draws a tile`() {
        // The alternative — a URL that returns a generic globe — makes every unknown service
        // look identical.
        assertNull(ServiceIconUrl.forServiceName("ジム月会費"))
        assertNull(ServiceIconUrl.forServiceName("水道代"))
        assertNull(ServiceIconUrl.forServiceName(""))
        assertNull(ServiceIconUrl.forServiceName("   "))
    }

    @Test
    fun `the favicon URL is percent-encoded into the proxy query`() {
        val url = ServiceIconUrl.forServiceName("Netflix")
        assertNotNull(url)
        assertTrue(url!!.startsWith("https://wsrv.nl/?url="))
        // Unencoded, the inner "?domain=" would terminate the proxy's own query string.
        assertTrue(url.contains("%3Fdomain%3D"))
    }

    private fun String?.encodesDomain(domain: String): Boolean =
        this != null && contains("domain%3D$domain")
}
