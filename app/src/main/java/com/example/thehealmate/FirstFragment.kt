package com.example.thehealmate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.Button
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentFirstBinding
import android.telephony.SmsManager
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        
        if (isHospital) {
            setupHospitalDashboard()
        } else {
            setupPatientDashboard()
        }

        binding.buttonSos.setOnClickListener {
            handleSosClick()
        }
    }

    private fun handleSosClick() {
        val prefs = requireContext().getSharedPreferences("healmate_emergency", android.content.Context.MODE_PRIVATE)
        val contactsString = prefs.getString("contacts_list", "")
        
        if (contactsString.isNullOrEmpty()) {
            Toast.makeText(context, "Please add at least one emergency contact first", Toast.LENGTH_SHORT).show()
            showEmergencyContactDialog()
            return
        }

        val contacts = contactsString.split("|")
        val phones = contacts.mapNotNull { it.split(",").lastOrNull() }

        val hasSms = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasLoc = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (hasSms && hasLoc) {
            fetchLocationAndSendSms(phones)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    private fun fetchLocationAndSendSms(phones: List<String>) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val locationMsg = if (location != null) {
                "My location: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else {
                "My location: Nagpur (Last known)"
            }
            phones.forEach { phone ->
                sendSosSms(phone, locationMsg)
            }
        }
    }

    private fun sendSosSms(phone: String, locationMsg: String) {
        try {
            val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)
            val message = "EMERGENCY! I need help. $locationMsg"
            smsManager.sendTextMessage(phone, null, message, null, null)
            Toast.makeText(context, "SOS Message Sent to $phone", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_emergency -> {
                showEmergencyContactDialog()
                true
            }
            R.id.action_contact_us -> {
                showContactUsDialog()
                true
            }
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performLogout() {
        // Sign out from Firebase
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

        // Sign out from Google if applicable
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // Navigate back to Login
            findNavController().navigate(R.id.action_FirstFragment_to_LoginFragment)
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showContactUsDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_contact_us, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.view.View>(R.id.button_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEmergencyContactDialog() {
        val prefs = requireContext().getSharedPreferences("healmate_emergency", android.content.Context.MODE_PRIVATE)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_emergency, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.container_contacts)
        val btnAdd = dialogView.findViewById<Button>(R.id.button_add_contact)

        fun addContactRow(name: String = "", phone: String = "") {
            val rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_emergency_input, container, false)
            val editName = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_name)
            val editPhone = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_phone)
            val btnRemove = rowView.findViewById<ImageButton>(R.id.button_remove)

            editName.setText(name)
            editPhone.setText(phone)
            btnRemove.setOnClickListener { container.removeView(rowView) }
            container.addView(rowView)
        }

        val currentContacts = prefs.getString("contacts_list", "") ?: ""
        if (currentContacts.isNotEmpty()) {
            currentContacts.split("|").forEach {
                val parts = it.split(",")
                if (parts.size == 2) {
                    addContactRow(parts[0], parts[1])
                }
            }
        } else {
            addContactRow() // Add one empty row by default
        }

        btnAdd.setOnClickListener { addContactRow() }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_emergency_contact))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val contacts = mutableListOf<String>()
                for (i in 0 until container.childCount) {
                    val row = container.getChildAt(i)
                    val name = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_name).text.toString()
                    val phone = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_phone).text.toString()
                    if (name.isNotEmpty() && phone.isNotEmpty()) {
                        contacts.add("$name,$phone")
                    }
                }
                
                val combined = contacts.joinToString("|")
                prefs.edit().apply {
                    putString("contacts_list", combined)
                    // Keep old phone key for compatibility with existing SOS logic if it uses first contact
                    if (contacts.isNotEmpty()) {
                        putString("phone", contacts[0].split(",")[1])
                    }
                    apply()
                }
                Toast.makeText(context, getString(R.string.emergency_updated), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupPatientDashboard() {
        binding.textRoleTitle.text = "Patient Dashboard"
        binding.gridPatientFeatures.visibility = View.VISIBLE
        binding.gridHospitalFeatures.visibility = View.GONE
        
        binding.cardAppoint.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_HospitalListFragment)
        }
        
        binding.cardMedicine.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_MedicalStoreFragment)
        }
        
        binding.cardCommunity.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_CommunityFragment)
        }

        binding.cardRecords.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment)
        }

        showDailyWellnessContent()
        fetchUpcomingAppointment()
    }

    private fun fetchUpcomingAppointment() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val now = Calendar.getInstance().time
        val sdf = SimpleDateFormat("d/M/yyyy h:mm a", Locale.getDefault())

        db.collection("users").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                var latestUpcoming: com.google.firebase.firestore.DocumentSnapshot? = null
                
                for (doc in documents) {
                    val dateStr = doc.getString("date") ?: ""
                    val slotStr = doc.getString("slot") ?: ""
                    try {
                        val apptDate = sdf.parse("$dateStr $slotStr")
                        if (apptDate != null && apptDate.after(now)) {
                            latestUpcoming = doc
                            break // Found the most recent upcoming one (since ordered by timestamp DESC)
                        }
                    } catch (e: Exception) { }
                }

                if (latestUpcoming != null) {
                    binding.cardUpcomingAppointment.visibility = View.VISIBLE
                    binding.textUpcomingHospital.text = latestUpcoming.getString("hospitalName")
                    binding.textUpcomingDatetime.text = "${latestUpcoming.getString("date")} at ${latestUpcoming.getString("slot")}"
                    binding.textUpcomingToken.text = "Token: ${latestUpcoming.get("token")}"
                    
                    binding.cardUpcomingAppointment.setOnClickListener {
                        findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment)
                    }
                } else {
                    binding.cardUpcomingAppointment.visibility = View.GONE
                }
            }
    }

    private fun showDailyWellnessContent() {
        val wellnessTips = listOf(
            "Food is not just fuel—it’s information for your body.",
            "Drinking 2–3 liters of water daily improves energy and metabolism.",
            "30 minutes of exercise a day can reduce heart disease risk by up to 30%.",
            "Your diet is a bank account—good choices are good investments.",
            "Eating 5 servings of fruits and vegetables daily boosts immunity.",
            "Exercise is a celebration of what your body can do—not a punishment.",
            "Getting 7–9 hours of sleep improves brain function and recovery.",
            "10,000 steps a day can significantly improve cardiovascular health.",
            "A little progress each day adds up to big results.",
            "Reducing sugar intake lowers diabetes risk by nearly 40%.",
            "Your body hears everything your mind says—stay positive.",
            "Strength training 2–3 times a week improves muscle and bone health.",
            "Take care of your body—it’s the only place you have to live.",
            "Sitting more than 8 hours a day increases health risks.",
            "Health is not a goal—it’s a lifestyle.",
            "Losing just 5–10% of body weight improves overall health markers.",
            "Consistency beats perfection in fitness and health.",
            "Eating breakfast improves concentration and energy levels.",
            "Sleep, nutrition, and exercise together form 100% of your wellness.",
            "A healthy outside starts from a healthy inside."
        )

        val randomTip = wellnessTips.random()
        binding.textWellness.text = "Daily Wellness Fact: $randomTip"
    }

    private fun setupHospitalDashboard() {
        binding.textRoleTitle.text = "Hospital Management"
        binding.gridPatientFeatures.visibility = View.GONE
        binding.gridHospitalFeatures.visibility = View.VISIBLE
        binding.buttonSos.visibility = View.GONE
        binding.textSubtitle.text = "Nagpur Central Hospital"

        binding.cardHospitalAppoint.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_HospitalAppointmentsFragment)
        }
        
        binding.cardPatientRecords.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isHospital", true)
            }
            findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}