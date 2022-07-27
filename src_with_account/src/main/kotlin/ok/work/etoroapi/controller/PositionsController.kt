package ok.work.etoroapi.controller

import ok.work.etoroapi.client.EtoroHttpClient
import ok.work.etoroapi.client.EtoroPosition
import ok.work.etoroapi.client.EtoroMirrorPosition
import ok.work.etoroapi.client.EtoroPositionForUpdate
import ok.work.etoroapi.model.Position
import ok.work.etoroapi.model.ofString
import ok.work.etoroapi.transactions.Transaction
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/positions")
class PositionsController {

    @Autowired
    lateinit var httpClient: EtoroHttpClient


    @GetMapping
    fun getPositions(@RequestHeader(defaultValue = "Demo") mode: String): List<EtoroPosition> {
        return httpClient.getPositions(ofString(mode))
    }

    @GetMapping(value = ["/history"])
    fun getHistoryPositions(@RequestParam(defaultValue = "500") limit: String, @RequestParam(defaultValue = "1") page: String, @RequestParam(defaultValue = "") StartTime: String, @RequestHeader(defaultValue = "Demo") mode: String): List<EtoroPosition> {
        return httpClient.getHistoryPositions(limit, page, StartTime, ofString(mode))
    }

    @GetMapping(value = ["/publichistory"])
    fun getPublicHistoryPositions(@RequestParam(defaultValue = "500") limit: String, @RequestParam(defaultValue = "1") page: String, @RequestParam(defaultValue = "") StartTime: String, @RequestParam(defaultValue = "") cid: String, @RequestHeader(defaultValue = "Demo") mode: String): List<EtoroPosition> {
        return httpClient.getPublicHistoryPositions(limit, page, StartTime, cid, ofString(mode))
    }

    @GetMapping(value = ["/liveProfolioMaster"])
    fun getLiveProfolioMaster(@RequestParam(defaultValue = "") cid: String): List<EtoroPosition> {
        return httpClient.getLiveProfolioMaster(cid)
    }

    @GetMapping(value = ["/liveProfolioMirror"])
    fun getLiveProfolioMirror(@RequestParam(defaultValue = "") cid: String): List<EtoroMirrorPosition> {
        return httpClient.getLiveProfolioMirror(cid)
    }

    @GetMapping(value = ["/privateMirror"])
    fun getPrivateMirror(@RequestParam(defaultValue = "") cid: String): List<EtoroPosition> {
        return httpClient.getPrivateMirror(cid)
    }

    @GetMapping(value = ["/liveProfolioDetail"])
    fun getLiveProfolioDetail(@RequestParam(defaultValue = "") cid: String, @RequestParam(defaultValue = "") instrumentID: String): List<EtoroPosition> {
        return httpClient.getLiveProfolioDetail(cid, instrumentID)
    }

    @PostMapping(value = ["/open"])
    fun openPosition(@RequestBody position: Position, @RequestHeader(defaultValue = "Demo") mode: String, @RequestHeader(defaultValue = "positions") order_type: String): Transaction {
        /* System.out.print("position:");
        System.out.println(position); */
        return httpClient.openPosition(position, ofString(mode), order_type)
    }

    @PutMapping(value = ["/update"])
    fun updatePosition(@RequestBody etoroPosition: EtoroPositionForUpdate, @RequestHeader(defaultValue = "Demo") mode: String): Transaction {
        return httpClient.updatePosition(etoroPosition, ofString(mode))
    }

    @PutMapping(value = ["/updateTP"])
    fun updatePositionTP(@RequestBody etoroPosition: EtoroPositionForUpdate, @RequestHeader(defaultValue = "Demo") mode: String): Transaction {
        return httpClient.updatePositionTP(etoroPosition, ofString(mode))
    }

    @DeleteMapping(value = ["/close"])
    fun closePosition(id: String, @RequestHeader(defaultValue = "Demo") mode: String) {
        httpClient.deletePosition(id, ofString(mode))
    }

}
