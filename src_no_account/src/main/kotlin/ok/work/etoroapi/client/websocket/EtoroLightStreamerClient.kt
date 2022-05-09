package ok.work.etoroapi.client.websocket

import com.lightstreamer.client.LightstreamerClient
import com.lightstreamer.client.Subscription
import ok.work.etoroapi.client.UserContext
import ok.work.etoroapi.client.browser.EtoroMetadataService
import ok.work.etoroapi.client.websocket.listeners.EtoroPositionListener
import ok.work.etoroapi.client.websocket.listeners.EtoroPriceListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

val subscriptionFields = arrayOf( "InstrumentID", "Ask", "Bid", "IsMarketOpen",
       "ConversionRateBid", "ConversionRateAsk",
        "AllowBuy", "AllowSell",
        "LastExecution",
        "OfficialClosingPrice", "PriceRateID", "UnitMarginAsk", "UnitMarginBid",
        "MaxPositionUnits", "IsInstrumentActive", "AskDiscounted", "BidDiscounted","UnitMarginAskDiscounted", "UnitMarginBidDiscounted")

@Component
class EtoroLightStreamerClient {

    lateinit var client: LightstreamerClient

    lateinit var realClient: LightstreamerClient

    @Autowired
    lateinit var priceListener: EtoroPriceListener

    @Autowired
    lateinit var positionListener: EtoroPositionListener

    @Autowired
    lateinit var userContext: UserContext

    @Autowired
    private lateinit var metadataService: EtoroMetadataService



    @Autowired
    private lateinit var credentialsService: EtoroMetadataService

    @PostConstruct
    fun init() {

        println("connected")
    }

}
