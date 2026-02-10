package com.boris55555.calendarwidget

import android.graphics.Color

fun isValidHex(color: String): Boolean {
    return try {
        Color.parseColor(color)
        true
    } catch (e: Exception) {
        false
    }
}