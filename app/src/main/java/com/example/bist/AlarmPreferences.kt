package com.example.bist

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ALARMS = "key_alarms"
        private const val KEY_WEB_APP_URL = "key_web_app_url"
        private const val KEY_FAVORITES = "key_favorites"
        private const val KEY_LAST_STOCKS = "key_last_stocks"
        private const val DEFAULT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzdAF6DmFa-X1HL8ToYE9lCN7iZ3asotrmD8GLn8IPssM1DMvCGX3SWQu--VqCRmyU/exec"
    }

    fun getWebAppUrl(): String {
        val url = prefs.getString(KEY_WEB_APP_URL, DEFAULT_WEB_APP_URL) ?: DEFAULT_WEB_APP_URL
        if (url.contains("YOUR_SCRIPT_ID") || url.isEmpty()) {
            return DEFAULT_WEB_APP_URL
        }
        return url
    }

    fun saveWebAppUrl(url: String) {
        prefs.edit().putString(KEY_WEB_APP_URL, url).apply()
    }

    fun getAlarms(): List<StockAlarm> {
        val alarmsJson = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<StockAlarm>>() {}.type
            gson.fromJson(alarmsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAlarms(alarms: List<StockAlarm>) {
        val alarmsJson = gson.toJson(alarms)
        prefs.edit().putString(KEY_ALARMS, alarmsJson).apply()
    }

    fun addAlarm(alarm: StockAlarm) {
        val currentAlarms = getAlarms().toMutableList()
        currentAlarms.add(alarm)
        saveAlarms(currentAlarms)
    }

    fun deleteAlarm(alarmId: String) {
        val currentAlarms = getAlarms().toMutableList()
        currentAlarms.removeAll { it.id == alarmId }
        saveAlarms(currentAlarms)
    }

    fun toggleAlarmActive(alarmId: String) {
        val currentAlarms = getAlarms().map {
            if (it.id == alarmId) it.copy(isActive = !it.isActive) else it
        }
        saveAlarms(currentAlarms)
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(hisse: String): Set<String> {
        val currentFavorites = getFavorites().toMutableSet()
        val formattedHisse = hisse.uppercase().trim()
        if (currentFavorites.contains(formattedHisse)) {
            currentFavorites.remove(formattedHisse)
        } else {
            currentFavorites.add(formattedHisse)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, currentFavorites).apply()
        return currentFavorites
    }

    fun getLastStocks(): List<StockInfo> {
        val stocksJson = prefs.getString(KEY_LAST_STOCKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<StockInfo>>() {}.type
            gson.fromJson(stocksJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLastStocks(stocks: List<StockInfo>) {
        val stocksJson = gson.toJson(stocks)
        prefs.edit().putString(KEY_LAST_STOCKS, stocksJson).apply()
    }
}
