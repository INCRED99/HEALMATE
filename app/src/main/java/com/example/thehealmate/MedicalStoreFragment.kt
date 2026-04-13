package com.example.thehealmate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentMedicalStoreBinding

data class MedicalStore(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

class MedicalStoreFragment : Fragment() {

    private var _binding: FragmentMedicalStoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicalStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val stores = listOf(
        MedicalStore("Nagpur Central Pharmacy", "Dharampeth, Nagpur", 21.1415, 79.0680),
        MedicalStore("Generic Meds Point", "Sitabuldi, Nagpur", 21.1458, 79.0832),
        MedicalStore("LifeCare Medicals", "Itwari, Nagpur", 21.1550, 79.1080),
        MedicalStore("Wellness Forever", "Ramdaspeth, Nagpur", 21.1350, 79.0750)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerStores.layoutManager = LinearLayoutManager(context)
        binding.recyclerStores.adapter = StoreAdapter(stores)
    }

    private fun handleStoreClick(store: MedicalStore) {
        val gmmIntentUri = Uri.parse("geo:${store.latitude},${store.longitude}?q=${Uri.encode(store.name + ", " + store.address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(store.name + ", " + store.address)}"))
            startActivity(browserIntent)
        }
    }

    inner class StoreAdapter(val list: List<MedicalStore>) : RecyclerView.Adapter<StoreAdapter.VH>() {
        
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.text_store_name)
            val address: TextView = view.findViewById(R.id.text_store_address)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medical_store, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.address.text = item.address
            
            holder.itemView.setOnClickListener {
                handleStoreClick(item)
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}