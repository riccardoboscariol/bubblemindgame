package com.chisara.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val hhmmFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayTimeFmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

    fun time(ts: Long): String = hhmmFmt.format(Date(ts))
    fun dayTime(ts: Long): String = dayTimeFmt.format(Date(ts))
}
