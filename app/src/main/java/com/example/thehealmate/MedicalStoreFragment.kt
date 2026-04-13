package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentMedicalStoreBinding

data class MedicalStore(val name: String, val location: String)

class MedicalStoreFragment : Fragment() {

    private var _binding: FragmentMedicalStoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicalStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stores = listOf(
            MedicalStore("Nagpur Central Pharmacy", "Dharampeth, Nagpur"),
            MedicalStore("Generic Meds Point", "Sitabuldi, Nagpur"),
            MedicalStore("LifeCare Medicals", "Itwari, Nagpur"),
            MedicalStore("Wellness Forever", "Ramdaspeth, Nagpur")
        )

        binding.recyclerStores.layoutManager = LinearLayoutManager(context)
        binding.recyclerStores.adapter = StoreAdapter(stores)
    }

    inner class StoreAdapter(val list: List<MedicalStore>) : RecyclerView.Adapter<StoreAdapter.VH>() {
        
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name = view.findViewById<TextView>(R.id.text_store_name)
            val loc = view.findViewById<TextView>(R.id.text_store_location)
            val btn = view.findViewById<View>(R.id.button_select_store)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_store, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.loc.text = item.location
            holder.btn.setOnClickListener {
                Toast.makeText(context, "${item.name} selected as your primary pharmacy.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount() = list.size
    }
}