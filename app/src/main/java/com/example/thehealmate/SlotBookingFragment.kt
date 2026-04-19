package com.example.thehealmate

import android.app.DatePickerDialog
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentSlotBookingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class SlotBookingFragment : Fragment() {

    private var _binding: FragmentSlotBookingBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: String? = null
    private var selectedSlot: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val slotMaxLimit = 2

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
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val slotButtons = listOf(binding.slot1, binding.slot2, binding.slot3, binding.slot4)
        
        binding.buttonSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = "$dayOfMonth/${month + 1}/$year"
                    binding.textSelectedDate.text = "Date: $selectedDate"
                    binding.textSelectedDate.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary40, null))
                    binding.textSelectedDate.setTypeface(null, android.graphics.Typeface.BOLD)
                    
                    // After date selection, refresh slot availability
                    updateSlotAvailability(hospitalName, slotButtons)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        slotButtons.forEach { button ->
            button.setOnClickListener {
                if (selectedDate == null) {
                    Toast.makeText(context, "Please select a date first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Reset all button backgrounds
                slotButtons.forEach { 
                    if (it.isEnabled) {
                        it.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                        it.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary40, null))
                    }
                }
                // Highlight selected button
                button.setBackgroundColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary50, null))
                button.setTextColor(resources.getColor(android.R.color.white, null))

                selectedSlot = button.tag as? String ?: button.text.toString().split("\n")[0]
            }
        }

        binding.buttonConfirmBooking.setOnClickListener {
            if (selectedDate != null && selectedSlot != null) {
                confirmBooking()
            } else {
                Toast.makeText(context, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSlotAvailability(hospitalName: String, buttons: List<com.google.android.material.button.MaterialButton>) {
        if (selectedDate == null) return

        buttons.forEach { button ->
            val slotTime = button.tag as? String ?: button.text.toString().split("\n")[0]
            button.tag = slotTime 

            db.collection("appointments")
                .whereEqualTo("hospitalName", hospitalName)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("slot", slotTime)
                .get()
                .addOnSuccessListener { documents ->
                    val count = documents.size()
                    val remaining = slotMaxLimit - count
                    
                    if (remaining <= 0) {
                        button.isEnabled = false
                        button.text = "$slotTime\n(Full)"
                        button.alpha = 0.5f
                    } else {
                        button.isEnabled = true
                        button.text = "$slotTime\n($remaining Left)"
                        button.alpha = 1.0f
                    }
                }
        }
    }

    private fun confirmBooking() {
        val token = (1000..9999).random()
        val hospitalName = arguments?.getString("hospitalName") ?: "Hospital"
        val userId = auth.currentUser?.uid ?: return

        // 1. Save to User's records
        val record = hashMapOf(
            "hospitalName" to hospitalName,
            "date" to selectedDate,
            "slot" to selectedSlot,
            "token" to token,
            "type" to "Appointment",
            "prescription" to "Pending Visit: Please consult doctor at scheduled time.",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        // 2. Also save to a global appointments collection for slot counting
        val appointmentData = hashMapOf(
            "hospitalName" to hospitalName,
            "date" to selectedDate,
            "slot" to selectedSlot,
            "patientId" to userId,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("appointments").add(appointmentData)
            .addOnSuccessListener {
                db.collection("users").document(userId).collection("records").add(record)
                    .addOnSuccessListener {
                        checkAndSendReminder(hospitalName, token)
                        addDummyRecords(userId)
                        Toast.makeText(context, "Appointment Confirmed! Token: $token", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack(R.id.FirstFragment, false)
                    }
            }
    }

    private fun checkAndSendReminder(hospitalName: String, token: Int) {
        val sdf = SimpleDateFormat("d/M/yyyy h:mm a", Locale.getDefault())
        try {
            val appointmentTime = sdf.parse("$selectedDate $selectedSlot") ?: return
            val diff = appointmentTime.time - System.currentTimeMillis()
            val hours = diff / (1000 * 60 * 60)

            val prefs = requireContext().getSharedPreferences("healmate_emergency", android.content.Context.MODE_PRIVATE)
            val phone = prefs.getString("phone", "7007914594") ?: "7007914594"

            val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)
            
            // Standard Confirmation
            val msg1 = "Appointment Confirmed at $hospitalName for $selectedDate at $selectedSlot. Token: $token"
            smsManager.sendTextMessage(phone, null, msg1, null, null)

            // 24-hour logic
            if (hours in 0..24) {
                val msg2 = "REMINDER: Your appointment at $hospitalName is in less than $hours hours (at $selectedSlot). Be on time!"
                smsManager.sendTextMessage(phone, null, msg2, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addDummyRecords(userId: String) {
        val dummyRecords = listOf(
            hashMapOf(
                "hospitalName" to "City General Hospital",
                "date" to "15/10/2023",
                "slot" to "10:00 AM",
                "token" to 4452,
                "type" to "Past Consultation",
                "prescription" to "Tab. Paracetamol 500mg (1-0-1)\nSyrup Dexorange 10ml twice daily\nRest for 3 days.",
                "timestamp" to com.google.firebase.Timestamp(Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time)
            ),
            hashMapOf(
                "hospitalName" to "Metro Cardiac Care",
                "date" to "02/11/2023",
                "slot" to "05:30 PM",
                "token" to 1129,
                "type" to "Follow-up",
                "prescription" to "Tab. EcoSpirin 75mg (0-0-1)\nTab. Atorvas 10mg (0-0-1)\nLifestyle: Low Sodium Diet, Daily 30 min walk.",
                "timestamp" to com.google.firebase.Timestamp(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -5) }.time)
            )
        )

        dummyRecords.forEach { record ->
            db.collection("users").document(userId).collection("records")
                .whereEqualTo("date", record["date"])
                .get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        db.collection("users").document(userId).collection("records").add(record)
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}