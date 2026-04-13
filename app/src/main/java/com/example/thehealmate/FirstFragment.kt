package com.example.thehealmate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.thehealmate.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

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
            findNavController().navigate(R.id.action_FirstFragment_to_RecordsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}