package md.utm.cloudapp.quotes

data class QuoteStats(
    val total: Int,
    val uniqueAuthors: Int,
    val topAuthors: Map<String, Int>
)