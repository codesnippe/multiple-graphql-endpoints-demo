package com.example.multiplegraphqlendpointsdemo.author

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorController {

    @QueryMapping
    fun author(@Argument id: Long): Author {
        return authors.first { it.id == id }
    }

    @QueryMapping
    fun authors(): List<Author> {
        return authors
    }

    companion object {
        val authors = listOf(
            Author(1, "John", "Doe", "secret1"),
            Author(2, "Jane", "Doe", "secret2"),
            Author(3, "Brian", "Bell", "secret3")
        )
    }
}