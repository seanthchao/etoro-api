package ok.work.etoroapi.client

import ok.work.etoroapi.client.browser.BrowserHttpClient
import ok.work.etoroapi.client.browser.EtoroMetadataService
import ok.work.etoroapi.model.TradingMode
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.util.*
import javax.annotation.PostConstruct

@Component
class UserContext {

    lateinit var exchangeToken: String
    lateinit var requestId: String
    lateinit var demogcid: String
    lateinit var realgcid: String
    private lateinit var userdata: JSONObject

    private val client: HttpClient = HttpClient.newHttpClient()

    @Autowired
    private lateinit var metadataService: EtoroMetadataService

    @Autowired
    private lateinit var browserHttpClient: BrowserHttpClient

    @PostConstruct
    fun setupAuthorizationContext() {
        requestId = UUID.randomUUID().toString().toLowerCase()
        val token = System.getenv("TOKEN")
        if (token != null) {
            exchangeToken = token
        } else {
            exchangeToken = ""
        }
        /* val accountData = browserHttpClient.fetchAccountData("Demo")
        println(accountData) */
        //getAccountData(TradingMode.REAL)
    }


}
