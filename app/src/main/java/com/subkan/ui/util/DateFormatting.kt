package com.subkan.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DisplayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

/** 24-hour, zero-padded — matches the time picker the setting opens. */
private val DisplayTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.formatForDisplay(): String = format(DisplayDate)

fun LocalTime.formatForDisplay(): String = format(DisplayTime)

/**
 * Material's date picker speaks in UTC-midnight millis; the app stores a plain calendar date.
 *
 * Converting through [ZoneOffset.UTC] rather than the device zone is what keeps a date picked at
 * 23:00 in Tokyo from coming back as the day before.
 */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun localDateFromPickerMillis(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
