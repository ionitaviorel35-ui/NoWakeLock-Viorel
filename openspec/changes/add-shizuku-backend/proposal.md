# Change: Add Optional Shizuku Backend Alongside Xposed

## Why
Currently, NoWakeLock requires Xposed/LSPosed framework to function. Many users want battery optimization and wakelock management without unlocking bootloader or installing root/Xposed frameworks, by utilizing Shizuku (ADB or root). Adding an optional Shizuku backend allows NoWakeLock to reach non-Xposed users while preserving all existing Xposed functionality intact.

## What Changes
- Introduce `PrivilegedBackend` abstraction interface for execution backends.
- Wrap existing Xposed behavior in `XposedBackend`.
- Introduce `ShizukuBackend` utilizing `dev.rikka.shizuku` API and an isolated `UserService`.
- Provide backend preference selection in Settings (Auto, Xposed, Shizuku).
- Add per-app wakelock blocking via AppOps (`appops set <pkg> WAKE_LOCK ignore / default`) with warning confirmations.
- Add read-only dumpsys power and alarm inspection grouped by app.
- Extend Module Status Check screen to report Shizuku binder and permission status.
- In Shizuku mode, gracefully disable Xposed-specific capabilities (per-tag filtering, alarm/service hooks, granular statistics) with clear explanatory UI.

## Impact
- Affected specs: `privileged-backend`, `module-health`, `configuration-management`.
- Affected code: `com.js.nowakelock.data.backend.*`, `app/build.gradle`, `AndroidManifest.xml`, UI screens (Settings, ModuleCheck, Apps, DAs).
- Xposed hooks in `xposed-entry/` remain completely untouched.
