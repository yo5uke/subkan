package com.subkan.data.icon

import java.net.URLEncoder

/**
 * Turns a service name into a logo URL, or nothing.
 *
 * Returning null rather than a best-effort URL is the point. Google's favicon service answers an
 * unknown domain with a generic globe, and a list of identical globes is worse than a list of
 * lettered tiles — so the caller falls back to [com.subkan.core.model.ServiceAccent] instead.
 *
 * A URL is produced in exactly two cases:
 *   1. The name contains a known keyword, which maps to that service's real domain.
 *   2. The user typed something domain-shaped ("example.com"), which is used as-is.
 */
object ServiceIconUrl {

    /**
     * Keyword → official domain.
     *
     * Order matters: `google` sits last so 「Google One」 does not swallow Gemini or YouTube, which
     * are Google properties on their own domains.
     */
    private val domainByKeyword: List<Pair<String, String>> = listOf(
        "claude" to "claude.ai",
        "anthropic" to "claude.ai",
        "chatgpt" to "openai.com",
        "openai" to "openai.com",
        "gemini" to "gemini.google.com",
        "youtube" to "youtube.com",
        "amazon" to "amazon.co.jp",
        "prime" to "amazon.co.jp",
        "netflix" to "netflix.com",
        "spotify" to "spotify.com",
        "notion" to "notion.so",
        "hulu" to "hulu.com",
        "github" to "github.com",
        "disney" to "disneyplus.com",
        "microsoft" to "microsoft.com",
        "office" to "microsoft.com",
        "m365" to "microsoft.com",
        "adobe" to "adobe.com",
        "photoshop" to "adobe.com",
        "illustrator" to "adobe.com",
        "nintendo" to "nintendo.com",
        "switch" to "nintendo.com",
        "playstation" to "playstation.com",
        "psplus" to "playstation.com",
        "psn" to "playstation.com",
        "apple" to "apple.com",
        "icloud" to "apple.com",
        "unext" to "unext.jp",
        "u-next" to "unext.jp",
        "duolingo" to "duolingo.com",
        "canva" to "canva.com",
        "slack" to "slack.com",
        "dropbox" to "dropbox.com",
        "figma" to "figma.com",
        "zoom" to "zoom.us",
        "dazn" to "dazn.com",
        "google" to "google.com",
    )

    fun forServiceName(serviceName: String): String? {
        val lower = serviceName.trim().lowercase()
        if (lower.isEmpty()) return null

        val domain = domainByKeyword.firstOrNull { (keyword, _) -> lower.contains(keyword) }?.second
            ?: lower.replace(WHITESPACE, "").takeIf { it.contains('.') }
            ?: return null

        // Google's favicon endpoint redirects to gstatic and answers without CORS headers. The
        // wsrv.nl proxy follows the redirect and re-serves the image, which is what made the same
        // URL work from Flutter Web as well as the phone — kept because it also normalises every
        // logo to one size and format.
        val favicon = "https://www.google.com/s2/favicons?domain=$domain&sz=128"
        return "https://wsrv.nl/?url=${URLEncoder.encode(favicon, "UTF-8")}&w=128&h=128&output=png"
    }

    private val WHITESPACE = Regex("\\s+")
}
