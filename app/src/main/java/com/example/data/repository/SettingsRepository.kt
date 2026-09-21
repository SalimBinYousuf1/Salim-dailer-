package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salim_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val TRUE_BLACK = booleanPreferencesKey("true_black_oled")
        val GLASS_OPACITY = floatPreferencesKey("liquid_glass_opacity")
        val DIALPAD_SOUND = booleanPreferencesKey("dialpad_sound")
        val DIALPAD_HAPTIC = booleanPreferencesKey("dialpad_haptic")
        val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown_callers")
        val BLOCK_PRIVATE = booleanPreferencesKey("block_private_numbers")
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: false // Default Apple White
    }

    val trueBlackFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TRUE_BLACK] ?: true // When dark mode is on, use true black
    }

    val glassOpacityFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GLASS_OPACITY] ?: 0.72f // Apple Liquid Glass default
    }

    val dialpadSoundFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DIALPAD_SOUND] ?: true
    }

    val dialpadHapticFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DIALPAD_HAPTIC] ?: true
    }

    val blockUnknownFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCK_UNKNOWN] ?: false
    }

    val blockPrivateFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCK_PRIVATE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun setTrueBlack(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRUE_BLACK] = enabled
        }
    }

    suspend fun setGlassOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLASS_OPACITY] = opacity.coerceIn(0.2f, 1.0f)
        }
    }

    suspend fun setDialpadSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIALPAD_SOUND] = enabled
        }
    }

    suspend fun setDialpadHaptic(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIALPAD_HAPTIC] = enabled
        }
    }

    suspend fun setBlockUnknown(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_UNKNOWN] = enabled
        }
    }

    suspend fun setBlockPrivate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_PRIVATE] = enabled
        }
    }
}
