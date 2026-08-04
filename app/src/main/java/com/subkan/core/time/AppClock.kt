package com.subkan.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Indirection over "now" so the countdown badge (済 / 今日 / あと N 日) can be tested without
 * waiting for midnight.
 */
interface AppClock {
    fun nowMillis(): Long
    fun today(): LocalDate

    /** Wall-clock now, used to work out when the next daily reminder is due. */
    fun now(): LocalDateTime
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now()
    override fun now(): LocalDateTime = LocalDateTime.now()
}
