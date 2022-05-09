package ok.work.etoroapi.controller

import ok.work.etoroapi.watchlist.Asset
import ok.work.etoroapi.watchlist.Watchlist
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

data class WatchAssetRequest(val param: String)

@RestController
@RequestMapping("/watchlist")
class WatchlistController {

    @Autowired
    lateinit var watchlist: Watchlist

    @GetMapping
    fun getWatchlist(): List<Asset> {
        return watchlist.watchlist()
    }

    @DeleteMapping(value = ["/byName"])
    fun removeFromWatchlistByName(@RequestParam("name") name: String) {
        watchlist.removeByName(name)
    }

    @DeleteMapping(value = ["/byId"])
    fun removeFromWatchlistById(@RequestParam("id") id: String) {
        watchlist.removeById(id)
    }
    @DeleteMapping
    fun resetWatchlist() {
        watchlist.watchlist().forEach {
            watchlist.removeById(it.id)
        }
    }
}
