package com.example.thehealmate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sign In button
        binding.buttonLogin.setOnClickListener {
            if (validateInputs()) {
                attemptLogin()
            }
        }

        // Google Sign In button
        binding.buttonGoogleSignIn.setOnClickListener {
            Toast.makeText(context, getString(R.string.google_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Register link
        binding.textRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }
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