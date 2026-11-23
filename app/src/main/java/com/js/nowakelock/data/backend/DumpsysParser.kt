package com.js.nowakelock.data.backend

import android.content.Context
import android.content.pm.PackageManager
import com.js.nowakelock.data.model.AppDumpsysGroup
import com.js.nowakelock.data.model.DumpsysAlarm
import com.js.nowakelock.data.model.DumpsysWakelock

/**
 * Parser for `dumpsys power` and `dumpsys alarm` output.
 */
object DumpsysParser {

    private val WAKELOCK_LINE_REGEX = Regex("""^\s*([A-Z_]+WAKE_LOCK[A-Z_]*)\s+'([^']*)'(.*)$""")
    private val UID_REGEX = Regex("""uid=(\d+)""")
    private val PID_REGEX = Regex("""pid=(\d+)""")
    private val PKG_REGEX = Regex("""(?:pkg|package)=([a-zA-Z0-9._]+)""")
    private val WORKSOURCE_PKG_REGEX = Regex("""WorkSource\{(?:\d+\s+)?([a-zA-Z0-9._]+)\}""")

    fun parsePower(output: String, context: Context? = null): List<DumpsysWakelock> {
        val list = mutableListOf<DumpsysWakelock>()
        if (output.isBlank()) return list

        val pm = context?.packageManager
        var inWakeLocksSection = false

        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Wake Locks: size=", ignoreCase = true)) {
                inWakeLocksSection = true
                continue
            }

            if (inWakeLocksSection) {
                // Section ends when encountering an unindented or different section header
                if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
                    inWakeLocksSection = false
                    continue
                }

                val match = WAKELOCK_LINE_REGEX.find(line)
                if (match != null) {
                    val lockType = match.groupValues[1]
                    val tag = match.groupValues[2]
                    val rest = match.groupValues[3]

                    val uid = UID_REGEX.find(rest)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val pid = PID_REGEX.find(rest)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                    val explicitPkg = PKG_REGEX.find(rest)?.groupValues?.get(1)
                        ?: WORKSOURCE_PKG_REGEX.find(rest)?.groupValues?.get(1)

                    val resolvedPkg = explicitPkg ?: resolvePackageFromUid(uid, pm)

                    list.add(
                        DumpsysWakelock(
                            packageName = resolvedPkg,
                            tag = tag,
                            type = lockType,
                            uid = uid,
                            pid = pid,
                            details = rest.trim()
                        )
                    )
                }
            }
        }
        return list
    }

    fun parseAlarm(output: String, context: Context? = null): List<DumpsysAlarm> {
        val list = mutableListOf<DumpsysAlarm>()
        if (output.isBlank()) return list

        val pm = context?.packageManager

        // Strategy 1: Parse "Top Alarms:" section
        var inTopAlarms = false
        val topAlarmRegex = Regex("""\+(\S+)\s+running,\s+(\d+)\s+wakeups,\s+(\d+)\s+alarms:\s+(\d+):([a-zA-Z0-9._]+)""")

        // Strategy 2: Parse individual Batch / Alarm entries
        val alarmEntryRegex = Regex("""Alarm\{[^\}]*\s+([a-zA-Z0-9._]+)\s*\}""")
        val tagRegex = Regex("""(?:tag=|\*alarm\*:)([^\s,}]+)""")

        // Strategy 3: Parse "Alarm Stats:" section
        var inAlarmStats = false
        var currentStatPkg = ""
        val statHeaderRegex = Regex("""^([a-zA-Z0-9._]+)\s+\+\S+\s+running,\s+(\d+)\s+wakeups:""")
        val statEntryRegex = Regex("""^\s+\+\S+\s+(\d+)\s+wakes\s+(\d+)\s+alarms[^\n]*:\s*(\S+)""")

        for (line in output.lines()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("Top Alarms:", ignoreCase = true)) {
                inTopAlarms = true
                inAlarmStats = false
                continue
            }
            if (trimmed.startsWith("Alarm Stats:", ignoreCase = true)) {
                inAlarmStats = true
                inTopAlarms = false
                continue
            }

            if (inTopAlarms) {
                if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
                    inTopAlarms = false
                    continue
                }
                val match = topAlarmRegex.find(trimmed)
                if (match != null) {
                    val alarmsCount = match.groupValues[3].toIntOrNull() ?: 1
                    val pkg = match.groupValues[5]
                    list.add(
                        DumpsysAlarm(
                            packageName = pkg,
                            tag = "Total Alarms (${match.groupValues[2]} wakeups)",
                            type = "TopAlarm",
                            count = alarmsCount,
                            details = trimmed
                        )
                    )
                }
            } else if (inAlarmStats) {
                if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
                    val headerMatch = statHeaderRegex.find(line)
                    if (headerMatch != null) {
                        currentStatPkg = headerMatch.groupValues[1]
                    } else {
                        inAlarmStats = false
                    }
                    continue
                }

                if (currentStatPkg.isNotEmpty()) {
                    val entryMatch = statEntryRegex.find(line)
                    if (entryMatch != null) {
                        val count = entryMatch.groupValues[2].toIntOrNull() ?: 1
                        val tag = entryMatch.groupValues[3]
                        list.add(
                            DumpsysAlarm(
                                packageName = currentStatPkg,
                                tag = tag,
                                type = "AlarmStat",
                                count = count,
                                details = trimmed
                            )
                        )
                    }
                }
            } else {
                val alarmMatch = alarmEntryRegex.find(line)
                if (alarmMatch != null) {
                    val pkg = alarmMatch.groupValues[1]
                    val tag = tagRegex.find(line)?.groupValues?.get(1) ?: "Alarm"
                    list.add(
                        DumpsysAlarm(
                            packageName = pkg,
                            tag = tag,
                            type = "ScheduledAlarm",
                            count = 1,
                            details = trimmed
                        )
                    )
                }
            }
        }

        return list
    }

    fun groupWakelocks(wakelocks: List<DumpsysWakelock>, context: Context): List<AppDumpsysGroup> {
        val pm = context.packageManager
        return wakelocks.groupBy { it.packageName }
            .map { (pkg, locks) ->
                val label = getAppLabel(pkg, pm)
                AppDumpsysGroup(
                    packageName = pkg,
                    appLabel = label,
                    wakelocks = locks,
                    alarms = emptyList()
                )
            }
            .sortedByDescending { it.wakelocks.size }
    }

    fun groupAlarms(alarms: List<DumpsysAlarm>, context: Context): List<AppDumpsysGroup> {
        val pm = context.packageManager
        return alarms.groupBy { it.packageName }
            .map { (pkg, alarmList) ->
                val label = getAppLabel(pkg, pm)
                AppDumpsysGroup(
                    packageName = pkg,
                    appLabel = label,
                    wakelocks = emptyList(),
                    alarms = alarmList
                )
            }
            .sortedByDescending { it.alarms.sumOf { a -> a.count } }
    }

    private fun resolvePackageFromUid(uid: Int, pm: PackageManager?): String {
        if (uid == 1000) return "android"
        if (pm != null) {
            try {
                val names = pm.getPackagesForUid(uid)
                if (!names.isNullOrEmpty()) {
                    return names[0]
                }
            } catch (_: Exception) {}
        }
        return if (uid > 0) "uid_$uid" else "unknown"
    }

    private fun getAppLabel(packageName: String, pm: PackageManager): String {
        if (packageName == "android") return "Android System"
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
