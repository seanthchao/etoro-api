package ok.work.etoroapi.client.browser
import ok.work.etoroapi.client.UserContext

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.By

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.util.*
import javax.annotation.PostConstruct


import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

import kotlin.system.exitProcess

data class EtoroMetadata(val cookies: String, val token: String, val cToken: String, val lsPassword: String, val baseUrl: String, val domain: String, val realCid: String, val demoCid: String, val expirationTime: Date, val deviceid: String)

@Component
class EtoroMetadataService(@Value("\${etoro.baseUrl}") val baseUrl: String, @Value("\${etoro.domain}") val domain: String, @Value("\${server.port}") val port: String) {

    private lateinit var cookies: String
    private lateinit var token: String
    private lateinit var cToken: String
    private lateinit var expirationTime: Date
    private lateinit var driver: ChromeDriver
    private lateinit var opts: ChromeOptions
    private lateinit var realCid: String
    private lateinit var demoCid: String
    private lateinit var deviceid: String
    private lateinit var deviceExsist: String

    @Autowired
    private lateinit var userContext: UserContext
    /* private lateinit var prefs: kotlin.Map */

    @PostConstruct
    fun init() {

        val pathToDriver: String = when {
            System.getProperty("os.name").startsWith("Mac") -> {
                "drivers/mac/chromedriver"
            }
            System.getProperty("os.name").toLowerCase().contains("windows") -> {
                "drivers/windows/chromedriver.exe"
            }
            else -> {
              "drivers/ubuntu/brave/chromedriver_9223"
              /* "/bin/chromedriver_${port}" */
                /* "drivers/ubuntu/chromedriver" */
            }
        }
        var prefs = HashMap<String, Int>()
        opts = ChromeOptions()
        System.setProperty("webdriver.chrome.driver", pathToDriver)
        opts.setExperimentalOption("debuggerAddress", "127.0.0.1:9223")
        /* opts.addArguments("start-maximized") */
        /* opts.addArguments("--no-sandbox")
        opts.addArguments("--disable-dev-shm-usage")
        opts.addArguments("--disable-blink-features=AutomationControlled")
        opts.addArguments("disable-infobars") */
        /* opts.addArguments("--disable-extensions") */
        /* opts.setBinary("/opt/brave.com/brave/brave") */
        /* opts.setBinary("/opt/brave.com/brave/brave_${port}") */
        /* opts.setBinary("/opt/google/chrome/chrome_${port}") */
        /* prefs.put("profile.managed_default_content_settings.images", 2) */
        /* opts.setExperimentalOption("useAutomationExtension", false) */
        /* opts.setExperimentalOption("excludeSwitches",Collections.singletonList("enable-automation")) */
        /* prefs.put("useAutomationExtension", 0) */
        /* opts.setExperimentalOption("prefs", prefs) */
        /* println("Wait...") */
        /* Thread.sleep(30000) */

        login()
        /* println("Test Logout")
        logout()
        println("Logout done.")
        println("Login....")
        login() */
        /* exitProcess(0) */
    }

    fun getDriver(): ChromeDriver {
        System.gc()
        return driver
    }

    fun login() {
        try{
          if (driver.toString().contains("(null)")){
            /* println("Quit the old chromedriver")
            driver.quit()
            driver = ChromeDriver(opts) */
          }
          else{
            println("Existing device...")
            deviceExsist = "1"

          }
        }
        catch(e: Exception){
          driver = ChromeDriver(opts)
          deviceExsist = "0"
        }


        /* driver.get("$baseUrl/login")
        Thread.sleep(2000)
        driver.get("$baseUrl/login")
        Thread.sleep(2000)
        val email = System.getenv("LOGIN")
        val password = System.getenv("PASSWORD")
        if (email == null || password == null) {
            throw RuntimeException("LOGIN and/or PASSWORD environment variables are missing")
        }

        if (deviceExsist == "0"){
          var un = driver.findElementById("username")
          un.clear()
          un.sendKeys(email)
        }
        driver.findElementById("password").sendKeys(password)
        driver.executeScript("document.getElementsByClassName(\"button-default blue-btn\")[0].click()")

        println("waiting 2fa...")
        Thread.sleep(10000) */

        var seconds = 0
        while (true) {
            try {
                token = driver.executeScript("return JSON.parse(atob(window.localStorage.loginData)).stsData_app_1.accessToken;") as String
                cToken = (driver.executeScript("return window.localStorage.cToken") as String).replace("\"", "")
                realCid = driver.executeScript("return JSON.parse(atob(window.localStorage.loginData)).authenticationData.realCid;").toString()
                demoCid = driver.executeScript("return JSON.parse(atob(window.localStorage.loginData)).authenticationData.demoCid;").toString()
                @Suppress("UNCHECKED_CAST")
                deviceid = ""
                println("Token retrieved after %d seconds".format(seconds))
                break
            } catch (e: Exception) {
                if (seconds > 5) {
                    e.printStackTrace()
                    throw RuntimeException("Failed to retrieve token")
                }
                Thread.sleep(1000)
                seconds++
            }
        }
        expirationTime = Date(driver.executeScript("return JSON.parse(atob(window.localStorage.loginData)).stsData_app_1.expirationUnixTimeMs;") as Long)
        println("expires at: $expirationTime")
        val cookiesSet = driver.manage().cookies
        cookies = cookiesSet.toList().joinToString("; ") { cookie -> "${cookie.name}=${cookie.value}" }
    }

    fun logout() {
      driver = getDriver()
      val thisReq = "return JSON.stringify(await (await fetch(\"https://www.etoro.com/api/sts/v2/logout?client_request_id=${userContext.requestId}\", {\n" +
              "  \"headers\": {\n" +
              "    \"accept\": \"application/json, text/plain, */*\",\n" +
              "    \"accept-language\": \"zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
              "    \"content-type\": \"application/json;charset=UTF-8\",\n" +
              "    \"accounttype\": \"Demo\",\n" +
              "    \"applicationidentifier\": \"ReToro\",\n" +
              "    \"applicationversion\": \"384.0.1\",\n" +
              "    \"authorization\": \"${token}\",\n" +
              "    \"sec-fetch-dest\": \"empty\",\n" +
              "    \"sec-fetch-mode\": \"cors\",\n" +
              "    \"sec-fetch-site\": \"same-origin\",\n" +
              "    \"x-csrf-token\": \"${cToken}\"\n" +
              "  },\n" +
              "  \"referrer\": \"https://www.etoro.com/watchlists\",\n" +
              "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
              "  \"method\": \"POST\",\n" +
              "  \"mode\": \"cors\",\n" +
              "})));"
      driver.executeScript(thisReq)
      /* println("body(): $body") */
    }

    fun getMetadata(): EtoroMetadata {
        var long_Time = driver.executeScript("return JSON.parse(atob(window.localStorage.loginData)).stsData_app_1.expirationUnixTimeMs;") as Long
        /* println("long_Time: $long_Time") */
        /* long_Time = long_Time-86385000 */
        expirationTime = Date(long_Time)
        /* expirationTime -= Period.ofHours(23) */
        /* expirationTime -= Period.ofMinutes(59) */
        println("test expiretime: $expirationTime")
        /* var diffInMillies = */
        /* if (Date().after(expirationTime)) {
            println("expired, logout...")
            logout()
            Thread.sleep(5000)
            println("login...")
            login()
        } */
        return EtoroMetadata(
                cookies,
                token,
                cToken,
                """{"UserAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36","ApplicationVersion":"213.0.2","ApplicationName":"ReToro","AccountType":"Demo","ApplicationIdentifier":"ReToro"}""",
                baseUrl,
                domain,
                realCid,
                demoCid,
                expirationTime,
                deviceid
        )
    }
}
