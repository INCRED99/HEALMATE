package com.example.thehealmate

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedViewHolder) {
            holder.bind(message)
        }

        holder.itemView.setOnLongClickListener {
            if (!message.isSent) return@setOnLongClickListener false
            
            val context = holder.itemView.context
            val othersWhoSaw = message.seenBy
                .filterKeys { it != message.senderId }
                .values
                .map { parseSeenEntry(it) }
                .sortedBy { it.first.lowercase(Locale.getDefault()) }
            val seenSection = if (othersWhoSaw.isEmpty()) {
                "No one yet"
            } else {
                othersWhoSaw.joinToString("\n") { (name, seenTime) ->
                    if (seenTime.isBlank()) "• $name" else "• $name ($seenTime)"
                }
            }
            val sentAt = message.timestamp.ifBlank { "Unknown time" }
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Message Info")
                .setMessage("Sent at: $sentAt\n\nSeen by:\n$seenSection")
                .setPositiveButton("OK", null)
                .show()
            true
        }
    }

    override fun getItemCount() = messages.size

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.text_message_sent)
        private val textTime: TextView = itemView.findViewById(R.id.text_time_sent)
        private val imageView: ImageView = itemView.findViewById(R.id.image_sent)

        fun bind(message: ChatMessage) {
            textMessage.text = message.message
            textTime.text = message.timestamp

            if (!message.imageUrl.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                textMessage.visibility = if (message.message.isEmpty()) View.GONE else View.VISIBLE
                loadImageFromUrl(message.imageUrl, imageView)
            } else {
                imageView.visibility = View.GONE
                textMessage.visibility = View.VISIBLE
            }
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textAvatar: TextView = itemView.findViewById(R.id.text_avatar_initial)
        private val textSender: TextView = itemView.findViewById(R.id.text_sender_name)
        private val textMessage: TextView = itemView.findViewById(R.id.text_message_received)
        private val textTime: TextView = itemView.findViewById(R.id.text_time_received)
        private val imageView: ImageView = itemView.findViewById(R.id.image_received)

        fun bind(message: ChatMessage) {
            textSender.text = message.sender
            textMessage.text = message.message
            textTime.text = message.timestamp

            // Show first letter of sender's name as avatar
            val initial = message.sender.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            textAvatar.text = initial

            // Pick a color from sender name hash
            val colors = listOf(
                "#0A66C2", "#E91E63", "#009688", "#FF5722",
                "#673AB7", "#2196F3", "#4CAF50", "#FF9800"
            )
            val colorHex = colors[Math.abs(message.sender.hashCode()) % colors.size]
            textAvatar.setBackgroundResource(R.drawable.bg_avatar_circle)
            (textAvatar.background as? android.graphics.drawable.GradientDrawable)?.setColor(
                android.graphics.Color.parseColor(colorHex)
            )

            if (!message.imageUrl.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                textMessage.visibility = if (message.message.isEmpty()) View.GONE else View.VISIBLE
                loadImageFromUrl(message.imageUrl, imageView)
            } else {
                imageView.visibility = View.GONE
                textMessage.visibility = View.VISIBLE
            }
        }
    }
}

private fun parseSeenEntry(rawEntry: String): Pair<String, String> {
    val trimmed = rawEntry.trim()
    val separator = " at "
    val separatorIndex = trimmed.lastIndexOf(separator)
    return if (separatorIndex > 0 && separatorIndex < trimmed.length - separator.length) {
        val name = trimmed.substring(0, separatorIndex).trim()
        val seenTime = trimmed.substring(separatorIndex + separator.length).trim()
        (if (name.isBlank()) trimmed else name) to seenTime
    } else {
        trimmed to ""
    }
}
private fun loadImageFromUrl(url: String, imageView: ImageView) {
    Thread {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            imageView.post { imageView.setImageBitmap(bitmap) }
        } catch (e: Exception) {
            // Silently ignore image load errors
        }
    }.start()
}
