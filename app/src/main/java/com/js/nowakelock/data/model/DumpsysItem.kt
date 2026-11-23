package com.js.nowakelock.data.model

data class DumpsysWakelock(
    val packageName: String,
    val tag: String,
    val type: String,
    val uid: Int = 0,
    val pid: Int = 0,
    val details: String = ""
)

data class DumpsysAlarm(
    val packageName: String,
    val tag: String,
    val type: String = "Alarm",
    val count: Int = 1,
    val details: String = ""
)

data class AppDumpsysGroup(
    val packageName: String,
    val appLabel: String,
    val wakelocks: List<DumpsysWakelock> = emptyList(),
    val alarms: List<DumpsysAlarm> = emptyList()
)
