package com.example.bist

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YahooFinanceRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // BIST hisse senetleri (Sembol -> Şirket Adı Eşleştirmesi)
    val bistStocksMap = mapOf(
        "A1CAP" to "A1 Capital Yatirim Menkul Degerler AS",
        "A1YEN" to "A1 Yenilenebilir Enerji Uretim AS",
        "AAGYO" to "Agaoglu Avrasya Gayrmnkl Ytm Orlg AS",
        "ACSEL" to "Aciselsan Acipayam Seluloz Sny v Tcrt AS",
        "ADEL" to "Adel Kalemcilik Ticaret ve Sanayi AS",
        "ADESE" to "Adese Gayrimenkul Yatirim AS",
        "ADGYO" to "Adra Gayrimenkul Yatirim Ortakligi AS",
        "AEFES" to "Anadolu Efes Biracilik ve Malt SanayiiAS",
        "AFYON" to "Afyon Cimento Sanayi TAS",
        "AGESA" to "Agesa Hayat ve Emeklilik AS",
        "AGHOL" to "AG Anadolu Grubu Holding AS",
        "AGROT" to "Agrotech Yuksek Teknoloji ve Yatirim AS",
        "AGYO" to "Atakule Gayrimenkul Yatirim Ortakligi AS",
        "AHGAZ" to "Ahltc Dgl Gaz Dagitim Enerji ve Ytrm AS",
        "AHSGY" to "Ahes Gayrimenkul Yatirim Ortakligi AS",
        "AKBNK" to "Akbank TAS",
        "AKCNS" to "Akcansa Cimento Sanayi ve Ticaret AS",
        "AKENR" to "Akenerji Elektrik Uretim AS",
        "AKFGY" to "Akfen Gayrimenkul Yatirim Ortakligi AS",
        "AKFIS" to "Akfen Insaat Turizm ve Ticaret AS",
        "AKFYE" to "Akfen Yenilenebilir Enerji AS",
        "AKGRT" to "AK Sigorta AS",
        "AKHAN" to "Akhan Un Fabrikasi ve Tarim Urunleri Gida Sanayi Ticaret AS",
        "AKMGY" to "Akmerkez Gayrimenkul Yatirim Ort AS",
        "AKSA" to "Aksa Akrilik Kimya Sanayii AS",
        "AKSEN" to "Aksa Enerji Uretim AS",
        "AKSGY" to "Akis Gayrimenkul Yatirim Ortakligi AS",
        "AKSUE" to "Aksu Enerji ve Ticaret A.S.",
        "AKYHO" to "Akdeniz Yatirim Holding AS",
        "ALARK" to "Alarko Holding Inc",
        "ALBRK" to "Albaraka Turk Katilim Bankasi AS",
        "ALCAR" to "Alarko Carrier Sanayi ve Ticaret AS",
        "ALCTL" to "Alcatel Lucent Teletas Telekomunksyn AS",
        "ALFAS" to "Alfa Solar Enerji Sanayi ve Ticaret AS",
        "ALGYO" to "Alarko Gayrimenkul Yatirim Ortakligi AS",
        "ALKA" to "Alkim Kagit Sanayi ve Ticaret A.S.",
        "ALKIM" to "Alkim Alkali Kimya AS",
        "ALKLC" to "Altinkilic Gida ve Sut Sanayi Ticaret AS",
        "ALBTNH" to "Albotan Holding A.S.",
        "ALTINS1" to "Darphane Altin Sertifikasi",
        "ALTNY" to "Altinay Savunma Teknolojileri AS",
        "ALVES" to "Alves Kablo Sanayi ve Ticaret AS",
        "ANELE" to "Anel Elektrik Proje Taahhut ve Ticart AS",
        "ANGEN" to "Anatolia TaniveBytklj Unli Ar Ge SiveTiA",
        "ANHYT" to "Anadolu Hayat Emeklilik AS",
        "ANSGR" to "Anadolu Anonim Turk Sigorta Sti",
        "ARASE" to "Dogu Aras Enerji Yatirimlari AS",
        "ARCLK" to "Arcelik AS",
        "ARDYZ" to "Ard Grup Bilisim Teknolojileri AS",
        "ARENA" to "Arena Bilgisayar Sanayi ve Ticaret AS",
        "ARFYE" to "Arf Bio Yenilenebilir Enerji Uretim AS",
        "ARMGD" to "Armada Gida Ticaret Sanayi AS",
        "ARSAN" to "Arsan Holding AS",
        "ARTMS" to "Artemis Hali AS",
        "ARZUM" to "Arzum Elektrikli Ev Altri Sny Ve Tcrt AS",
        "ASELS" to "Aselsan Elektronik Sanayi ve Ticaret AS",
        "ASGYO" to "Asce Gayrimenkul Yatirim Ortakligi AS",
        "ASTOR" to "Astor Enerji AS",
        "ASUZU" to "Anadolu Isuzu Otomotiv Sanayii v Tcrt AS",
        "ATAGY" to "Ata Gayrimenkul Yatirim Ortakligi AS",
        "ATAKP" to "Atakey Patates Gida Sanayi ve Ticaret AS",
        "ATATP" to "Atp Yazilim ve Teknoloji AS",
        "ATATR" to "Ata Trzm Itmclk Tsmk Mcalk Kmlk Sy V D T",
        "ATEKS" to "Akin Tekstil AS",
        "ATLAS" to "Atlas Menkl Kymtlr Ytrm rtklg AS",
        "ATSYH" to "Atlantis Yatirim Holding AS",
        "AVGYO" to "Avrasya Gayrimenkul Yatirim Ortakligi AS",
        "AVHOL" to "Avrupa Yatirim Holding AS",
        "AVOD" to "AVOD Kurutulmus Gid ve Ta Uru Sa Tic AS",
        "AVPGY" to "Avrupakent Gayrimenkul Yatirim Ortklg AS",
        "AVTUR" to "Avrasya Petrol ve Turistik Tslr Ytmlr AS",
        "AYCES" to "Altin Yunus Cesme Turistik Tesisler AS",
        "AYDEM" to "Aydem Yenilenebilir Enerji AS",
        "AYEN" to "Ayen Enerji AS",
        "AYES" to "Ayes Akdeniz Yapi Elemanlari San",
        "AYGAZ" to "Aygaz AS",
        "AZTEK" to "Aztek Teknoloji Urunleri Ticaret AS",
        "BAGFS" to "Bagfas Bandirma Gubre Fabrikalari AS",
        "BAHKM" to "Bahadir Kimya Sanayi Ve Ticaret AS",
        "BAKAB" to "Bak Ambalaj Sanayi ve Ticaret AS",
        "BALAT" to "Balatacilar Balatacilik Sanayi v Tcrt AS",
        "BALSU" to "Balsu Gida Sanayi ve Ticaret AS",
        "BANVT" to "Banvit Bandirma Vitaminli Yem Sanayi AS",
        "BARMA" to "Barem Ambalaj Sanayi ve Ticaret AS",
        "BASCM" to "Bastas Baskent Cimento Sanayi ve Ticaret",
        "BASGZ" to "Baskent Dogalgaz Dagitim Gayr Yat OrtAS",
        "BAYRK" to "Bayrak Ebt Taban Sanayi Ve Ticaret AS",
        "BEGYO" to "Bati Ege Gayrimenkul Yatrm Ortklgi AS",
        "BERA" to "Bera Holding AS",
        "BESLR" to "Besler Gida ve Kimya Sanayi ve Ticart AS",
        "BESTE" to "Best Brands Grup Enerji Yatirim AS",
        "BETAE" to "Beta Enerji ve Teknoloji AS",
        "BEYAZ" to "Beyaz Filo Oto Kiralama AS",
        "BFREN" to "Bosch Fren Sistemleri Sanayi ve Ticrt AS",
        "BIENY" to "Bien Yapi Urunlri Snyi Turzm ve Tcrt AS",
        "BIGCH" to "Buyuk Sflr Gd Trzm Tkstl Dnsmnlk rgnzsyn",
        "BIGEN" to "Birlesim Grup Enerji Yatirimlari AS",
        "BIGTK" to "Big Medya Teknoloji AS",
        "BIMAS" to "BIM Birlesik Magazalar AS",
        "BINBN" to "Bin Ulasim ve Akilli Sehir Teknolojlr AS",
        "BINHO" to "1000 Yatirimlar Holding AS",
        "BIOEN" to "Biotrend Cevre ve Enerji Yatirimlari AS",
        "BIZIM" to "Bizim Toptan Satis Magazalari AS",
        "BJKAS" to "Besiktas Futbol Yatirimlar Sny v Tcrt AS",
        "BLCYT" to "Bilici Yatirim Sanayi ve Ticaret AS",
        "BLUME" to "Metemtur Yatirim Enerji Trzm ve Inst AS",
        "BMSCH" to "BMS Celik Hasir Sanayi ve Ticaret AS",
        "BMSTL" to "Bms Birlesik Metal Sanayi ve Ticaret AS",
        "BNTAS" to "Bantas Bandirma Ambalaj Sanayi ve Tic AS",
        "BOBET" to "Bogazici Beton Sanayi ve Ticaret AS",
        "BORLS" to "Borlease Otomotiv AS",
        "BORSK" to "Bor Seker AS",
        "BOSSA" to "Bossa Ticaret ve Sanayi Isletmeleri TAS",
        "BRISA" to "Brisa Bridgestone Snc Lstk Sny v Tcrt AS",
        "BRKO" to "Birko Birlesik Koyunlul Me Ti ve Sa AS",
        "BRKSN" to "Berkosan Yalitim ve Tec Mad Ur ve Tic AS",
        "BRKVY" to "Birikim Varlik Yonetim AS",
        "BRLSM" to "Birlesim Muk Ista Sga Hva Sy ve Tcrt AS",
        "BRMEN" to "Birlik Mensucat Tcrt v Sny sltms AS",
        "BRSAN" to "Borusan Birlesik Boru Fbrklr Sn V Tc AS",
        "BRYAT" to "Borusan Yatirim ve Pazarlama AS",
        "BSOKE" to "Batisoke Soke Cimento Sanayii TAS",
        "BTCIM" to "Baticim Bati Anadolu Cimento Sanayii AS",
        "BUCIM" to "Bursa Cimento Fabrikasi AS",
        "BULGS" to "Bulls Girisim Sermayesi Yatirm Ortklg AS",
        "BURCE" to "Burcelik Bursa Celik Dokum Sanayi AS",
        "BURVA" to "Burcelik Vana Sanayi ve Ticaret A.S.",
        "BVSAN" to "Bulbuloglu Vinc Sanayi ve Ticaret AS",
        "BYDNR" to "Baydoner Restoranlari AS",
        "CANTE" to "Can2 Termik AS",
        "CASA" to "Casa Emtia Ptrl Kmyv Ve Trvlr SnVeTcrtAS",
        "CATES" to "Cates Elektrik Uretim AS",
        "CCOLA" to "Coca-Cola Icecek AS",
        "CELHA" to "Celik Halat ve Tel Sanayii AS",
        "CEMAS" to "Cemas Dokum Sanayi AS",
        "CEMTS" to "Cemtas Celik Makina Sanayi ve Ticaret AS",
        "CEMZY" to "Cem Zeytin AS",
        "CEOEM" to "CEO Event Medya AS",
        "CGCAM" to "Cagdas Cam Sanayi ve Ticaret AS",
        "CIMSA" to "Cimsa Cimento Sanayi ve Ticaret AS",
        "CLEBI" to "Celebi Hava Servisi AS",
        "CMBTN" to "Cimbeton Hzrbtn v Prfbrk Yp lmnlr Sny v",
        "CMENT" to "Cimentas Izmir Cimento Fabrikasi TAS",
        "CONSE" to "Consus Enerji sltmclg v Hzmtlr AS",
        "COSMO" to "Cosmos Yatirim Holding AS",
        "CRDFA" to "Creditwest Faktoring AS",
        "CRFSA" to "CarrefourSA Carrefour Sabnc Tic Mei AS",
        "CUSAN" to "Cuhadaroglu Metal Sanayi ve Pazarlama AS",
        "CVKMD" to "Cvk Maden Isletmeleri Snyi ve Ticaret AS",
        "CWENE" to "Cw Enerji Muhendislik Ticaret ve Sany AS",
        "DAGI" to "Dagi Giyim Sanayi ve Ticaret AS",
        "DAPGM" to "Dap Gayrimenkul Gelistirme AS",
        "DARDL" to "Dardanel Onentas Gida Sanayi AS",
        "DCTTR" to "Dct Trading Dis Ticaret AS",
        "DENGE" to "Denge Yatirim Holding AS",
        "DERHL" to "Derluks Yatirim Holding AS",
        "DERIM" to "Derimod Konfeksyn Aykkb Dr Sny v Tcrt AS",
        "DESA" to "Desa Deri Sanayi ve Ticaret A.S.",
        "DESPC" to "Despec Bilgisayar Pazarlama v Ticaret AS",
        "DEVA" to "Deva Holding AS",
        "DGATE" to "Datagate Bilgisayar Malzemeleri Ticrt AS",
        "DGGYO" to "Dogus Gayrimenkul Yatirim Ortakligi AS",
        "DGNMO" to "Doganlar Mobilya Grub Imlt Sny v Tcrt AS",
        "DIRIT" to "Diriteks Dirilis Tekstil Sany ve Tcrt AS",
        "DITAS" to "Ditas Dogan Yedek Parc Imalt ve Teknk AS",
        "DMRGD" to "Dmr Unlu Mlr Urtm Gda Tptn Prkd Ihrct AS",
        "DMSAS" to "Demisas Dokum Emaye Mamulleri Sanayi AS",
        "DNISI" to "Dinmk Isi Mkn Yltm Mlzmlr Sny ve Tcrt AS",
        "DOAS" to "Dogus Otomotiv Servis ve Ticaret AS",
        "DOCO" to "DO & CO AG",
        "DOFER" to "Dofer Yapi Malzemelri Sanyi ve Ticart AS",
        "DOFRB" to "Dof Robotik Sanayi AS",
        "DOGUB" to "Dogusan Boru Sanayi ve Ticaret AS",
        "DOHOL" to "Dogan Sirketler Grubu Holding AS",
        "DOKTA" to "Doktas Dokumculuk Ticaret ve Sanayi AS",
        "DSTKF" to "Destek Finans Faktoring AS",
        "DUNYH" to "Dunya Holding AS",
        "DURDO" to "Duran-Dogan Basim ve Ambalaj Sanayi AS",
        "DURKN" to "Durukan Sekerleme Sanayi ve Ticaret AS",
        "DYOBY" to "Dyo Boya Fabrikalari Sanayi Ve Ticart AS",
        "DZGYO" to "Deniz Gayrimenkul Yatirim Ortakligi AS",
        "EBEBK" to "Ebebek Magazacilik AS",
        "ECILC" to "EIS czcbs lc Sn v Fnnsl Ytrmlr Sny v Tcr",
        "ECOGR" to "Ecogreen Enerji Holding AS",
        "ECZYT" to "Eczacibasi Yatirim Holding Ortakligi AS",
        "EDATA" to "E-Data Teknoloji Pazarlama AS",
        "EDIP" to "Edip Gayrimenkul Yatirm Sany ve Ticrt AS",
        "EFOR" to "Efor Yatirim Sanayi Ticaret AS",
        "EGEEN" to "Ege Endustri ve Ticaret AS",
        "EGEGY" to "Egeyapi Avrupa Gayrimenkl Ytrm Ortklg AS",
        "EGEPO" to "Nasmed Ozel Saglik Hizmetleri Ticaret AS",
        "EGGUB" to "Ege Gubre Sanayii AS",
        "EGPRO" to "Ege Profil Ticaret ve Sanayi AS",
        "EGSER" to "Ege Seramik Sanayi ve Ticaret AS",
        "EKDMR" to "Ekinciler Demir ve Celik Sanayi AS",
        "EKGYO" to "Emlak Konut Gayrimenkul Yatirim Ortak AS",
        "EKIM" to "Ekim Turizm Ticaret ve Sanayi AS",
        "EKIZ" to "Ekiz Kimya Sanayi ve Ticaret AS",
        "EKOS" to "Ekos Teknoloji ve Elektrik AS",
        "EKSUN" to "Eksun Gida Tarim Sanayi ve Ticaret AS",
        "ELITE" to "Elite Naturel Orgnk Gida Snyi ve Tcrt AS",
        "EMKEL" to "EMEK Elektrik Endustrisi AS",
        "EMNIS" to "Eminis Ambalaj Sanayi ve Ticaret AS",
        "EMPAE" to "Empa Elektronik Sanayi ve Ticaret AS",
        "ENDAE" to "Enda Enerji Holding AS",
        "ENERY" to "Enerya Enerji AS",
        "ENJSA" to "Enerjisa Enerji AS",
        "ENKAI" to "ENKA Insaat ve Sanayi AS",
        "ENPRA" to "Enpara Bank AS",
        "ENSRI" to "Ensari Sinai Yatirimlar AS",
        "ENTRA" to "Ic Enterra Yenilenebilir Enerji AS",
        "EPLAS" to "Egeplast Ege Plastik Ticaret ve Sanyi AS",
        "ERBOS" to "Erbosan Erciyas Boru Sanayi ve Ticart AS",
        "ERCB" to "Erciyas Celik Boru Sanayi AS",
        "EREGL" to "Eregli Demir ve Celik Fabrikalari TAS",
        "ERSU" to "Ersu Meyve ve Gida Sanayi AS",
        "ESCAR" to "Escar Filo Kiralama Hizmetleri AS",
        "ESCOM" to "Escort Teknoloji Yatirim AS",
        "ESEN" to "Esenboga Elektrik Uretim AS",
        "ETILR" to "Etiler Gd ve Ticr Ytrmlr Sany ve Tcrt AS",
        "ETYAT" to "Euro Trend Yatirim Ortakligi AS",
        "EUHOL" to "Euro Yatirim Holding AS",
        "EUKYO" to "Euro Kapital Yatirim Ortakligi AS",
        "EUPWR" to "Eurpwr Enrj v Otmsyn Tknljlr Sny Tcrt AS",
        "EUREN" to "Europen Endustri Insat Sanyi ve Ticrt AS",
        "EUYO" to "Euro Menkul Kiymet Yatirim Ortakligi AS",
        "EYGYO" to "Eyg Gayrimenkul Yatirim Ortakligi AS",
        "FADE" to "Fade Gida Yatirim Sanayi Ticaret AS",
        "FENER" to "Fenerbahce Futbol AS",
        "FLAP" to "Flap Kongr Tplnt Hzmtlr Otmtv ve Trzm AS",
        "FMIZP" to "Federl Mgl Izmt Pstn ve Pim Urtm Tslr AS",
        "FONET" to "Fonet Bilgi Teknolojileri AS",
        "FORMT" to "Formet Metal ve Cam Sanayi AS",
        "FORTE" to "Forte Bilgi Iltsm Tknljlr ve Svnm Sny AS",
        "FRIGO" to "Frigo-Pak Gida Maddelri Snyi ve Tcart AS",
        "FRMPL" to "Formul Plastik ve Metal Sanayi AS",
        "FROTO" to "Ford Otomotiv Sanayi AS",
        "FZLGY" to "Fuzul Gayrimenkul Yatirim Ortakligi AS",
        "GARAN" to "Turkiye Garanti Bankasi AS",
        "GARFA" to "Garanti Faktoring AS",
        "GATEG" to "Gate Grp Tknlj Mdy ve Sbr Gnlk Hzmtlr AS",
        "GEDIK" to "Gedik Yatirim Menkul Degerler AS",
        "GEDZA" to "Gediz Ambalaj Sanayi ve Ticaret AS",
        "GENIL" to "Gen Ilac ve Saglik Urnlr Snyi ve Tcrt AS",
        "GENKM" to "Gentas Kimya Sanayi ve Ticaret Pzrlm",
        "GENTS" to "Gentas Dekoratif Yuzeyler Sny ve Tcrt AS",
        "GEREL" to "Gersan Elektrik Ticaret ve Sanayi AS",
        "GESAN" to "Girisim Elektrik Sanayi Thht ve Tcrt AS",
        "GIPTA" to "Gipta Ofs Krtsy V Pryn Urnr Imlt Sny AS",
        "GLBMD" to "Global Menkul Degerler AS",
        "GLCVY" to "Gelecek Varlik Yonetimi AS",
        "GLRMK" to "Gulermak Agir Sanayi Insaat v Taahhut AS",
        "GLRYH" to "Guler Yatirim Holding AS",
        "GLYHO" to "Global Yatirim Holding AS",
        "GMTAS" to "Gimat Magazacilik Sanayi ve Ticaret AS",
        "GOKNR" to "Goknur Gida Mddlr nrj mlt thlt hrct Tcrt",
        "GOLDA" to "Golda Gida Sanayi Ve Ticaret AS",
        "GOLTS" to "Goltas Goller Blgs Cmnt Sny v Tcrt",
        "GOODY" to "Goodyear Lastikleri TAS",
        "GOZDE" to "Gozde Girisim Sermayesi Yatirm Ortklg AS",
        "GRNYO" to "Garanti Yatirim Ortakligi AS",
        "GRSEL" to "Gur-Sel Turizm Tasimacilk v Srvs Tcrt AS",
        "GRTHO" to "Grainturk Holding AS",
        "GSDDE" to "GSD Denizcilik Gyrmnk Inst Sny v Tcrt AS",
        "GSDHO" to "GSD Holding AS",
        "GSRAY" to "Galatasaray Sportif Sinai ve Ticr Yat AS",
        "GUBRF" to "Gubre Fabrikalari TAS",
        "GUNDG" to "Gundogdu Gda Sut Unlr Sny Ve Dis Tcrt AS",
        "GWIND" to "Galata Wind Enerji Anonim Sirket",
        "GZNMI" to "Gezinomi Seyahat Turizm Ticaret AS",
        "HALKB" to "Turkiye Halk Bankasi AS",
        "HATEK" to "Hateks Hatay Tekstil Isletmeleri AS",
        "HATSN" to "Hat-San Gm Ins Bk Or Dnz Nkt Sn v Tct AS",
        "HDFGS" to "Hedef Girisim Sermayesi Yatirim Ortk AS",
        "HEDEF" to "Hedef Holding AS",
        "HEKTS" to "Hektas Ticaret TAS",
        "HKTM" to "Hidropar Hrkt Kntrl Tknljlr Mrkz Sny v T",
        "HLGYO" to "Halk Gayrimenkul Yatirim Ortakligi AS",
        "HOROZ" to "Horoz Lojistik Kargo Hizmetler v Tcrt AS",
        "HRKET" to "Hareket Proje Tasimacilg v Yk Mhndslg AS",
        "HTTBT" to "Hitit Bilgisayar Hizmetleri AS",
        "HUBVC" to "Hub Girisim Sermayesi Yatirim Ortklg AS",
        "HUNER" to "Hun Yenilenebilir Enerji Uretim AS",
        "HURGZ" to "Hurriyet Gazetecilik ve Matbaacilik AS",
        "ICBCT" to "ICBC Turkey Bank AS",
        "ICUGS" to "Icu Girisim Sermayesi Yatirim Ortaklg AS",
        "IDGYO" to "Idealist Gayrimenkul Yatirim Ortakli AS",
        "IEYHO" to "Isiklar Enerji ve Yapi Holding AS",
        "IHAAS" to "Ihlas Haber Ajansi AS",
        "IHEVA" to "Ihlas Ev ltlr mlt Sny v Tcrt AS",
        "IHGZT" to "Ihlas Gazetecilik AS",
        "IHLAS" to "Ihlas Holding AS",
        "IHLGM" to "Ihlas Gayrimenkul Proje Gelist ve Tic AS",
        "IHYAY" to "Ihlas Yayin Holding AS",
        "IMASM" to "Imas Makina Sanayi AS",
        "INDES" to "Indeks Blgsyr Sstmlr Mhndslk Sny v Tcrt",
        "INFO" to "Info Yatirim Menkul Degerler AS",
        "INGRM" to "Ingram Micro Bilisim Sistemleri AS",
        "INTEK" to "Innosa Teknoloji AS",
        "INTEM" to "Intema Insaat ve Tesisat Malzemeleri",
        "INVEO" to "Inveo Yatirim Holding AS",
        "INVES" to "Investco Holding AS",
        "ISATR" to "Turkiye Is Bankasi A",
        "ISBIR" to "Isbir Holding Inc",
        "ISBTR" to "Turkiye Is Bankasi B",
        "ISCTR" to "Turkiye Is Bankasi C",
        "ISDMR" to "Iskenderun Demir ve Celik AS",
        "ISFIN" to "Is Finansal Kiralama AS",
        "ISGSY" to "Is Girisim Sermayesi Yatirim Ortaklig AS",
        "ISGYO" to "Is Gayrimenkul Yatirim Ortakligi AS",
        "ISKPL" to "Isik Plastik Sanayi v Ds Tcrt Pzrlm AS",
        "ISKUR" to "Turkiye Is Bankasi AS",
        "ISMEN" to "Is Yatirim Menkul Degerler AS",
        "ISSEN" to "Isbir Sentetik Dokuma Sanayi AS",
        "ISVEA" to "Isvea Seramik ve Banyo Urunleri Sanayi AS",
        "ISYAT" to "Is Yatirim Ortakligi AS",
        "IZENR" to "Izdemir Enerji Elektrik Uretim AS",
        "IZFAS" to "Izmir Firca Sanayi ve Ticaret AS",
        "IZINV" to "Iz Hayvancilik Tarim ve Gida Sny Tcrt AS",
        "IZMDC" to "Izmir Demir Celik Sanayi AS",
        "JANTS" to "Jantsa Jant Sanayi ve Ticaret AS",
        "KAPLM" to "Kaplamin Ambalaj Sanayi ve Ticaret AS",
        "KAREL" to "Karel Elektronik Sanayi ve Ticaret A.S.",
        "KARSN" to "Karsan Otomotiv Sanayii ve Ticaret AS",
        "KARTN" to "Kartonsan Karton Sanayi ve Ticaret AS",
        "KATMR" to "Katmerciler Arac Ustu Ekipm San ve Ti AS",
        "KAYSE" to "Kayseri Seker Fabrikasi AS",
        "KBORU" to "Kuzey Boru AS",
        "KCAER" to "Kocaer Celik Sanayi ve Ticaret AS",
        "KCHOL" to "Koc Holding AS",
        "KENT" to "Kent Gida Maddeleri Sanayi ve Ticaret AS",
        "KERVN" to "Kervansaray Yatirim Holding AS",
        "KFEIN" to "Kafein Yazilim Hizmetleri Ticaret AS",
        "KGYO" to "Koray Gayrimenkul Yatirim Ortakligi AS",
        "KIMMR" to "Ersan Alisvrs Hzmtlr ve Gda Snay Tcrt AS",
        "KLGYO" to "Kiler Gayrimenkul Yatirim Ortakligi A S",
        "KLKIM" to "Kalekim Kimyevi Maddeler Sani ve Tit AS",
        "KLMSN" to "Klimasan Klima Sanayi ve Ticaret AS",
        "KLNMA" to "Turkiye Kalkinma ve Yatirim Bankasi AS",
        "KLRHO" to "Kiler Holding AS",
        "KLSER" to "Kaleseramik Canakkale Kalbdr Srmk Sny AS",
        "KLSYN" to "Koleksiyon Mobilya Sanayi AS",
        "KLYPV" to "Kalyon Gunes Teknolojileri Uretim AS",
        "KMPUR" to "Kimteks Poliuretan Sanayi Ve Ticaret AS",
        "KNFRT" to "Konfrut Tarim AS",
        "KOCMT" to "Koc Metalurji AS",
        "KONKA" to "Konya Kagit Sanayi ve Ticaret AS",
        "KONTR" to "Kontrolmatik Teknoloji Enji ve Muhnlk AS",
        "KONYA" to "Konya Cimento Sanayii AS",
        "KOPOL" to "Koza Polyester Sanayi ve Ticaret AS",
        "KORDS" to "Kordsa Teknik Tekstil AS",
        "KOTON" to "Koton Magazacilik Tekstil Sny ve Tcrt AS",
        "KRDMA" to "Kardemir Karabuk Demir Celik Sanayi A",
        "KRDMB" to "Kardemir Karabuk Demir Celik Sanayi B",
        "KRDMD" to "Kardemir Karabuk Demir Celik Sanayi D",
        "KRGYO" to "Korfez Gayrimenkul Yatirim Ortakligi AS",
        "KRONT" to "Kron Teknoloji AS",
        "KRPLS" to "Koroplst Tmzk Amblj Unlr Sny v Ds Trt AS",
        "KRSTL" to "Kristal Kola ve Mesrubat Sany Ticaret AS",
        "KRTEK" to "Karsu Tekstil Sanayi ve Ticaret AS",
        "KRVGD" to "Kervan Gida Sanayi ve Ticaret AS",
        "KSTUR" to "Kustur Kusadasi Turizm Endustri",
        "KTLEV" to "Katilimevim Tasarruf Finansman AS",
        "KTSKR" to "Kutahya Seker Fabrikasi AS",
        "KUTPO" to "Kutahya Porselen Sanayi AS",
        "KUVVA" to "Kuvva Gida Ticaret ve Sanayi Yatirmlr AS",
        "KUYAS" to "Kuyas Yatirim AS",
        "KZBGY" to "Kizilbuk Gayrimenkul Yatirim Ort AS",
        "KZGYO" to "Kuzugrup Gayrimnkul Yatirim Ortakligi AS",
        "LIDER" to "LDR Turizm AS",
        "LIDFA" to "Lider Faktoring AS",
        "LILAK" to "Lila Kagit Sanayi ve Ticaret AS",
        "LINK" to "Link Blgsyr Sstmlr Yzlm v Dnnm Sny v Tcr",
        "LKMNH" to "Lokman Hekim Sa Tu Eg Hi AS",
        "LMKDC" to "Limak Dogu Anadolu Cimento Sny AS",
        "LOGO" to "Logo Yazilim Sanayi ve Ticaret AS",
        "LRSHO" to "Loras Holding AS",
        "LUKSK" to "Luks Kadife Ticaret ve Sanayi AS",
        "LXGYO" to "Luxera Gayrimenkul Yatirim Ortakligi AS",
        "LYDHO" to "Lydia Holding AS",
        "LYDYE" to "Lydia Yesil Enerji Kaynaklari AS",
        "MAALT" to "Marmaris Altinyunus Turistik Tesisler AS",
        "MACKO" to "Mackolik Internet Hizmetleri Ticaret AS",
        "MAGEN" to "Margun Enerji Uretim Sanayi & Ticaret AS",
        "MAKIM" to "Makim Makina Teknolojilri Sny ve Tcrt AS",
        "MAKTK" to "Makina Takim Endustrisi AS",
        "MANAS" to "Manas Enerji Yonetimi Sanayi ve Ticrt AS",
        "MARBL" to "Tureks Turunc Madnclk Ic ve Dis Ticrt AS",
        "MARKA" to "Marka Yatirim Holding AS",
        "MARMR" to "Marmara Holding AS",
        "MARTI" to "Marti Otel Isletmeleri AS",
        "MAVI" to "Mavi Giyim Sanayi ve Ticaret AS",
        "MCARD" to "Metropal Kurumsal Hizmetler AS",
        "MEDTR" to "Meditera Tibbi Mlzm Sny Tcrt AS",
        "MEGAP" to "Mega Polietilen Kopuk Sanayi ve Ticrt AS",
        "MEGMT" to "Mega Metal Sanayi ve Ticaret AS",
        "MEKAG" to "Meka Global Makine Imalat Sany v Tcrt AS",
        "MEPET" to "Break Mola Turizm Yatirimlar AS",
        "MERCN" to "Mercan Kimya Sanayi ve Ticaret AS",
        "MERIT" to "Merit Turizm Yatirim ve Isletme AS",
        "MERKO" to "Merko Gida Sanayi ve Ticaret AS",
        "METRO" to "Metro Ticari ve Mali Yatirimlar Hldg AS",
        "MEYSU" to "Meysu Gida Sanayi ve Ticaret AS",
        "MGROS" to "Migros Ticaret AS",
        "MHRGY" to "Mhr Gayrimenkul Yatirim Ortakligi AS",
        "MIATK" to "Mia Teknoloji AS",
        "MMCAS" to "MMC Sanayi ve Ticari Yatirimlar AS",
        "MNDRS" to "Menderes Tekstil Sanayi ve Ticaret AS",
        "MNDTR" to "Mondi Turkey Olkl Mkav Kgt ve Amj Sny AS",
        "MOBTL" to "Mobiltel Iletisim Hizmetlr Sny V Tcrt AS",
        "MOGAN" to "Mogan Enerji Yatirim Holding AS",
        "MOPAS" to "Mopas Marketcilik Gida Sanayi ve Tcrt AS",
        "MPARK" to "MLP Saglik Hizmetleri AS",
        "MRGYO" to "Marti Gayrimenkul Yatirim Ortakligi AS",
        "MRSHL" to "Marshall Boya ve Vernik Sanayi AS",
        "MSGYO" to "Mistral Gayrimenkul Yatirim Ortakligi AS",
        "MTRKS" to "Matriks Finansal Teknolojiler AS",
        "MTRYO" to "Metro Yatirim Ortakligi AS",
        "MZHLD" to "Mazhar Zorlu Holding AS",
        "NATEN" to "Naturel Yenilenebilir Enerji Ticaret AS",
        "NETAS" to "Netas Telekomunikasyon AS",
        "NETCD" to "Netcad Yazilim AS",
        "NIBAS" to "Nigbas Nigde Beton Sanayii ve Ticaret AS",
        "NTGAZ" to "Naturelgaz Sanayi ve Ticaret AS",
        "NTHOL" to "Net Holding AS",
        "NUGYO" to "Nurol Gayrimenkul Yatirim Ortakligi AS",
        "NUHCM" to "Nuh Cimento Sanayi AS",
        "OBAMS" to "Oba Makarnacilik Sanayi ve Ticaret AS",
        "OBASE" to "Obase Bilgisayr ve Dnsmlk Hzmtlr Tcrt AS",
        "ODAS" to "ODAS Elektrik Uretim Sanayi Ticaret AS",
        "ODINE" to "Odine Solutins Teknolji Ticrt ve Snyi AS",
        "OFSYM" to "Ofis Yem Gida Sanayi Ticaret AS",
        "ONCSM" to "Oncosem Onkolojik Sistemlr Sny v Tcrt AS",
        "ONRYT" to "Onur Yuksek Teknoloji AS",
        "ORCAY" to "Orcay Ortakoy Cay Sanayi ve Ticaret AS",
        "ORGE" to "Orge Enerji Elektrik Taahhut AS",
        "ORMA" to "Orma Orman Mahsulleri Intr Sny v Tcrt AS",
        "ORZAX" to "Orzaks Ilac Ve Kimya Sanayi Ticaret AS",
        "OSMEN" to "Osmanli Yatirim Menkul Degerler AS",
        "OSTIM" to "Ostim Endustriyel Yatirimlar Isletme AS",
        "OTKAR" to "Otokar Otomotiv ve Savunma Sanayi AS",
        "OTTO" to "Otto Holding AS",
        "OYAKC" to "Oyak Cimento Fabrikalari AS",
        "OYAYO" to "Oyak Yatirim Ortakligi A.S.",
        "OYLUM" to "Oylum Sinai Yatirimlar AS",
        "OYYAT" to "Oyak Yatirim Menkul Degerler AS",
        "OZATD" to "Ozata Denizcilik Sanayi ve Ticaret AS",
        "OZGYO" to "Ozderici Gayrimenkul Yatirim Ortklg AS",
        "OZKGY" to "Ozak Gayrimenkul Yatirim Ortakligi AS",
        "OZRDN" to "Özerden Ambalaj Sanayi A.Ş.",
        "OZSUB" to "Ozsu Balik Uretim AS",
        "OZYSR" to "Ozyasar Tel ve Galvanizleme Sanayi AS",
        "PAGYO" to "Panora Gayrimenkul Yatirim Ortakligi AS",
        "PAHOL" to "Pasifik Holding AS",
        "PAMEL" to "Pamel Yenilenebilir Elektrik Uretim AS",
        "PAPIL" to "Papilon Svnm-Gvnlk Sstmlr AS",
        "PARSN" to "Parsan Makina Parcalari Sanayii AS",
        "PASEU" to "Pasifik Eurasia Lojistik Dis Ticaret AS",
        "PATEK" to "Pasifik Teknoloji AS",
        "PCILT" to "PC Iletisim Ve Medya Hizmtlr AS",
        "PEKGY" to "Peker Gayrimenkul Yatirim Ortakligi AS",
        "PENGD" to "Penguen Gida Sanayi AS",
        "PENTA" to "Penta Teknoloji Urunleri Dagitim AS",
        "PETKM" to "Petkim Petrokimya Holding AS",
        "PETUN" to "Pinar Entegre Et ve Un Sanayii AS",
        "PGSUS" to "Pegasus Hava Tasimaciligi AS",
        "PINSU" to "Pinar Su ve Icecek Sanayi ve Ticaret AS",
        "PKART" to "Plastikkart Akilli Krt AS",
        "PKENT" to "Petrokent Turizm AS",
        "PLTUR" to "Platform Turizm Tsmclk AS",
        "PNLSN" to "Panelsan Cati Cephe Sistemlr AS",
        "PNSUT" to "Pinar Sut Mamulleri Sanayii AS",
        "POLHO" to "Polisan Holding AS",
        "POLTK" to "Politeknik Metal Sanayi ve Ticaret AS",
        "PRDGS" to "Pardus Girisim Sermayesi Ortklg AS",
        "PRKAB" to "Turk Prysmian Kablo ve Sistemleri AS",
        "PRKME" to "Park Elektrik Uretim AS",
        "PRZMA" to "Prizma Prs Mtbaclk AS",
        "PSDTC" to "Pergamon Status Dis Ticaret AS",
        "PSGYO" to "Pasifik Gayrimenkul Yatirim Ortakligi AS",
        "QNBFK" to "Qnb Finansal Kiralama AS",
        "QNBTR" to "Qnb Bank AS",
        "QUAGR" to "Qua Granite Hayl Yp AS",
        "RALYH" to "Ral Yatirim Holding AS",
        "RAYSG" to "Ray Sigorta AS",
        "REEDR" to "Reeder Teknoloji Sanayi Ve Ticaret AS",
        "RGYAS" to "Ronesans Gayrimenkul Yatirim AS",
        "RNPOL" to "Rainbow Polikarbonat Sanayi Ticaret AS",
        "RODRG" to "Rodrigo Tekstil Sanayi ve Ticaret AS",
        "RTALB" to "RTA Lbrtvrlr Byljk rnlr AS",
        "RUBNS" to "Rubenis Tekstil Sanayi Ticaret AS",
        "RUZYE" to "Ruzy Madnclk v Enrj AS",
        "RYGYO" to "Reysas Gayrimenkul Yatirim Ortakligi AS",
        "RYSAS" to "Reysas Tasimacilik ve Lojistik AS",
        "SAFKR" to "Safkar g Sgtmclk Klm AS",
        "SAHOL" to "Haci Omer Sabanci Holding AS",
        "SAMAT" to "Saray Matbaacilik KK Ticaret AS",
        "SANEL" to "SAN-EL Muhendislik Elektrik San",
        "SANFM" to "Sanifom Endstr Ve Tktm AS",
        "SANKO" to "Sanko Pazarlama Ithalat Ihracat AS",
        "SARAE" to "Sa-Ra Enerji Insaat Ticaret ve Sanayi AS",
        "SARKY" to "Sarkuysan Elektrltk Bakr Sny AS",
        "SASA" to "SASA Polyester Sanayi AS",
        "SAYAS" to "Say Yenileneblr Enrji AS",
        "SDTTR" to "Sdt Uzay ve Savunma Teknolojileri AS",
        "SEGMN" to "Segmen Kardesler Gid Urt AS",
        "SEGYO" to "Seker Gayrimenkul Yatirim Ortakligi AS",
        "SEKFK" to "Seker Finansal Kiralama AS",
        "SEKUR" to "Sekuro Plastik Ambalaj Sanayi AS",
        "SELEC" to "Selcuk Ecza Deposu Ticaret AS",
        "SELVA" to "Selva Gida Sanayi AS",
        "SERNT" to "Seranit Granit Seramik AS",
        "SEYKM" to "Seyitler Kimya Sanayi AS",
        "SILVR" to "Silverline Endustri ve Ticaret AS",
        "SISE" to "Turkiye Sise ve Cam Fabrikalari AS",
        "SKBNK" to "Sekerbank TAS",
        "SKTAS" to "Soktas Tekstil Sanayi ve Ticaret AS",
        "SKYLP" to "Skyalp Finansal Teknlojlr AS",
        "SKYMD" to "Seker Yatirim Menkul Degerler AS",
        "SMART" to "Smartiks Yazilim AS",
        "SMRTG" to "Smrt Gns Enrjs AS",
        "SMRVA" to "Sumer Varlik Yonetim AS",
        "SNGYO" to "Sinpas Gayrimenkul Yatirim Ortakligi AS",
        "SNICA" to "Sanica Isi Sanayi AS",
        "SNPAM" to "Sonmez Pamuklu Sanayii AS",
        "SODSN" to "Sodas Sodyum Sanayii AS",
        "SOHOE" to "Soho Giyim Ve Enerji AS",
        "SOKE" to "Soke Degirmencilik Sanayi AS",
        "SOKM" to "Sok Marketler Ticaret AS",
        "SONME" to "Sonmez Filament Sentetik AS",
        "SRVGY" to "Servet Gayrimenkul Yatirim Ortakligi AS",
        "SSAAT" to "Saat ve Saat Sanayi ve Ticaret AS",
        "SUMAS" to "Sumas Suni Tahta ve Mobilya Sanayi",
        "SUNTK" to "Sun Tekstil Sanayi ve Ticaret AS",
        "SURGY" to "Sur Tatil Evleri Gayrmnkl AS",
        "SUWEN" to "Suwen Tekstil Sanayi Pazarlama AS",
        "SVGYO" to "Savur Gayrimenkul Yatirim Ortakligi AS",
        "TABGD" to "Tab Gida Sanayi ve Ticaret AS",
        "TARKM" to "Tarkim Bitki Koruma Sanayi AS",
        "TATEN" to "Tatlipinar Enerji Uretim AS",
        "TATGD" to "Tat Gida Sanayi AS",
        "TAVHL" to "TAV Havalimanlari Holding AS",
        "TBORG" to "Turk Tuborg Bira ve Malt Sanayii AS",
        "TCELL" to "Turkcell Iletisim Hizmetleri AS",
        "TCKRC" to "Kirac Glz Tkn Mt AS",
        "TDGYO" to "Trend Gayrimenkul Yatirim Ortakligi AS",
        "TEHOL" to "Tera Yatirim Teknoloji Holding AS",
        "TEKTU" to "Tek Art Insaat Ticaret Tur San AS",
        "TERA" to "Tera Yatirim Menkul Degerler AS",
        "TEZOL" to "Europap Tezol Kagit Sanayi AS",
        "TGSAS" to "TGS Dis Ticaret AS",
        "THYAO" to "Turk Hava Yollari AO",
        "TKFEN" to "Tekfen Holding AS",
        "TKNSA" to "Teknosa Ic ve Dis Ticaret AS",
        "TLMAN" to "Trabzon Liman Isletmeciligi AS",
        "TMPOL" to "Temapol Polimer Plstk AS",
        "TMSN" to "Tumosan Motor ve Traktor Sanayi AS",
        "TNZTP" to "Tapdi ksjn zl Sglk AS",
        "TOASO" to "Tofas Turk Otomobil Fabrikasi AS",
        "TRALT" to "Turk Altin Isletmeleri AS",
        "TRCAS" to "Turcas Holding AS",
        "TRENJ" to "TR Dogal Enerji Kynklr AS",
        "TRGYO" to "Torunlar Gayrimenkul Yatirim Ortaklig AS",
        "TRHOL" to "Tera Finansal Yatirimlar Holding AS",
        "TRILC" to "Turk Ilac Ve Serum Sanayi AS",
        "TRMET" to "TR Anadolu Metal AS",
        "TSGYO" to "TSKB Gayrimenkul Yatirim Ortakligi AS",
        "TSKB" to "Industrial Development Bnk of Trky",
        "TSPOR" to "Trabzonspor Sprtf Ytrm AS",
        "TTKOM" to "Turk Telekomunikasyon AS",
        "TTRAK" to "Turk Traktor ve Ziraat Makineleri AS",
        "TUCLK" to "Tugcelik lmnym AS",
        "TUKAS" to "Tukas Gida Sanayi ve Ticaret AS",
        "TUPRS" to "Turkiye Petrol Rafinerileri AS",
        "TUREX" to "Tureks Turizm Tasimacilik AS",
        "TURGG" to "Turker Proje Gayrimnkl AS",
        "TURSG" to "Turkiye Sigorta AS",
        "UCAYM" to "Ucay Muhndslk Enrj AS",
        "UFUK" to "Ufuk Yatirim Yonetim Ve Gayrimenkul AS",
        "ULAS" to "Ulaslar Trzm Enrj AS",
        "ULKER" to "Ulker Biskuvi Sanayi AS",
        "ULUFA" to "Ulusal Faktoring AS",
        "ULUSE" to "Ulusoy Elektrik Imalat AS",
        "ULUUN" to "Ulusoy Un Sanayi Ve Ticaret AS",
        "UMPAS" to "Umpas Holding",
        "UNLU" to "Unlu Yatirim Holding AS",
        "USAK" to "Usak Seramik Sanayi AS",
        "VAKBN" to "Turkiye Vakiflar Bankasi TAO",
        "VAKFA" to "Vakif Faktoring AS",
        "VAKFN" to "Vakif Finansal Kiralama AS",
        "VAKKO" to "Vakko Tekstil AS",
        "VANGD" to "Vanet Gida Sanayi Ic ve Dis Ticaret AS",
        "VBTYZ" to "VBT Yazilim AS",
        "VERTU" to "Verusaturk Girisim Ortkg AS",
        "VERUS" to "Verusa Holding AS",
        "VESBE" to "Vestel Beyaz Esya Sanayi AS",
        "VESTL" to "Vestel Elektronik Sanayi AS",
        "VKFYO" to "Vakif Menkul Kiymet Yatirim Ortakligi AS",
        "VKGYO" to "Vakif Gayrimenkul Yatirim Ortakligi AS",
        "VKING" to "Viking Kagit ve Seluloz AS",
        "VRGYO" to "Vera Konsept Gayrmnkul Ortklgi AS",
        "VSNMD" to "Visne Madencilik Uretim AS",
        "YAPRK" to "Yaprak Sut ve Besi Cftlklr AS",
        "YATAS" to "Yatas Yatak ve Yorgan Sanayi AS",
        "YAYLA" to "Yayla Enrji Urtm AS",
        "YBTAS" to "Yibts Yzgt Isc Brlg AS",
        "YEOTK" to "Yeo Teknoloji Enerji AS",
        "YESIL" to "Yesil Yatirim Holding AS",
        "YGGYO" to "Yeni Gimat Gayrimenkul Ortakg AS",
        "YIGIT" to "Yigit Aku Mlzmlr AS",
        "YKBNK" to "Yapi ve Kredi Bankasi AS",
        "YKSLN" to "Yukselen Celik AS",
        "YONGA" to "Yonga Mobilya Sanayi AS",
        "YUNSA" to "Yunsa Yunlu Sanayi AS",
        "YYAPI" to "Yesil Yapi Endustrisi AS",
        "YYLGD" to "Yayla Agro Gida Sanayi AS",
        "ZEDUR" to "Zedur Enerji Elektrik Uretim AS",
        "ZERGY" to "Zeray Gayrimenkul Ortakligi AS",
        "ZGYO" to "Z Gayrimenkul Ortakligi AS",
        "ZOREN" to "Zorlu Enerji Elektrik Uretim AS",
        "ZRGYO" to "Ziraat Gayrimenkul Ortakligi AS"
    )

    // Makro Göstergeler (Sembol -> Başlık)
    val indicesMap = mapOf(
        "XU030.IS" to Pair("XU030", "BİST 30"),
        "XU050.IS" to Pair("XU050", "BİST 50"),
        "XU100.IS" to Pair("XU100", "BİST 100"),
        "USDTRY=X" to Pair("USD/TRY", "Dolar / TL"),
        "EURTRY=X" to Pair("EUR/TRY", "Euro / TL"),
        "GC=F" to Pair("XAU/USD", "Ons Altın ($)")
    )

    // Popüler Kripto Paralar (Sembol -> İsim)
    val cryptosMap = mapOf(
        "BTC-USD" to "Bitcoin",
        "ETH-USD" to "Ethereum",
        "BNB-USD" to "Binance Coin",
        "XRP-USD" to "XRP",
        "SOL-USD" to "Solana",
        "TRX-USD" to "TRON",
        "DOGE-USD" to "Dogecoin",
        "ZEC-USD" to "Zcash",
        "XMR-USD" to "Monero",
        "ADA-USD" to "Cardano",
        "XLM-USD" to "Stellar",
        "LINK-USD" to "ChainLink",
        "TON-USD" to "TON Coin",
        "BCH-USD" to "Bitcoin Cash",
        "LTC-USD" to "Litecoin",
        "SUI-USD" to "Sui",
        "NEAR-USD" to "NEAR Protocol",
        "DOT-USD" to "Polkadot",
        "ETC-USD" to "Ethereum Classic",
        "QNT-USD" to "Quant",
        "KAS-USD" to "Kaspa",
        "ALGO-USD" to "Algorand",
        "ATOM-USD" to "Cosmos",
        "BDX-USD" to "Beldex",
        "FIL-USD" to "Filecoin",
        "DASH-USD" to "Dash",
        "VET-USD" to "VeChain",
        "DATA-USD" to "Streamr",
        "STX-USD" to "Stacks",
        "TIA-USD" to "Celestia",
        "BSV-USD" to "Bitcoin SV",
        "XTZ-USD" to "Tezos",
        "CFX-USD" to "Conflux",
        "DCR-USD" to "Decred",
        "KAIA-USD" to "Kaia",
        "IOTA-USD" to "IOTA",
        "NEO-USD" to "Neo",
        "MANA-USD" to "Decentraland",
        "ZANO-USD" to "Zano",
        "GLM-USD" to "Golem",
        "QTUM-USD" to "Qtum",
        "ZRX-USD" to "0x",
        "ZEN-USD" to "Horizen",
        "DGB-USD" to "Digibyte",
        "HOT-USD" to "Holo",
        "QRL-USD" to "Quantum Resistant Ledger",
        "RVN-USD" to "Ravencoin",
        "AIOZ-USD" to "AIOZ Network",
        "ZIG-USD" to "Zignaly",
        "NANO-USD" to "Nano",
        "TFUEL-USD" to "Theta Fuel",
        "QUBIC-USD" to "QUBIC",
        "KAVA-USD" to "Kava",
        "ZIL-USD" to "Zilliqa",
        "CKB-USD" to "Nervos Network",
        "AXL-USD" to "Axelar",
        "ARRR-USD" to "Pirate Chain",
        "FLOW-USD" to "Flow",
        "TRB-USD" to "Tellor",
        "CTC-USD" to "Creditcoin",
        "ONT-USD" to "Ontology",
        "VANA-USD" to "Vana",
        "RON-USD" to "Ronin",
        "HNT-USD" to "Helium",
        "XVG-USD" to "Verge",
        "SC-USD" to "Siacoin",
        "G-USD" to "Gravity",
        "STORJ-USD" to "Storj",
        "PLUME-USD" to "Plume",
        "FB-USD" to "Fractal Bitcoin",
        "WAVES-USD" to "Waves",
        "ICX-USD" to "ICON",
        "RLC-USD" to "iExec RLC",
        "PEAQ-USD" to "peaq",
        "GPS-USD" to "GoPlus Security",
        "SKL-USD" to "SKALE Network",
        "DAG-USD" to "Constellation",
        "IOTX-USD" to "IoTeX",
        "STEEM-USD" to "STEEM",
        "CSPR-USD" to "Casper Network",
        "LCX-USD" to "LCX",
        "ARK-USD" to "Ark",
        "CTSI-USD" to "Cartesi",
        "SAHARA-USD" to "Sahara AI",
        "ARDR-USD" to "Ardor",
        "ONE-USD" to "Harmony",
        "STRAX-USD" to "Stratis",
        "ERG-USD" to "Ergo",
        "COTI-USD" to "COTI",
        "AEVO-USD" to "Aevo",
        "LSK-USD" to "Lisk",
        "OHO-USD" to "Oho",
        "CGPT-USD" to "ChainGPT",
        "FLUX-USD" to "Flux",
        "PIPPIN-USD" to "pippin",
        "CYBER-USD" to "CyberConnect",
        "BFC-USD" to "Bifrost",
        "CORN-USD" to "Corn",
        "CTK-USD" to "Shentu",
        "SHIB-USD" to "Shiba Inu",
        "PEPE-USD" to "Pepe",
        "AVAX-USD" to "Avalanche",
        "FLOKI-USD" to "Floki",
        "BONK-USD" to "Bonk"
    )

    suspend fun fetchBistStocks(): List<StockInfo> = withContext(Dispatchers.IO) {
        val symbols = bistStocksMap.keys.map { "$it.IS" }
        val result = fetchYahooQuote(symbols)
        
        result.map { info ->
            val cleanCode = info.hisse.removeSuffix(".IS")
            val companyName = bistStocksMap[cleanCode] ?: cleanCode
            info.copy(hisse = cleanCode, sirket = companyName)
        }
    }

    suspend fun fetchIndices(): List<StockInfo> = withContext(Dispatchers.IO) {
        val symbols = indicesMap.keys.toList()
        val rawResult = fetchYahooQuote(symbols)

        var usdPrice = 0.0
        var usdChange = 0.0
        var goldPrice = 0.0
        var goldChange = 0.0

        val mapBySymbol = mutableMapOf<String, StockInfo>()

        rawResult.forEach { info ->
            val mapped = indicesMap[info.hisse]
            val displayCode = mapped?.first ?: info.hisse
            val displayName = mapped?.second ?: info.sirket

            if (info.hisse == "USDTRY=X") {
                usdPrice = info.fiyat
                usdChange = info.degisim
            } else if (info.hisse == "GC=F") {
                goldPrice = info.fiyat
                goldChange = info.degisim
            }

            mapBySymbol[info.hisse] = info.copy(hisse = displayCode, sirket = displayName)
        }

        val orderedList = mutableListOf<StockInfo>()

        // Tam istenen sıralama: BİST 30, BİST 50, BİST 100, Dolar, Euro, Gram Altın, Ons Altın
        val orderKeys = listOf("XU030.IS", "XU050.IS", "XU100.IS", "USDTRY=X", "EURTRY=X")
        orderKeys.forEach { key ->
            mapBySymbol[key]?.let { orderedList.add(it) }
        }

        // Gram Altın (TL) hesaplaması: (Ons Altın / 31.1035) * Dolar
        if (goldPrice > 0 && usdPrice > 0) {
            val gramPrice = (goldPrice / 31.1035) * usdPrice
            val gramChange = goldChange + usdChange
            orderedList.add(
                StockInfo(
                    hisse = "XAU/TRY",
                    sirket = "Gram Altın",
                    fiyat = Math.round(gramPrice * 100) / 100.0,
                    degisim = Math.round(gramChange * 100) / 100.0,
                    hacim = 0.0
                )
            )
        }

        mapBySymbol["GC=F"]?.let { orderedList.add(it) }

        orderedList
    }

    suspend fun fetchCryptos(): List<StockInfo> = withContext(Dispatchers.IO) {
        val symbols = cryptosMap.keys.toList()
        val result = fetchYahooQuote(symbols)

        result.map { info ->
            val name = cryptosMap[info.hisse] ?: info.sirket
            info.copy(sirket = name)
        }
    }

    suspend fun fetchSingleTrend(ticker: String, range: String): SingleTrendResponse = withContext(Dispatchers.IO) {
        val clean = ticker.trim().uppercase()
        val yahooTicker = when {
            clean == "XAU/TRY" || clean.contains("ALTIN") -> "GC=F"
            clean == "USD/TRY" -> "USDTRY=X"
            clean == "EUR/TRY" -> "EURTRY=X"
            clean == "XU100" || clean == "^XU100" || clean == "XU100.IS" || clean.contains("BİST 100") || clean.contains("BIST 100") -> "XU100.IS"
            clean == "XU050" || clean == "^XU050" || clean == "XU050.IS" || clean.contains("BİST 50") || clean.contains("BIST 50") -> "XU050.IS"
            clean == "XU030" || clean == "^XU030" || clean == "XU030.IS" || clean.contains("BİST 30") || clean.contains("BIST 30") -> "XU030.IS"
            clean.contains("-") || clean.contains("=") || clean.endsWith(".IS") -> clean
            else -> "$clean.IS"
        }

        var interval = "1d"
        var effectiveRange = range
        when (range) {
            "1d" -> interval = "5m"
            "1w" -> { effectiveRange = "5d"; interval = "15m" }
            "3mo", "1y" -> interval = "1d"
            else -> { effectiveRange = "1mo"; interval = "1d" }
        }

        val prices = mutableListOf<Double>()
        val dates = mutableListOf<String>()

        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooTicker?range=$effectiveRange&interval=$interval"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body()?.string() ?: return@withContext SingleTrendResponse(clean, emptyList(), emptyList())

            val json = JSONObject(body)
            val chart = json.optJSONObject("chart")
            val resultArr = chart?.optJSONArray("result")
            if (resultArr != null && resultArr.length() > 0) {
                val resultObj = resultArr.getJSONObject(0)
                val timestamps = resultObj.optJSONArray("timestamp")
                val indicators = resultObj.optJSONObject("indicators")
                val quoteArr = indicators?.optJSONArray("quote")
                val quoteObj = quoteArr?.optJSONObject(0)
                val closes = quoteObj?.optJSONArray("close")

                // Gram Altın ise Dolar kuru alıp TL serisine dönüştür
                var usdRate = 1.0
                if (clean == "XAU/TRY") {
                    try {
                        val usdQuote = fetchYahooQuote(listOf("USDTRY=X"))
                        if (usdQuote.isNotEmpty() && usdQuote[0].fiyat > 0) {
                            usdRate = usdQuote[0].fiyat
                        }
                    } catch (e: Exception) {
                        Log.e("YahooRepository", "USD Rate error for Gram Gold trend", e)
                    }
                }

                if (timestamps != null && closes != null) {
                    for (i in 0 until closes.length()) {
                        val closeVal = closes.optDouble(i, Double.NaN)
                        val timestamp = timestamps.optLong(i, 0L)

                        if (!closeVal.isNaN() && closeVal > 0 && timestamp > 0) {
                            val finalPrice = if (clean == "XAU/TRY") (closeVal / 31.1035) * usdRate else closeVal
                            prices.add(Math.round(finalPrice * 100) / 100.0)

                            val dateObj = java.util.Date((timestamp + 3 * 3600) * 1000)
                            val dateStr = if (effectiveRange == "1d" || effectiveRange == "5d") {
                                val hr = String.format(java.util.Locale.US, "%02d", dateObj.hours)
                                val min = String.format(java.util.Locale.US, "%02d", dateObj.minutes)
                                "$hr:$min"
                            } else {
                                val day = String.format(java.util.Locale.US, "%02d", dateObj.date)
                                val month = String.format(java.util.Locale.US, "%02d", dateObj.month + 1)
                                val yr = (dateObj.year + 1900).toString().takeLast(2)
                                "$day.$month.$yr"
                            }
                            dates.add(dateStr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("YahooRepository", "Error fetching trend for $ticker", e)
        }

        SingleTrendResponse(clean, prices, dates)
    }

    private suspend fun fetchYahooQuote(symbols: List<String>): List<StockInfo> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyList()

        val stockList = java.util.Collections.synchronizedList(mutableListOf<StockInfo>())

        coroutineScope {
            val jobs = symbols.map { symbol ->
                async {
                    val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=1d&interval=1d"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()

                    try {
                        val response = client.newCall(request).execute()
                        val body = response.body()?.string() ?: return@async

                        val json = JSONObject(body)
                        val chart = json.optJSONObject("chart")
                        val resultArr = chart?.optJSONArray("result")
                        if (resultArr != null && resultArr.length() > 0) {
                            val meta = resultArr.getJSONObject(0).optJSONObject("meta")
                            if (meta != null) {
                                val price = meta.optDouble("regularMarketPrice", 0.0)
                                val prevClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", price))
                                val change = if (prevClose > 0) ((price - prevClose) / prevClose) * 100.0 else 0.0
                                val volume = meta.optDouble("regularMarketVolume", 0.0)
                                val longName = meta.optString("shortName", meta.optString("longName", symbol))

                                if (price > 0) {
                                    stockList.add(
                                        StockInfo(
                                            hisse = symbol,
                                            sirket = longName,
                                            fiyat = Math.round(price * 100) / 100.0,
                                            degisim = Math.round(change * 100) / 100.0,
                                            hacim = volume
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("YahooRepository", "Error fetching Yahoo Finance data for $symbol", e)
                    }
                }
            }
            jobs.awaitAll()
        }

        stockList
    }
}
