package com.example.thehealmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

        fun bind(message: ChatMessage) {
            textMessage.text = message.message
            textTime.text = message.timestamp
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSender: TextView = itemView.findViewById(R.id.text_sender_name)
        private val textMessage: TextView = itemView.findViewById(R.id.text_message_received)
        private val textTime: TextView = itemView.findViewById(R.id.text_time_received)

        fun bind(message: ChatMessage) {
            textSender.text = message.sender
            textMessage.text = message.message
            textTime.text = message.timestamp
        }
    }
}
