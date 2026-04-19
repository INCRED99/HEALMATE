package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.thehealmate.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfile()

        binding.buttonSave.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSave.isEnabled = false

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("emergencyContact") ?: ""
                    val role = document.getString("role")?.uppercase() ?: "PATIENT"

                    binding.editName.setText(name)
                    binding.editEmail.setText(email)
                    binding.editPhone.setText(phone)
                    binding.textNameHeader.text = name
                    binding.textRoleBadge.text = role
                }
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
            }
    }

    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val name = binding.editName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.layoutName.error = "Name cannot be empty"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSave.isEnabled = false

        val updates = hashMapOf(
            "name" to name,
            "emergencyContact" to phone
        )

        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                binding.textNameHeader.text = name
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
