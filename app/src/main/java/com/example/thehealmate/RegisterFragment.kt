package com.example.thehealmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("RegisterFragment", "Google sign in failed", e)
            Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("253153362142-67anasahd0psnsnfgik8mjvqmr3ejmhf.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Register button
        binding.buttonRegister.setOnClickListener {
            if (validateInputs()) {
                attemptRegister()
            }
        }

        // Google Sign Up button
        binding.buttonGoogleSignUp.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Sign In link
        binding.textSignInLink.setOnClickListener {
            findNavController().navigate(R.id.action_RegisterFragment_to_LoginFragment)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToFirestore(user?.uid, user?.displayName, user?.email, "patient", "")
                    
                    val bundle = Bundle().apply {
                        putBoolean("isHospital", false)
                    }
                    findNavController().navigate(R.id.action_RegisterFragment_to_FirstFragment, bundle)
                } else {
                    Toast.makeText(context, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String?, name: String?, email: String?, role: String, emergencyContact: String) {
        if (uid == null) return
        val db = FirebaseFirestore.getInstance()
        val userMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role,
            "emergencyContact" to emergencyContact,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userMap, SetOptions.merge())
    }

    private fun validateInputs(): Boolean {
        val name = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val confirmPassword = binding.inputConfirmPassword.text.toString()
        val emergencyContact = binding.inputEmergencyContact.text.toString().trim()

        // Reset errors
        binding.inputNameLayout.error = null
        binding.inputEmailLayout.error = null
        binding.inputPasswordLayout.error = null
        binding.inputConfirmPasswordLayout.error = null
        binding.inputEmergencyContactLayout.error = null

        // Name validation
        if (name.isEmpty()) {
            binding.inputNameLayout.error = getString(R.string.error_name_empty)
            binding.inputName.requestFocus()
            return false
        }

        // Email validation
        if (email.isEmpty()) {
            binding.inputEmailLayout.error = getString(R.string.error_email_empty)
            binding.inputEmail.requestFocus()
            return false
        }
        if (!email.contains("@")) {
            binding.inputEmailLayout.error = getString(R.string.error_email_invalid)
            binding.inputEmail.requestFocus()
            return false
        }
        if (!email.endsWith("gmail.com")) {
            binding.inputEmailLayout.error = getString(R.string.error_email_format)
            binding.inputEmail.requestFocus()
            return false
        }

        // Emergency Contact validation (optional but good to have)
        if (emergencyContact.isEmpty() && binding.radioPatient.isChecked) {
            binding.inputEmergencyContactLayout.error = "Emergency contact is required for patients"
            binding.inputEmergencyContact.requestFocus()
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.inputPasswordLayout.error = getString(R.string.error_password_empty)
            binding.inputPassword.requestFocus()
            return false
        }
        if (password.length < 6) {
            binding.inputPasswordLayout.error = getString(R.string.error_password_short)
            binding.inputPassword.requestFocus()
            return false
        }

        // Confirm password
        if (confirmPassword != password) {
            binding.inputConfirmPasswordLayout.error = getString(R.string.error_password_mismatch)
            binding.inputConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun attemptRegister() {
        val name = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val emergencyContact = binding.inputEmergencyContact.text.toString().trim()
        val isHospital = binding.radioHospital.isChecked
        val role = if (isHospital) "hospital" else "patient"

        // Use Firebase Auth for registration
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    saveUserToFirestore(uid, name, email, role, emergencyContact)
                    
                    Toast.makeText(context, getString(R.string.registration_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_RegisterFragment_to_LoginFragment)
                } else {
                    Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
