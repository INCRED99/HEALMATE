package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.thehealmate.databinding.FragmentRecordsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isHospital = arguments?.getBoolean("isHospital") ?: false
        if (isHospital) {
            binding.textTitle.text = "Patient Records (Shared)"
            binding.textSubtitle.text = "Nagpur Central Hospital"
        }

        fetchRealRecords()

        binding.appointment1.setOnClickListener {
            showAppointmentDetail("Kingsway Hospital", "Sep 10, 2023", "Dr. Patil", "Checkup")
        }
        binding.appointment2.setOnClickListener {
            showAppointmentDetail("Care Hospital", "Aug 05, 2023", "Dr. Deshpande", "Fever")
        }
        binding.appointment3.setOnClickListener {
            showAppointmentDetail("Orange City Hospital", "June 20, 2023", "Dr. Kulkarni", "Consultation")
        }
    }

    private fun fetchRealRecords() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Clear placeholder or just append
                    // For simplicity, we'll append to the layout
                    for (document in documents) {
                        val hospital = document.getString("hospitalName") ?: "Unknown Hospital"
                        val date = document.getString("date") ?: ""
                        val slot = document.getString("slot") ?: ""
                        val token = document.get("token")?.toString() ?: ""
                        
                        addRecordToUI(hospital, date, slot, token)
                    }
                }
            }
    }

    private fun addRecordToUI(hospital: String, date: String, slot: String, token: String) {
        val textView = TextView(requireContext()).apply {
            text = "• $hospital - $date ($slot) [Token: $token]"
            setPadding(8, 8, 8, 8)
            setTextColor(resources.getColor(R.color.primary_medical, null))
            textSize = 14f
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                showAppointmentDetail(hospital, "$date $slot", "TBD", "Online Booking (Token: $token)")
            }
        }
        binding.layoutPastAppointments.addView(textView, 0) // Add to top
    }

    private fun showAppointmentDetail(hospital: String, date: String, doctor: String, reason: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(hospital)
            .setMessage("Date: $date\nDoctor: $doctor\nReason: $reason\nStatus: Confirmed")
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}