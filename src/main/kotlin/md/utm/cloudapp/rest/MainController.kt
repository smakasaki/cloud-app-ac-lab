package md.utm.cloudapp.rest

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController {

    private val log = LoggerFactory.getLogger(MainController::class.java)

    @GetMapping("/")
    fun root(): Map<String, String> {
        log.debug("GET / -> root endpoint accessed")
        return mapOf(
            "app" to "cloud-application",
            "status" to "ok",
            "endpoints" to "/quotes, /quotes/{id}, /quotes/random, /quotes/author/{author}, /quotes/stats"
        )
    }
}