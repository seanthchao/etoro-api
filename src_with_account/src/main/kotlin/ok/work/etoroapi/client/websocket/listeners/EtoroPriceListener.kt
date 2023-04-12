package ok.work.etoroapi.client.websocket.listeners

import com.lightstreamer.client.ItemUpdate
import com.lightstreamer.client.Subscription
import ok.work.etoroapi.client.websocket.subscriptionFields
import ok.work.etoroapi.watchlist.Watchlist
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import java.io.IOException



@Component
class EtoroPriceListener : EtoroListener() {

    @Autowired
    lateinit var watchlist: Watchlist

    @Autowired
    lateinit var simpMessagingTemplate: SimpMessagingTemplate

    override fun onListenEnd(subscription: Subscription) {
        println("onListenEnd")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemUpdate(itemUpdate: ItemUpdate) {
        val id = itemUpdate.itemName.replace("instrument:", "")

        if (watchlist.getById(id) !== null) {
            watchlist.updatePrice(id, itemUpdate.getValue(2), itemUpdate.getValue(3))
            // For some market (HKG50), the market status could be wrongly set. we assume market open when there is price update
/*            watchlist.updateMarketStatus(id, true)*/
            watchlist.updateMarketStatus(id, itemUpdate.getValue(4)!!.toBoolean())

            watchlist.updateDiscounted(id, itemUpdate.getValue(16)!!.toDouble(), itemUpdate.getValue(17)!!.toDouble())

            val log = StringBuilder()
            var update = HashMap<String, String>()
            for (i in 1..subscriptionFields.size) {
              if (i == 1 || i == 9 || i == 16 || i == 17){
                update.put(subscriptionFields[i-1], itemUpdate.getValue(i))
                log.append("${itemUpdate.getValue(i)} | ")
              }
            }
            println(log.toString())
            /* if (itemUpdate.getValue(16) == itemUpdate.getValue(17)){
              val now = LocalDateTime.now()
              val formatter = DateTimeFormatter.ISO_DATE_TIME
              val formatted = now.format(formatter)
              val dt = formatted.split(".")[0]
              log.append("$dt | ")

              val fileName = "possible_assets.txt"

              try {
                  appendToFile(fileName, log.toString())
                  println("Content written to $fileName")
              } catch (e: IOException) {
                  println("Error occurred while appending content: ${e.message}")
              }
            } */
            simpMessagingTemplate.convertAndSend("/api/price", update)
        }
    }

    @Throws(IOException::class)
    fun appendToFile(filePath: String, content: String) {
        val file = File(filePath)

        if (!file.exists()) {
            file.createNewFile()
        }

        file.appendText("\n$content")
    }
}
