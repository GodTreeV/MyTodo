package com.aceyan.mytodo.utils

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import com.aceyan.mytodo.checklist.Todo
import java.util.Date

object TimeUtil {
    @SuppressLint("SimpleDateFormat")
    fun getTimeFromStr(timeStr: String): Long {
        return SimpleDateFormat("yyyy/MM/dd HH:mm:00").parse(timeStr)!!.time
    }

    @SuppressLint("SimpleDateFormat")
    fun getTimeStrFromTime(time: Long): String {
        return SimpleDateFormat("yyyy/MM/dd HH:mm:00").format(Date(time))
    }
}