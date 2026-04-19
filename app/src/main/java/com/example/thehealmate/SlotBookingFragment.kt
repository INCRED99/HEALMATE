package com.example.thehealmate

import android.app.DatePickerDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentSlotBookingBinding
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SlotBookingFragment : Fragment() {

    private var _binding: FragmentSlotBookingBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: String? = null
    private var selectedSlot: String? = null
    private val db = FirebaseFirestore.getInstance()

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
                    binding.textSelectedDate.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary40, null))
                    binding.textSelectedDate.setTypeface(null, android.graphics.Typeface.BOLD)
                    
                    // Reset selected slot when date changes
                    selectedSlot = null
                    val slotButtons = listOf(binding.slot1, binding.slot2, binding.slot3, binding.slot4)
                    slotButtons.forEach { it.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null)) }
                    
                    checkSlotAvailability(hospitalName, selectedDate!!)
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
                if (button.tag == "full") {
                    Toast.makeText(context, "This slot is full", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Reset all button backgrounds (using default button background)
                slotButtons.forEach { 
                    if (it.tag != "full") {
                        it.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                    }
                }
                // Highlight selected button with primary color
                button.setBackgroundColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary50, null))

                selectedSlot = button.text.toString().replace(Regex(" \\(.*\\)"), "")
                Toast.makeText(context, "Selected slot: $selectedSlot", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonConfirmBooking.setOnClickListener {
            if (selectedDate != null && selectedSlot != null) {
                val token = (1000..9999).random()
                val hospitalName = arguments?.getString("hospitalName") ?: "Hospital"
                
                // Save to Firestore
                saveAppointmentToFirestore(hospitalName, selectedDate!!, selectedSlot!!, token)

                val message = "Appointment Confirmed at $hospitalName for $selectedDate at $selectedSlot. Your Token is: $token"
                
                // Get user's own phone or emergency phone for demo
                val prefs = requireContext().getSharedPreferences("healmate_emergency", android.content.Context.MODE_PRIVATE)
                val phone = prefs.getString("phone", "7007914594")

                if (!phone.isNullOrEmpty()) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        sendSms(phone, message)
                        // Check if reminder is needed (less than 24h)
                        checkAndSendReminder(phone, hospitalName, selectedDate!!, selectedSlot!!)
                    } else {
                        Toast.makeText(context, "Please grant SMS permission in SOS settings", Toast.LENGTH_SHORT).show()
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 101)
                    }
                }
                
                Toast.makeText(context, "Appointment Confirmed! Token: $token", Toast.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.FirstFragment, false)
            } else {
                Toast.makeText(context, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkSlotAvailability(hospital: String, date: String) {
        val slotButtons = listOf(binding.slot1, binding.slot2, binding.slot3, binding.slot4)
        val maxSlots = 2
        
        db.collection("appointments")
            .whereEqualTo("hospitalName", hospital)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                val bookedSlotsMap = documents.groupBy { it.getString("slot") ?: "" }
                
                slotButtons.forEach { button ->
                    val slotText = button.text.toString().replace(Regex(" \\(.*\\)"), "")
                    val bookingsCount = bookedSlotsMap[slotText]?.size ?: 0
                    val remaining = maxSlots - bookingsCount
                    
                    if (remaining <= 0) {
                        button.isEnabled = false
                        button.tag = "full"
                        button.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                        button.text = "$slotText (Full)"
                    } else {
                        button.isEnabled = true
                        button.tag = null
                        button.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                        button.text = "$slotText ($remaining Left)"
                    }
                }
            }
    }

    private fun saveAppointmentToFirestore(hospital: String, date: String, slot: String, token: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        val appointment = hashMapOf(
            "hospitalName" to hospital,
            "date" to date,
            "slot" to slot,
            "token" to token,
            "userId" to userId,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "type" to "Appointment"
        )

        db.collection("appointments").add(appointment)
        
        // Also add to a general 'records' collection for the "My Records" section
        db.collection("users").document(userId).collection("records").add(appointment)
    }

    private fun sendSms(phone: String, message: String) {
        try {
            val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, message, null, null)
            Toast.makeText(context, "SMS sent to $phone", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SlotBooking", "SMS failed", e)
        }
    }

    private fun checkAndSendReminder(phone: String, hospital: String, dateStr: String, slotStr: String) {
        try {
            // Include slot time for accurate reminder check
            // Format for dateStr is d/M/yyyy (e.g. 5/3/2025)
            // Format for slotStr is hh:mm a (e.g. 10:00 AM)
            val fullDateTime = "$dateStr $slotStr"
            val sdf = SimpleDateFormat("d/M/yyyy h:mm a", Locale.getDefault())
            val appointmentDateTime = sdf.parse(fullDateTime)
            val now = Calendar.getInstance().time
            
            if (appointmentDateTime != null) {
                val diff = appointmentDateTime.time - now.time
                val diffInHours = diff.toDouble() / (1000.0 * 60.0 * 60.0)
                
                Log.d("SlotBooking", "Checking reminder: Appointment at $appointmentDateTime, Now: $now, Diff hours: $diffInHours")
                
                // If appointment is in the future and within 24 hours
                // Ensure it's not in the past (diffInHours >= 0)
                if (diffInHours >= 0 && diffInHours <= 24.0) {
                    val reminderMsg = "Reminder: You have an appointment at $hospital within 24 hours ($dateStr at $slotStr)."
                    sendSms(phone, reminderMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("SlotBooking", "Reminder check failed: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}