package com.example.bist

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class StockInfo(
    @SerializedName("hisse") val hisse: String,
    @SerializedName("sirket") val sirket: String,
    @SerializedName("fiyat") val fiyat: Double,
    @SerializedName("degisim") val degisim: Double,
    @SerializedName("hacim") val hacim: Double,
    @SerializedName("trend") val trend: List<Double> = emptyList()
)

enum class AlarmType {
    PRICE_ABOVE,  // Fiyat belirlenen değerin üstünde/eşit
    PRICE_BELOW,  // Fiyat belirlenen değerin altında/eşit
    CHANGE_ABOVE, // Günlük değişim yüzdesi belirlenen değerin üstünde (örn. +5%)
    CHANGE_BELOW  // Günlük değişim yüzdesi belirlenen değerin altında (örn. -3%)
}

data class StockAlarm(
    val id: String = UUID.randomUUID().toString(),
    val hisse: String,
    val type: AlarmType,
    val thresholdValue: Double,
    val isActive: Boolean = true
)

data class SingleTrendResponse(
    @SerializedName("ticker") val ticker: String,
    @SerializedName("trend") val trend: List<Double>,
    @SerializedName("dates") val dates: List<String> = emptyList()
)
