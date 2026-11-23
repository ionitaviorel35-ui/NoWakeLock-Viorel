package com.js.nowakelock.data.backend

import androidx.annotation.StringRes

/**
 * Common abstraction for privileged execution backends (Xposed hook vs Shizuku command runner).
 */
interface PrivilegedBackend {
    val type: BackendType
    @get:StringRes
    val nameResId: Int

    fun isAvailable(): Boolean
    fun getStatus(): BackendStatus

    // Capability queries
    fun supportsPerTagBlocking(): Boolean
    fun supportsAlarmControl(): Boolean
    fun supportsServiceControl(): Boolean
    fun supportsStatistics(): Boolean
    fun supportsAppOpsBlocking(): Boolean
    fun supportsDumpsys(): Boolean

    // AppOps wakelock operations (Shizuku)
    suspend fun getWakelockAppOp(packageName: String, userId: Int = 0): Boolean = false
    suspend fun setWakelockAppOp(packageName: String, blocked: Boolean, userId: Int = 0): Result<Unit> =
        Result.failure(UnsupportedOperationException("AppOps wakelock control not supported on $type"))

    // Dumpsys operations (Shizuku)
    suspend fun getDumpsysPower(): String = ""
    suspend fun getDumpsysAlarm(): String = ""
}
