package com.example.thehealmate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.Toast
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
        val inputName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_emergency_name)
        val inputPhone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_emergency_phone)
        
        // Use hint to indicate multi-input support
        inputName.hint = "Name (Separate multiple by |)"
        inputPhone.hint = "Phone (Separate multiple by |)"
        
        val currentNames = prefs.getString("names", "")
        val currentPhones = prefs.getString("phone", "")
        
        inputName.setText(currentNames)
        inputPhone.setText(currentPhones)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_emergency_contact))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val names = inputName.text.toString()
                val phones = inputPhone.text.toString()
                
                val namesList = names.split("|")
                val phonesList = phones.split("|")
                
                val combined = namesList.zip(phonesList).joinToString("|") { "${it.first},${it.second}" }

                prefs.edit().apply {
                    putString("names", names)
                    putString("phone", phones)
                    putString("contacts_list", combined)
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