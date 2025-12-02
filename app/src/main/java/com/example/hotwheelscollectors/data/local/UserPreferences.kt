package com.example.hotwheelscollectors.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.hotwheelscollectors.model.PersonalStorageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    val preferences: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val isDarkTheme: Flow<Boolean> = preferences.map { prefs ->
        prefs[PreferencesKeys.DARK_THEME] ?: false
    }

    val storageLocation: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.STORAGE_LOCATION] ?: "Device"
    }

    val storageType: Flow<PersonalStorageType> = preferences.map { prefs ->
        PersonalStorageType.fromString(prefs[PreferencesKeys.STORAGE_TYPE] ?: "LOCAL")
    }

    val lastSync: Flow<Long> = preferences.map { prefs ->
        prefs[PreferencesKeys.LAST_SYNC] ?: 0L
    }

    val userName: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.USER_NAME] ?: ""
    }

    val userEmail: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.USER_EMAIL] ?: ""
    }

    val sortOrder: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.SORT_ORDER] ?: "name"
    }

    val filterPremium: Flow<Boolean> = preferences.map { prefs ->
        prefs[PreferencesKeys.FILTER_PREMIUM] ?: false
    }

    val gridViewEnabled: Flow<Boolean> = preferences.map { prefs ->
        prefs[PreferencesKeys.GRID_VIEW] ?: true
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_THEME] = enabled
        }
    }

    suspend fun updateStorageLocation(location: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.STORAGE_LOCATION] = location
        }
    }

    suspend fun updateStorageType(storageType: PersonalStorageType) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.STORAGE_TYPE] = storageType.value
        }
    }

    suspend fun updateLastSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_SYNC] = timestamp
        }
    }

    suspend fun updateUserInfo(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_NAME] = name
            prefs[PreferencesKeys.USER_EMAIL] = email
        }
    }

    suspend fun updateSortOrder(order: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SORT_ORDER] = order
        }
    }

    suspend fun updateFilterPremium(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FILTER_PREMIUM] = enabled
        }
    }

    suspend fun updateGridView(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.GRID_VIEW] = enabled
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }

    private object PreferencesKeys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val STORAGE_LOCATION = stringPreferencesKey("storage_location")
        val STORAGE_TYPE = stringPreferencesKey("storage_type")
        val LAST_SYNC = longPreferencesKey("last_sync")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FILTER_PREMIUM = booleanPreferencesKey("filter_premium")
        val GRID_VIEW = booleanPreferencesKey("grid_view")
    }
}