package com.example.thehealmate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentGroupChatListBinding
import com.example.thehealmate.databinding.ItemGroupCardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class SupportGroup(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    var memberCount: String = "0",
    var isJoined: Boolean = false
)

class GroupChatListFragment : Fragment() {

    private var _binding: FragmentGroupChatListBinding? = null
    private val binding get() = _binding!!
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val groups = mutableListOf<SupportGroup>()
    private lateinit var adapter: GroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GroupAdapter(groups)
        binding.recyclerGroups.layoutManager = LinearLayoutManager(context)
        binding.recyclerGroups.adapter = adapter

        fetchGroups()
    }

    private fun fetchGroups() {
        val uid = auth.currentUser?.uid ?: return

        // Fetch Joined groups for this user
        db.collection("users").document(uid).collection("joined_groups")
            .get()
            .addOnSuccessListener { joinedSnapshot ->
                val joinedGroupIds = joinedSnapshot.documents.map { it.id }.toSet()

                // Fetch All groups
                db.collection("support_groups").get()
                    .addOnSuccessListener { groupsSnapshot ->
                        if (groupsSnapshot.isEmpty) {
                            seedDefaultGroups()
                            return@addOnSuccessListener
                        }

                        groups.clear()
                        for (doc in groupsSnapshot) {
                            val id = doc.id
                            val name = doc.getString("name") ?: ""
                            val desc = doc.getString("description") ?: ""
                            val count = doc.getString("memberCount") ?: "0"
                            val isJoined = joinedGroupIds.contains(id)
                            groups.add(SupportGroup(id, name, desc, count, isJoined))
                        }
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to load support groups", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun seedDefaultGroups() {
        val defaultGroups = listOf(
            SupportGroup("sg_diabetes", "Diabetes Support Group", "Sharing tips and support for managing diabetes.", "156"),
            SupportGroup("sg_heart", "Heart Health", "Community for heart surgery recovery and prevention.", "89"),
            SupportGroup("sg_mental", "Mental Wellness Circle", "A safe space for discussing mental health and stress.", "245"),
            SupportGroup("sg_parents", "New Moms Club", "Support and advice for new parents and prenatal care.", "112"),
            SupportGroup("sg_yoga", "Yoga & Holistic Health", "Daily tips for natural living and mindfulness.", "300")
        )

        for (group in defaultGroups) {
            val data = hashMapOf(
                "name" to group.name,
                "description" to group.description,
                "memberCount" to group.memberCount
            )
            db.collection("support_groups").document(group.id).set(data)
        }
        
        // Re-fetch after seeding
        binding.recyclerGroups.postDelayed({ fetchGroups() }, 1000)
    }

    private inner class GroupAdapter(private val groupList: List<SupportGroup>) :
        RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemGroupCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemGroupCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val group = groupList[position]
            holder.binding.textGroupName.text = group.name
            holder.binding.textGroupDescription.text = group.description
            holder.binding.textMemberCount.text = "${group.memberCount} Members"

            if (group.isJoined) {
                holder.binding.buttonJoin.text = "Open Chat"
            } else {
                holder.binding.buttonJoin.text = "Request to Join"
            }

            holder.binding.buttonJoin.setOnClickListener {
                if (!group.isJoined) {
                    val uid = auth.currentUser?.uid ?: return@setOnClickListener
                    holder.binding.buttonJoin.isEnabled = false
                    
                    // Join logic
                    val joinData = hashMapOf("joinedAt" to com.google.firebase.Timestamp.now())
                    db.collection("users").document(uid).collection("joined_groups").document(group.id).set(joinData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Joined ${group.name}", Toast.LENGTH_SHORT).show()
                            group.isJoined = true
                            notifyItemChanged(position)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to join", Toast.LENGTH_SHORT).show()
                            holder.binding.buttonJoin.isEnabled = true
                        }
                } else {
                    val bundle = Bundle().apply {
                        putString("groupName", group.name)
                        putString("groupId", group.id)
                    }
                    findNavController().navigate(R.id.action_GroupChatListFragment_to_GroupChatFragment, bundle)
                }
            }
        }

        override fun getItemCount() = groupList.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
