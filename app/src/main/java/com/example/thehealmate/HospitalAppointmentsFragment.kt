package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thehealmate.databinding.FragmentHospitalAppointmentsBinding

class HospitalAppointmentsFragment : Fragment() {

    private var _binding: FragmentHospitalAppointmentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHospitalAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mockAppointments = listOf(
            Appointment("Rahul Sharma", "15/05/2024", "10:30 AM", "4821"),
            Appointment("Priya Verma", "15/05/2024", "11:15 AM", "5932"),
            Appointment("Amit Deshmukh", "15/05/2024", "12:00 PM", "1024"),
            Appointment("Sneha Patil", "16/05/2024", "09:45 AM", "8872"),
            Appointment("Vikram Singh", "16/05/2024", "02:30 PM", "3341")
        )

        binding.recyclerAppointments.layoutManager = LinearLayoutManager(context)
        binding.recyclerAppointments.adapter = AppointmentAdapter(mockAppointments)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}