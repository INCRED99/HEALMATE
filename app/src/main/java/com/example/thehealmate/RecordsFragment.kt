package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.thehealmate.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appointment1.setOnClickListener {
            showAppointmentDetails("Kingsway Hospital", "Sep 10, 2023", "Dr. Amit Patil", "Regular Checkup", "Everything is normal. Advised to stay hydrated.")
        }

        binding.appointment2.setOnClickListener {
            showAppointmentDetails("Care Hospital", "Aug 05, 2023", "Dr. Snehal Kulkarni", "Fever", "Diagnosed with viral fever. Prescribed rest and antibiotics.")
        }

        binding.appointment3.setOnClickListener {
            showAppointmentDetails("Orange City Hospital", "June 20, 2023", "Dr. Vinay Deshpande", "Fracture Follow-up", "Bone healing well. Suggested light exercises.")
        }
    }

    private fun showAppointmentDetails(hospital: String, date: String, doctor: String, reason: String, notes: String) {
        val message = "Hospital: $hospital\nDate: $date\nDoctor: $doctor\nReason: $reason\n\nNotes: $notes"
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Appointment Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}