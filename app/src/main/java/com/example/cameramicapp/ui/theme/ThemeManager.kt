package com.example.cameramicapp.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.cameramicapp.R

enum class AppTheme {
    IPN, ESCOM
}

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "theme_prefs", Context.MODE_PRIVATE
    )

    fun getTheme(): AppTheme {
        val themeName = prefs.getString("app_theme", AppTheme.IPN.name)
        return AppTheme.valueOf(themeName ?: AppTheme.IPN.name)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun applyTheme(activity: AppCompatActivity) {
        when (getTheme()) {
            AppTheme.IPN -> activity.setTheme(R.style.Theme_CameraMicApp_IPN)
            AppTheme.ESCOM -> activity.setTheme(R.style.Theme_CameraMicApp_ESCOM)
        }
    }

    companion object {
        fun setNightMode(nightMode: Boolean) {
            if (nightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        fun followSystemTheme() {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}