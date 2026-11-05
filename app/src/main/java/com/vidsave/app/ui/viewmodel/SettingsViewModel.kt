package com.vidsave.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppTheme {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AppLanguage {
    ENGLISH,
    ARABIC
}

data class SettingsUiState(
    val apiEndpointUrl: String = "https://api.vidsave.app/v1/extract",
    val theme: AppTheme = AppTheme.DARK,
    val language: AppLanguage = AppLanguage.ARABIC,
    val installedVersionName: String = "1.0.0",
    val installedVersionCode: Long = 1L
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val PREFS = "vidsave_settings"
    private val KEY_API_URL = "api_url"
    private val KEY_THEME = "app_theme"
    private val KEY_LANG = "app_lang"

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiEndpointUrl = prefs.getString(KEY_API_URL, "https://api.vidsave.app/v1/extract") ?: "https://api.vidsave.app/v1/extract",
            theme = try { AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.DARK.name)!!) } catch (e: Exception) { AppTheme.DARK },
            language = try { AppLanguage.valueOf(prefs.getString(KEY_LANG, AppLanguage.ARABIC.name)!!) } catch (e: Exception) { AppLanguage.ARABIC },
            installedVersionName = getVersionName(application),
            installedVersionCode = getVersionCode(application)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateApiUrl(newUrl: String) {
        prefs.edit().putString(KEY_API_URL, newUrl).apply()
        _uiState.update { it.copy(apiEndpointUrl = newUrl) }
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _uiState.update { it.copy(theme = theme) }
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANG, language.name).apply()
        _uiState.update { it.copy(language = language) }
    }

    private fun getVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun getVersionCode(context: Context): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }
}
