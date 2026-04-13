package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.thehealmate.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appointment1.setOnClickListener {
            Toast.makeText(context, "Opening Kingsway Hospital details...", Toast.LENGTH_SHORT).show()
        }

        binding.appointment2.setOnClickListener {
            Toast.makeText(context, "Opening Care Hospital details...", Toast.LENGTH_SHORT).show()
        }

        binding.appointment3.setOnClickListener {
            Toast.makeText(context, "Opening Orange City Hospital details...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}