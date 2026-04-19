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
import androidx.recyclerview.widget.LinearLayoutManager
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

        setupChat()

        binding.buttonGroupSend.setOnClickListener { sendTextMessage() }
        binding.buttonAttach.setOnClickListener { openImagePicker() }

        loadRealMessages()
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerGroupChat.apply {
            layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun loadRealMessages() {
        messageListener = db.collection("groups").document(groupId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val text = doc.getString("message") ?: ""
                        val senderName = doc.getString("senderName") ?: "User"
                        val senderId = doc.getString("senderId") ?: ""
                        val time = doc.getString("timeString") ?: ""
                        val imageUrl = doc.getString("imageUrl")
                        val isSentByMe = senderId == auth.currentUser?.uid
                        messages.add(ChatMessage(text, senderName, time, isSentByMe, imageUrl))
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        binding.recyclerGroupChat.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendTextMessage(imageUrl: String? = null) {
        val text = binding.editGroupMessage.text.toString().trim()
        if (text.isEmpty() && imageUrl == null) return

        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val user = auth.currentUser
        val senderName = user?.displayName?.takeIf { it.isNotEmpty() }
            ?: user?.email?.substringBefore("@") ?: "User"
        val senderId = user?.uid ?: ""

        val messageData = hashMapOf(
            "message" to text,
            "senderName" to senderName,
            "senderId" to senderId,
            "timeString" to timeString,
            "timestamp" to Timestamp.now()
        )
        if (imageUrl != null) messageData["imageUrl"] = imageUrl

        db.collection("groups").document(groupId).collection("messages").add(messageData)
        binding.editGroupMessage.text.clear()
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
        _binding = null
    }
}
