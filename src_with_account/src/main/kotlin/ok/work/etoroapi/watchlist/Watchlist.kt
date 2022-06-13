package ok.work.etoroapi.watchlist

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ok.work.etoroapi.client.EtoroHttpClient
import ok.work.etoroapi.client.websocket.EtoroLightStreamerClient
import ok.work.etoroapi.model.PositionType
import org.apache.xpath.operations.Bool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import java.lang.RuntimeException
import javax.annotation.PostConstruct

data class EtoroAsset(val InstrumentID: String, val SymbolFull: String, val InstrumentDisplayName: String) {
    override fun toString(): String {
        return SymbolFull
    }
}
data class EtoroAssetFromJson(val instrumentID: String, val symbolFull: String, val instrumentDisplayName: String) {
    override fun toString(): String {
        return symbolFull
    }
}
data class EtoroFullAsset(val InstrumentID: String, val SymbolFull: String, val InstrumentDisplayName: String, val images: List<Image>) {
    override fun toString(): String {
        return SymbolFull
    }
}

data class Asset(val id: String, val name: String, val fullName: String, var buy: Double?, var sell: Double?, var marketOpen: Boolean?, var askDiscounted: Double, var bidDiscounted: Double)

data class Image(val Width: Int, val Height: Int, val Uri: String)

@Component
class Watchlist {
    private var assetsMapIDs: MutableMap<String, EtoroAsset> = mutableMapOf()
    private var assetsMapNames: MutableMap<String, EtoroAsset> = mutableMapOf()

    private var assetsMapIDs_: MutableMap<String, EtoroAssetFromJson> = mutableMapOf()
    private var assetsMapNames_: MutableMap<String, EtoroAssetFromJson> = mutableMapOf()

    private val watchlist: MutableMap<String, Asset> = mutableMapOf()

    @Autowired
    lateinit var etoroClient: EtoroHttpClient

    @Autowired
    lateinit var lightStreamerClient: EtoroLightStreamerClient

    val SAVED_LIST_PATH = "watchlist.json"
    val SAVED_ASSETID_LIST_PATH = "assets_map_IDs.json"
    val SAVED_ASSETNAME_LIST_PATH = "assets_map_names.json"


    @PostConstruct
    fun init() {
        etoroClient.getInstrumentIDs().forEach { asset ->
            assetsMapIDs.put(asset.InstrumentID, asset)
            assetsMapNames.put(asset.SymbolFull.toLowerCase(), asset)
        }
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
        val json = File(SAVED_LIST_PATH)
        if (json.exists()) {
            mapper.readValue<MutableMap<String, Asset>>(json).forEach { p -> watchlist[p.key] = p.value}
            lightStreamerClient.subscribeByIds( watchlist.toList().map { pair -> pair.first })
        }
        saveAssetMaps()
        assetsMapIDs = mutableMapOf()
        assetsMapNames = mutableMapOf()
        /* println(assetsMapIDs) */
    }

    fun getById(id: String): Asset? {
        return watchlist[id]
    }

    fun addAssetToWatchlistById(id: String): MutableMap<String, Asset> {
        if (watchlist[id] != null) {
            throw RuntimeException("Already in watchlist $watchlist")
        }
        val asset = getAssetsMapIDs(id)
        /* val asset = assetsMapIDs[id] */
        if (asset != null) {
            lightStreamerClient.subscribeById(asset.instrumentID)
            watchlist[id] = Asset(asset.instrumentID, asset.symbolFull, asset.instrumentDisplayName, null, null, null, 0.0, 0.0)
            saveToFile()
            return watchlist
        } else {
            throw RuntimeException("Asset with InstrumentID $id was not found.")
        }
    }

    fun addAssetToWatchlistByName(name: String): MutableMap<String, Asset> {
        /* val asset = assetsMapNames[name.toLowerCase()] */
        val asset = getAssetsMapNames(name.toLowerCase())
        if (asset != null) {
            lightStreamerClient.subscribeById(asset.instrumentID)
            watchlist[asset.instrumentID] = Asset(asset.instrumentID, asset.symbolFull.toLowerCase(), asset.instrumentDisplayName, null, null, null, 0.0, 0.0)
            saveToFile()
            return watchlist
        } else {
            throw RuntimeException("Asset with name $name was not found.")
        }
    }

    fun watchlist(): List<Asset> {
        return watchlist.map { p -> p.value }.toList()
    }

    private fun saveToFile() {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
        var file = File(SAVED_LIST_PATH)
        if (!file.exists()) {
            /* file.createNewFile() */
        }
        /* mapper.writeValue(File(SAVED_LIST_PATH), watchlist) */
    }

    private fun saveAssetMaps() {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
        var file = File(SAVED_ASSETID_LIST_PATH)
        if (!file.exists()) {
            /* file.createNewFile() */
        }
        /* mapper.writeValue(File(SAVED_ASSETID_LIST_PATH), assetsMapIDs) */

        file = File(SAVED_ASSETNAME_LIST_PATH)
        if (!file.exists()) {
            /* file.createNewFile() */
        }
        /* mapper.writeValue(File(SAVED_ASSETNAME_LIST_PATH), assetsMapNames) */
    }

    private fun getAssetsMapIDs(id: String): EtoroAssetFromJson? {
      val json = File(SAVED_ASSETID_LIST_PATH)
      val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
              .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
      if (json.exists()) {
          mapper.readValue<MutableMap<String, EtoroAssetFromJson>>(json).forEach { p -> assetsMapIDs_[p.key] = p.value}
          /* lightStreamerClient.subscribeByIds( assetsMapIDs.toList().map { pair -> pair.first }) */
      }
      val res = assetsMapIDs_[id]
      assetsMapIDs_ = mutableMapOf()
      return res
    }

    private fun getAssetsMapNames(name: String): EtoroAssetFromJson? {
      val json = File(SAVED_ASSETNAME_LIST_PATH)
      val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
              .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
      /* println("gotMapper") */
      if (json.exists()) {
          mapper.readValue<MutableMap<String, EtoroAssetFromJson>>(json).forEach { p -> assetsMapNames_[p.key] = p.value}
          /* lightStreamerClient.subscribeByIds( assetsMapNames.toList().map { pair -> pair.first }) */
      }
      /* println("assetsMapNames") */
      /* println(assetsMapNames_) */
      val res = assetsMapNames_[name]
      assetsMapNames_ = mutableMapOf()
      return res
    }

    fun updatePrice(id: String, buy: String?, sell: String?) {
        watchlist[id]?.buy = buy?.toDouble()
        watchlist[id]?.sell = sell?.toDouble()
    }

    fun getPrice(id: String, type: PositionType, discounted: Boolean): Double {
        val asset = watchlist[id]
        print("wlist")
        println(watchlist)
        print("asset")
        println(asset)
        if (asset != null) {
            if (type == PositionType.BUY && asset.buy != null) {
                if (discounted) return asset.askDiscounted
                return asset.buy!!
            } else if (type == PositionType.SELL && asset.sell != null) {
                if (discounted) return asset.bidDiscounted
                return asset.sell!!
            } else {
                throw RuntimeException("None $type price available for id $id")
            }
        }
        throw RuntimeException("Asset with id $id was not found.")
    }

    fun updateMarketStatus(id: String, value: Boolean) {
        watchlist[id]?.marketOpen = value
    }

    fun isMarketOpen(instrumentId: String): Boolean {
        return watchlist[instrumentId]?.marketOpen ?: false
    }

    fun getInstrumentIdByName(name: String): String {
      /* return assetsMapNames[name]?.InstrumentID ?: throw RuntimeException("No InstrumentID was found for the name: $name") */
        return getAssetsMapNames(name.toLowerCase())?.instrumentID ?: throw RuntimeException("No InstrumentID was found for the name: $name")
    }

    fun updateDiscounted(id: String, ask: Double, bid: Double) {
        watchlist[id]?.askDiscounted = ask
        watchlist[id]?.bidDiscounted = bid
    }

    fun removeById(id: String) {
        watchlist.remove(id)
        saveToFile()
    }

    fun  removeByName(name: String) {
        val id = getAssetsMapNames(name.toLowerCase())?.instrumentID !!
        removeById(id)
    }
}
