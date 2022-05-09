package ok.work.etoroapi.client.browser

import ok.work.etoroapi.client.UserContext
import ok.work.etoroapi.client.clientTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BrowserHttpClient {

    @Autowired
    lateinit var metadataService: EtoroMetadataService

    @Autowired
    lateinit var userContext: UserContext

    fun fetchPublicHistory(limit: String = "100",
                           page: String = "1",
                           StartTime: String, cid: String, mode: String): String {
        val driver = metadataService.getDriver()
        val req = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-data-real/history/public/credit/flat?CID=$cid&ItemsPerPage=$limit&PageNumber=$page&StartTime=$StartTime\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\",\n" +
                "    \"accept-language\": \"en-GB,en-US;q=0.9,en;q=0.8\",\n" +
                "    \"accounttype\": \"${mode}\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"332.0.5\",\n" +
                "    \"sec-ch-ua\": \"\\\" Not;A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"91\\\", \\\"Chromium\\\";v=\\\"91\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/portfolio/history\",\n" +
                "  \"referrerPolicy\": \"strict-origin-when-cross-origin\",\n" +
                "  \"body\": null,\n" +
                "  \"method\": \"GET\",\n" +
                "  \"mode\": \"cors\",\n" +
                "  \"credentials\": \"include\"\n" +
                "})).json());"
        return driver.executeScript(req) as String
    }

    fun fetchLiveProfolioDetail(cid: String, instrumentID: String): String {
        val driver = metadataService.getDriver()
        val req = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-data-real/live/public/positions?InstrumentID=$instrumentID&cid=$cid&client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\",\n" +
                "    \"accept-language\": \"en-GB,en-US;q=0.9,en;q=0.8\",\n" +
                "    \"accounttype\": \"real\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"332.0.5\",\n" +
                "    \"sec-ch-ua\": \"\\\" Not;A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"91\\\", \\\"Chromium\\\";v=\\\"91\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/portfolio/history\",\n" +
                "  \"referrerPolicy\": \"strict-origin-when-cross-origin\",\n" +
                "  \"body\": null,\n" +
                "  \"method\": \"GET\",\n" +
                "  \"mode\": \"cors\",\n" +
                "  \"credentials\": \"include\"\n" +
                "})).json());"
        return driver.executeScript(req) as String
    }

    fun fetchLiveProfolioMaster(cid: String): String {
        val driver = metadataService.getDriver()
        val req = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-data-real/live/public/portfolios?CID=$cid&client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\",\n" +
                "    \"accept-language\": \"en-GB,en-US;q=0.9,en;q=0.8\",\n" +
                "    \"accounttype\": \"real\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"332.0.5\",\n" +
                "    \"sec-ch-ua\": \"\\\" Not;A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"91\\\", \\\"Chromium\\\";v=\\\"91\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/portfolio/history\",\n" +
                "  \"referrerPolicy\": \"strict-origin-when-cross-origin\",\n" +
                "  \"body\": null,\n" +
                "  \"method\": \"GET\",\n" +
                "  \"mode\": \"cors\",\n" +
                "  \"credentials\": \"include\"\n" +
                "})).json());"
        return driver.executeScript(req) as String
    }

    fun fetchPrivateMirror(cid: String): String {
        val driver = metadataService.getDriver()
        val req = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/sapi/trade-data-real/live/public/mirrors/$cid?client_request_id=${userContext.requestId}\", {\n" +
                "  \"headers\": {\n" +
                "    \"accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\",\n" +
                "    \"accept-language\": \"en-GB,en-US;q=0.9,en;q=0.8\",\n" +
                "    \"accounttype\": \"real\",\n" +
                "    \"applicationidentifier\": \"ReToro\",\n" +
                "    \"applicationversion\": \"332.0.5\",\n" +
                "    \"sec-ch-ua\": \"\\\" Not;A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"91\\\", \\\"Chromium\\\";v=\\\"91\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-origin\",\n" +
                "  },\n" +
                "  \"referrer\": \"https://www.etoro.com/portfolio/history\",\n" +
                "  \"referrerPolicy\": \"strict-origin-when-cross-origin\",\n" +
                "  \"body\": null,\n" +
                "  \"method\": \"GET\",\n" +
                "  \"mode\": \"cors\",\n" +
                "  \"credentials\": \"include\"\n" +
                "})).json());"
        return driver.executeScript(req) as String
    }
}
