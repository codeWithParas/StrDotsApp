package com.xyz.strapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    val DATABASE_NAME: String = "Rts_Database"

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }
}