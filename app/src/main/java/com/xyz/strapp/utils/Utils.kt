package com.xyz.strapp.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

object Utils {

    val DATABASE_NAME: String = "Rts_Database"

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun getCurrentDateTimeInIsoFormatTruncatedToSecond(): String {
        val currentInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        return DateTimeFormatter.ISO_INSTANT.format(currentInstant)
    }
}

/*
fun main() { // Or within any Android component
    val formattedDateTimeTruncated = getCurrentDateTimeInIsoFormatTruncatedToSecond()
    println(formattedDateTimeTruncated) // Output will be something like: 2024-07-29T12:34:56.789Z (milliseconds might be included by default with ISO_INSTANT)
}*/
