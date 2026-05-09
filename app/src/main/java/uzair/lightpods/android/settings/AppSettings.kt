package uzair.lightpods.android.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )

    private val _themeMode =
        MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> =
        _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit()
            .putString(KEY_THEME, mode.name)
            .apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME, null)
            ?: return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(stored)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    companion object {
        private const val PREFS_NAME = "lightpods_settings"
        private const val KEY_THEME = "theme_mode"
    }
}
