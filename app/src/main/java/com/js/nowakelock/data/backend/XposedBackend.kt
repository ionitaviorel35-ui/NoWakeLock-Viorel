package com.js.nowakelock.data.backend

import android.content.Context
import com.js.nowakelock.R
import com.js.nowakelock.base.isModuleActive

/**
 * PrivilegedBackend implementation wrapping existing Xposed framework functionality.
 */
class XposedBackend(
    private val context: Context
) : PrivilegedBackend {

    override val type: BackendType = BackendType.XPOSED
    override val nameResId: Int = R.string.backend_xposed

    override fun isAvailable(): Boolean {
        return isModuleActive()
    }

    override fun getStatus(): BackendStatus {
        return if (isModuleActive()) BackendStatus.READY else BackendStatus.NOT_RUNNING
    }

    override fun supportsPerTagBlocking(): Boolean = true
    override fun supportsAlarmControl(): Boolean = true
    override fun supportsServiceControl(): Boolean = true
    override fun supportsStatistics(): Boolean = true
    override fun supportsAppOpsBlocking(): Boolean = false
    override fun supportsDumpsys(): Boolean = false
}
