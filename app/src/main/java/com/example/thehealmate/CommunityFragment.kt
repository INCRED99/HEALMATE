package com.example.thehealmate

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thehealmate.databinding.FragmentCommunityBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())

    // Bot responses for auto-reply
    private val botResponses = listOf(
        "That's a great health tip! Thanks for sharing 😊",
        "Has anyone tried yoga for back pain? It helped me a lot!",
        "Remember to drink at least 8 glasses of water daily 💧",
        "I just had my annual checkup. Everything looks good!",
        "Walking 30 minutes a day can really improve your health 🚶",
        "Don't forget to take your vitamins! Vitamin D is essential ☀️",
        "Has anyone visited the new clinic on MG Road? How is it?",
        "Meditation has really helped me manage my stress levels 🧘",
        "Pro tip: Always keep your medical records organized 📋",
        "Stay safe everyone! Flu season is coming up 🤧"
    )

    private val botNames = listOf(
        "Dr. Priya", "HealthBot", "Nurse Aisha", "Dr. Mehta", "WellnessGuru"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChat()
        loadInitialMessages()

        binding.buttonSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun loadInitialMessages() {
        val initialMessages = listOf(
            ChatMessage(
                "Welcome to HealMate Community! 🏥 Ask health questions, share tips, and connect with others.",
                "HealthBot",
                "9:00 AM",
                false
            ),
            ChatMessage(
                "Hi everyone! Remember, an apple a day keeps the doctor away 🍎",
                "Dr. Priya",
                "9:15 AM",
                false
            ),
            ChatMessage(
                "Quick tip: Regular handwashing is the simplest way to prevent infections! 🧼",
                "Nurse Aisha",
                "9:30 AM",
                false
            ),
            ChatMessage(
                "Good morning community! Don't skip breakfast — it boosts your metabolism and keeps you energized throughout the day ☀️",
                "Dr. Mehta",
                "10:00 AM",
                false
            )
        )

        messages.addAll(initialMessages)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()
    }

    private fun sendMessage() {
        val messageText = binding.editMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        // Add sent message
        val sentMessage = ChatMessage(
            message = messageText,
            sender = "You",
            timestamp = currentTime,
            isSent = true
        )
        messages.add(sentMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()

        // Clear input
        binding.editMessage.text?.clear()

        // Auto-reply after 1-3 seconds
        val delay = (1000L..3000L).random()
        handler.postDelayed({
            if (_binding != null) {
                addBotReply()
            }
        }, delay)
    }

    private fun addBotReply() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val botMessage = ChatMessage(
            message = botResponses.random(),
            sender = botNames.random(),
            timestamp = currentTime,
            isSent = false
        )
        messages.add(botMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding.recyclerChat.post {
            if (messages.isNotEmpty()) {
                binding.recyclerChat.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}