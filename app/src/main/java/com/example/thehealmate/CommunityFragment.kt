package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentCommunityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val defaultMessageHint = "Share your thoughts..."
    private var replyToMessage: ChatMessage? = null
    private var currentUserName: String = "User"
    private var isNameResolved = false
    private val pendingSeenMessages = mutableSetOf<String>()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messageListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resolveCurrentUserName()

        setupChat()
        listenForRealTimeMessages()

        binding.buttonSend.setOnClickListener {
            sendMessage()
        }

        binding.editMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && messages.isNotEmpty()) {
                binding.recyclerChat.postDelayed({
                    binding.recyclerChat.smoothScrollToPosition(messages.size - 1)
                }, 200)
            }
        }
    }

    private fun markMessageAsSeen(messageId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = currentUserName.ifBlank { "User" }
        val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        val seenPath = "seenBy.$userId"
        db.collection("global_community").document(messageId)
            .update(seenPath, "$userName at $timeNow")
    }

    private fun resolveCurrentUserName() {
        val user = auth.currentUser ?: return
        
        // Initial fallback from Auth
        val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "User"
        
        currentUserName = fallbackName
        
        // If we have a better name than "User" immediately, we can proceed
        if (fallbackName != "User") {
            isNameResolved = true
            processPendingSeenMessages()
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val profileName = doc.getString("name")?.trim().orEmpty()
                if (profileName.isNotEmpty()) {
                    currentUserName = profileName
                }
                isNameResolved = true
                processPendingSeenMessages()
            }
            .addOnFailureListener {
                isNameResolved = true
                processPendingSeenMessages()
            }
    }

    private fun processPendingSeenMessages() {
        if (pendingSeenMessages.isEmpty()) return
        val toMark = pendingSeenMessages.toList()
        pendingSeenMessages.clear()
        toMark.forEach { markMessageAsSeen(it) }
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
        attachSwipeToReply()
    }

    private fun attachSwipeToReply() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val selectedMessage = messages.getOrNull(position)
                if (selectedMessage != null) {
                    setReplyTarget(selectedMessage)
                }
                chatAdapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerChat)
    }

    private fun setReplyTarget(message: ChatMessage) {
        replyToMessage = message
        val preview = message.message
            .replace("\n", " ")
            .trim()
            .ifBlank { if (!message.imageUrl.isNullOrEmpty()) "Photo" else "Message" }
            .take(40)

        binding.editMessage.hint = "Replying to ${message.sender}: $preview"
        binding.editMessage.requestFocus()
        binding.editMessage.setSelection(binding.editMessage.text?.length ?: 0)
        Toast.makeText(context, "Replying to ${message.sender}", Toast.LENGTH_SHORT).show()
    }

    private fun clearReplyTarget() {
        replyToMessage = null
        binding.editMessage.hint = defaultMessageHint
    }

    private fun buildReplyPrefix(message: ChatMessage): String {
        val preview = message.message
            .replace("\n", " ")
            .trim()
            .ifBlank { if (!message.imageUrl.isNullOrEmpty()) "Photo" else "Message" }
            .take(40)
        return "↪ ${message.sender}: $preview"
    }

    private fun listenForRealTimeMessages() {
        // Fetching from a global community collection
        messageListener = db.collection("global_community")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Failed to load messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val msgId = doc.id
                        val text = doc.getString("message") ?: ""
                        val senderName = doc.getString("senderName") ?: "Anonymous"
                        val senderId = doc.getString("senderId") ?: ""
                        val time = doc.getString("timeString") ?: ""
                        val seenBy = doc.get("seenBy") as? Map<String, String> ?: emptyMap()
                        
                        val isSentByMe = senderId == auth.currentUser?.uid
                        
                        val message = ChatMessage(msgId, text, senderName, senderId, time, isSentByMe, null, seenBy)
                        messages.add(message)

                        // Mark as seen if it's not our own message
                        if (!isSentByMe && !seenBy.containsKey(auth.currentUser?.uid)) {
                            if (isNameResolved) {
                                markMessageAsSeen(msgId)
                            } else {
                                pendingSeenMessages.add(msgId)
                            }
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    scrollToBottom()
                }
            }
    }

    private fun sendMessage() {
        val rawText = binding.editMessage.text.toString().trim()
        if (rawText.isEmpty()) return

        val text = replyToMessage?.let { "${buildReplyPrefix(it)}\n$rawText" } ?: rawText

        val user = auth.currentUser
        val userId = user?.uid ?: "unknown"
        val senderName = currentUserName.ifBlank { "User" }
        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        val messageData = hashMapOf(
            "message" to text,
            "senderName" to senderName,
            "senderId" to userId,
            "timeString" to timeString,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "seenBy" to mapOf(userId to "$senderName at $timeString")
        )

        binding.editMessage.text?.clear()
        clearReplyTarget()

        db.collection("global_community").add(messageData)
            .addOnFailureListener {
                Toast.makeText(context, "Error sending message", Toast.LENGTH_SHORT).show()
            }
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
        messageListener?.remove()
        replyToMessage = null
        _binding = null
    }
}
