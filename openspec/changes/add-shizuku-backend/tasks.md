# Tasks: Add Shizuku Backend

## 1. Abstraction and Xposed Wrapper
- [x] 1.1 Define `PrivilegedBackend`, `BackendType`, `BackendStatus` interfaces
- [x] 1.2 Implement `XposedBackend` wrapping existing Xposed status and capabilities
- [x] 1.3 Add `backendPreference` persistence to `UserPreferencesRepository`
- [x] 1.4 Implement `BackendManager` and wire into Koin DI

## 2. Shizuku Setup and Status
- [ ] 2.1 Add Shizuku dependencies and configure AIDL in `app/build.gradle`
- [ ] 2.2 Declare `ShizukuProvider` and `UserService` in `AndroidManifest.xml`
- [ ] 2.3 Implement `IUserService.aidl` and `UserService.kt`
- [ ] 2.4 Implement `ShizukuBackend.kt` handling binder ping, permissions, and command execution
- [ ] 2.5 Update Module Status Check screen with Shizuku status display

## 3. App List with Wakelock Toggle (AppOps)
- [ ] 3.1 Implement AppOps WAKE_LOCK read/write commands in `ShizukuBackend`
- [ ] 3.2 Update Apps screen and AppListItem to support per-app wakelock blocking toggle
- [ ] 3.3 Add safety confirmation warning dialog before blocking wakelocks

## 4. Dumpsys Viewer
- [ ] 4.1 Parse active wakelocks and alarms from `dumpsys power` and `dumpsys alarm`
- [ ] 4.2 Group dumpsys data by application
- [ ] 4.3 Add UI viewer for active wakelocks and alarms in Shizuku mode

## 5. UI Polishing, Texts and Docs
- [ ] 5.1 Add backend selector in SettingsScreen
- [ ] 5.2 Gracefully disable/explain Xposed-only features in Shizuku mode
- [ ] 5.3 Localize all new strings (English, Chinese, etc.)
- [ ] 5.4 Update documentation and memory-bank
