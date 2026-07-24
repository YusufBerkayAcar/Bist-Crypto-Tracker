package com.example.bist

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SortMode {
    DEFAULT,
    GAINERS,
    LOSERS,
    VOLUME
}

// Curated modern Slate/Teal color system matching premium FinTech designs
object BistTheme {
    val Background = Color(0xFF0B0F19)      // Slate 950 deep space blue
    val CardBackground = Color(0xFF111827)  // Deep rich gray-900 card
    val CardBorder = Color(0xFF1F2937)      // Gray 800 subtle borders
    
    val Primary = Color(0xFF4F46E5)         // Indigo 600
    val PrimaryLight = Color(0xFF6366F1)    // Indigo 500
    val Accent = Color(0xFF0EA5E9)          // Sky 500 for tabs & highlights
    
    val TextPrimary = Color(0xFFF9FAFB)     // Gray 50
    val TextSecondary = Color(0xFF9CA3AF)   // Gray 400
    
    val Green = Color(0xFF10B981)           // Emerald 500
    val GreenBg = Color(0xFF064E3B)         // Emerald 900
    
    val Red = Color(0xFFEF4444)             // Rose 500
    val RedBg = Color(0xFF7F1D1D)           // Rose 900
    
    val HeartPink = Color(0xFFF43F5E)       // Vibrant heart rose pink
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: StockViewModel) {
    val stocks by viewModel.stocks.collectAsState()
    val indices by viewModel.indices.collectAsState()
    val cryptos by viewModel.cryptos.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val webAppUrl by viewModel.webAppUrl.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showUrlDialog by remember { mutableStateOf(false) }
    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var selectedStockForAlarm by remember { mutableStateOf<String?>(null) }
    var currentTab by remember { mutableStateOf(0) } // 0: Favoriler, 1: Hisseler, 2: Kripto, 3: Alarmlar, 4: Portföy
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortMode by remember { mutableStateOf(SortMode.DEFAULT) }
    var selectedStockForDetails by remember { mutableStateOf<StockInfo?>(null) }
    var isIndicesExpanded by rememberSaveable { mutableStateOf(true) }

    val portfolio by viewModel.portfolio.collectAsState()
    var showAddPortfolioDialog by remember { mutableStateOf(false) }

    // Silme Onay Diyalogları için State'ler
    var alarmToDelete by remember { mutableStateOf<StockAlarm?>(null) }
    var portfolioItemToDelete by remember { mutableStateOf<PortfolioItem?>(null) }

    LaunchedEffect(selectedStockForDetails) {
        selectedStockForDetails?.let { stock ->
            viewModel.fetchStockTrend(stock.hisse)
        } ?: run {
            viewModel.clearActiveTrend()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "BIST & KRİPTO TAKİP",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = BistTheme.TextPrimary,
                        fontSize = 19.sp
                    )
                },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                            color = BistTheme.PrimaryLight,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.fetchStocks(bypassCache = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = BistTheme.TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BistTheme.Background
                )
            )
        },
        floatingActionButton = {
            if (currentTab == 3) { // Alarmlar
                FloatingActionButton(
                    onClick = {
                        selectedStockForAlarm = null
                        showAddAlarmDialog = true
                    },
                    containerColor = BistTheme.Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .padding(bottom = 4.dp, end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alarm Ekle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else if (currentTab == 4) { // Portföy
                FloatingActionButton(
                    onClick = { showAddPortfolioDialog = true },
                    containerColor = BistTheme.Green,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .padding(bottom = 4.dp, end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Varlık Ekle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BistTheme.Background, Color(0xFF030712))
                    )
                )
        ) {

            // Error banner
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BistTheme.CardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            errorMessage!!,
                            color = BistTheme.TextSecondary,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { viewModel.fetchStocks() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Tekrar Dene", tint = BistTheme.Accent)
                        }
                    }
                }
            }

            if (indices.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAKRO GÖSTERGELER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BistTheme.TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isIndicesExpanded) "GİZLE" else "GÖSTER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BistTheme.Accent,
                        modifier = Modifier
                            .clickable { isIndicesExpanded = !isIndicesExpanded }
                            .padding(vertical = 4.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isIndicesExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(indices) { indexInfo ->
                            Card(
                                modifier = Modifier
                                    .width(155.dp)
                                    .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedStockForDetails = indexInfo },
                                colors = CardDefaults.cardColors(containerColor = BistTheme.CardBackground)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = indexInfo.sirket,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BistTheme.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = indexInfo.hisse,
                                        fontSize = 10.sp,
                                        color = BistTheme.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isCryptoOrUsd = indexInfo.hisse.contains("-USD") || indexInfo.hisse.contains("/USD") || indexInfo.sirket.contains("Ons Altın")
                                        val isIndex = indexInfo.hisse.startsWith("XU") || indexInfo.hisse.startsWith("^")
                                        val priceFormatted = when {
                                            isIndex -> String.format(java.util.Locale.US, "%.2f", indexInfo.fiyat)
                                            isCryptoOrUsd -> "$${String.format(java.util.Locale.US, "%.2f", indexInfo.fiyat)}"
                                            else -> "${String.format(java.util.Locale.US, "%.2f", indexInfo.fiyat)} ₺"
                                        }
                                        Text(
                                            text = priceFormatted,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BistTheme.TextPrimary
                                        )
                                        
                                        val isPositive = indexInfo.degisim >= 0
                                        val changeSign = if (isPositive) "+" else ""
                                        val badgeBgColor = if (isPositive) BistTheme.GreenBg.copy(alpha = 0.4f) else BistTheme.RedBg.copy(alpha = 0.4f)
                                        val badgeTextColor = if (isPositive) BistTheme.Green else BistTheme.Red
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(badgeBgColor)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = String.format(java.util.Locale.US, "%s%.2f%%", changeSign, indexInfo.degisim),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Tabs
            val favorites by viewModel.favorites.collectAsState()

            ScrollableTabRow(
                selectedTabIndex = currentTab,
                containerColor = BistTheme.Background,
                contentColor = BistTheme.PrimaryLight,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                        color = BistTheme.Accent,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = BistTheme.CardBorder, thickness = 1.dp)
                }
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("Favoriler (${(stocks + indices + cryptos).filter { it.hisse.uppercase().trim() in favorites }.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = BistTheme.Accent,
                    unselectedContentColor = BistTheme.TextSecondary
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("Hisseler (${stocks.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = BistTheme.Accent,
                    unselectedContentColor = BistTheme.TextSecondary
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text("Kripto (${cryptos.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = BistTheme.Accent,
                    unselectedContentColor = BistTheme.TextSecondary
                )
                Tab(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    text = { Text("Alarmlar (${alarms.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = BistTheme.Accent,
                    unselectedContentColor = BistTheme.TextSecondary
                )
                Tab(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    text = { Text("💼 Portföy (${portfolio.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = BistTheme.Green,
                    unselectedContentColor = BistTheme.TextSecondary
                )
            }

            // Search Bar & Sort Options
            if (currentTab == 0 || currentTab == 1 || currentTab == 2) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ara (Hisse, Coin veya İsim)...", color = BistTheme.TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = BistTheme.TextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = BistTheme.TextSecondary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BistTheme.TextPrimary,
                            unfocusedTextColor = BistTheme.TextPrimary,
                            focusedContainerColor = BistTheme.CardBackground,
                            unfocusedContainerColor = BistTheme.CardBackground,
                            focusedBorderColor = BistTheme.PrimaryLight,
                            unfocusedBorderColor = BistTheme.CardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sıralama Çipleri (Sorting Chips)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sortOptions = listOf(
                            "Tümü" to SortMode.DEFAULT,
                            "🚀 En Çok Yükselenler" to SortMode.GAINERS,
                            "🔻 En Çok Düşenler" to SortMode.LOSERS,
                            "📊 Yüksek Hacimliler" to SortMode.VOLUME
                        )
                        items(sortOptions) { (label, mode) ->
                            val isSelected = selectedSortMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSortMode = mode },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BistTheme.Primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = BistTheme.CardBackground,
                                    labelColor = BistTheme.TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = BistTheme.CardBorder,
                                    selectedBorderColor = BistTheme.PrimaryLight
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                // Performans Optimizasyonu: Lazy Tab Rendering
                // Filtreleme ve sıralama SADECE aktif sekmede hesaplanır, diğer sekmelerde boş liste döner.
                // Bu sayede kullanılmayan sekmelerde sıfır CPU harcanır.

                val favStocks = remember(currentTab, searchQuery, selectedSortMode, stocks, indices, cryptos, favorites) {
                    if (currentTab != 0) emptyList()
                    else {
                        val rawFav = (stocks + indices + cryptos).filter { 
                            it.hisse.uppercase().trim() in favorites &&
                            (it.hisse.contains(searchQuery, ignoreCase = true) || it.sirket.contains(searchQuery, ignoreCase = true))
                        }
                        when (selectedSortMode) {
                            SortMode.GAINERS -> rawFav.sortedByDescending { it.degisim }
                            SortMode.LOSERS -> rawFav.sortedBy { it.degisim }
                            SortMode.VOLUME -> rawFav.sortedByDescending { it.hacim }
                            SortMode.DEFAULT -> rawFav
                        }
                    }
                }

                val filteredStocks = remember(currentTab, searchQuery, selectedSortMode, stocks) {
                    if (currentTab != 1) emptyList()
                    else {
                        val rawStocks = stocks.filter {
                            it.hisse.contains(searchQuery, ignoreCase = true) || it.sirket.contains(searchQuery, ignoreCase = true)
                        }
                        when (selectedSortMode) {
                            SortMode.GAINERS -> rawStocks.sortedByDescending { it.degisim }
                            SortMode.LOSERS -> rawStocks.sortedBy { it.degisim }
                            SortMode.VOLUME -> rawStocks.sortedByDescending { it.hacim }
                            SortMode.DEFAULT -> rawStocks
                        }
                    }
                }

                val filteredCryptos = remember(currentTab, searchQuery, selectedSortMode, cryptos) {
                    if (currentTab != 2) emptyList()
                    else {
                        val rawCryptos = cryptos.filter {
                            it.hisse.contains(searchQuery, ignoreCase = true) || it.sirket.contains(searchQuery, ignoreCase = true)
                        }
                        when (selectedSortMode) {
                            SortMode.GAINERS -> rawCryptos.sortedByDescending { it.degisim }
                            SortMode.LOSERS -> rawCryptos.sortedBy { it.degisim }
                            SortMode.VOLUME -> rawCryptos.sortedByDescending { it.hacim }
                            SortMode.DEFAULT -> rawCryptos
                        }
                    }
                }

                when (currentTab) {
                    0 -> { // Favoriler

                        if (favStocks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    if (searchQuery.isEmpty()) {
                                        "Favori hisseniz bulunmuyor.\nHisseler sekmesinden kalp simgesine tıklayarak ekleyebilirsiniz."
                                    } else {
                                        "Aramanızla eşleşen favori hisse bulunamadı."
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(24.dp),
                                    color = BistTheme.TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(favStocks, key = { it.hisse }) { stock ->
                                    StockCard(
                                        stock = stock,
                                        isFavorite = true,
                                        onFavoriteToggle = { viewModel.toggleFavorite(stock.hisse) },
                                        onAddAlarmClick = {
                                            selectedStockForAlarm = stock.hisse
                                            showAddAlarmDialog = true
                                        },
                                        onItemClick = { selectedStockForDetails = stock }
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // Tüm Hisseler
                        if (stocks.isEmpty() && isRefreshing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BistTheme.PrimaryLight)
                            }
                        } else if (stocks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Hisse yükleniyor...", color = BistTheme.TextSecondary)
                            }
                        } else if (filteredStocks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Aramanızla eşleşen hisse bulunamadı.", color = BistTheme.TextSecondary)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredStocks, key = { it.hisse }) { stock ->
                                    val isFav = stock.hisse.uppercase().trim() in favorites
                                    StockCard(
                                        stock = stock,
                                        isFavorite = isFav,
                                        onFavoriteToggle = { viewModel.toggleFavorite(stock.hisse) },
                                        onAddAlarmClick = {
                                            selectedStockForAlarm = stock.hisse
                                            showAddAlarmDialog = true
                                        },
                                        onItemClick = { selectedStockForDetails = stock }
                                    )
                                }
                            }
                        }
                    }
                    2 -> { // Kripto Paralar
                        if (cryptos.isEmpty() && isRefreshing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BistTheme.PrimaryLight)
                            }
                        } else if (cryptos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Kripto para yükleniyor...", color = BistTheme.TextSecondary)
                            }
                        } else if (filteredCryptos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Aramanızla eşleşen kripto para bulunamadı.", color = BistTheme.TextSecondary)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredCryptos, key = { it.hisse }) { stock ->
                                    val isFav = stock.hisse.uppercase().trim() in favorites
                                    StockCard(
                                        stock = stock,
                                        isFavorite = isFav,
                                        onFavoriteToggle = { viewModel.toggleFavorite(stock.hisse) },
                                        onAddAlarmClick = {
                                            selectedStockForAlarm = stock.hisse
                                            showAddAlarmDialog = true
                                        },
                                        onItemClick = { selectedStockForDetails = stock }
                                    )
                                }
                            }
                        }
                    }
                    3 -> { // Alarmlar
                        if (alarms.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alarm Yok",
                                        tint = BistTheme.TextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Kurulu Fiyat Alarmı Bulunmuyor",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BistTheme.TextPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sağ alttaki + butonuna basarak istediğiniz hisse veya kripto para için alarm oluşturabilirsiniz.",
                                        fontSize = 13.sp,
                                        color = BistTheme.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(alarms, key = { it.id }) { alarm ->
                                    val stockInfo = (stocks + indices + cryptos).find { it.hisse.uppercase().trim() == alarm.hisse.uppercase().trim() }
                                    AlarmCard(
                                        alarm = alarm,
                                        stockInfo = stockInfo,
                                        onToggle = { viewModel.toggleAlarmActive(alarm.id) },
                                        onDelete = { alarmToDelete = alarm }
                                    )
                                }
                            }
                        }
                    }
                    4 -> { // Portföy
                        if (portfolio.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Portföy Boş",
                                        tint = BistTheme.TextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Portföyünüzde Varlık Bulunmuyor",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BistTheme.TextPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sağ alttaki yeşil + butonuna basarak aldığınız hisse veya kripto paraları ekleyebilir, canlı Kar/Zarar durumunuzu takip edebilirsiniz.",
                                        fontSize = 13.sp,
                                        color = BistTheme.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            val allStocks = stocks + indices + cryptos
                            
                            // Toplam maliyet, mevcut değer ve Kar/Zarar hesapları
                            var totalCostTL = 0.0
                            var currentValueTL = 0.0

                            portfolio.forEach { item ->
                                val currentStock = allStocks.find { 
                                    it.hisse.uppercase().trim() == item.symbol.uppercase().trim() ||
                                    "${it.hisse}-USD".uppercase().trim() == item.symbol.uppercase().trim()
                                }
                                val currentPrice = currentStock?.fiyat ?: item.buyPrice
                                totalCostTL += (item.amount * item.buyPrice)
                                currentValueTL += (item.amount * currentPrice)
                            }

                            val totalProfitTL = currentValueTL - totalCostTL
                            val totalProfitPercent = if (totalCostTL > 0) (totalProfitTL / totalCostTL) * 100.0 else 0.0

                            LazyColumn(
                                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Portföy Genel Özet Kartı
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(20.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = BistTheme.CardBackground
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                "TOPLAM PORTFÖY DEĞERİ",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BistTheme.TextSecondary,
                                                letterSpacing = 0.8.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${String.format(java.util.Locale.US, "%.2f", currentValueTL)} ₺",
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Black,
                                                color = BistTheme.TextPrimary
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))
                                            HorizontalDivider(color = BistTheme.CardBorder, thickness = 0.8.dp)
                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("TOPLAM MALİYET", fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                                                    Text("${String.format(java.util.Locale.US, "%.2f", totalCostTL)} ₺", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BistTheme.TextPrimary)
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("TOPLAM KÂR / ZARAR", fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                                                    val isPositive = totalProfitTL >= 0
                                                    val profitColor = if (isPositive) BistTheme.Green else BistTheme.Red
                                                    val sign = if (isPositive) "+" else ""
                                                    Text(
                                                        "$sign${String.format(java.util.Locale.US, "%.2f", totalProfitTL)} ₺ (%$sign${String.format(java.util.Locale.US, "%.2f", totalProfitPercent)})",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = profitColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                items(portfolio, key = { it.id }) { item ->
                                    val currentStock = allStocks.find { 
                                        it.hisse.uppercase().trim() == item.symbol.uppercase().trim() ||
                                        "${it.hisse}-USD".uppercase().trim() == item.symbol.uppercase().trim()
                                    }
                                    PortfolioCard(
                                        item = item,
                                        currentStock = currentStock,
                                        onDelete = { portfolioItemToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showUrlDialog) {
            UrlSettingDialog(
                currentUrl = webAppUrl,
                onDismiss = { showUrlDialog = false },
                onSave = { newUrl ->
                    viewModel.updateWebAppUrl(newUrl)
                    showUrlDialog = false
                }
            )
        }

        if (showAddAlarmDialog) {
            AddAlarmDialog(
                preselectedStock = selectedStockForAlarm,
                allStocks = stocks + indices + cryptos,
                onDismiss = { showAddAlarmDialog = false },
                onSave = { hisse, type, value ->
                    viewModel.addAlarm(hisse, type, value)
                    showAddAlarmDialog = false
                }
            )
        }

        if (showAddPortfolioDialog) {
            AddPortfolioDialog(
                allStocks = stocks + indices + cryptos,
                onDismiss = { showAddPortfolioDialog = false },
                onSave = { symbol, amount, buyPrice ->
                    viewModel.addPortfolioItem(symbol, amount, buyPrice)
                    showAddPortfolioDialog = false
                }
            )
        }

        if (alarmToDelete != null) {
            AlertDialog(
                onDismissRequest = { alarmToDelete = null },
                containerColor = BistTheme.CardBackground,
                title = { Text("Alarmı Sil", color = BistTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("${alarmToDelete!!.hisse} için kurulan alarmı silmek istediğinize emin misiniz?", color = BistTheme.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAlarm(alarmToDelete!!.id)
                            alarmToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BistTheme.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sil", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { alarmToDelete = null }) {
                        Text("İptal", color = BistTheme.TextSecondary)
                    }
                }
            )
        }

        if (portfolioItemToDelete != null) {
            AlertDialog(
                onDismissRequest = { portfolioItemToDelete = null },
                containerColor = BistTheme.CardBackground,
                title = { Text("Varlığı Sil", color = BistTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("${portfolioItemToDelete!!.symbol} varlığını portföyünüzden silmek istediğinize emin misiniz?", color = BistTheme.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePortfolioItem(portfolioItemToDelete!!.id)
                            portfolioItemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BistTheme.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sil", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { portfolioItemToDelete = null }) {
                        Text("İptal", color = BistTheme.TextSecondary)
                    }
                }
            )
        }

        if (selectedStockForDetails != null) {
            StockDetailsDialog(
                stock = selectedStockForDetails!!,
                viewModel = viewModel,
                onDismiss = { selectedStockForDetails = null },
                onAddAlarmClick = {
                    selectedStockForAlarm = selectedStockForDetails!!.hisse
                    selectedStockForDetails = null
                    showAddAlarmDialog = true
                }
            )
        }
    }
}

@Composable
fun StockCard(
    stock: StockInfo,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddAlarmClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(20.dp))
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = BistTheme.CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite Button
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (isFavorite) BistTheme.HeartPink else BistTheme.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Stock Details
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = stock.hisse,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BistTheme.TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.sirket,
                    fontSize = 11.sp,
                    color = BistTheme.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trend Graphic (Sparkline) — Path Caching Optimizasyonu
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Path hesaplamasını remember ile önbelleğe al (kaydırmada sıfır CPU yükü)
                val trendPath = remember(stock.trend, stock.degisim) {
                    stock.trend to stock.degisim // sadece key olarak kullan, path Canvas içinde çizilecek
                }

                Canvas(modifier = Modifier.size(50.dp, 22.dp)) {
                    val width = size.width
                    val height = size.height
                    val path = androidx.compose.ui.graphics.Path()

                    if (trendPath.first.size > 1) {
                        val trendData = trendPath.first
                        val minVal = trendData.minOrNull() ?: 0.0
                        val maxVal = trendData.maxOrNull() ?: 0.0
                        val delta = maxVal - minVal

                        if (delta > 0.0) {
                            val stepX = width / (trendData.size - 1)
                            for (index in trendData.indices) {
                                val price = trendData[index]
                                val x = index * stepX
                                val normalizedY = (price - minVal) / delta
                                val y = (height - (normalizedY * height * 0.8f + height * 0.1f)).toFloat()
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                        } else {
                            path.moveTo(0f, height / 2f)
                            path.lineTo(width, height / 2f)
                        }
                    } else {
                        // Mock curve fallback if trend data is not available yet
                        val degisim = trendPath.second
                        path.moveTo(0f, height * if (degisim >= 0) 0.8f else 0.2f)
                        if (degisim >= 0) {
                            path.quadraticTo(width * 0.3f, height * 0.7f, width * 0.5f, height * 0.4f)
                            path.quadraticTo(width * 0.75f, height * 0.1f, width, height * 0.15f)
                        } else {
                            path.quadraticTo(width * 0.3f, height * 0.3f, width * 0.5f, height * 0.6f)
                            path.quadraticTo(width * 0.75f, height * 0.9f, width, height * 0.85f)
                        }
                    }

                    drawPath(
                        path = path,
                        color = if (trendPath.second >= 0) BistTheme.Green else BistTheme.Red,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }

            // Price and Badge Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                val isCryptoOrUsd = stock.hisse.contains("-USD") || stock.hisse.contains("/USD") || stock.hisse == "XAU/USD" || stock.sirket.contains("Ons Altın")
                val isIndex = stock.hisse.startsWith("XU") || stock.hisse.startsWith("^")
                val currencySymbol = when {
                    isIndex -> ""
                    isCryptoOrUsd -> "$"
                    else -> "₺"
                }
                val priceFormatted = when {
                    isIndex -> String.format(java.util.Locale.US, "%.2f", stock.fiyat)
                    isCryptoOrUsd -> "$currencySymbol${String.format(java.util.Locale.US, "%.2f", stock.fiyat)}"
                    else -> "${String.format(java.util.Locale.US, "%.2f", stock.fiyat)} ₺"
                }
                Text(
                    text = priceFormatted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BistTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val isPositive = stock.degisim >= 0
                val badgeColor = if (isPositive) BistTheme.Green else BistTheme.Red
                val badgeBg = if (isPositive) BistTheme.GreenBg else BistTheme.RedBg
                val changeSign = if (isPositive) "+" else ""
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg.copy(alpha = 0.4f))
                        .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$changeSign${String.format(java.util.Locale.US, "%.2f", stock.degisim)}%",
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Alarm Button
            IconButton(
                onClick = onAddAlarmClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BistTheme.Primary.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alarm Ekle",
                    tint = BistTheme.PrimaryLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: StockAlarm,
    stockInfo: StockInfo?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isActive) BistTheme.CardBackground else BistTheme.CardBackground.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.hisse,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (alarm.isActive) BistTheme.TextPrimary else BistTheme.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val conditionText = when (alarm.type) {
                    AlarmType.PRICE_ABOVE -> "Fiyat >= ${alarm.thresholdValue} TL"
                    AlarmType.PRICE_BELOW -> "Fiyat <= ${alarm.thresholdValue} TL"
                    AlarmType.CHANGE_ABOVE -> "Günlük Değişim >= %${alarm.thresholdValue}"
                    AlarmType.CHANGE_BELOW -> "Günlük Değişim <= %${alarm.thresholdValue}"
                }
                Text(
                    text = "Hedef: $conditionText",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BistTheme.TextSecondary
                )
                if (stockInfo != null) {
                    val currentValueText = when (alarm.type) {
                        AlarmType.PRICE_ABOVE, AlarmType.PRICE_BELOW -> "${String.format(java.util.Locale.US, "%.2f", stockInfo.fiyat)} TL"
                        AlarmType.CHANGE_ABOVE, AlarmType.CHANGE_BELOW -> "%${String.format(java.util.Locale.US, "%.2f", stockInfo.degisim)}"
                    }
                    Text(
                        text = "Güncel: $currentValueText",
                        fontSize = 12.sp,
                        color = if (alarm.isActive) BistTheme.PrimaryLight else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isActive,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BistTheme.Accent,
                        checkedTrackColor = BistTheme.Primary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = BistTheme.Background
                    )
                )

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = BistTheme.Red
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlSettingDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BistTheme.CardBackground,
        titleContentColor = BistTheme.TextPrimary,
        textContentColor = BistTheme.TextSecondary,
        title = { Text("Google Web App URL Ayarı", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Lütfen Google Sheets Apps Script'ten aldığınız Web App URL'ini girin:",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Apps Script URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BistTheme.TextPrimary,
                        unfocusedTextColor = BistTheme.TextPrimary,
                        focusedLabelColor = BistTheme.PrimaryLight,
                        unfocusedLabelColor = BistTheme.TextSecondary,
                        focusedBorderColor = BistTheme.PrimaryLight,
                        unfocusedBorderColor = BistTheme.CardBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(urlText) },
                colors = ButtonDefaults.buttonColors(containerColor = BistTheme.Primary)
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BistTheme.TextSecondary)
            ) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    preselectedStock: String?,
    allStocks: List<StockInfo>,
    onDismiss: () -> Unit,
    onSave: (String, AlarmType, Double) -> Unit
) {
    var stockCode by remember { mutableStateOf(preselectedStock ?: "") }
    var valueText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AlarmType.PRICE_ABOVE) }

    val filteredSuggestions = remember(stockCode) {
        if (stockCode.isEmpty()) emptyList()
        else allStocks.filter { 
            it.hisse.contains(stockCode, ignoreCase = true) || it.sirket.contains(stockCode, ignoreCase = true) 
        }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BistTheme.CardBackground,
        titleContentColor = BistTheme.TextPrimary,
        textContentColor = BistTheme.TextSecondary,
        title = { Text("Yeni Alarm Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (preselectedStock != null) {
                    Text(
                        "Hisse: $preselectedStock",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BistTheme.Accent
                    )
                } else {
                    Column {
                        OutlinedTextField(
                            value = stockCode,
                            onValueChange = { stockCode = it },
                            label = { Text("Hisse veya Kripto Kodu (Örn: THYAO, BTC-USD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BistTheme.TextPrimary,
                                unfocusedTextColor = BistTheme.TextPrimary,
                                focusedLabelColor = BistTheme.PrimaryLight,
                                unfocusedLabelColor = BistTheme.TextSecondary,
                                focusedBorderColor = BistTheme.PrimaryLight,
                                unfocusedBorderColor = BistTheme.CardBorder
                            )
                        )

                        if (filteredSuggestions.isNotEmpty() && stockCode.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BistTheme.Background),
                                border = BorderStroke(0.5.dp, BistTheme.CardBorder)
                            ) {
                                Column {
                                    filteredSuggestions.forEach { stock ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { stockCode = stock.hisse }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stock.hisse,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BistTheme.Accent
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stock.sirket,
                                                fontSize = 12.sp,
                                                color = BistTheme.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (stock != filteredSuggestions.last()) {
                                            HorizontalDivider(color = BistTheme.CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Alarm Koşulu:", fontWeight = FontWeight.Bold, color = BistTheme.TextPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = AlarmType.PRICE_ABOVE }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == AlarmType.PRICE_ABOVE,
                            onClick = { selectedType = AlarmType.PRICE_ABOVE },
                            colors = RadioButtonDefaults.colors(selectedColor = BistTheme.Accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fiyatın Üstüne Çıkarsa (>= ₺)", fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = AlarmType.PRICE_BELOW }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == AlarmType.PRICE_BELOW,
                            onClick = { selectedType = AlarmType.PRICE_BELOW },
                            colors = RadioButtonDefaults.colors(selectedColor = BistTheme.Accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fiyatın Altına Düşerse (<= ₺)", fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = AlarmType.CHANGE_ABOVE }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == AlarmType.CHANGE_ABOVE,
                            onClick = { selectedType = AlarmType.CHANGE_ABOVE },
                            colors = RadioButtonDefaults.colors(selectedColor = BistTheme.Accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Günlük Artış Yüzdesi (>= %)", fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = AlarmType.CHANGE_BELOW }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == AlarmType.CHANGE_BELOW,
                            onClick = { selectedType = AlarmType.CHANGE_BELOW },
                            colors = RadioButtonDefaults.colors(selectedColor = BistTheme.Accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Günlük Düşüş Yüzdesi (<= %)", fontSize = 14.sp)
                    }
                }

                val inputLabel = when (selectedType) {
                    AlarmType.PRICE_ABOVE, AlarmType.PRICE_BELOW -> "Hedef Fiyat (TL)"
                    AlarmType.CHANGE_ABOVE, AlarmType.CHANGE_BELOW -> "Hedef Değişim Yüzdesi (%)"
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(inputLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BistTheme.TextPrimary,
                        unfocusedTextColor = BistTheme.TextPrimary,
                        focusedLabelColor = BistTheme.PrimaryLight,
                        unfocusedLabelColor = BistTheme.TextSecondary,
                        focusedBorderColor = BistTheme.PrimaryLight,
                        unfocusedBorderColor = BistTheme.CardBorder
                    )
                )
            }
        },
        confirmButton = {
            val targetVal = valueText.toDoubleOrNull()
            Button(
                onClick = {
                    if (stockCode.isNotEmpty() && targetVal != null) {
                        onSave(stockCode, selectedType, targetVal)
                    }
                },
                enabled = stockCode.isNotEmpty() && targetVal != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BistTheme.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = BistTheme.Primary.copy(alpha = 0.3f),
                    disabledContentColor = Color.Gray
                )
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BistTheme.TextSecondary)
            ) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailsDialog(
    stock: StockInfo,
    viewModel: StockViewModel,
    onDismiss: () -> Unit,
    onAddAlarmClick: () -> Unit
) {
    val activeTrend by viewModel.activeStockTrend.collectAsState()
    val activeDates by viewModel.activeStockDates.collectAsState()
    val isTrendLoading by viewModel.isTrendLoading.collectAsState()

    var selectedTimeframe by remember { mutableStateOf("1mo") }

    LaunchedEffect(selectedTimeframe) {
        viewModel.fetchStockTrend(stock.hisse, selectedTimeframe)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BistTheme.CardBackground,
        titleContentColor = BistTheme.TextPrimary,
        textContentColor = BistTheme.TextSecondary,
        confirmButton = {
            Button(
                onClick = onAddAlarmClick,
                colors = ButtonDefaults.buttonColors(containerColor = BistTheme.Primary)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alarm Kur")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BistTheme.TextSecondary)
            ) {
                Text("Kapat")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stock.hisse, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BistTheme.TextPrimary)
                    Text(text = stock.sirket, fontSize = 12.sp, color = BistTheme.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                val favorites by viewModel.favorites.collectAsState()
                val isFav = stock.hisse.uppercase().trim() in favorites
                
                IconButton(onClick = { viewModel.toggleFavorite(stock.hisse) }) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (isFav) BistTheme.HeartPink else BistTheme.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val isCryptoOrUsd = stock.hisse.contains("-USD") || stock.hisse.contains("/USD") || stock.hisse == "XAU/USD"
                val isIndex = stock.hisse.startsWith("XU") || stock.hisse.startsWith("^")
                val detailCurrencySymbol = when {
                    isIndex -> ""
                    isCryptoOrUsd -> "$"
                    else -> "₺"
                }
                val detailPriceFormatted = when {
                    isIndex -> String.format(java.util.Locale.US, "%.2f", stock.fiyat)
                    isCryptoOrUsd -> "$detailCurrencySymbol${String.format(java.util.Locale.US, "%.2f", stock.fiyat)}"
                    else -> "${String.format(java.util.Locale.US, "%.2f", stock.fiyat)} ₺"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("GÜNCEL FİYAT", fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                        Text(detailPriceFormatted, fontSize = 20.sp, fontWeight = FontWeight.Black, color = BistTheme.TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("GÜNLÜK DEĞİŞİM", fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                        val isPositive = stock.degisim >= 0
                        val badgeColor = if (isPositive) BistTheme.Green else BistTheme.Red
                        val changeSign = if (isPositive) "+" else ""
                        Text("$changeSign${String.format(java.util.Locale.US, "%.2f", stock.degisim)}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = badgeColor)
                    }
                }

                // Seçilen zaman dilimine göre değişim oranı
                val periodChangeLabel = when(selectedTimeframe) {
                    "1d" -> "GÜNLÜK DEĞİŞİM"
                    "1w" -> "HAFTALIK DEĞİŞİM"
                    "1mo" -> "AYLIK DEĞİŞİM"
                    "3mo" -> "3 AYLIK DEĞİŞİM"
                    "1y" -> "YILLIK DEĞİŞİM"
                    else -> "DEĞİŞİM"
                }

                // Trend verisinden dönemsel değişim hesapla
                val periodChangePercent = if (selectedTimeframe == "1d") {
                    stock.degisim
                } else if (activeTrend != null && activeTrend!!.size > 1) {
                    val firstPrice = activeTrend!!.first()
                    val lastPrice = activeTrend!!.last()
                    if (firstPrice > 0) ((lastPrice - firstPrice) / firstPrice) * 100.0 else 0.0
                } else {
                    null // Henüz yüklenmiyor
                }

                val periodPriceChange = if (selectedTimeframe == "1d") {
                    null
                } else if (activeTrend != null && activeTrend!!.size > 1) {
                    activeTrend!!.last() - activeTrend!!.first()
                } else {
                    null
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("İŞLEM HACMİ", fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                        val formattedHacim = when {
                            stock.hacim >= 1_000_000_000 -> "${String.format(java.util.Locale.US, "%.2f", stock.hacim / 1_000_000_000.0)} B"
                            stock.hacim >= 1_000_000 -> "${String.format(java.util.Locale.US, "%.2f", stock.hacim / 1_000_000.0)} M"
                            stock.hacim >= 1_000 -> "${String.format(java.util.Locale.US, "%.2f", stock.hacim / 1_000.0)} K"
                            else -> String.format(java.util.Locale.US, "%.0f", stock.hacim)
                        }
                        Text(formattedHacim, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BistTheme.TextPrimary)
                    }

                    if (selectedTimeframe != "1d") {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(periodChangeLabel, fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            if (isTrendLoading || periodChangePercent == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = BistTheme.PrimaryLight,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (periodPriceChange != null) {
                                        val priceChangeSign = if (periodPriceChange >= 0) "+" else ""
                                        val priceChangeSuffix = when {
                                            isIndex -> ""
                                            isCryptoOrUsd -> " $"
                                            else -> " ₺"
                                        }
                                        Text(
                                            text = "$priceChangeSign${String.format(java.util.Locale.US, "%.2f", periodPriceChange)}$priceChangeSuffix",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BistTheme.TextSecondary
                                        )
                                    }

                                    val isPeriodPositive = periodChangePercent >= 0
                                    val periodBadgeColor = if (isPeriodPositive) BistTheme.Green else BistTheme.Red
                                    val periodBadgeBg = if (isPeriodPositive) BistTheme.GreenBg else BistTheme.RedBg
                                    val periodSign = if (isPeriodPositive) "+" else ""

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(periodBadgeBg.copy(alpha = 0.4f))
                                            .border(0.5.dp, periodBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$periodSign${String.format(java.util.Locale.US, "%.2f", periodChangePercent)}%",
                                            color = periodBadgeColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val titleText = when(selectedTimeframe) {
                    "1d" -> "GÜNLÜK FİYAT TRENDİ (5 Dakikalık Veriler)"
                    "1w" -> "HAFTALIK FİYAT TRENDİ (15 Dakikalık Veriler)"
                    "1mo" -> "30 GÜNLÜK FİYAT TRENDİ"
                    "3mo" -> "3 AYLIK FİYAT TRENDİ"
                    "1y" -> "1 YILLIK FİYAT TRENDİ"
                    else -> "FİYAT TRENDİ"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(titleText, fontSize = 10.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val timeframes = listOf(
                        "1d" to "1G",
                        "1w" to "1H",
                        "1mo" to "1A",
                        "3mo" to "3A",
                        "1y" to "1Y"
                    )
                    timeframes.forEach { (key, label) ->
                        val isSelected = selectedTimeframe == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BistTheme.Accent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BistTheme.Accent else BistTheme.CardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTimeframe = key }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BistTheme.Accent else BistTheme.TextSecondary
                            )
                        }
                    }
                }

                // Interactive Chart with touch crosshair
                var touchedIndex by remember { mutableStateOf<Int?>(null) }

                // Dokunulan noktadaki fiyat ve tarih bilgisi tooltip
                if (touchedIndex != null && activeTrend != null && activeTrend!!.size > 1) {
                    val idx = touchedIndex!!.coerceIn(0, activeTrend!!.size - 1)
                    val touchedPrice = activeTrend!![idx]
                    val touchedDate = if (activeDates != null && idx < activeDates!!.size) activeDates!![idx] else ""
                    val currSuffix = when {
                        isIndex -> ""
                        isCryptoOrUsd -> " $"
                        else -> " ₺"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BistTheme.Accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = BistTheme.Accent.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TARİH", fontSize = 8.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                                Text(touchedDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BistTheme.Accent)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("FİYAT", fontSize = 8.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                                Text(
                                    "${String.format(java.util.Locale.US, "%.2f", touchedPrice)}$currSuffix",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BistTheme.TextPrimary
                                )
                            }
                            // Başlangıca göre değişim
                            if (activeTrend!!.first() > 0) {
                                val chgFromStart = ((touchedPrice - activeTrend!!.first()) / activeTrend!!.first()) * 100.0
                                val chgPositive = chgFromStart >= 0
                                val chgSign = if (chgPositive) "+" else ""
                                val chgColor = if (chgPositive) BistTheme.Green else BistTheme.Red
                                val chgBg = if (chgPositive) BistTheme.GreenBg else BistTheme.RedBg
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DEĞİŞİM", fontSize = 8.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(chgBg.copy(alpha = 0.4f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "$chgSign${String.format(java.util.Locale.US, "%.2f", chgFromStart)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = chgColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = BistTheme.Background)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTrendLoading) {
                            CircularProgressIndicator(color = BistTheme.PrimaryLight)
                        } else if (activeTrend != null && activeTrend!!.size > 1) {
                            val trendData = activeTrend!!
                            val minVal = trendData.minOrNull() ?: 0.0
                            val maxVal = trendData.maxOrNull() ?: 0.0
                            val delta = maxVal - minVal

                            val isPeriodPositive = if (trendData.isNotEmpty()) {
                                trendData.last() >= trendData.first()
                            } else {
                                stock.degisim >= 0
                            }
                            val chartColor = if (isPeriodPositive) BistTheme.Green else BistTheme.Red

                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val priceSuffix = when {
                                        isIndex -> ""
                                        isCryptoOrUsd -> " $"
                                        else -> " ₺"
                                    }
                                    Text("En Düşük: ${String.format(java.util.Locale.US, "%.2f", minVal)}$priceSuffix", fontSize = 9.sp, color = BistTheme.TextSecondary)
                                    Text("En Yüksek: ${String.format(java.util.Locale.US, "%.2f", maxVal)}$priceSuffix", fontSize = 9.sp, color = BistTheme.TextSecondary)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .pointerInput(trendData) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    val stepX = size.width.toFloat() / (trendData.size - 1).coerceAtLeast(1)
                                                    val idx = (offset.x / stepX).toInt().coerceIn(0, trendData.size - 1)
                                                    touchedIndex = idx
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    val stepX = size.width.toFloat() / (trendData.size - 1).coerceAtLeast(1)
                                                    val idx = (change.position.x / stepX).toInt().coerceIn(0, trendData.size - 1)
                                                    touchedIndex = idx
                                                },
                                                onDragEnd = {
                                                    touchedIndex = null
                                                },
                                                onDragCancel = {
                                                    touchedIndex = null
                                                }
                                            )
                                        }
                                        .pointerInput(trendData) {
                                            detectTapGestures { offset ->
                                                val stepX = size.width.toFloat() / (trendData.size - 1).coerceAtLeast(1)
                                                val idx = (offset.x / stepX).toInt().coerceIn(0, trendData.size - 1)
                                                touchedIndex = if (touchedIndex == idx) null else idx
                                            }
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height
                                        val path = androidx.compose.ui.graphics.Path()

                                        val stepX = width / (trendData.size - 1)
                                        val effectiveDelta = if (delta > 0.0) delta else 1.0

                                        // Y koordinatlarını hesapla (crosshair için lazım)
                                        val yPoints = mutableListOf<Float>()
                                        for (index in trendData.indices) {
                                            val price = trendData[index]
                                            val x = index * stepX
                                            val normalizedY = (price - minVal) / effectiveDelta
                                            val y = (height - (normalizedY * height * 0.8f + height * 0.1f)).toFloat()
                                            yPoints.add(y)
                                            if (index == 0) {
                                                path.moveTo(x, y)
                                            } else {
                                                path.lineTo(x, y)
                                            }
                                        }

                                        // Gradient dolgu
                                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                                            addPath(path)
                                            lineTo(width, height)
                                            lineTo(0f, height)
                                            close()
                                        }
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(chartColor.copy(alpha = 0.25f), Color.Transparent)
                                            )
                                        )

                                        // Ana çizgi
                                        drawPath(
                                            path = path,
                                            color = chartColor,
                                            style = Stroke(
                                                width = 2.5.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )

                                        // Crosshair göstergesi
                                        if (touchedIndex != null) {
                                            val idx = touchedIndex!!.coerceIn(0, trendData.size - 1)
                                            val crossX = idx * stepX
                                            val crossY = yPoints[idx]

                                            // Dikey kesikli çizgi
                                            drawLine(
                                                color = BistTheme.Accent.copy(alpha = 0.6f),
                                                start = Offset(crossX, 0f),
                                                end = Offset(crossX, height),
                                                strokeWidth = 1.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                            )

                                            // Yatay kesikli çizgi
                                            drawLine(
                                                color = BistTheme.Accent.copy(alpha = 0.3f),
                                                start = Offset(0f, crossY),
                                                end = Offset(width, crossY),
                                                strokeWidth = 1.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                            )

                                            // Dış halka (glow efekti)
                                            drawCircle(
                                                color = BistTheme.Accent.copy(alpha = 0.15f),
                                                radius = 12.dp.toPx(),
                                                center = Offset(crossX, crossY)
                                            )

                                            // İç dolu nokta
                                            drawCircle(
                                                color = BistTheme.Accent,
                                                radius = 5.dp.toPx(),
                                                center = Offset(crossX, crossY)
                                            )

                                            // Beyaz çekirdek
                                            drawCircle(
                                                color = Color.White,
                                                radius = 2.5.dp.toPx(),
                                                center = Offset(crossX, crossY)
                                            )
                                        }
                                    }
                                }

                                if (activeDates != null && activeDates!!.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(activeDates!!.first(), fontSize = 9.sp, color = BistTheme.TextSecondary)
                                        if (activeDates!!.size > 2) {
                                            val midIdx = activeDates!!.size / 2
                                            Text(activeDates!![midIdx], fontSize = 9.sp, color = BistTheme.TextSecondary)
                                        }
                                        Text(activeDates!!.last(), fontSize = 9.sp, color = BistTheme.TextSecondary)
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Google Finance verisi yükleniyor veya bu hisse için geçmiş fiyat bulunmuyor.",
                                fontSize = 11.sp,
                                color = BistTheme.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioCard(
    item: PortfolioItem,
    currentStock: StockInfo?,
    onDelete: () -> Unit
) {
    val currentPrice = currentStock?.fiyat ?: item.buyPrice
    val totalCost = item.amount * item.buyPrice
    val currentValue = item.amount * currentPrice
    val profitLoss = currentValue - totalCost
    val profitPercent = if (totalCost > 0) (profitLoss / totalCost) * 100.0 else 0.0

    val isCryptoOrUsd = item.symbol.contains("-USD") || item.symbol.contains("/USD")
    val symbolChar = if (isCryptoOrUsd) "$" else "₺"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BistTheme.CardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = BistTheme.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.symbol,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BistTheme.TextPrimary
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.2f", item.amount)} Lot / Adet",
                        fontSize = 12.sp,
                        color = BistTheme.TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPositive = profitLoss >= 0
                    val profitColor = if (isPositive) BistTheme.Green else BistTheme.Red
                    val sign = if (isPositive) "+" else ""

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$sign${String.format(java.util.Locale.US, "%.2f", profitLoss)} $symbolChar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = profitColor
                        )
                        Text(
                            text = "%$sign${String.format(java.util.Locale.US, "%.2f", profitPercent)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = profitColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = BistTheme.TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BistTheme.CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ALIŞ FİYATI", fontSize = 9.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${String.format(java.util.Locale.US, "%.2f", item.buyPrice)} $symbolChar", fontSize = 13.sp, color = BistTheme.TextPrimary, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GÜNCEL FİYAT", fontSize = 9.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${String.format(java.util.Locale.US, "%.2f", currentPrice)} $symbolChar", fontSize = 13.sp, color = BistTheme.TextPrimary, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("TOPLAM DEĞER", fontSize = 9.sp, color = BistTheme.TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${String.format(java.util.Locale.US, "%.2f", currentValue)} $symbolChar", fontSize = 13.sp, color = BistTheme.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPortfolioDialog(
    allStocks: List<StockInfo>,
    onDismiss: () -> Unit,
    onSave: (symbol: String, amount: Double, buyPrice: Double) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }

    val filteredSuggestions = remember(symbol) {
        if (symbol.isEmpty()) emptyList()
        else allStocks.filter { 
            it.hisse.contains(symbol, ignoreCase = true) || it.sirket.contains(symbol, ignoreCase = true) 
        }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BistTheme.CardBackground,
        title = {
            Text("Portföye Varlık Ekle", color = BistTheme.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text("Hisse veya Kripto Kodu (Örn: THYAO, BTC-USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BistTheme.TextPrimary,
                            unfocusedTextColor = BistTheme.TextPrimary,
                            focusedLabelColor = BistTheme.PrimaryLight,
                            unfocusedLabelColor = BistTheme.TextSecondary,
                            focusedBorderColor = BistTheme.PrimaryLight,
                            unfocusedBorderColor = BistTheme.CardBorder
                        )
                    )

                    if (filteredSuggestions.isNotEmpty() && symbol.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BistTheme.Background),
                            border = BorderStroke(0.5.dp, BistTheme.CardBorder)
                        ) {
                            Column {
                                filteredSuggestions.forEach { stock ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                symbol = stock.hisse
                                                buyPriceText = String.format(java.util.Locale.US, "%.2f", stock.fiyat)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stock.hisse,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BistTheme.Accent
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stock.sirket,
                                            fontSize = 12.sp,
                                            color = BistTheme.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (stock != filteredSuggestions.last()) {
                                        HorizontalDivider(color = BistTheme.CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Miktar / Lot Adedi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BistTheme.TextPrimary,
                        unfocusedTextColor = BistTheme.TextPrimary,
                        focusedLabelColor = BistTheme.PrimaryLight,
                        unfocusedLabelColor = BistTheme.TextSecondary,
                        focusedBorderColor = BistTheme.PrimaryLight,
                        unfocusedBorderColor = BistTheme.CardBorder
                    )
                )

                OutlinedTextField(
                    value = buyPriceText,
                    onValueChange = { buyPriceText = it },
                    label = { Text("Alış Fiyatı (TL / USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BistTheme.TextPrimary,
                        unfocusedTextColor = BistTheme.TextPrimary,
                        focusedLabelColor = BistTheme.PrimaryLight,
                        unfocusedLabelColor = BistTheme.TextSecondary,
                        focusedBorderColor = BistTheme.PrimaryLight,
                        unfocusedBorderColor = BistTheme.CardBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val buyPrice = buyPriceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (symbol.isNotEmpty() && amount > 0 && buyPrice > 0) {
                        onSave(symbol, amount, buyPrice)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BistTheme.Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Ekle", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = BistTheme.TextSecondary)
            }
        }
    )
}
