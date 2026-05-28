package md.utm.cloudapp.quotes

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/quotes")
class QuoteController(private val quoteRepository: QuoteRepository) {

    private val log = LoggerFactory.getLogger(QuoteController::class.java)

    @GetMapping
    fun all(): List<Quote> {
        val quotes = quoteRepository.findAll()
        log.info("GET /quotes -> {} quotes returned", quotes.size)
        return quotes
    }

    @GetMapping("/{id}")
    fun byId(@PathVariable id: Long): ResponseEntity<Quote> {
        val result = quoteRepository.findById(id)
        return if (result.isPresent) {
            log.info("GET /quotes/{} -> found", id)
            ResponseEntity.ok(result.get())
        } else {
            log.warn("GET /quotes/{} -> not found", id)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/random")
    fun random(): ResponseEntity<Quote> {
        val all = quoteRepository.findAll()
        if (all.isEmpty()) {
            log.warn("GET /quotes/random -> no quotes in database")
            return ResponseEntity.notFound().build()
        }
        val quote = all.random()
        log.info("GET /quotes/random -> returned quote id={}", quote.id)
        return ResponseEntity.ok(quote)
    }

    @GetMapping("/author/{author}")
    fun byAuthor(@PathVariable author: String): List<Quote> {
        val quotes = quoteRepository.findByAuthorContainingIgnoreCase(author)
        log.info("GET /quotes/author/{} -> {} quotes found", author, quotes.size)
        return quotes
    }

    @GetMapping("/stats")
    fun stats(): QuoteStats {
        val all = quoteRepository.findAll()
        val stats = QuoteStats(
            total = all.size,
            uniqueAuthors = all.map { it.author }.distinct().size,
            topAuthors = all
                .groupingBy { it.author }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .associate { it.key to it.value }
        )
        log.info("GET /quotes/stats -> total={}, uniqueAuthors={}", stats.total, stats.uniqueAuthors)
        return stats
    }

    @PostMapping
    fun create(@RequestBody body: Map<String, String>): ResponseEntity<Quote> {
        val text = body["text"]
        val author = body["author"]
        if (text == null || author == null) {
            log.warn("POST /quotes -> bad request: text present={}, author present={}", text != null, author != null)
            return ResponseEntity.badRequest().build()
        }
        val saved = quoteRepository.save(Quote(text = text, author = author))
        log.info("POST /quotes -> created quote id={}, author='{}'", saved.id, author)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        if (!quoteRepository.existsById(id)) {
            log.warn("DELETE /quotes/{} -> not found", id)
            return ResponseEntity.notFound().build()
        }
        quoteRepository.deleteById(id)
        log.info("DELETE /quotes/{} -> deleted", id)
        return ResponseEntity.noContent().build()
    }
}