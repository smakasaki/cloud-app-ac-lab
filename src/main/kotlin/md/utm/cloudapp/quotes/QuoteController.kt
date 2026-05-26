package md.utm.cloudapp.quotes

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/quotes")
class QuoteController(private val quoteRepository: QuoteRepository) {

    @GetMapping
    fun all(): List<Quote> = quoteRepository.findAll()

    @GetMapping("/{id}")
    fun byId(@PathVariable id: Long): ResponseEntity<Quote> =
        quoteRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @GetMapping("/random")
    fun random(): ResponseEntity<Quote> {
        val all = quoteRepository.findAll()
        if (all.isEmpty()) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(all.random())
    }

    @GetMapping("/author/{author}")
    fun byAuthor(@PathVariable author: String): List<Quote> =
        quoteRepository.findByAuthorContainingIgnoreCase(author)

    @GetMapping("/stats")
    fun stats(): QuoteStats {
        val all = quoteRepository.findAll()
        return QuoteStats(
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
    }

    @PostMapping
    fun create(@RequestBody body: Map<String, String>): ResponseEntity<Quote> {
        val text = body["text"] ?: return ResponseEntity.badRequest().build()
        val author = body["author"] ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(quoteRepository.save(Quote(text = text, author = author)))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        if (!quoteRepository.existsById(id)) return ResponseEntity.notFound().build()
        quoteRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}