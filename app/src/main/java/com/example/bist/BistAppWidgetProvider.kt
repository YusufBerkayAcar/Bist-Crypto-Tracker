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
        when (intent.action) {
            ACTION_REFRESH -> {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<StockCheckWorker>().build()
                workManager.enqueue(workRequest)
            }
            ACTION_SWITCH_FAVORITES -> {
                val alarmPrefs = AlarmPreferences(context)
                alarmPrefs.setWidgetMode("favorites")
                updateWidget(context)
            }
            ACTION_SWITCH_PORTFOLIO -> {
                val alarmPrefs = AlarmPreferences(context)
                alarmPrefs.setWidgetMode("portfolio")
                updateWidget(context)
            }
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
        const val ACTION_SWITCH_FAVORITES = "com.example.bist.ACTION_WIDGET_SWITCH_FAVORITES"
        const val ACTION_SWITCH_PORTFOLIO = "com.example.bist.ACTION_WIDGET_SWITCH_PORTFOLIO"

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
            val widgetMode = alarmPrefs.getWidgetMode() // "favorites" veya "portfolio"
            val cachedStocks = alarmPrefs.getLastStocks()

            // Widget boyut bilgileri
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)

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

            // Header görünürlük
            views.setViewVisibility(R.id.widget_header_layout, if (showHeader) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_header_divider, if (showHeader) View.VISIBLE else View.GONE)

            // === Sekme Butonları Stili (Aktif/Pasif) ===
            if (widgetMode == "favorites") {
                views.setTextColor(R.id.widget_tab_favorites, Color.parseColor("#F9FAFB"))
                views.setInt(R.id.widget_tab_favorites, "setBackgroundColor", Color.parseColor("#4F46E5"))
                views.setTextColor(R.id.widget_tab_portfolio, Color.parseColor("#9CA3AF"))
                views.setInt(R.id.widget_tab_portfolio, "setBackgroundColor", Color.TRANSPARENT)
            } else {
                views.setTextColor(R.id.widget_tab_favorites, Color.parseColor("#9CA3AF"))
                views.setInt(R.id.widget_tab_favorites, "setBackgroundColor", Color.TRANSPARENT)
                views.setTextColor(R.id.widget_tab_portfolio, Color.parseColor("#F9FAFB"))
                views.setInt(R.id.widget_tab_portfolio, "setBackgroundColor", Color.parseColor("#10B981"))
            }

            // === Sekme Tıklama Intent'leri ===
            val favIntent = Intent(context, BistAppWidgetProvider::class.java).apply {
                action = ACTION_SWITCH_FAVORITES
            }
            val favPendingIntent = PendingIntent.getBroadcast(
                context, 1, favIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_tab_favorites, favPendingIntent)

            val portfolioIntent = Intent(context, BistAppWidgetProvider::class.java).apply {
                action = ACTION_SWITCH_PORTFOLIO
            }
            val portfolioPendingIntent = PendingIntent.getBroadcast(
                context, 2, portfolioIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_tab_portfolio, portfolioPendingIntent)

            // === Yenile Butonu ===
            val refreshIntent = Intent(context, BistAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // === Uygulama açma intent'i ===
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // === Veri Kaynağına Göre Doldurum ===
            if (widgetMode == "favorites") {
                populateFavorites(views, alarmPrefs, cachedStocks, maxVisibleItems, hideCompanyName, smallText, appPendingIntent)
            } else {
                populatePortfolio(views, alarmPrefs, cachedStocks, maxVisibleItems, hideCompanyName, smallText, appPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun populateFavorites(
            views: RemoteViews,
            alarmPrefs: AlarmPreferences,
            cachedStocks: List<StockInfo>,
            maxVisibleItems: Int,
            hideCompanyName: Boolean,
            smallText: Boolean,
            appPendingIntent: PendingIntent
        ) {
            val favorites = alarmPrefs.getFavorites().toList()

            views.setViewVisibility(R.id.no_portfolio_text, View.GONE)

            if (favorites.isEmpty()) {
                views.setViewVisibility(R.id.no_favorites_text, View.VISIBLE)
                views.setViewVisibility(R.id.widget_items_container, View.GONE)
            } else {
                views.setViewVisibility(R.id.no_favorites_text, View.GONE)
                views.setViewVisibility(R.id.widget_items_container, View.VISIBLE)

                populateItems(views, favorites.take(maxVisibleItems).map { ticker ->
                    val stock = cachedStocks.find { it.hisse.uppercase().trim() == ticker.uppercase().trim() }
                    WidgetRowData(ticker, stock?.sirket ?: "Yükleniyor...", stock?.fiyat, stock?.degisim, ticker)
                }, maxVisibleItems, hideCompanyName, smallText, appPendingIntent)
            }
        }

        private fun populatePortfolio(
            views: RemoteViews,
            alarmPrefs: AlarmPreferences,
            cachedStocks: List<StockInfo>,
            maxVisibleItems: Int,
            hideCompanyName: Boolean,
            smallText: Boolean,
            appPendingIntent: PendingIntent
        ) {
            val portfolio = alarmPrefs.getPortfolio()

            views.setViewVisibility(R.id.no_favorites_text, View.GONE)

            if (portfolio.isEmpty()) {
                views.setViewVisibility(R.id.no_portfolio_text, View.VISIBLE)
                views.setViewVisibility(R.id.widget_items_container, View.GONE)
            } else {
                views.setViewVisibility(R.id.no_portfolio_text, View.GONE)
                views.setViewVisibility(R.id.widget_items_container, View.VISIBLE)

                populateItems(views, portfolio.take(maxVisibleItems).map { item ->
                    val stock = cachedStocks.find {
                        it.hisse.uppercase().trim() == item.symbol.uppercase().trim() ||
                        "${it.hisse}-USD".uppercase().trim() == item.symbol.uppercase().trim()
                    }
                    val currentPrice = stock?.fiyat ?: item.buyPrice
                    val profitLoss = (currentPrice - item.buyPrice) / item.buyPrice * 100.0
                    val isCryptoOrUsd = item.symbol.contains("-USD") || item.symbol.contains("/USD")
                    val symbolChar = if (isCryptoOrUsd) "$" else "₺"
                    val priceStr = String.format(java.util.Locale.US, "%.2f %s", currentPrice, symbolChar)
                    val lotInfo = "${String.format(java.util.Locale.US, "%.1f", item.amount)} Lot"
                    WidgetRowData(item.symbol, lotInfo, currentPrice, profitLoss, item.symbol, priceStr)
                }, maxVisibleItems, hideCompanyName, smallText, appPendingIntent)
            }
        }

        private data class WidgetRowData(
            val ticker: String,
            val subtitle: String,
            val price: Double?,
            val changePct: Double?,
            val rawSymbol: String,
            val priceFormatted: String? = null
        )

        private fun populateItems(
            views: RemoteViews,
            items: List<WidgetRowData>,
            maxVisibleItems: Int,
            hideCompanyName: Boolean,
            smallText: Boolean,
            appPendingIntent: PendingIntent
        ) {
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
                if (i < items.size && i < maxVisibleItems) {
                    val item = items[i]
                    views.setViewVisibility(layoutId, View.VISIBLE)
                    views.setTextViewText(hisseViews[i], item.ticker)
                    views.setOnClickPendingIntent(layoutId, appPendingIntent)

                    views.setTextViewTextSize(hisseViews[i], android.util.TypedValue.COMPLEX_UNIT_SP, if (smallText) 11f else 14f)
                    views.setViewVisibility(nameViews[i], if (hideCompanyName) View.GONE else View.VISIBLE)
                    views.setTextViewText(nameViews[i], item.subtitle)

                    if (item.price != null) {
                        val priceText = item.priceFormatted ?: if (item.ticker.contains("XU")) {
                            String.format(java.util.Locale.US, "%.2f", item.price)
                        } else {
                            String.format(java.util.Locale.US, "%.2f ₺", item.price)
                        }
                        views.setTextViewText(priceViews[i], priceText)

                        val changePct = item.changePct ?: 0.0
                        val changeSign = if (changePct >= 0) "+" else ""
                        views.setTextViewText(changeViews[i], String.format(java.util.Locale.US, "%s%.2f%%", changeSign, changePct))
                        views.setTextColor(changeViews[i], if (changePct >= 0) Color.parseColor("#10B981") else Color.parseColor("#EF4444"))
                    } else {
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
    }
}
