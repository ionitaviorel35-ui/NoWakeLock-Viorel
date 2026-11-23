# Privileged Backend Specification

## Purpose
Defines the abstraction layer for privilege-requiring system interactions, supporting both Xposed hook-based interception and Shizuku-based ADB/shell operations.

## ADDED Requirements

### Requirement: PrivilegedBackend Abstraction
The system SHALL provide a `PrivilegedBackend` interface that abstracts backend capabilities and privileged operations.

#### Scenario: Backend capability query
- **WHEN** the application inspects the active backend
- **THEN** it can query whether per-tag blocking, alarm control, service control, statistics, AppOps blocking, and dumpsys are supported

### Requirement: Backend Preference Selection
The system SHALL allow users to select their backend preference among Auto, Xposed, and Shizuku, persisting the preference in DataStore.

#### Scenario: Auto backend selection
- **WHEN** backend preference is set to Auto
- **THEN** Xposed is prioritized if active, otherwise Shizuku if available, falling back to Xposed default

#### Scenario: Explicit backend selection
- **WHEN** user chooses Xposed or Shizuku explicitly
- **THEN** the application utilizes the selected backend

### Requirement: AppOps Wakelock Management
In Shizuku mode, the system SHALL support reading and toggling the `WAKE_LOCK` AppOp mode (`ignore` vs `default`) per application package.

#### Scenario: Block wakelock for app
- **WHEN** user toggles wakelock blocking ON for an app and confirms the warning dialog
- **THEN** `appops set <pkg> WAKE_LOCK ignore` is executed via Shizuku

#### Scenario: Unblock wakelock for app
- **WHEN** user toggles wakelock blocking OFF for an app
- **THEN** `appops set <pkg> WAKE_LOCK default` is executed via Shizuku

### Requirement: Active Wakelock and Alarm Dumpsys Inspection
In Shizuku mode, the system SHALL inspect active wakelocks and alarms via `dumpsys power` and `dumpsys alarm` grouped by application.

#### Scenario: View active wakelocks
- **WHEN** user views wakelocks under Shizuku mode
- **THEN** active wakelocks from `dumpsys power` are displayed grouped by application
