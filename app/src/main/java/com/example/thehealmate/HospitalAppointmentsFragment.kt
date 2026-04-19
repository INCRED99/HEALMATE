package com.example.thehealmate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thehealmate.databinding.FragmentHospitalAppointmentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HospitalAppointmentsFragment : Fragment() {

    private var _binding: FragmentHospitalAppointmentsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appointments = mutableListOf<Appointment>()
    private lateinit var adapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHospitalAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppointmentAdapter(appointments)
        binding.recyclerAppointments.layoutManager = LinearLayoutManager(context)
        binding.recyclerAppointments.adapter = adapter

        fetchAppointments()
    }

    private fun fetchAppointments() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("appointments").whereEqualTo("hospitalId", uid).get()
            .addOnSuccessListener { snapshot ->
                appointments.clear()
                if (snapshot.isEmpty) {
                    Toast.makeText(context, "No appointments found", Toast.LENGTH_SHORT).show()
                } else {
                    for (doc in snapshot.documents) {
                        val name = doc.getString("patientName") ?: "Unknown"
                        val date = doc.getString("date") ?: "--/--/----"
                        val time = doc.getString("time") ?: "--:--"
                        val ref = doc.getString("reference") ?: doc.id.takeLast(4)
                        appointments.add(Appointment(name, date, time, ref))
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}