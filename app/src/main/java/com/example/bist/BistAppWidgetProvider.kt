package com.example.bist

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews

class BistAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // Trigger background check using existing WorkManager configurations
            val workManager = androidx.work.WorkManager.getInstance(context)
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<StockCheckWorker>().build()
            workManager.enqueue(workRequest)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        const val ACTION_REFRESH = "com.example.bist.ACTION_WIDGET_REFRESH"

        fun updateWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BistAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.bist_widget)
            val alarmPrefs = AlarmPreferences(context)
            val favorites = alarmPrefs.getFavorites().toList()
            val cachedStocks = alarmPrefs.getLastStocks()

            // Fetch current widget dimensions
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)

            // Dynamic adjustments based on size
            val showHeader = minHeight >= 120
            val maxVisibleItems = when {
                minHeight < 80 -> 1
                minHeight < 120 -> 2
                minHeight < 160 -> 3
                minHeight < 220 -> 4
                else -> 5
            }
            val hideCompanyName = minWidth < 185
            val smallText = minWidth < 140

            // Apply header visibility responsive rules
            views.setViewVisibility(R.id.widget_header_layout, if (showHeader) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_header_divider, if (showHeader) View.VISIBLE else View.GONE)

            // Setup Refresh Click
            val refreshIntent = Intent(context, BistAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // Setup Main App Click (clicking any row or widget launches app)
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (favorites.isEmpty()) {
                views.setViewVisibility(R.id.no_favorites_text, View.VISIBLE)
                views.setViewVisibility(R.id.widget_items_container, View.GONE)
            } else {
                views.setViewVisibility(R.id.no_favorites_text, View.GONE)
                views.setViewVisibility(R.id.widget_items_container, View.VISIBLE)

                val layouts = intArrayOf(
                    R.id.item1_layout, R.id.item2_layout, R.id.item3_layout, R.id.item4_layout, R.id.item5_layout
                )
                val hisseViews = intArrayOf(
                    R.id.item1_hisse, R.id.item2_hisse, R.id.item3_hisse, R.id.item4_hisse, R.id.item5_hisse
                )
                val nameViews = intArrayOf(
                    R.id.item1_name, R.id.item2_name, R.id.item3_name, R.id.item4_name, R.id.item5_name
                )
                val priceViews = intArrayOf(
                    R.id.item1_price, R.id.item2_price, R.id.item3_price, R.id.item4_price, R.id.item5_price
                )
                val changeViews = intArrayOf(
                    R.id.item1_change, R.id.item2_change, R.id.item3_change, R.id.item4_change, R.id.item5_change
                )

                for (i in 0 until 5) {
                    val layoutId = layouts[i]
                    if (i < favorites.size && i < maxVisibleItems) {
                        val ticker = favorites[i]
                        val stock = cachedStocks.find { it.hisse.uppercase().trim() == ticker.uppercase().trim() }

                        views.setViewVisibility(layoutId, View.VISIBLE)
                        views.setTextViewText(hisseViews[i], ticker)
                        views.setOnClickPendingIntent(layoutId, appPendingIntent)

                        // Dynamic text size adjustments
                        views.setTextViewTextSize(hisseViews[i], android.util.TypedValue.COMPLEX_UNIT_SP, if (smallText) 11f else 14f)
                        views.setViewVisibility(nameViews[i], if (hideCompanyName) View.GONE else View.VISIBLE)

                        if (stock != null) {
                            views.setTextViewText(nameViews[i], stock.sirket)
                            val priceFormatted = if (ticker.contains("XU")) {
                                String.format(java.util.Locale.US, "%.2f", stock.fiyat)
                            } else {
                                String.format(java.util.Locale.US, "%.2f ₺", stock.fiyat)
                            }
                            views.setTextViewText(priceViews[i], priceFormatted)

                            val changePct = stock.degisim
                            val changeSign = if (changePct >= 0) "+" else ""
                            views.setTextViewText(changeViews[i], String.format(java.util.Locale.US, "%s%.2f%%", changeSign, changePct))
                            views.setTextColor(changeViews[i], if (changePct >= 0) Color.parseColor("#10B981") else Color.parseColor("#EF4444"))
                        } else {
                            views.setTextViewText(nameViews[i], "Yükleniyor...")
                            views.setTextViewText(priceViews[i], "- ₺")
                            views.setTextViewText(changeViews[i], "-%")
                            views.setTextColor(changeViews[i], Color.parseColor("#9CA3AF"))
                        }

                        views.setTextViewTextSize(priceViews[i], android.util.TypedValue.COMPLEX_UNIT_SP, if (smallText) 11f else 13f)
                        views.setTextViewTextSize(changeViews[i], android.util.TypedValue.COMPLEX_UNIT_SP, if (smallText) 9f else 11f)
                    } else {
                        views.setViewVisibility(layoutId, View.GONE)
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
