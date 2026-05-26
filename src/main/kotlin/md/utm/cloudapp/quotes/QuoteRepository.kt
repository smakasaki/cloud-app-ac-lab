package md.utm.cloudapp.quotes

import org.springframework.data.jpa.repository.JpaRepository

interface QuoteRepository : JpaRepository<Quote, Long> {
    fun findByAuthorContainingIgnoreCase(author: String): List<Quote>
}