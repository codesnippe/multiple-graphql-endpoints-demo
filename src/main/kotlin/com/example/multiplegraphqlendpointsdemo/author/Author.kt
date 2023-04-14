package com.example.multiplegraphqlendpointsdemo.author

data class Author(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val privateInfo: String
)
