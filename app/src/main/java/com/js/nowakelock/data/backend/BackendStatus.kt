package com.js.nowakelock.data.backend

/**
 * Operational status of a privileged backend.
 */
enum class BackendStatus {
    READY,
    NOT_RUNNING,
    PERMISSION_MISSING,
    ERROR
}
