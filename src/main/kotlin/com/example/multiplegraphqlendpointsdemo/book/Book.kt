package com.example.multiplegraphqlendpointsdemo.book

import java.time.LocalDate

data class Book(
    val id: Long,
    val name: String,
    val date: LocalDate,
    val review: String,
    val authorId: Long
)
