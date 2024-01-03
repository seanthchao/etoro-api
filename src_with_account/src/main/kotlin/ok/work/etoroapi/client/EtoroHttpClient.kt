package ok.work.etoroapi.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ok.work.etoroapi.client.browser.BrowserHttpClient
import ok.work.etoroapi.client.browser.EtoroMetadataService
import ok.work.etoroapi.client.browser.EtoroMetadata
import ok.work.etoroapi.model.*
import ok.work.etoroapi.transactions.Transaction
import ok.work.etoroapi.transactions.TransactionPool
import ok.work.etoroapi.watchlist.EtoroAsset
import ok.work.etoroapi.watchlist.EtoroFullAsset
import ok.work.etoroapi.watchlist.Image
import ok.work.etoroapi.watchlist.Watchlist
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.annotation.PostConstruct


data class ViewContext(val ClientViewRate: Double)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EtoroPosition(
        val PositionID: String?,
        val InstrumentID: String,
        val IsBuy: Boolean,
        val Leverage: Int,
        val StopLossRate: Double,
        val TakeProfitRate: Double,
        val IsTslEnabled: Boolean,
        val View_MaxPositionUnits: Int,
        val View_Units: Double,
        val View_openByUnits: Boolean?,
        val Amount: Double,
        val ViewRateContext: ViewContext?,
        val OpenDateTime: String?,
        val IsDiscounted: Boolean?,
        val OpenRate: Double?,
        val CloseRate: Double?,
        val NetProfit: Double?,
        val CloseDateTime: String?
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EtoroMirrorPosition(
        val MirrorID: String,
        val ParentCID: String,
        val ParentUsername: String,
        val Invested: Double,
        val NetProfit: Double,
        val Value: Double
)

data class EtoroPositionForOpen(
        val CID: String, val PositionID: String?, val InstrumentID: String, val IsBuy: Boolean, val Leverage: Int,
        val StopLossRate: Double, val TakeProfitRate: Double, val IsTslEnabled: Boolean,
        val View_MaxPositionUnits: Int, val View_Units: Double, val View_openByUnits: Boolean?,
        val Amount: Double, val ViewRateContext: ViewContext?, val OpenDateTime: String?, val IsDiscounted: Boolean?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EtoroPositionForUpdate(
        val PositionID: String,
        val StopLossRate: Double?,
        val TakeProfitRate: Double?,
        val IsTslEnabled: Boolean?
)

data class AssetInfoRequest(val instrumentIds: Array<String>)

@Component
class EtoroHttpClient {

    @Autowired
    private lateinit var userContext: UserContext

    @Autowired
    private lateinit var watchlist: Watchlist

    @Autowired
    private lateinit var transactionPool: TransactionPool

    @Autowired
    private lateinit var metadataService: EtoroMetadataService

    @Autowired
    private lateinit var browserHttpClient: BrowserHttpClient



    private val client = HttpClient.newHttpClient()

    var okHttpClient = OkHttpClient()

    var cachedInstruments: ArrayList<EtoroFullAsset> = arrayListOf()

    @PostConstruct
    fun init() {
        getInstruments()
    }

    fun getInstruments(): List<EtoroFullAsset> {
        val req = HttpRequest.newBuilder()
                .uri(URI("https://api.etorostatic.com/sapi/instrumentsmetadata/V1.1/instruments/bulk?bulkNumber=1&cv=77286b759effc7a624555e466cfb7c86_48a07d20d16ee784216c9eed65623d62&totalBulks=1"))
                .GET()
                .build()
        if (cachedInstruments.isEmpty()) {
            val response = client.send(req, HttpResponse.BodyHandlers.ofString()).body()
            val jsonArray: JSONArray = JSONObject(response).getJSONArray("InstrumentDisplayDatas")
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val images = item.getJSONArray("Images")
                val imageList: ArrayList<Image> = arrayListOf()
                for (j in 0 until images.length()) {
                    val imageData = images.getJSONObject(j)
                    if (!imageData.has("Uri") || !imageData.has("Width")) {
                        continue;
                    }
                    val image = Image(imageData.getInt("Width"), imageData.getInt("Height"), imageData.getString("Uri"))
                    imageList.add(image)
                }
                var id: String
                try {
                    id = item.getString("InstrumentID")
                } catch (e: Exception) {
                    id = item.getInt("InstrumentID").toString()
                }
                val asset = EtoroFullAsset(
                        id,
                        item.getString("SymbolFull"),
                        item.getString("InstrumentDisplayName"),
                        item.getInt("InstrumentTypeID"),
                        item.getBoolean("IsInternalInstrument"),
                        imageList.toList()
                )
                cachedInstruments.add(asset)
            }
        }
        return cachedInstruments.toList()
    }

    fun getPositionsSimple(mode: TradingMode): List<EtoroPosition> {
        val response = JSONObject(browserHttpClient.fetchAccountData(mode.toString()))
                .getJSONObject("AggregatedResult")
                .getJSONObject("ApiResponses")
                .getJSONObject("PrivatePortfolio")
                .getJSONObject("Content")
                .getJSONObject("ClientPortfolio")
                .getJSONArray("Positions")
                .toString()

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(response.toString())
    }

    fun getPositions(mode: TradingMode): List<EtoroPosition> {
        val response = JSONObject(browserHttpClient.fetchAccountData(mode.toString()))
                .getJSONObject("AggregatedResult")
                .getJSONObject("ApiResponses")
                .getJSONObject("PrivatePortfolio")
                .getJSONObject("Content")
                .getJSONObject("ClientPortfolio")
                .getJSONArray("Positions")
                .toString()

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)

        val positions: List<EtoroPosition> = mapper.readValue(response)
        return positions.map {
            val instrumentId = it.InstrumentID
            val assetInfo = getAssetInfo(instrumentId, mode)
            /* println(assetInfo) */
            if (watchlist.getById(instrumentId) != null) {
                if (it.IsBuy) {
                    val price = watchlist.getPrice(
                            instrumentId,
                            PositionType.SELL,
                            assetInfo.getBoolean("AllowDiscountedRates")
                    )
                    it.copy(NetProfit = (price - it.OpenRate!!) * it.Leverage * it.Amount / it.OpenRate)
                } else {
                    val price =
                            watchlist.getPrice(instrumentId, PositionType.BUY, assetInfo.getBoolean("AllowDiscountedRates"))
                    it.copy(NetProfit = (it.OpenRate!! - price) * it.Leverage * it.Amount / it.OpenRate)
                }
            } else {
                it
            }
        }
    }

    fun getSLPercView(mode: TradingMode) {
      val driver = metadataService.getDriver()
      val thisReq2 = "return JSON.stringify(await (await fetch(\"https://etorologsapi.etoro.com/api/v2/monitoring?applicationIdentifier=ReToro\", {\n" +
                    "  \"headers\": {\n" +
                      "  \"accept\": \"*/*\",\n" +
                      "  \"accept-language\": \"en-US,en;q=0.9\",\n" +
                      "  \"content-type\": \"application/json\",\n" +
                      "  \"sec-fetch-dest\": \"empty\",\n" +
                      "  \"sec-fetch-mode\": \"cors\",\n" +
                      "  \"sec-fetch-site\": \"same-site\",\n" +
                      "  \"sec-gpc\": \"1\"\n" +
                    "  },\n" +
                    "  \"referrer\": \"https://www.etoro.com/\",\n" +
                    "  \"referrerPolicy\": \"strict-origin-when-cross-origin\",\n" +
                      "\"body\": \"{\\\"LogEvents\\\":[{\\\"Message\\\":\\\"UI Table Update Columns\\\",\\\"Categories\\\":\\\"Infra UI_Table\\\",\\\"Columns\\\":[\\\"amount\\\",\\\"unit\\\",\\\"open\\\",\\\"slPercent\\\",\\\"tp\\\",\\\"plDollars\\\",\\\"plPercentage\\\"],\\\"TableId\\\":\\\"portfolioBreakdown_positions\\\",\\\"Level\\\":\\\"info\\\",\\\"PushStatus\\\":true,\\\"IsLoggedIn\\\":true,\\\"CID\\\":11281986,\\\"Username\\\":\\\"seanthchao\\\",\\\"BatchIndex\\\":71,\\\"ApplicationIdentifier\\\":\\\"ReToro\\\",\\\"ApplicationVersion\\\":\\\"455.0.2\\\",\\\"SessionGuid\\\":\\\"d3d504a5-2541-4aa9-8a87-7b332f7870c9\\\",\\\"UserSessionGuid\\\":${userContext.requestId},\\\"LSSessionId\\\":\\\"S7d0f2fc56f34928cM67fT2611101\\\",\\\"ClientDateTime\\\":\\\"2022-08-01T11:41:18.269Z\\\",\\\"SecondsFromStartUp\\\":908}],\\\"MonitorEvents\\\":[]}\",\n" +
                    "  \"method\": \"POST\",\n" +
                    "  \"mode\": \"cors\",\n" +
                    "  \"credentials\": \"omit\"\n" +
                    "  })).json());"
      val body = driver.executeScript(thisReq2) as String
    }

    fun getLoginData(mode: String): JSONObject {
        val loginData = browserHttpClient.fetchAccountData(mode)
        return JSONObject(loginData)
    }

    fun getMirrors(mode: String): List<Mirror> {
        val loginData = getLoginData(mode)
        val mirrors = loginData.getJSONObject("AggregatedResult")
            .getJSONObject("ApiResponses")
            .getJSONObject("PrivatePortfolio")
            .getJSONObject("Content")
            .getJSONObject("ClientPortfolio")
            .getJSONArray("Mirrors")
        val hasUserData =  loginData.getJSONObject("AggregatedResult")
            .getJSONObject("ApiResponses")
            .getJSONObject("MirrorsUserData")
            .getInt("StatusCode") == 200
        if (hasUserData) {
            var userData = loginData.getJSONObject("AggregatedResult")
                .getJSONObject("ApiResponses")
                .getJSONObject("MirrorsUserData")
                .getJSONObject("Content")
                .getJSONArray("users")
            for (i in 0 until mirrors.length()) {
                val mirror = mirrors.getJSONObject(i)
                val user = userData.find {
                    it is JSONObject && it.getInt("realCID") == mirror.getInt("ParentCID")
                }
                if (user is JSONObject) {
                    mirror.put("User", user)
                }
            }
        }
        val json = mirrors.toString()
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(json)
    }

    fun getMirrorPositions(mode: String, mirror_id: String? = null): List<EtoroPosition> {
        val mirrorsData = getLoginData(mode)
            .getJSONObject("AggregatedResult")
            .getJSONObject("ApiResponses")
            .getJSONObject("PrivatePortfolio")
            .getJSONObject("Content")
            .getJSONObject("ClientPortfolio")
            .getJSONArray("Mirrors")
        if (mirror_id != null) {
            val mirror = mirrorsData.find { it is JSONObject && it.getInt("MirrorID") == mirror_id.toInt() }
            if (mirror is JSONObject) {
                val positionsJSON = mirror.getJSONArray("Positions").toString()
                val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                return mapper.readValue(positionsJSON)
            }
            return listOf()
        } else {
            var allPositions: List<EtoroPosition> = listOf()
            for (i in 0 until mirrorsData.length()) {
                val mirrorData = mirrorsData.getJSONObject(i)
                val positionsJSON = mirrorData.getJSONArray("Positions").toString()
                val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                val mirrorPositions: List<EtoroPosition> = mapper.readValue(positionsJSON)
                allPositions = allPositions + mirrorPositions
            }
            return allPositions
        }
    }

    fun getMirroredInstrumentIds(mode: String): List<String> {
        return getMirrorPositions(mode).map { it.InstrumentID }.distinct().sorted()
    }

    fun getHistoryPositions(
            limit: String = "100",
            page: String = "1",
            StartTime: String = "",
            mode: TradingMode
    ): List<EtoroPosition> {
        val req = prepareRequest(
                "sapi/trade-data-${mode.name.toLowerCase()}/history/private/credit/flat?ItemsPerPage=$limit&PageNumber=$page&StartTime=$StartTime",
                userContext.exchangeToken, mode, metadataService.getMetadata()
        )
                .GET()
                .build()

        val response = JSONObject(browserHttpClient.fetchHistory(limit, page, StartTime ,mode.toString()))
                .getJSONArray("HistoryPositions")
                .toString()

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(response)
    }

    fun getPublicHistoryPositions(
            limit: String = "100",
            page: String = "1",
            StartTime: String,
            cid: String,
            mode: TradingMode
    ): List<EtoroPosition> {
        val req = prepareRequest(
                "sapi/trade-data-${mode.name.toLowerCase()}/history/public/credit/flat?CID=$cid&ItemsPerPage=$limit&PageNumber=$page&StartTime=$StartTime",
                userContext.exchangeToken, mode, metadataService.getMetadata()
        )
                .GET()
                .build()

        val response = JSONObject(browserHttpClient.fetchPublicHistory(limit, page, StartTime, cid, mode.toString()))
                .getJSONArray("PublicHistoryPositions")
                .toString()

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(response)
    }

    fun getMirrorHistory(
            cid: String): List<EtoroPosition> {

        val response = JSONObject(browserHttpClient.fetchDemoMirrorHistory(cid))
                .getJSONArray("HistoryPositions")
                .toString()

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(response)
    }

    fun getExpireTime(): String {
      var metaData = metadataService.getMetadata()
      return metaData.expirationTime.toString()
    }

    fun getLiveProfolioMaster(
            cid: String
    ): List<EtoroPosition> {
        val resMajor = JSONObject(browserHttpClient.fetchLiveProfolioMaster(cid))
              .getJSONArray("AggregatedPositions")
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(resMajor.toString())
    }

    fun getLiveProfolioMirror(
            cid: String
    ): List<EtoroMirrorPosition> {
      val resMajor = JSONObject(browserHttpClient.fetchLiveProfolioMaster(cid))
            .getJSONArray("AggregatedMirrors")
      val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
      return mapper.readValue(resMajor.toString())
    }

    fun getLiveProfolioDetail(
            cid: String,
            instrumentID: String
    ): List<EtoroPosition> {
        val res = JSONObject(browserHttpClient.fetchLiveProfolioDetail(cid, instrumentID))
                .getJSONArray("PublicPositions")
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(res.toString())
    }

    fun getPrivateMirror(
            cid: String
    ): List<EtoroPosition> {
        val res = JSONObject(browserHttpClient.fetchPrivateMirror(cid))
                .getJSONObject("PublicMirror").getJSONArray("Positions")
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(res.toString())
    }

    fun getInstrumentIDs(): List<EtoroAsset> {
        val req = HttpRequest.newBuilder()
                .uri(URI("https://api.etorostatic.com/sapi/instrumentsmetadata/V1.1/instruments?cv=1c85198476a3b802326706d0c583e99b_beb3f4faa55c3a46ed44fc6d763db563"))
                .GET()
                .build()

        val response =
                JSONObject(client.send(req, HttpResponse.BodyHandlers.ofString()).body()).get("InstrumentDisplayDatas")
                        .toString()
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        return mapper.readValue(response)
    }

    fun openPosition(position: Position, mode: TradingMode, order_type: String): Transaction {
        /* System.out.println(position) */
        val type = position.type.equals(PositionType.BUY)
        val instrumentId = position.instrumentId ?: watchlist.getInstrumentIdByName(position.name ?: "")
        val assetInfo = getAssetInfo(instrumentId, mode)
        /* print("assetInfo") */
        /* println("assetInfo: "+assetInfo) */
        val price = watchlist.getPrice(instrumentId, position.type, assetInfo.getBoolean("AllowDiscountedRates"))
        val leverages = assetInfo.getJSONArray("Leverages")
        val minPositionAmount = assetInfo.getInt("MinPositionAmount")
        val minPositionAmountAbsolute = assetInfo.getInt("MinPositionAmountAbsolute")

/*        if (watchlist.isMarketOpen(instrumentId)) {*/
        if (!leverages.contains(position.leverage)) {
            throw RuntimeException("x${position.leverage} is not permitted. You can use $leverages")
        }
        if (minPositionAmount > position.leverage * position.amount || position.amount < minPositionAmountAbsolute) {
            throw RuntimeException("You cannot open less than minimum position amount $$minPositionAmount, and minimum absolute amount $$minPositionAmountAbsolute")
        }


        if (position.type == PositionType.SELL) {
            when {
                position.stopLossAmountRate > 0.0 -> position.stopLossRate =
                        price + (price * position.stopLossAmountRate / 100) / position.leverage
                position.stopLossRate > 0.0 -> position.stopLossRate = price + (price * position.stopLossRate / 100)
                position.stopLoss > 0.0 -> position.stopLossRate = position.stopLoss
                else -> {
                    val maxSL = assetInfo.getInt("MaxStopLossPercentage")
                    position.stopLossRate = price + (price * maxSL / 100)
                }
            }

            when {
                /* position.takeProfitAmountRate > 0.0 -> position.takeProfitRate =
                        price - (price * position.takeProfitAmountRate / 100) / position.leverage */
                position.takeProfitAmountRate > 0.0 -> position.takeProfitRate = price + position.takeProfitAmountRate
                position.takeProfitRate > 0.0 -> position.takeProfitRate =
                        price - (price * position.takeProfitRate / 100)
                position.takeProfit > 0.0 -> position.takeProfitRate = position.takeProfit
                else -> {
                    /* position.takeProfitRate = price - 0.001 */
                    position.takeProfitRate = price - (price * 50 / 100)
                }
            }
        } else if (position.type == PositionType.BUY) {
            when {
                position.stopLossAmountRate > 0.0 -> position.stopLossRate =
                        price - (price * position.stopLossAmountRate / 100) / position.leverage
                position.stopLossRate > 0.0 -> position.stopLossRate = price - (price * position.stopLossRate / 100)
                position.stopLoss > 0.0 -> position.stopLossRate = position.stopLoss
                else -> {
                    val maxSL = assetInfo.getInt("MaxStopLossPercentage")
                    /* position.stopLossRate = price */
                    position.stopLossRate = price - (price * maxSL / 100)
                }
            }
            when {
                /* position.takeProfitAmountRate > 0.0 -> position.takeProfitRate =
                        price + (price * position.takeProfitAmountRate / 100) / position.leverage */
                position.takeProfitAmountRate > 0.0 -> position.takeProfitRate = price + position.takeProfitAmountRate
                position.takeProfitRate > 0.0 -> position.takeProfitRate =
                        price + (price * position.takeProfitRate / 100)
                position.takeProfit > 0.0 -> position.takeProfitRate = position.takeProfit
                else -> {
                    /* position.takeProfitRate = price + 0.001 */
                    position.takeProfitRate = price + (price * 50 / 100)
                }

            }
        }
        position.takeProfitRate = position.takeProfitRate.round(3)
        position.stopLossRate = position.stopLossRate.round(3)
        var credentials = metadataService.getMetadata()
        val driver = metadataService.getDriver()
        val thisReq2 = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-${mode.name.toLowerCase()}/${order_type}?client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
                "    \"accounttype\": \"${mode.name.toLowerCase()}\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"606.0.1\",\n" +
                "    \"authorization\": \"${credentials.token}\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "    \"x-csrf-token\": \"${credentials.cToken}\"\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
                "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
                "  \"body\": \"{\\\"InstrumentID\\\":${instrumentId},\\\"IsBuy\\\":${type},\\\"Leverage\\\":${position.leverage},\\\"IsTslEnabled\\\":${position.tsl},\\\"View_MaxPositionUnits\\\":20,\\\"IsDiscounted\\\":false,\\\"Amount\\\":${position.amount},\\\"ViewRateContext\\\":{\\\"ClientViewRate\\\":${price}}}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"mode\": \"cors\",\n" +
                "})).json());"
        val body = driver.executeScript(thisReq2) as String
        println("body(): $body")
        /* Thread.sleep(1500) */

        val transactionId = JSONObject(body)["Token"].toString()
        /* println("transactionId(): $transactionId") */
        /* println(transactionId) */
        return transactionPool.getFromPool(transactionId) ?: Transaction(transactionId, null, null, null, null)
    }

    fun updatePosition(positionRequestBody: EtoroPositionForUpdate, mode: TradingMode): Transaction {
        val id = positionRequestBody.PositionID
        var credentials = metadataService.getMetadata()
        val driver = metadataService.getDriver()
        val thisReq2 = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-${mode.name.toLowerCase()}/positions/${id}?client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
                "    \"accounttype\": \"Demo\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"384.0.1\",\n" +
                "    \"authorization\": \"${credentials.token}\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "    \"x-csrf-token\": \"${credentials.cToken}\"\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
                "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
                "  \"body\": \"{\\\"StopLossRate\\\":${positionRequestBody.StopLossRate},\\\"PositionID\\\":${id}}\",\n" +
                "  \"method\": \"PUT\",\n" +
                "  \"mode\": \"cors\",\n" +
                "})).json());"
        val body = driver.executeScript(thisReq2) as String
        println("body(): $body")

        val transactionId = JSONObject(body).getString("Token").toString()
        return transactionPool.getFromPool(transactionId) ?: Transaction(transactionId, null, null, null, null)
    }

    fun updatePositionTP(positionRequestBody: EtoroPositionForUpdate, mode: TradingMode): Transaction {
        val id = positionRequestBody.PositionID
        var credentials = metadataService.getMetadata()
        val driver = metadataService.getDriver()
        val thisReq2 = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-${mode.name.toLowerCase()}/positions/${id}?client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
                "    \"accounttype\": \"Demo\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"384.0.1\",\n" +
                "    \"authorization\": \"${credentials.token}\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "    \"x-csrf-token\": \"${credentials.cToken}\"\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
                "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
                "  \"body\": \"{\\\"TakeProfitRate\\\":${positionRequestBody.TakeProfitRate},\\\"PositionID\\\":${id}}\",\n" +
                "  \"method\": \"PUT\",\n" +
                "  \"mode\": \"cors\",\n" +
                "})).json());"
        val body = driver.executeScript(thisReq2) as String
        println("body(): $body")

        val transactionId = JSONObject(body).getString("Token").toString()
        return transactionPool.getFromPool(transactionId) ?: Transaction(transactionId, null, null, null, null)
    }

    fun deletePosition(id: String, mode: TradingMode) {
        /* val req = prepareOkRequest(
                "sapi/trade-${mode.name.toLowerCase()}/positions/$id?PositionID=$id&client_request_id=${userContext.requestId}",
                userContext.exchangeToken,
                mode,
                metadataService.getMetadata()
        )
        req.delete("{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())) */
        var credentials = metadataService.getMetadata()
        val driver = metadataService.getDriver()
        val thisReq2 = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-${mode.name.toLowerCase()}/positions/$id?PositionID=$id&client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
                "    \"accounttype\": \"Demo\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"384.0.1\",\n" +
                "    \"authorization\": \"${credentials.token}\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "    \"x-csrf-token\": \"${credentials.cToken}\"\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
                "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
                "  \"body\": \"{}\",\n" +
                "  \"method\": \"DELETE\",\n" +
                "  \"mode\": \"cors\",\n" +
                "})).json());"
        /* val body = driver.executeScript(thisReq2) */
        val body = driver.executeScript(thisReq2) as String
        /* val code = okHttpClient.newCall(req.build()).execute().code

        if (code != 200) {
            throw RuntimeException("Failed close positionID $id")
        } */
    }

    fun deleteBulkPosition(ids: List<String>, mode: TradingMode) {
        /* val req = prepareOkRequest(
                "sapi/trade-${mode.name.toLowerCase()}/positions/$id?PositionID=$id&client_request_id=${userContext.requestId}",
                userContext.exchangeToken,
                mode,
                metadataService.getMetadata()
        )
        req.delete("{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())) */
        var credentials = metadataService.getMetadata()
        val driver = metadataService.getDriver()
        val thisReq2 = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-${mode.name.toLowerCase()}/positions/bulk-delete?client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
                "    \"accounttype\": \"Demo\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"384.0.1\",\n" +
                "    \"authorization\": \"${credentials.token}\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "    \"x-csrf-token\": \"${credentials.cToken}\"\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
                "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
                "  \"body\": \"{\\\"PositionIDs\\\":${ids}}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"mode\": \"cors\",\n" +
                "})).json());"
        /* val body = driver.executeScript(thisReq2) */
        val body = driver.executeScript(thisReq2) as String
        /* val code = okHttpClient.newCall(req.build()).execute().code

        if (code != 200) {
            throw RuntimeException("Failed close positionID $id")
        } */
    }

    fun watchMirroredAssets(mode: String): Int {
        val mirroredAssets = getMirroredInstrumentIds(mode)
        for (id in mirroredAssets) {
            if (watchlist.getById(id) == null) {
                watchlist.addAssetToWatchlistById(id)
            }
        }
        return mirroredAssets.size
    }

    fun getAssetInfo(id: String, mode: TradingMode): JSONObject {
        return JSONObject(browserHttpClient.fetchAssetInfo(id, mode.toString())).getJSONArray("PrivateInstruments").getJSONObject(0)
    }

    fun getCash(mode: TradingMode): Double {
        return JSONObject(browserHttpClient.fetchAccountData(mode.toString()))
                .getJSONObject("AggregatedResult")
                .getJSONObject("ApiResponses")
                .getJSONObject("PrivatePortfolio")
                .getJSONObject("Content")
                .getJSONObject("ClientPortfolio")
                .getDouble("Credit")
    }

    fun logout():Any {
      var credentials = metadataService.getMetadata()
      val driver = metadataService.getDriver()
      val thisReq = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/api/sts/v2/logout?client_request_id=${userContext.requestId}\", {\n" +
              "  \"headers\": {\n" +
              "    \"accept\": \"application/json, text/plain, */*\",\n" +
              "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
              "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
              "    \"accounttype\": \"Demo\",\n" +
              "    \"applicationidentifier\": \"ReToro\",\n" +
              "    \"applicationversion\": \"384.0.1\",\n" +
              "    \"authorization\": \"${credentials.token}\",\n" +
              "    \"sec-fetch-dest\": \"empty\",\n" +
              "    \"sec-fetch-mode\": \"cors\",\n" +
              "    \"sec-fetch-site\": \"same-origin\",\n" +
              "    \"x-csrf-token\": \"${credentials.cToken}\"\n" +
              "  },\n" +
              "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
              "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
              "  \"method\": \"POST\",\n" +
              "  \"mode\": \"cors\",\n" +
              "})));"
      driver.executeScript(thisReq)
      return "logged out"
    }

}
