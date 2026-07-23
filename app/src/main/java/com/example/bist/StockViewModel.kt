package com.example.bist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmPrefs = AlarmPreferences(application)
    private val workManager = WorkManager.getInstance(application)

    private val _stocks = MutableStateFlow<List<StockInfo>>(emptyList())
    val stocks: StateFlow<List<StockInfo>> = _stocks.asStateFlow()

    private val _indices = MutableStateFlow<List<StockInfo>>(emptyList())
    val indices: StateFlow<List<StockInfo>> = _indices.asStateFlow()

    private val _cryptos = MutableStateFlow<List<StockInfo>>(emptyList())
    val cryptos: StateFlow<List<StockInfo>> = _cryptos.asStateFlow()

    private val _alarms = MutableStateFlow<List<StockAlarm>>(emptyList())
    val alarms: StateFlow<List<StockAlarm>> = _alarms.asStateFlow()

    private val _webAppUrl = MutableStateFlow("")
    val webAppUrl: StateFlow<String> = _webAppUrl.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null

    init {
        _alarms.value = alarmPrefs.getAlarms()
        _favorites.value = alarmPrefs.getFavorites()
        
        // Start polling when ViewModel is created
        startPolling()
        // Sync WorkManager according to active alarms
        syncBackgroundWorker()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchStocks()
                delay(60000) // Poll every 1 minute
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    fun fetchStocks(bypassCache: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                val stocksDeferred = async { YahooFinanceRepository.fetchBistStocks() }
                val indicesDeferred = async { YahooFinanceRepository.fetchIndices() }
                val cryptosDeferred = async { YahooFinanceRepository.fetchCryptos() }
                
                val stocksResponse = stocksDeferred.await()
                val indicesResponse = indicesDeferred.await()
                val cryptosResponse = cryptosDeferred.await()

                _stocks.value = stocksResponse
                _indices.value = indicesResponse
                _cryptos.value = cryptosResponse
                
                alarmPrefs.saveLastStocks(stocksResponse + indicesResponse + cryptosResponse)
                BistAppWidgetProvider.updateWidget(getApplication())
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error fetching data", e)
                _errorMessage.value = "Veri çekilemedi: ${e.localizedMessage ?: "Bağlantı hatası"}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun updateWebAppUrl(newUrl: String) {
        alarmPrefs.saveWebAppUrl(newUrl)
        _webAppUrl.value = newUrl
        fetchStocks()
    }

    fun addAlarm(hisse: String, type: AlarmType, thresholdValue: Double) {
        val alarm = StockAlarm(
            hisse = hisse.uppercase().trim(),
            type = type,
            thresholdValue = thresholdValue
        )
        alarmPrefs.addAlarm(alarm)
        _alarms.value = alarmPrefs.getAlarms()
        syncBackgroundWorker()
    }

    fun deleteAlarm(alarmId: String) {
        alarmPrefs.deleteAlarm(alarmId)
        _alarms.value = alarmPrefs.getAlarms()
        syncBackgroundWorker()
    }

    fun toggleAlarmActive(alarmId: String) {
        alarmPrefs.toggleAlarmActive(alarmId)
        _alarms.value = alarmPrefs.getAlarms()
        syncBackgroundWorker()
    }

    fun toggleFavorite(hisseCode: String) {
        val updated = alarmPrefs.toggleFavorite(hisseCode)
        _favorites.value = updated
        BistAppWidgetProvider.updateWidget(getApplication())
    }

    private fun syncBackgroundWorker() {
        val workRequest = PeriodicWorkRequestBuilder<StockCheckWorker>(15, TimeUnit.MINUTES)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "BistStockCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.d("StockViewModel", "Background worker scheduled.")
    }

    private val _activeStockTrend = MutableStateFlow<List<Double>?>(null)
    val activeStockTrend: StateFlow<List<Double>?> = _activeStockTrend.asStateFlow()

    private val _activeStockDates = MutableStateFlow<List<String>?>(null)
    val activeStockDates: StateFlow<List<String>?> = _activeStockDates.asStateFlow()

    private val _isTrendLoading = MutableStateFlow(false)
    val isTrendLoading: StateFlow<Boolean> = _isTrendLoading.asStateFlow()

    fun fetchStockTrend(ticker: String, range: String = "1mo") {
        viewModelScope.launch {
            _activeStockTrend.value = null
            _activeStockDates.value = null
            _isTrendLoading.value = true
            try {
                val response = YahooFinanceRepository.fetchSingleTrend(ticker, range)
                _activeStockTrend.value = response.trend
                _activeStockDates.value = response.dates
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error fetching trend for $ticker", e)
                _activeStockTrend.value = emptyList()
                _activeStockDates.value = emptyList()
            } finally {
                _isTrendLoading.value = false
            }
        }
    }

    fun clearActiveTrend() {
        _activeStockTrend.value = null
        _activeStockDates.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
