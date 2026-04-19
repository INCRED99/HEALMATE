package com.example.thehealmate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentGroupChatBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var groupName: String = "Support Group"
    private var groupId: String = "default_group"
    private val defaultGroupMessageHint = "Share with the group..."
    private var replyToMessage: ChatMessage? = null
    private var currentUserName: String = "User"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var messageListener: ListenerRegistration? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let { uploadImageAndSend(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupName = arguments?.getString("groupName") ?: "Support Group"
        groupId = arguments?.getString("groupId") ?: "default_group"
        binding.textGroupChatName.text = groupName
        resolveCurrentUserName()

        setupChat()

        binding.buttonGroupSend.setOnClickListener { sendTextMessage() }
        binding.buttonAttach.setOnClickListener { openImagePicker() }

        binding.editGroupMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && messages.isNotEmpty()) {
                binding.recyclerGroupChat.postDelayed({
                    binding.recyclerGroupChat.smoothScrollToPosition(messages.size - 1)
                }, 200)
            }
        }

        loadRealMessages()
    }

    private fun markMessageAsSeen(messageId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = currentUserName.ifBlank { "User" }
        val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        val seenPath = "seenBy.$userId"
        db.collection("groups").document(groupId).collection("messages").document(messageId)
            .update(seenPath, "$userName at $timeNow")
    }

    private fun resolveCurrentUserName() {
        val user = auth.currentUser ?: return
        currentUserName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "User"

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val profileName = doc.getString("name")?.trim().orEmpty()
                if (profileName.isNotEmpty()) {
                    currentUserName = profileName
                }
            }
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerGroupChat.apply {
            layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
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
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerGroupChat)
    }

    private fun setReplyTarget(message: ChatMessage) {
        replyToMessage = message
        val preview = message.message
            .replace("\n", " ")
            .trim()
            .ifBlank { if (!message.imageUrl.isNullOrEmpty()) "Photo" else "Message" }
            .take(40)

        binding.editGroupMessage.hint = "Replying to ${message.sender}: $preview"
        binding.editGroupMessage.requestFocus()
        binding.editGroupMessage.setSelection(binding.editGroupMessage.text?.length ?: 0)
        Toast.makeText(context, "Replying to ${message.sender}", Toast.LENGTH_SHORT).show()
    }

    private fun clearReplyTarget() {
        replyToMessage = null
        binding.editGroupMessage.hint = defaultGroupMessageHint
    }

    private fun buildReplyPrefix(message: ChatMessage): String {
        val preview = message.message
            .replace("\n", " ")
            .trim()
            .ifBlank { if (!message.imageUrl.isNullOrEmpty()) "Photo" else "Message" }
            .take(40)
        return "↪ ${message.sender}: $preview"
    }

    private fun loadRealMessages() {
        messageListener = db.collection("groups").document(groupId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val msgId = doc.id
                        val text = doc.getString("message") ?: ""
                        val senderName = doc.getString("senderName") ?: "User"
                        val senderId = doc.getString("senderId") ?: ""
                        val time = doc.getString("timeString") ?: ""
                        val imageUrl = doc.getString("imageUrl")
                        val seenBy = doc.get("seenBy") as? Map<String, String> ?: emptyMap()
                        
                        val isSentByMe = senderId == auth.currentUser?.uid
                        
                        val message = ChatMessage(msgId, text, senderName, senderId, time, isSentByMe, imageUrl, seenBy)
                        messages.add(message)
                        
                        // Mark as seen if it's not our own message
                        if (!isSentByMe && !seenBy.containsKey(auth.currentUser?.uid)) {
                            markMessageAsSeen(msgId)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        binding.recyclerGroupChat.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendTextMessage(imageUrl: String? = null) {
        val rawText = binding.editGroupMessage.text.toString().trim()
        val replyPrefix = replyToMessage?.let { buildReplyPrefix(it) }
        val text = when {
            rawText.isNotEmpty() && !replyPrefix.isNullOrEmpty() -> "$replyPrefix\n$rawText"
            rawText.isNotEmpty() -> rawText
            imageUrl != null && !replyPrefix.isNullOrEmpty() -> replyPrefix
            else -> rawText
        }
        if (text.isEmpty() && imageUrl == null) return

        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val user = auth.currentUser
        val userId = user?.uid ?: ""
        val senderName = currentUserName.ifBlank { "User" }

        val messageData = hashMapOf(
            "message" to text,
            "senderName" to senderName,
            "senderId" to userId,
            "timeString" to timeString,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "imageUrl" to imageUrl,
            "seenBy" to mapOf(userId to "$senderName at $timeString")
        )

        db.collection("groups").document(groupId).collection("messages").add(messageData)
        binding.editGroupMessage.text.clear()
        clearReplyTarget()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImageAndSend(uri: Uri) {
        Toast.makeText(context, "Uploading image…", Toast.LENGTH_SHORT).show()
        val filename = "group_media/${groupId}/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(filename)
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    sendTextMessage(imageUrl = downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageListener?.remove()
        replyToMessage = null
        _binding = null
    }
}
