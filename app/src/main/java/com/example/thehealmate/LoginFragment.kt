package com.example.thehealmate

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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
            Log.e("LoginFragment", "Google sign in failed code: ${e.statusCode}", e)
            Toast.makeText(context, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
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
        
        // Use the Web Client ID from res/values/strings.xml which must match Firebase configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.buttonLogin.setOnClickListener {
            if (validateInputs()) {
                attemptLogin()
            }
        }

        binding.buttonGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        binding.textRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val isHospital = binding.radioHospital.isChecked
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToFirestore(user?.uid, user?.displayName, user?.email, isHospital)
                    
                    val bundle = Bundle().apply {
                        putBoolean("isHospital", isHospital)
                    }
                    findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment, bundle)
                } else {
                    Toast.makeText(context, "Firebase Auth with Google Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String?, name: String?, email: String?, isHospital: Boolean) {
        if (uid == null) return
        val db = FirebaseFirestore.getInstance()
        val role = if (isHospital) "hospital" else "patient"
        val userMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role,
            "lastLogin" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userMap, SetOptions.merge())
    }

    private fun validateInputs(): Boolean {
        val email = binding.inputUsername.text.toString().trim()
        val password = binding.inputPassword.text.toString()

        binding.inputEmailLayout.error = null
        binding.inputPasswordLayout.error = null

        if (email.isEmpty()) {
            binding.inputEmailLayout.error = getString(R.string.error_email_empty)
            return false
        }
        if (!email.contains("@")) {
            binding.inputEmailLayout.error = getString(R.string.error_email_invalid)
            return false
        }

        if (password.isEmpty()) {
            binding.inputPasswordLayout.error = getString(R.string.error_password_empty)
            return false
        }

        return true
    }

    private fun attemptLogin() {
        val email = binding.inputUsername.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val isHospital = binding.radioHospital.isChecked

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val bundle = Bundle().apply {
                        putBoolean("isHospital", isHospital)
                    }
                    findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment, bundle)
                } else {
                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
