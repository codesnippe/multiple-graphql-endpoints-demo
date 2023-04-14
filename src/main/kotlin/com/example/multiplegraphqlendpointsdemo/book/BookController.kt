package com.example.multiplegraphqlendpointsdemo.book

import java.time.LocalDate
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class BookController {

    @QueryMapping
    fun book(@Argument id: Long): Book {
        return books.first { it.id == id }
    }

    @QueryMapping
    fun books(): List<Book> {
        return books
    }

    companion object {
        val books = listOf(
            Book(1, "Book1", LocalDate.now().minusYears(10), "4/5", 1),
            Book(2, "Book2", LocalDate.now().minusYears(5), "5/5",2),
            Book(3, "Book3", LocalDate.now().minusYears(1), "2/5",3)
        )
    }
}