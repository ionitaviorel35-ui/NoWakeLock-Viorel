package com.js.nowakelock.data.backend

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.js.nowakelock.BuildConfig
import com.js.nowakelock.R
import com.js.nowakelock.base.LogUtil
import com.js.nowakelock.shizuku.IUserService
import com.js.nowakelock.shizuku.UserService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * PrivilegedBackend implementation utilizing Shizuku (ADB or Root).
 */
class ShizukuBackend(
    private val context: Context
) : PrivilegedBackend {

    companion object {
        private const val TAG = "ShizukuBackend"
        const val PERMISSION_REQUEST_CODE = 2001
    }

    override val type: BackendType = BackendType.SHIZUKU
    override val nameResId: Int = R.string.backend_shizuku

    @Volatile
    private var userService: IUserService? = null
    private var connectingDeferred: CompletableDeferred<IUserService>? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        LogUtil.d(TAG, "Shizuku binder received")
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        LogUtil.d(TAG, "Shizuku binder dead")
        userService = null
        connectingDeferred = null
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            LogUtil.d(TAG, "UserService onServiceConnected: binder=${binder != null}")
            if (binder != null && binder.pingBinder()) {
                val service = IUserService.Stub.asInterface(binder)
                userService = service
                connectingDeferred?.complete(service)
            } else {
                connectingDeferred?.completeExceptionally(IllegalStateException("UserService binder is dead or null"))
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            LogUtil.d(TAG, "UserService onServiceDisconnected")
            userService = null
            connectingDeferred = null
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            LogUtil.e(TAG, "Failed to register Shizuku binder listeners: ${e.message}")
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuRunning()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = PERMISSION_REQUEST_CODE) {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            LogUtil.e(TAG, "Failed to request Shizuku permission: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean {
        return isShizukuRunning() && isPermissionGranted()
    }

    override fun getStatus(): BackendStatus {
        return when {
            !isShizukuRunning() -> BackendStatus.NOT_RUNNING
            !isPermissionGranted() -> BackendStatus.PERMISSION_MISSING
            else -> BackendStatus.READY
        }
    }

    override fun supportsPerTagBlocking(): Boolean = false
    override fun supportsAlarmControl(): Boolean = false
    override fun supportsServiceControl(): Boolean = false
    override fun supportsStatistics(): Boolean = false
    override fun supportsAppOpsBlocking(): Boolean = true
    override fun supportsDumpsys(): Boolean = true

    private suspend fun getService(): IUserService? = withContext(Dispatchers.IO) {
        val currentService = userService
        if (currentService != null && currentService.asBinder().isBinderAlive) {
            return@withContext currentService
        }

        if (!isPermissionGranted()) {
            return@withContext null
        }

        val deferred = synchronized(this@ShizukuBackend) {
            val existing = connectingDeferred
            if (existing != null && !existing.isCompleted) {
                existing
            } else {
                val newDeferred = CompletableDeferred<IUserService>()
                connectingDeferred = newDeferred

                val args = Shizuku.UserServiceArgs(
                    ComponentName(context.packageName, UserService::class.java.name)
                )
                    .daemon(false)
                    .processNameSuffix("service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE)

                try {
                    Shizuku.bindUserService(args, serviceConnection)
                } catch (e: Throwable) {
                    LogUtil.e(TAG, "Failed to bind UserService: ${e.message}")
                    newDeferred.completeExceptionally(e)
                }
                newDeferred
            }
        }

        try {
            withTimeoutOrNull(5000) {
                deferred.await()
            }
        } catch (e: Throwable) {
            LogUtil.e(TAG, "Awaiting UserService connection failed: ${e.message}")
            synchronized(this@ShizukuBackend) {
                connectingDeferred = null
            }
            null
        }
    }

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = getService()
                ?: return@withContext Result.failure(IllegalStateException("Shizuku UserService unavailable or permission denied"))
            val output = service.executeCommand(command)
            Result.success(output)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    // AppOps implementations
    override suspend fun getWakelockAppOp(packageName: String, userId: Int): Boolean {
        val result = executeCommand("cmd appops get --user $userId $packageName WAKE_LOCK")
        return result.map { output ->
            output.contains("ignore", ignoreCase = true)
        }.getOrDefault(false)
    }

    override suspend fun setWakelockAppOp(packageName: String, blocked: Boolean, userId: Int): Result<Unit> {
        val mode = if (blocked) "ignore" else "default"
        val result = executeCommand("cmd appops set --user $userId $packageName WAKE_LOCK $mode")
        return result.map { }
    }

    // Dumpsys implementations
    override suspend fun getDumpsysPower(): String {
        return executeCommand("dumpsys power").getOrDefault("")
    }

    override suspend fun getDumpsysAlarm(): String {
        return executeCommand("dumpsys alarm").getOrDefault("")
    }
}
