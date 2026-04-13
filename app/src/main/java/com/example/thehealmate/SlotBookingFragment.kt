package com.example.thehealmate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentSlotBookingBinding
import java.util.Calendar

class SlotBookingFragment : Fragment() {

    private var _binding: FragmentSlotBookingBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: String? = null
    private var selectedSlot: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlotBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hospitalName = arguments?.getString("hospitalName") ?: "Hospital"
        binding.textHospitalName.text = hospitalName

        binding.buttonSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = "$dayOfMonth/${month + 1}/$year"
                    binding.textSelectedDate.text = "Date: $selectedDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val slotButtons = listOf(binding.slot1, binding.slot2, binding.slot3, binding.slot4)
        slotButtons.forEach { button ->
            button.setOnClickListener {
                selectedSlot = button.text.toString()
                Toast.makeText(context, "Selected slot: $selectedSlot", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonConfirmBooking.setOnClickListener {
            if (selectedDate != null && selectedSlot != null) {
                val token = (1000..9999).random()
                Toast.makeText(context, "Appointment Confirmed! SMS sent with Token: $token", Toast.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.FirstFragment, false)
            } else {
                Toast.makeText(context, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}