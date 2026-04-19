package com.example.thehealmate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.telephony.SmsManager
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentFirstBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isHospital = arguments?.getBoolean("isHospital") ?: false
        
        loadUserData()
        setupFeatureCards(isHospital)

        if (isHospital) {
            setupHospitalDashboard()
        } else {
            setupPatientDashboard()
            fetchUpcomingAppointment()
        }

        binding.buttonSos.setOnClickListener {
            handleSosClick()
        }

        binding.buttonProfile.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_ProfileFragment)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        binding.textUserName.text = user.displayName ?: "User"
        
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && _binding != null) {
                    binding.textUserName.text = doc.getString("name") ?: user.displayName ?: "User"
                }
            }
    }

    private fun setupFeatureCards(isHospital: Boolean) {
        // Patient Features
        binding.cardTelemedicine.textFeatureTitle.text = "Consultation"
        binding.cardTelemedicine.imageFeatureIcon.setImageResource(android.R.drawable.presence_video_online)
        binding.cardTelemedicine.root.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_TelemedicineFragment)
        }

        binding.cardAppoint.textFeatureTitle.text = "Appointments"
        binding.cardAppoint.imageFeatureIcon.setImageResource(android.R.drawable.ic_menu_today)
        binding.cardAppoint.root.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_HospitalListFragment)
        }

        binding.cardRecords.textFeatureTitle.text = "My Records"
        binding.cardRecords.imageFeatureIcon.setImageResource(android.R.drawable.ic_menu_agenda)
        binding.cardRecords.root.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment)
        }

        binding.cardPharmacy.textFeatureTitle.text = "Pharmacy"
        binding.cardPharmacy.imageFeatureIcon.setImageResource(android.R.drawable.ic_menu_save)
        binding.cardPharmacy.root.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_MedicalStoreFragment)
        }

        // Hospital Features
        binding.cardHospitalAppoint.textFeatureTitle.text = "Manage Appointments"
        binding.cardHospitalAppoint.imageFeatureIcon.setImageResource(android.R.drawable.ic_menu_today)
        binding.cardHospitalAppoint.root.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_HospitalAppointmentsFragment)
        }

        binding.cardPatientRecords.textFeatureTitle.text = "Patient Files"
        binding.cardPatientRecords.imageFeatureIcon.setImageResource(android.R.drawable.ic_menu_agenda)
        binding.cardPatientRecords.root.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isHospital", true) }
            findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment, bundle)
        }

        binding.cardManageGroups.textFeatureTitle.text = "Group Requests"
        binding.cardManageGroups.imageFeatureIcon.setImageResource(android.R.drawable.ic_input_add)
        binding.cardManageGroups.root.setOnClickListener {
            Toast.makeText(context, "Manage group requests feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.cardHospitalSettings.textFeatureTitle.text = "Settings"
        binding.cardHospitalSettings.imageFeatureIcon.setImageResource(android.R.drawable.ic_menu_preferences)
        binding.cardHospitalSettings.root.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_ProfileFragment)
        }
    }

    private fun setupPatientDashboard() {
        binding.textSectionTitle.text = "Patient Services"
        binding.gridPatientFeatures.visibility = View.VISIBLE
        binding.gridHospitalFeatures.visibility = View.GONE
        binding.buttonSos.visibility = View.VISIBLE
        binding.layoutHospitalStats.visibility = View.GONE
    }

    private fun setupHospitalDashboard() {
        binding.textSectionTitle.text = "Hospital Management"
        binding.gridPatientFeatures.visibility = View.GONE
        binding.gridHospitalFeatures.visibility = View.VISIBLE
        binding.buttonSos.visibility = View.GONE
        binding.layoutHospitalStats.visibility = View.VISIBLE
        binding.textGreeting.text = "Hospital Dashboard"
        
        binding.statPatients.textStatValue.text = "0"
        binding.statPatients.textStatLabel.text = "Active Patients"
        
        binding.statAppointments.textStatValue.text = "0"
        binding.statAppointments.textStatLabel.text = "Appointments"

        val uid = auth.currentUser?.uid ?: return
        db.collection("appointments").whereEqualTo("hospitalId", uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    val count = snapshot.size()
                    binding.statAppointments.textStatValue.text = count.toString()
                    // Assuming unique patients approach
                    val uniquePatients = snapshot.documents.map { it.getString("patientName") ?: "" }.filter { it.isNotEmpty() }.distinct().size
                    binding.statPatients.textStatValue.text = uniquePatients.toString()
                }
            }
    }

    private fun handleSosClick() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val phone = doc.getString("emergencyContact") ?: ""
                
                if (phone.isEmpty()) {
                    Toast.makeText(context, "Please add emergency contact in Profile settings", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fetchLocationAndSendSms(listOf(phone))
                } else {
                    requestPermissions(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION), 101)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchLocationAndSendSms(phones: List<String>) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val locationMsg = if (location != null) {
                "Emergency! My location: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else {
                "Emergency! I need help."
            }
            phones.forEach { phone ->
                sendSosSms(phone, locationMsg)
            }
        }
    }

    private fun sendSosSms(phone: String, message: String) {
        try {
            val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, message, null, null)
            Toast.makeText(context, "SOS Sent to $phone", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
        }
    }



    private fun fetchUpcomingAppointment() {
        val userId = auth.currentUser?.uid ?: return
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("d/M/yyyy h:mm a", Locale.getDefault())

        db.collection("users").document(userId).collection("records")
            .get()
            .addOnSuccessListener { documents ->
                val upcomingAppointments = mutableListOf<Pair<com.google.firebase.firestore.DocumentSnapshot, Date>>()
                
                for (doc in documents) {
                    val dateStr = doc.getString("date") ?: ""
                    val slotStr = doc.getString("slot") ?: ""
                    try {
                        val apptDate = sdf.parse("$dateStr $slotStr")
                        if (apptDate != null && apptDate.after(now.time)) {
                            upcomingAppointments.add(doc to apptDate)
                        }
                    } catch (e: Exception) { }
                }

                // Sort by date/time ascending to get the SOONEST future appointment
                upcomingAppointments.sortBy { it.second }

                if (upcomingAppointments.isNotEmpty() && _binding != null) {
                    val soonestDoc = upcomingAppointments[0].first
                    binding.cardUpcomingAppointment.visibility = View.VISIBLE
                    binding.textUpcomingHospital.text = soonestDoc.getString("hospitalName")
                    binding.textUpcomingDatetime.text = "${soonestDoc.getString("date")} at ${soonestDoc.getString("slot")}"
                    binding.textUpcomingToken.text = "Token: ${soonestDoc.get("token")}"
                    
                    binding.cardUpcomingAppointment.setOnClickListener {
                        findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment)
                    }
                } else if (_binding != null) {
                    binding.cardUpcomingAppointment.visibility = View.GONE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
