package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
        val userName = auth.currentUser?.displayName ?: "User"
        val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        val seenPath = "seenBy.$userId"
        db.collection("global_community").document(messageId)
            .update(seenPath, "$userName at $timeNow")
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
                            markMessageAsSeen(msgId)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    scrollToBottom()
                }
            }
    }

    private fun sendMessage() {
        val text = binding.editMessage.text.toString().trim()
        if (text.isEmpty()) return

        val user = auth.currentUser
        val userId = user?.uid ?: "unknown"
        val senderName = user?.displayName ?: "User"
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
        _binding = null
    }
}
