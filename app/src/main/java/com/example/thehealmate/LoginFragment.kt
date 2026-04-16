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
import com.example.thehealmate.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("LoginFragment", "Google sign in failed", e)
            Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

        // Sign In button
        binding.buttonLogin.setOnClickListener {
            if (validateInputs()) {
                attemptLogin()
            }
        }

        // Google Sign In button
        binding.buttonGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Register link
        binding.textRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToFirestore(user?.uid, user?.displayName, user?.email)
                    
                    val bundle = Bundle().apply {
                        putBoolean("isHospital", false)
                    }
                    findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment, bundle)
                } else {
                    Toast.makeText(context, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String?, name: String?, email: String?) {
        if (uid == null) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to "patient",
            "lastLogin" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
    }

    private fun validateInputs(): Boolean {
        val email = binding.inputUsername.text.toString().trim()
        val password = binding.inputPassword.text.toString()

        // Reset errors
        binding.inputEmailLayout.error = null
        binding.inputPasswordLayout.error = null

        // Email validation
        if (email.isEmpty()) {
            binding.inputEmailLayout.error = getString(R.string.error_email_empty)
            binding.inputUsername.requestFocus()
            return false
        }
        if (!email.contains("@")) {
            binding.inputEmailLayout.error = getString(R.string.error_email_invalid)
            binding.inputUsername.requestFocus()
            return false
        }
        if (!email.endsWith("gmail.com")) {
            binding.inputEmailLayout.error = getString(R.string.error_email_format)
            binding.inputUsername.requestFocus()
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

        return true
    }

    private fun attemptLogin() {
        val email = binding.inputUsername.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val isHospital = binding.radioHospital.isChecked

        val prefs = requireContext().getSharedPreferences("healmate_users", Context.MODE_PRIVATE)

        // Check if user exists
        val savedPassword = prefs.getString("user_${email}_password", null)
        if (savedPassword == null) {
            // No user found
            Toast.makeText(context, getString(R.string.error_no_user), Toast.LENGTH_LONG).show()
            return
        }

        // Check password
        if (savedPassword != password) {
            Toast.makeText(context, getString(R.string.error_wrong_password), Toast.LENGTH_LONG).show()
            return
        }

        // Success - navigate to home
        val bundle = Bundle().apply {
            putBoolean("isHospital", isHospital)
        }

        // Set the emergency phone number for the session if it's the specific test case
        val emergencyPrefs = requireContext().getSharedPreferences("healmate_emergency", Context.MODE_PRIVATE)
        emergencyPrefs.edit().putString("phone", "7007914594").apply()

        findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}