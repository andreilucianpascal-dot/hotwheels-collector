package com.example.hotwheelscollectors.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    val lastCloudSync: Flow<Long> = preferences.map { prefs ->
        prefs[PreferencesKeys.LAST_CLOUD_SYNC] ?: 0L
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

    // === THEME SETTINGS ===

    /**
     * Theme mode: "light", "dark", or "system".
     * Default: "system".
     */
    val themeMode: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: "system"
    }

    /**
     * Color scheme key from HotWheelsThemeManager (e.g., "default", "hotwheels_classic").
     * Default: "default".
     */
    val colorScheme: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.COLOR_SCHEME] ?: "default"
    }

    /**
     * Whether to use dynamic Material You colors when available.
     * Default: true.
     */
    val useDynamicColor: Flow<Boolean> = preferences.map { prefs ->
        prefs[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true
    }

    /**
     * Global font scale applied to the app typography.
     * Default: 1.0f (no scaling).
     */
    val fontScale: Flow<Float> = preferences.map { prefs ->
        prefs[PreferencesKeys.FONT_SCALE] ?: 1.0f
    }

    /**
     * Custom global color scheme (for "custom" mode) - 3 user-defined colors.
     * Stored as ARGB Int. 0 means "not set / use default".
     */
    val customSchemeColor1: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.CUSTOM_SCHEME_COLOR_1] ?: 0
    }

    val customSchemeColor2: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.CUSTOM_SCHEME_COLOR_2] ?: 0
    }

    val customSchemeColor3: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.CUSTOM_SCHEME_COLOR_3] ?: 0
    }

    /**
     * Custom colors for main screen category buttons.
     * Stored as ARGB Int. 0 means "use default color in UI".
     */
    val mainButtonMainlineColor: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_BTN_MAINLINE_COLOR] ?: 0
    }

    val mainButtonPremiumColor: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_BTN_PREMIUM_COLOR] ?: 0
    }

    val mainButtonSilverColor: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_BTN_SILVER_COLOR] ?: 0
    }

    val mainButtonTreasureHuntColor: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_BTN_TH_COLOR] ?: 0
    }

    val mainButtonSuperTreasureHuntColor: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_BTN_STH_COLOR] ?: 0
    }

    val mainButtonOthersColor: Flow<Int> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_BTN_OTHERS_COLOR] ?: 0
    }

    /**
     * Font family for MainScreen.
     * Values: "default", "sans", "serif", "mono".
     */
    val mainScreenFontFamily: Flow<String> = preferences.map { prefs ->
        prefs[PreferencesKeys.MAIN_SCREEN_FONT] ?: "default"
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
            // ✅ CRITICAL: Sync storageLocation when storageType changes
            // This ensures DynamicStorageRepository reads the correct preference
            val locationValue = when (storageType) {
                PersonalStorageType.LOCAL -> "Device"
                PersonalStorageType.GOOGLE_DRIVE -> "Google Drive"
            }
            prefs[PreferencesKeys.STORAGE_LOCATION] = locationValue
        }
    }

    suspend fun updateLastSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_SYNC] = timestamp
        }
    }

    suspend fun updateLastCloudSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_CLOUD_SYNC] = timestamp
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
        val LAST_CLOUD_SYNC = longPreferencesKey("last_cloud_sync")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FILTER_PREMIUM = booleanPreferencesKey("filter_premium")
        val GRID_VIEW = booleanPreferencesKey("grid_view")

        // Theme-related keys
        val THEME_MODE = stringPreferencesKey("theme_mode")          // "light", "dark", "system"
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")      // "default", "hotwheels_classic", etc.
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val CUSTOM_SCHEME_COLOR_1 = intPreferencesKey("custom_scheme_color_1")
        val CUSTOM_SCHEME_COLOR_2 = intPreferencesKey("custom_scheme_color_2")
        val CUSTOM_SCHEME_COLOR_3 = intPreferencesKey("custom_scheme_color_3")

        // Main screen button colors (category-level)
        val MAIN_BTN_MAINLINE_COLOR = intPreferencesKey("main_btn_mainline_color")
        val MAIN_BTN_PREMIUM_COLOR = intPreferencesKey("main_btn_premium_color")
        val MAIN_BTN_SILVER_COLOR = intPreferencesKey("main_btn_silver_color")
        val MAIN_BTN_TH_COLOR = intPreferencesKey("main_btn_th_color")
        val MAIN_BTN_STH_COLOR = intPreferencesKey("main_btn_sth_color")
        val MAIN_BTN_OTHERS_COLOR = intPreferencesKey("main_btn_others_color")
        val MAIN_SCREEN_FONT = stringPreferencesKey("main_screen_font")
    }

    // === THEME SETTINGS UPDATE HELPERS ===

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateColorScheme(scheme: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.COLOR_SCHEME] = scheme
        }
    }

    suspend fun updateUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FONT_SCALE] = scale
        }
    }

    /**
     * Update one of the 3 custom scheme colors (ARGB Int).
     * slot: 1, 2, or 3.
     */
    suspend fun updateCustomSchemeColor(slot: Int, color: Int) {
        context.dataStore.edit { prefs ->
            when (slot) {
                1 -> prefs[PreferencesKeys.CUSTOM_SCHEME_COLOR_1] = color
                2 -> prefs[PreferencesKeys.CUSTOM_SCHEME_COLOR_2] = color
                3 -> prefs[PreferencesKeys.CUSTOM_SCHEME_COLOR_3] = color
            }
        }
    }

    /**
     * Update color (ARGB Int) for a main screen category button.
     * Category ids: "mainline", "premium", "silver", "treasure_hunt", "super_treasure_hunt", "others".
     */
    suspend fun updateMainButtonColor(category: String, color: Int) {
        context.dataStore.edit { prefs ->
            when (category) {
                "mainline" -> prefs[PreferencesKeys.MAIN_BTN_MAINLINE_COLOR] = color
                "premium" -> prefs[PreferencesKeys.MAIN_BTN_PREMIUM_COLOR] = color
                "silver" -> prefs[PreferencesKeys.MAIN_BTN_SILVER_COLOR] = color
                "treasure_hunt" -> prefs[PreferencesKeys.MAIN_BTN_TH_COLOR] = color
                "super_treasure_hunt" -> prefs[PreferencesKeys.MAIN_BTN_STH_COLOR] = color
                "others" -> prefs[PreferencesKeys.MAIN_BTN_OTHERS_COLOR] = color
            }
        }
    }

    suspend fun updateMainScreenFontFamily(font: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MAIN_SCREEN_FONT] = font
        }
    }
}