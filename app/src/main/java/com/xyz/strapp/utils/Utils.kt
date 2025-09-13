package com.xyz.strapp.utils

import com.xyz.strapp.utils.Utils.convertToUtc
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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

    fun getFormattedTimeZ(dateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            date?.let {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
            } ?: dateTime
        } catch (e: Exception) {
            dateTime
        }
    }

    fun convertUtcToAddOffsetAndDisplayAsIst(utcDateTimeString: String): String {
        // 1. Parse the input string as a LocalDateTime.
        // This is because the string "2025-09-13T19:23:50" has no timezone/offset info.
        val localDateTime = LocalDateTime.parse(utcDateTimeString)

        // 2. Treat the parsed LocalDateTime as being in UTC.
        // This gives us a ZonedDateTime anchored to UTC.
        val utcZonedDateTime = localDateTime.atZone(ZoneOffset.UTC)

        // 3. Add 5 hours and 30 minutes to the UTC ZonedDateTime.
        val futureUtcZonedDateTime = utcZonedDateTime.plusHours(6).plusMinutes(30)

        // 4. Define the IST zone ID. "Asia/Kolkata" is the standard ID for IST.
        val istZoneId = ZoneId.systemDefault()

        // 5. Convert the future UTC ZonedDateTime to IST.
        // withZoneSameInstant ensures the point in time is the same, but represented in IST.
        val istZonedDateTime = futureUtcZonedDateTime.withZoneSameInstant(istZoneId)

        // 6. Format the resulting IST ZonedDateTime into a readable string.
        // Example format: "2025-09-14 06:23:50 AM IST"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a z")

        return getFormattedTimeZ(futureUtcZonedDateTime.toInstant().toString())
        //return istZonedDateTime.format(formatter)
    }

    /**
     * Converts a LocalDateTime object to a UTC formatted string (e.g., "2025-09-13T07:13:33Z"),
     * truncated to the second.
     * Assumes the provided LocalDateTime is already in UTC.
     */
    fun formatLocalDateTimeToUtcString(localDateTime: LocalDateTime): String {
        val truncatedLocalDateTime = localDateTime.truncatedTo(ChronoUnit.SECONDS)
        val zonedDateTimeUtc = truncatedLocalDateTime.atZone(ZoneId.of("UTC"))
        return DateTimeFormatter.ISO_INSTANT.format(zonedDateTimeUtc)
    }

    /**
     * Determines if a LocalDateTime is AM or PM.
     *
     * @param localDateTime The LocalDateTime object.
     * @return "AM" if the hour is between 0 and 11, "PM" otherwise.
     */
    fun getAmPmFromLocalDateTime(localDateTime: LocalDateTime): String {
        return if (localDateTime.hour < 12) {
            "AM"
        } else {
            "PM"
        }
    }

    /**
     * Determines if a UTC formatted time string (e.g., "2025-09-13T13:03:46Z") is AM or PM.
     *
     * @param utcString The UTC formatted time string.
     * @return "AM" or "PM".
     */
    fun getAmPmFromUtcString(utcString: String): String {
        val instant = Instant.parse(utcString)
        // Convert instant to LocalDateTime based on UTC (ZoneOffset.UTC)
        // This gives us the hour as it is in the UTC string
        val localDateTimeUtc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        return getAmPmFromLocalDateTime(localDateTimeUtc)
    }

    fun convertToUtc(dateTimeString: String): String {
        // Parse the string as a LocalDateTime since it has no timezone information.
        // The format "yyyy-MM-ddTHH:mm:ss" is implicitly understood by LocalDateTime.parse().
        val localDateTime = LocalDateTime.parse(dateTimeString)

        // Specify that this LocalDateTime should be interpreted as being in UTC.
        // This converts it to an Instant, which is always a point on the timeline in UTC.
        val instantInUtc: Instant = localDateTime.toInstant(ZoneOffset.UTC)

        // Return the standard ISO 8601 string representation for an Instant,
        // which includes the 'Z' for UTC.
        return instantInUtc.toString() // e.g., "2025-09-13T19:23:50Z"
    }
}

/*fun main() {
    // Example usage for the new function:
    val sampleLocalDateTime = LocalDateTime.now()
    val formattedUtcString = Utils.formatLocalDateTimeToUtcString(sampleLocalDateTime)
    println("Formatted LocalDateTime to UTC String: $formattedUtcString") // Expected: 2025-09-13T07:13:33Z
}*/


fun main() {
    val inputDateTimeString = "2025-09-13T19:23:50"
    val utcDateTimeString = convertToUtc(inputDateTimeString)

    println("Original String: $inputDateTimeString")
    println("UTC Representation: $utcDateTimeString")

    // If you need the Instant object itself for further manipulation:
    val localDateTime = LocalDateTime.parse(inputDateTimeString)
    val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)
    println("Instant object: $instant")
}

