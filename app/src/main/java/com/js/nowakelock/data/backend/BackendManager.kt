package com.js.nowakelock.data.backend

import android.content.Context
import com.js.nowakelock.data.repository.preferences.UserPreferencesRepository
import com.js.nowakelock.data.repository.preferences.UserPreferencesRepository.BackendPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manages privileged backend resolution based on user preference and runtime availability.
 */
class BackendManager(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    val xposedBackend: XposedBackend,
    var shizukuBackend: PrivilegedBackend? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    val backendPreference: StateFlow<BackendPreference> = userPreferencesRepository.backendPreference
        .stateIn(scope, SharingStarted.Eagerly, BackendPreference.AUTO)

    private val _activeBackend = MutableStateFlow<PrivilegedBackend>(xposedBackend)
    val activeBackend: StateFlow<PrivilegedBackend> = _activeBackend.asStateFlow()

    init {
        scope.launch {
            userPreferencesRepository.backendPreference.collect { pref ->
                _activeBackend.value = resolveBackend(pref)
            }
        }
    }

    fun resolveBackend(preference: BackendPreference = backendPreference.value): PrivilegedBackend {
        return when (preference) {
            BackendPreference.XPOSED -> xposedBackend
            BackendPreference.SHIZUKU -> shizukuBackend ?: xposedBackend
            BackendPreference.AUTO -> {
                if (xposedBackend.isAvailable()) {
                    xposedBackend
                } else if (shizukuBackend?.isAvailable() == true) {
                    shizukuBackend!!
                } else {
                    xposedBackend
                }
            }
        }
    }

    fun getActiveBackend(): PrivilegedBackend = _activeBackend.value

    fun refreshActiveBackend() {
        _activeBackend.value = resolveBackend(backendPreference.value)
    }

    suspend fun setBackendPreference(preference: BackendPreference) {
        userPreferencesRepository.setBackendPreference(preference)
        _activeBackend.value = resolveBackend(preference)
    }
}
