package com.example.thehealmate

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thehealmate.databinding.FragmentChatSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class ChatSessionFragment : Fragment() {

    private var _binding: FragmentChatSessionBinding? = null
    private val binding get() = _binding!!
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messageListener: ListenerRegistration? = null
    private var doctorName = "Doctor"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doctorName = arguments?.getString("doctorName") ?: "Doctor"
        binding.textChatDoctorName.text = doctorName

        setupChat()

        binding.buttonChatSend.setOnClickListener {
            sendMessage()
        }

        binding.buttonVideoCall.setOnClickListener {
            Toast.makeText(context, "Initiating encrypted video consultation...", Toast.LENGTH_LONG).show()
        }

        loadRealMessages()
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerChatSession.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun loadRealMessages() {
        val user = auth.currentUser
        val userId = user?.uid ?: "unknown"
        val chatId = "${userId}_${doctorName.replace(" ", "_")}"

        messageListener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val text = doc.getString("message") ?: ""
                        val senderName = doc.getString("senderName") ?: "User"
                        val senderId = doc.getString("senderId") ?: ""
                        val time = doc.getString("timeString") ?: ""
                        
                        val isSentByMe = senderId == auth.currentUser?.uid
                        messages.add(ChatMessage(text, senderName, time, isSentByMe))
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        binding.recyclerChatSession.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendMessage() {
        val text = binding.editChatMessage.text.toString().trim()
        if (text.isEmpty()) return

        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val user = auth.currentUser
        val senderName = user?.displayName ?: "Patient"
        val senderId = user?.uid ?: "unknown"

        val messageData = hashMapOf(
            "message" to text,
            "senderName" to senderName,
            "senderId" to senderId,
            "timeString" to timeString,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        val chatId = "${senderId}_${doctorName.replace(" ", "_")}"
        db.collection("chats").document(chatId).collection("messages").add(messageData)

        binding.editChatMessage.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageListener?.remove()
        _binding = null
    }
}
