package com.xyz.strapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.xyz.strapp.MainActivity

fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    context.startActivity(intent)
    if (context is Activity) {
        context.finishAffinity()
    }
}