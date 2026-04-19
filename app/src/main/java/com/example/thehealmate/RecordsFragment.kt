package com.example.thehealmate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentRecordsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class MedicalRecord(
    val title: String,
    val date: String,
    val details: String
)

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val records = mutableListOf<MedicalRecord>()
    private lateinit var adapter: RecordsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isHospital = arguments?.getBoolean("isHospital") ?: false
        if (isHospital) {
            binding.textTitle.text = "Patient Records (Shared)"
            binding.textSubtitle.text = "Shared records across network"
        }

        adapter = RecordsAdapter(records)
        binding.recyclerRecords.layoutManager = LinearLayoutManager(context)
        binding.recyclerRecords.adapter = adapter

        fetchRecords()
    }

    private fun fetchRecords() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(uid).collection("medical_records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                records.clear()
                if (snapshot.isEmpty) {
                    Toast.makeText(context, "No medical records found", Toast.LENGTH_SHORT).show()
                } else {
                    for (doc in snapshot.documents) {
                        val title = doc.getString("title") ?: "Appointment"
                        val date = doc.getString("date") ?: ""
                        val details = doc.getString("details") ?: ""
                        records.add(MedicalRecord(title, date, details))
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load records", Toast.LENGTH_SHORT).show()
            }
    }

    private inner class RecordsAdapter(private val recordList: List<MedicalRecord>) : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textTitle: TextView = view.findViewById(R.id.text_record_title)
            val textDate: TextView = view.findViewById(R.id.text_record_date)
            val textDetails: TextView = view.findViewById(R.id.text_record_details)
            val root: View = view
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = recordList[position]
            holder.textTitle.text = record.title
            holder.textDate.text = record.date
            holder.textDetails.text = record.details

            holder.root.setOnClickListener {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(record.title)
                    .setMessage("Date: ${record.date}\n\nDetails:\n${record.details}")
                    .setPositiveButton("Close", null)
                    .show()
            }
        }

        override fun getItemCount(): Int = recordList.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}