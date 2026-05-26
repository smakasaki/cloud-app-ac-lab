package md.utm.cloudapp.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController {

    @GetMapping("/")
    fun root(): Map<String, String> = mapOf(
        "app" to "cloud-app",
        "status" to "ok",
        "endpoints" to "/quotes, /quotes/{id}, /quotes/random, /quotes/author/{author}, /quotes/stats"
    )
}