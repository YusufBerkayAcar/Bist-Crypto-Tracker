function doGet(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();

  // 1. Tek hisse/endeks/kripto trend isteği (Yahoo Finance entegrasyonu)
  if (e && e.parameter && e.parameter.ticker) {
    var range = e.parameter.range || "1mo";
    return getSingleTrend(e.parameter.ticker, range);
  }

  // 2. Kripto Para isteği: ?type=crypto (CoinGecko Canlı API)
  if (e && e.parameter && e.parameter.type === "crypto") {
    return ContentService.createTextOutput(JSON.stringify(getCryptoFromCoinGecko()))
      .setMimeType(ContentService.MimeType.JSON);
  }

  // 3. Sayfa2 (Makro Endeks) isteği: ?type=indices
  if (e && e.parameter && e.parameter.type === "indices") {
    var sheet = ss.getSheetByName("Sayfa2");
    if (!sheet) {
      var sheets = ss.getSheets();
      sheet = sheets.length > 1 ? sheets[1] : sheets[0];
    }

    var data = sheet.getDataRange().getValues();
    var jsonData = [];

    for (var i = 1; i < data.length; i++) {
      var sembol = data[i][1]; // Sembol (B Sütunu)
      if (sembol && sembol.toString().trim() !== "") {
        jsonData.push({
          "hisse": sembol.toString().trim(),
          "sirket": (data[i][0] || "").toString().trim(), // Gösterge (A Sütunu)
          "fiyat": parseNum(data[i][2]), // Fiyat (C Sütunu)
          "degisim": parsePct(data[i][3]), // Günlük Değişim (D Sütunu)
          "hacim": 0,
          "trend": []
        });
      }
    }
    return ContentService.createTextOutput(JSON.stringify(jsonData))
      .setMimeType(ContentService.MimeType.JSON);
  }

  // 4. Varsayılan Sayfa1 (Hisse) isteği
  var sheet = ss.getSheetByName("Sayfa1") || ss.getSheets()[0];
  var data = sheet.getDataRange().getValues();
  var jsonData = [];

  for (var i = 1; i < data.length; i++) {
    var sembol = data[i][0]; // Sembol (A Sütunu)
    if (sembol && sembol.toString().trim() !== "") {
      var clean = sembol.toString().replace(/[^A-Za-z0-9]/g, "").toUpperCase();
      jsonData.push({
        "hisse": clean,
        "sirket": (data[i][1] || "").toString().trim(), // Şirket Adı (B Sütunu)
        "fiyat": parseNum(data[i][2]), // Fiyat (C Sütunu)
        "degisim": parsePct(data[i][3]), // Günlük Değişim (D Sütunu)
        "hacim": parseNum(data[i][4]), // Hacim (E Sütunu)
        "trend": []
      });
    }
  }

  return ContentService.createTextOutput(JSON.stringify(jsonData))
    .setMimeType(ContentService.MimeType.JSON);
}

// KRİPTO CANLI FİYAT ÇEKİCİ (Yahoo Finance API Entegrasyonu)
function getCryptoFromCoinGecko() {
  var coins = [
    { symbol: "BTC-USD", name: "Bitcoin" },
    { symbol: "ETH-USD", name: "Ethereum" },
    { symbol: "SOL-USD", name: "Solana" },
    { symbol: "XRP-USD", name: "XRP" },
    { symbol: "BNB-USD", name: "BNB" },
    { symbol: "DOGE-USD", name: "Dogecoin" },
    { symbol: "AVAX-USD", name: "Avalanche" },
    { symbol: "ADA-USD", name: "Cardano" },
    { symbol: "SHIB-USD", name: "Shiba Inu" },
    { symbol: "DOT-USD", name: "Polkadot" },
    { symbol: "LINK-USD", name: "Chainlink" },
    { symbol: "MATIC-USD", name: "Polygon (POL)" },
    { symbol: "PEPE-USD", name: "Pepe" },
    { symbol: "SUI-USD", name: "Sui" },
    { symbol: "NEAR-USD", name: "NEAR Protocol" },
    { symbol: "APT-USD", name: "Aptos" },
    { symbol: "FET-USD", name: "Artificial Superintelligence" },
    { symbol: "RENDER-USD", name: "Render" },
    { symbol: "INJ-USD", name: "Injective" },
    { symbol: "LTC-USD", name: "Litecoin" },
    { symbol: "TRX-USD", name: "TRON" },
    { symbol: "BCH-USD", name: "Bitcoin Cash" },
    { symbol: "ATOM-USD", name: "Cosmos" },
    { symbol: "FIL-USD", name: "Filecoin" },
    { symbol: "ARBM-USD", name: "Arbitrum" },
    { symbol: "OP-USD", name: "Optimism" },
    { symbol: "TIA-USD", name: "Celestia" },
    { symbol: "FLOKI-USD", name: "Floki" },
    { symbol: "BONK-USD", name: "Bonk" }
  ];

  var jsonData = [];

  coins.forEach(function (coin) {
    try {
      var url = "https://query1.finance.yahoo.com/v8/finance/chart/" + coin.symbol + "?range=1d&interval=1d";
      var response = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
      if (response.getResponseCode() === 200) {
        var json = JSON.parse(response.getContentText());
        if (json.chart && json.chart.result && json.chart.result.length > 0) {
          var meta = json.chart.result[0].meta;
          var price = meta.regularMarketPrice || 0;
          var prevClose = meta.chartPreviousClose || meta.previousClose || price;
          var change = prevClose > 0 ? ((price - prevClose) / prevClose) * 100 : 0;

          var volume = meta.regularMarketVolume || 0;

          jsonData.push({
            "hisse": coin.symbol,
            "sirket": coin.name,
            "fiyat": Math.round(price * 100) / 100,
            "degisim": Math.round(change * 100) / 100,
            "hacim": volume,
            "trend": []
          });
        }
      }
    } catch (e) {
      Logger.log("Kripto çekme hatası: " + e.toString());
    }
  });

  return jsonData;
}

function getSingleTrend(ticker, range) {
  var clean = ticker.trim().toUpperCase();
  var yahooTicker = "";

  if (clean.indexOf("-") !== -1) {
    yahooTicker = clean;
  } else if (clean.indexOf("/") !== -1) {
    yahooTicker = clean.replace("/", "") + "=X";
  } else if (clean.indexOf("=") !== -1) {
    yahooTicker = clean;
  } else {
    yahooTicker = clean + ".IS";
  }

  var prices = [];
  var dates = [];
  var interval = "1d";

  if (range === "1d") {
    interval = "5m";
  } else if (range === "1w") {
    range = "5d";
    interval = "15m";
  } else if (range === "3mo") {
    interval = "1d";
  } else if (range === "1y") {
    interval = "1d";
  } else {
    range = "1mo";
    interval = "1d";
  }

  try {
    var url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooTicker + "?range=" + range + "&interval=" + interval;
    var response = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
    var code = response.getResponseCode();
    var text = response.getContentText();

    if (code === 200) {
      var json = JSON.parse(text);
      if (json.chart && json.chart.result && json.chart.result.length > 0) {
        var result = json.chart.result[0];
        var closes = result.indicators.quote[0].close;
        var timestamps = result.timestamp;

        if (closes && timestamps) {
          for (var i = 0; i < closes.length; i++) {
            if (closes[i] !== null && closes[i] > 0 && timestamps[i]) {
              prices.push(Math.round(closes[i] * 100) / 100);

              var dateObj = new Date((timestamps[i] + 3 * 3600) * 1000);
              var dateStr = "";
              if (range === "1d" || range === "5d") {
                var hr = ("0" + dateObj.getUTCHours()).slice(-2);
                var min = ("0" + dateObj.getUTCMinutes()).slice(-2);
                dateStr = hr + ":" + min;
              } else {
                var day = ("0" + dateObj.getUTCDate()).slice(-2);
                var month = ("0" + (dateObj.getUTCMonth() + 1)).slice(-2);
                var year = dateObj.getUTCFullYear().toString().substring(2);
                dateStr = day + "." + month + "." + year;
              }
              dates.push(dateStr);
            }
          }
        }
      }
    }
  } catch (err) {
    prices = [];
    dates = [];
  }

  return ContentService.createTextOutput(JSON.stringify({
    "ticker": clean,
    "trend": prices,
    "dates": dates
  })).setMimeType(ContentService.MimeType.JSON);
}

function parseNum(rawVal) {
  if (rawVal === undefined || rawVal === null || rawVal === "") return 0;
  if (typeof rawVal === "number") return rawVal;
  var str = rawVal.toString().replace("₺", "").replace("$", "").replace(/\s/g, "");
  if (str.indexOf('.') !== -1 && str.indexOf(',') !== -1) str = str.replace(/\./g, "");
  str = str.replace(",", ".");
  var p = parseFloat(str);
  return isNaN(p) ? 0 : p;
}

function parsePct(rawVal) {
  if (rawVal === undefined || rawVal === null || rawVal === "") return 0;
  if (typeof rawVal === "number") return rawVal * 100;
  var str = rawVal.toString().replace("%", "").replace(/\s/g, "").replace(",", ".");
  var p = parseFloat(str);
  return isNaN(p) ? 0 : p;
}
