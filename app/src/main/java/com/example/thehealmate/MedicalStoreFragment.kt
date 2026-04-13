package com.example.thehealmate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentMedicalStoreBinding

data class MedicalStore(
    val name: String,
    val address: String,
    val rating: String,
    val distance: String,
    val latitude: Double,
    val longitude: Double
)

class MedicalStoreFragment : Fragment() {

    private var _binding: FragmentMedicalStoreBinding? = null
    private val binding get() = _binding!!
    private var pendingStore: MedicalStore? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openMaps(pendingStore)
        } else {
            Toast.makeText(context, "Location permission is required to show distance and open maps", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicalStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stores = listOf(
            MedicalStore("Nagpur Central Pharmacy", "Dharampeth, Nagpur", "4.8", "0.5 km", 21.1415, 79.0680),
            MedicalStore("Generic Meds Point", "Sitabuldi, Nagpur", "4.5", "1.2 km", 21.1458, 79.0832),
            MedicalStore("LifeCare Medicals", "Itwari, Nagpur", "4.2", "3.1 km", 21.1550, 79.1080),
            MedicalStore("Wellness Forever", "Ramdaspeth, Nagpur", "4.7", "1.8 km", 21.1350, 79.0750)
        )

        binding.recyclerStores.layoutManager = LinearLayoutManager(context)
        binding.recyclerStores.adapter = StoreAdapter(stores)
    }

    private fun handleStoreClick(store: MedicalStore) {
        pendingStore = store
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            openMaps(store)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun openMaps(store: MedicalStore?) {
        store?.let {
            val gmmIntentUri = Uri.parse("geo:${it.latitude},${it.longitude}?q=${Uri.encode(it.name + ", " + it.address)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(it.name + ", " + it.address)}"))
                startActivity(browserIntent)
            }
        }
    }

    inner class StoreAdapter(val list: List<MedicalStore>) : RecyclerView.Adapter<StoreAdapter.VH>() {
        
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.text_store_name)
            val address: TextView = view.findViewById(R.id.text_store_address)
            val rating: TextView = view.findViewById(R.id.text_rating)
            val distance: TextView = view.findViewById(R.id.text_distance)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medical_store, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.address.text = item.address
            holder.rating.text = item.rating
            holder.distance.text = item.distance
            
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