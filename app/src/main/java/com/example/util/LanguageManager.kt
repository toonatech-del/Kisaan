package com.example.util

import android.content.Context
import android.content.SharedPreferences

enum class AppLanguage(val code: String, val displayName: String) {
    HINDI("hi", "हिन्दी"),
    ENGLISH("en", "English")
}

object LanguageManager {
    private const val PREFS_NAME = "kisan_zameen_language_prefs"
    private const val KEY_LANG = "selected_language"

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANG, "hi") ?: "hi"
        return if (code == "en") AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.code).apply()
    }
}
