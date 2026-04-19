package com.example.thehealmate

import android.content.Intent
import android.net.Uri
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

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

data class MedicalStore(val name: String, val location: String)

class MedicalStoreFragment : Fragment() {

    private var _binding: FragmentMedicalStoreBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkGpsAndShowStores()
        } else {
            Toast.makeText(context, "GPS required for medical stores", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicalStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkGpsAndShowStores()
    }

    private fun checkGpsAndShowStores() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            Toast.makeText(context, "Please turn on GPS/Location to see nearby medical stores", Toast.LENGTH_LONG).show()
            // We can still show the list, but with a warning, or show an empty state. 
            // The prompt said: if gps is off, the app should ask for gps permissions.
            // Note: App cannot "turn on" GPS automatically, but it can prompt.
        }

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
                // Search for generic medical stores in the specific area
                val area = item.location.split(",")[0].trim()
                val query = "generic medical stores in $area Nagpur"
                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/${Uri.encode(query)}"))
                    startActivity(webIntent)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}