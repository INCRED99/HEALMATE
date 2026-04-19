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
