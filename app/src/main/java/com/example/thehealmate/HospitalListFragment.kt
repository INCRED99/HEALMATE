package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentHospitalListBinding

data class Hospital(val name: String, val departments: String)

class HospitalListFragment : Fragment() {

    private var _binding: FragmentHospitalListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHospitalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hospitals = listOf(
            Hospital("Orange City Hospital", "Cardiology, Orthopedics"),
            Hospital("Care Hospital Nagpur", "Neurology, Pediatrics"),
            Hospital("Kingsway Hospital", "ENT, General Medicine"),
            Hospital("Wockhardt Hospital", "Oncology, Dental")
        )

        binding.recyclerHospitals.layoutManager = LinearLayoutManager(context)
        binding.recyclerHospitals.adapter = HospitalAdapter(hospitals) { hospital ->
            val bundle = Bundle().apply {
                putString("hospitalName", hospital.name)
            }
            findNavController().navigate(R.id.action_HospitalListFragment_to_SlotBookingFragment, bundle)
        }
    }

    inner class HospitalAdapter(val list: List<Hospital>, val onClick: (Hospital) -> Unit) : 
        RecyclerView.Adapter<HospitalAdapter.VH>() {
        
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name = view.findViewById<TextView>(R.id.text_name)
            val deps = view.findViewById<TextView>(R.id.text_departments)
            val btn = view.findViewById<View>(R.id.button_book)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hospital, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.deps.text = item.departments
            holder.btn.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = list.size
    }
}