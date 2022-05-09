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
                        imageList.toList()
                )
                cachedInstruments.add(asset)
            }
        }
        return cachedInstruments.toList()
    }

    fun getPublicHistoryPositions(
            limit: String = "100",
            page: String = "1",
            StartTime: String,
            cid: String,
            mode: TradingMode
    ): List<EtoroPosition> {
        /* val req = prepareRequest(
                "sapi/trade-data-${mode.name.toLowerCase()}/history/public/credit/flat?CID=$cid&ItemsPerPage=$limit&PageNumber=$page&StartTime=$StartTime",
                userContext.exchangeToken, mode, metadataService.getMetadata()
        )
                .GET()
                .build() */

        val response = JSONObject(browserHttpClient.fetchPublicHistory(limit, page, StartTime, cid, mode.toString()))
                .getJSONArray("PublicHistoryPositions")
                .toString()

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        return mapper.readValue(response)
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

    /* fun watchMirroredAssets(mode: String): Int {
        val mirroredAssets = getMirroredInstrumentIds(mode)
        for (id in mirroredAssets) {
            if (watchlist.getById(id) == null) {
                watchlist.addAssetToWatchlistById(id)
            }
        }
        return mirroredAssets.size
    } */

}
