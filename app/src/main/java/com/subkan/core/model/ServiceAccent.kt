package com.subkan.core.model

/**
 * The palette slot used for a service's fallback tile — the lettered square shown when there is no
 * logo to load.
 *
 * Chosen from the service name so the same service always gets the same colour, on every device and
 * across reinstalls, without storing anything. The actual colours live in `ui/theme`.
 */
enum class ServiceAccent {
    Purple,
    Rose,
    Blue,
    Green,
    Orange,
    Lime,
    Cyan,
    Magenta,
    ;

    companion object {
        fun forName(name: String): ServiceAccent {
            val hash = name.sumOf { it.code }
            return entries[hash.mod(entries.size)]
        }
    }
}

/** The single character shown on the fallback tile; `?` when the name is blank or symbol-only. */
fun serviceInitial(name: String): String =
    name.trim().firstOrNull()?.uppercase() ?: "?"
