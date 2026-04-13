package com.example.thehealmate

data class ChatMessage(
    val message: String,
    val sender: String,
    val timestamp: String,
    val isSent: Boolean
)
