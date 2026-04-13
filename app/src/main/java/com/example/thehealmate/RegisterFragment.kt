package com.example.thehealmate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Register button
        binding.buttonRegister.setOnClickListener {
            if (validateInputs()) {
                attemptRegister()
            }
        }

        // Google Sign Up button
        binding.buttonGoogleSignUp.setOnClickListener {
            Toast.makeText(context, getString(R.string.google_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Sign In link
        binding.textSignInLink.setOnClickListener {
            findNavController().navigate(R.id.action_RegisterFragment_to_LoginFragment)
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val confirmPassword = binding.inputConfirmPassword.text.toString()

        // Reset errors
        binding.inputNameLayout.error = null
        binding.inputEmailLayout.error = null
        binding.inputPasswordLayout.error = null
        binding.inputConfirmPasswordLayout.error = null

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
        val isHospital = binding.radioHospital.isChecked

        val prefs = requireContext().getSharedPreferences("healmate_users", Context.MODE_PRIVATE)

        // Check if user already exists
        val existingUser = prefs.getString("user_${email}_password", null)
        if (existingUser != null) {
            Toast.makeText(context, getString(R.string.error_user_exists), Toast.LENGTH_LONG).show()
            return
        }

        // Save user to SharedPreferences
        prefs.edit()
            .putString("user_${email}_password", password)
            .putString("user_${email}_name", name)
            .putBoolean("user_${email}_isHospital", isHospital)
            .apply()

        Toast.makeText(context, getString(R.string.registration_success), Toast.LENGTH_SHORT).show()

        // Navigate to home
        val bundle = Bundle().apply {
            putBoolean("isHospital", isHospital)
        }
        findNavController().navigate(R.id.action_RegisterFragment_to_FirstFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
