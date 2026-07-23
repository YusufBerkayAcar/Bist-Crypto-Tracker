package com.example.bist

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StockCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("StockCheckWorker", "Work started: background BIST price check")
        val alarmPrefs = AlarmPreferences(applicationContext)
        val activeAlarms = alarmPrefs.getAlarms().filter { it.isActive }

        return try {
            val stocks = YahooFinanceRepository.fetchBistStocks()
            val indices = YahooFinanceRepository.fetchIndices()
            val cryptos = YahooFinanceRepository.fetchCryptos()
            
            val combined = stocks + indices + cryptos

            // Cache to preferences and update widget
            alarmPrefs.saveLastStocks(combined)
            BistAppWidgetProvider.updateWidget(applicationContext)

            val notificationHelper = NotificationHelper(applicationContext)
            val stockMap = combined.associateBy { it.hisse.uppercase().trim() }

            if (activeAlarms.isNotEmpty()) {
                for (alarm in activeAlarms) {
                    val stockCode = alarm.hisse.uppercase().trim()
                    val stockInfo = stockMap[stockCode]
                    if (stockInfo != null) {
                        val isTriggered = when (alarm.type) {
                            AlarmType.PRICE_ABOVE -> stockInfo.fiyat >= alarm.thresholdValue
                            AlarmType.PRICE_BELOW -> stockInfo.fiyat <= alarm.thresholdValue
                            AlarmType.CHANGE_ABOVE -> stockInfo.degisim >= alarm.thresholdValue
                            AlarmType.CHANGE_BELOW -> stockInfo.degisim <= alarm.thresholdValue
                        }

                        if (isTriggered) {
                            val currentValueText = when (alarm.type) {
                                AlarmType.PRICE_ABOVE, AlarmType.PRICE_BELOW -> "${stockInfo.fiyat} ₺"
                                AlarmType.CHANGE_ABOVE, AlarmType.CHANGE_BELOW -> "%${String.format(java.util.Locale.US, "%.2f", stockInfo.degisim)}"
                            }
                            Log.d("StockCheckWorker", "Alarm triggered for $stockCode: type ${alarm.type}, threshold ${alarm.thresholdValue}, current: $currentValueText")
                            notificationHelper.showAlarmNotification(alarm, currentValueText)
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("StockCheckWorker", "Error fetching stock data in background", e)
            Result.retry()
        }
    }
}
