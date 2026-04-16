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
            showAppointmentDetail("Kingsway Hospital", "Sep 10, 2023", "Dr. Patil", "Checkup")
        }
        binding.appointment2.setOnClickListener {
            showAppointmentDetail("Care Hospital", "Aug 05, 2023", "Dr. Deshpande", "Fever")
        }
        binding.appointment3.setOnClickListener {
            showAppointmentDetail("Orange City Hospital", "June 20, 2023", "Dr. Kulkarni", "Consultation")
        }
    }

    private fun showAppointmentDetail(hospital: String, date: String, doctor: String, reason: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(hospital)
            .setMessage("Date: $date\nDoctor: $doctor\nReason: $reason\nStatus: Completed")
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}