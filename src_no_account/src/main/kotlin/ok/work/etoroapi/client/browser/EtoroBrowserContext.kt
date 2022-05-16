package ok.work.etoroapi.client.browser

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.DesiredCapabilities
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.util.*
import javax.annotation.PostConstruct


data class EtoroMetadata(val cookies: String, val token: String, val cToken: String, val lsPassword: String, val baseUrl: String, val domain: String, val realCid: String, val demoCid: String, val expirationTime: Date, val deviceid: String)

@Component
class EtoroMetadataService(@Value("\${etoro.baseUrl}") val baseUrl: String, @Value("\${etoro.domain}") val domain: String, @Value("\${server.port}") val port: String) {

    private lateinit var cookies: String
    private lateinit var token: String
    private lateinit var cToken: String
    private lateinit var expirationTime: Date
    private lateinit var driver: ChromeDriver
    private lateinit var opts: ChromeOptions
    private lateinit var capabilities: DesiredCapabilities
    private lateinit var realCid: String
    private lateinit var demoCid: String
    private lateinit var deviceid: String
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
              /* "drivers/ubuntu/brave/chromedriver_99" */
              "bin/chromedriver_${port}"
              /* "drivers/ubuntu/chromedriver_${port}" */
                /* "drivers/ubuntu/chromedriver" */
            }
        }
        var prefs = HashMap<String, Any>()
        /* var prefs_cap = HashMap<String, Any>() */
        opts = ChromeOptions()
        opts.setExperimentalOption("debuggerAddress", "127.0.0.1:9224")
        System.setProperty("webdriver.chrome.driver", pathToDriver)
        /* opts.addArguments("start-maximized")
        opts.addArguments("--incognito") */
        /* opts.addArguments("--no-sandbox") */
        /* opts.addArguments("--disable-dev-shm-usage") */
        /* opts.addArguments("--disable-blink-features=AutomationControlled")
        opts.setExperimentalOption("excludeSwitches",Collections.singletonList("enable-automation")) */
        /* opts.addArguments("disable-infobars") */
        /* opts.addArguments("--disable-extensions") */
        /* opts.setBinary("/usr/bin/brave-browser") */
        /* opts.setBinary("/opt/brave.com/brave/brave") */
        /* opts.setBinary("/opt/google/chrome/chrome_${port}") */
        /* prefs.put("profile.managed_default_content_settings.images", 2)

        opts.setCapability( "browser.startup.page", 1 )
        opts.setCapability( "browser.startup.homepage", "https://www.google.com" )
        opts.setExperimentalOption("prefs", prefs) */
        /* opts.setCapability("prefs", prefs) */
        /* var capabilities = DesiredCapabilities() */
        /* capabilities.setCapability(ChromeOptions.CAPABILITY, opts) */


        /* WebDriver driver = new ChromeDriver(capabilities); */
        login()

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
        }
        catch(e: Exception){
          driver = ChromeDriver(opts)
        }


        /* driver.get("$baseUrl/login")
        Thread.sleep(5)
        val email = System.getenv("LOGIN")
        val password = System.getenv("PASSWORD")
        if (email == null || password == null) {
            throw RuntimeException("LOGIN and/or PASSWORD environment variables are missing")
        }
        driver.findElementById("username").sendKeys(email)
        driver.findElementById("password").sendKeys(password)
        driver.findElementByClassName("blue-btn").click() */
        /* driver.get("https://www.etoro.com/markets/btc") */
        Thread.sleep(1000)
        /* var seconds = 0
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
        cookies = cookiesSet.toList().joinToString("; ") { cookie -> "${cookie.name}=${cookie.value}" } */
        /* println("cookies: $cookies") */
        /* println("cToken: $cToken") */

        //driver.quit()
    }

}
