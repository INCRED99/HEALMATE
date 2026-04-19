package com.example.thehealmate

data class ChatMessage(
    val id: String = "",
    val message: String = "",
    val sender: String = "",
    val senderId: String = "",
    val timestamp: String = "",
    val isSent: Boolean = false,
    val imageUrl: String? = null,
    val seenBy: Map<String, String> = emptyMap() // Map of UserId to TimeSeen
)
