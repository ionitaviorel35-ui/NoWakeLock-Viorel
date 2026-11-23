package com.js.nowakelock.shizuku

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * UserService running inside Shizuku's privileged process (Shell or Root).
 */
@Keep
class UserService : IUserService.Stub {
    constructor() : super()
    constructor(context: Context) : super()

    override fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            process.waitFor()
            if (output.isNotEmpty()) output else error
        } catch (e: Exception) {
            Log.e("UserService", "Error executing command: $command", e)
            "ERROR: ${e.message}"
        }
    }

    override fun exit() {
        destroy()
    }

    override fun destroy() {
        exitProcess(0)
    }
}
