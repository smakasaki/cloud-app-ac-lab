package md.utm.cloudapp.quotes

import jakarta.persistence.*

@Entity
@Table(name = "quotes")
class Quote(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val text: String,
    val author: String
)