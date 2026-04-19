package com.example.thehealmate

data class ChatMessage(
    val message: String,
    val sender: String,
    val timestamp: String,
    val isSent: Boolean,
    val imageUrl: String? = null   // Optional image attachment
)
