package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentTelemedicineBinding
import com.example.thehealmate.databinding.ItemDoctorCardBinding
import com.google.firebase.firestore.FirebaseFirestore

data class Doctor(
    val id: String,
    val name: String,
    val specialization: String,
    val experience: String,
    val rating: String,
    val isOnline: Boolean = false
)

class TelemedicineFragment : Fragment() {

    private var _binding: FragmentTelemedicineBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val doctors = mutableListOf<Doctor>()
    private lateinit var doctorAdapter: DoctorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelemedicineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerDoctors.layoutManager = LinearLayoutManager(context)
        doctorAdapter = DoctorAdapter(doctors) { doctor ->
            val bundle = Bundle().apply {
                putString("doctorName", doctor.name)
            }
            findNavController().navigate(R.id.action_TelemedicineFragment_to_ChatSessionFragment, bundle)
        }
        binding.recyclerDoctors.adapter = doctorAdapter

        loadDoctorsFromFirestore()
    }

    private fun loadDoctorsFromFirestore() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("doctors")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                
                if (e != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    doctors.clear()
                    if (snapshot.isEmpty) {
                        // Populate with real data if db empty
                        val initialDoctors = listOf(
                            Doctor("1", "Dr. Alok Gupta", "Cardiologist", "12 Years", "4.9", true),
                            Doctor("2", "Dr. Priya Sharma", "Dermatologist", "8 Years", "4.8", false),
                            Doctor("3", "Dr. Rajesh Mehta", "General Physician", "15 Years", "4.7", true),
                            Doctor("4", "Dr. Sneha Patil", "Pediatrician", "10 Years", "4.9", true),
                            Doctor("5", "Dr. Vikram Singh", "Neurologist", "20 Years", "5.0", false)
                        )
                        initialDoctors.forEach { doc ->
                            val docMap = hashMapOf(
                                "id" to doc.id,
                                "name" to doc.name,
                                "specialization" to doc.specialization,
                                "experience" to doc.experience,
                                "rating" to doc.rating,
                                "isOnline" to doc.isOnline
                            )
                            db.collection("doctors").document(doc.id).set(docMap)
                        }
                    } else {
                        for (doc in snapshot.documents) {
                            doctors.add(
                                Doctor(
                                    doc.getString("id") ?: "",
                                    doc.getString("name") ?: "Doctor",
                                    doc.getString("specialization") ?: "",
                                    doc.getString("experience") ?: "",
                                    doc.getString("rating") ?: "",
                                    doc.getBoolean("isOnline") ?: false
                                )
                            )
                        }
                        doctorAdapter.notifyDataSetChanged()
                    }
                }
            }
    }

    private inner class DoctorAdapter(
        private val doctors: List<Doctor>,
        private val onClick: (Doctor) -> Unit
    ) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemDoctorCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDoctorCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val doctor = doctors[position]
            holder.binding.textDoctorName.text = doctor.name
            holder.binding.textSpecialization.text = doctor.specialization
            holder.binding.textExperience.text = doctor.experience + " Experience"
            holder.binding.textRating.text = doctor.rating
            
            // Real Online/Offline Status
            if (doctor.isOnline) {
                holder.binding.textStatus.text = "Online"
                holder.binding.textStatus.setTextColor(resources.getColor(R.color.success_green, null))
                holder.binding.viewStatusIndicator.setBackgroundResource(R.drawable.bg_status_online)
            } else {
                holder.binding.textStatus.text = "Offline"
                holder.binding.textStatus.setTextColor(resources.getColor(R.color.text_grey, null))
                holder.binding.viewStatusIndicator.setBackgroundResource(R.drawable.bg_status_offline)
            }
            
            holder.binding.buttonConsult.setOnClickListener { onClick(doctor) }
            holder.binding.buttonVideo.setOnClickListener { 
                if (doctor.isOnline) {
                    Toast.makeText(context, "Starting video call with ${doctor.name}...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "${doctor.name} is currently offline. Please book an appointment.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = doctors.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
