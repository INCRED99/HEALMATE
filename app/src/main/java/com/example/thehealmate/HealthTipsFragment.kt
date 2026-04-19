package com.example.thehealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thehealmate.databinding.FragmentHealthTipsBinding
import com.google.firebase.firestore.FirebaseFirestore

data class HealthTip(
    val emoji: String,
    val title: String,
    val body: String,
    val category: String
)

class HealthTipsFragment : Fragment() {

    private var _binding: FragmentHealthTipsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val tips = mutableListOf<HealthTip>()
    private lateinit var adapter: TipAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TipAdapter(tips)
        binding.recyclerTips.layoutManager = LinearLayoutManager(context)
        binding.recyclerTips.adapter = adapter
        loadTips()
    }

    private fun loadTips() {
        // Try Firestore first, fallback to curated built-in tips
        db.collection("health_tips").orderBy("order").limit(30).get()
            .addOnSuccessListener { snapshot ->
                tips.clear()
                if (!snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        tips.add(
                            HealthTip(
                                emoji = doc.getString("emoji") ?: "💡",
                                title = doc.getString("title") ?: "",
                                body = doc.getString("body") ?: "",
                                category = doc.getString("category") ?: "General"
                            )
                        )
                    }
                } else {
                    loadBuiltInTips()
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { loadBuiltInTips() }
    }

    private fun loadBuiltInTips() {
        tips.addAll(
            listOf(
                HealthTip("💧", "Stay Hydrated", "Drink at least 8 glasses of water daily. Proper hydration supports kidney function, energy levels, and skin health.", "Hydration"),
                HealthTip("🥗", "Eat More Vegetables", "Fill half your plate with colourful vegetables. They are rich in fibre, vitamins, and antioxidants that fight inflammation.", "Nutrition"),
                HealthTip("🏃", "Move Every Hour", "Sitting for long periods slows metabolism. Stand up and walk for 2 minutes every hour to improve circulation.", "Fitness"),
                HealthTip("😴", "Prioritise Sleep", "Adults need 7–9 hours of quality sleep. Poor sleep raises cortisol, increases hunger, and weakens immunity.", "Sleep"),
                HealthTip("🧘", "Practice Deep Breathing", "Taking 5 deep breaths activates the parasympathetic nervous system, reducing stress within seconds.", "Mental Health"),
                HealthTip("🌞", "Get Morning Sunlight", "Exposure to natural light within 30 minutes of waking resets your circadian rhythm and boosts serotonin.", "Wellness"),
                HealthTip("🍎", "Limit Processed Sugar", "High sugar intake worsens inflammation, disrupts gut flora, and increases the risk of type 2 diabetes.", "Nutrition"),
                HealthTip("🦷", "Brush Twice Daily", "Oral hygiene is linked to heart health. Brush for 2 minutes morning and night, and floss daily.", "Hygiene"),
                HealthTip("🚶", "Walk 10,000 Steps", "Regular walking lowers blood pressure, improves mood, and reduces the risk of cardiovascular disease.", "Fitness"),
                HealthTip("🧴", "Apply SPF Daily", "UV exposure is the leading cause of premature ageing and skin cancer. Use SPF 30+ even on cloudy days.", "Skin Health"),
                HealthTip("📵", "Digital Detox Before Bed", "Blue light from screens suppresses melatonin. Put your phone away 1 hour before sleep.", "Sleep"),
                HealthTip("🫀", "Monitor Blood Pressure", "High blood pressure has no symptoms. Check it regularly, especially if you have a family history.", "Prevention"),
                HealthTip("🥚", "Eat Complete Proteins", "Include eggs, legumes, or lean meat in every meal to support muscle repair and satiety.", "Nutrition"),
                HealthTip("🧠", "Stimulate Your Brain", "Read, solve puzzles, or learn a new skill daily to build cognitive reserve and reduce dementia risk.", "Mental Health"),
                HealthTip("🤝", "Stay Socially Connected", "Loneliness increases cortisol and inflammation. Regular social interaction is as vital as exercise.", "Mental Health")
            )
        )
        adapter.notifyDataSetChanged()
    }

    private inner class TipAdapter(private val list: List<HealthTip>) :
        RecyclerView.Adapter<TipAdapter.TipVH>() {

        inner class TipVH(view: View) : RecyclerView.ViewHolder(view) {
            val emoji: TextView = view.findViewById(R.id.text_tip_emoji)
            val title: TextView = view.findViewById(R.id.text_tip_title)
            val body: TextView = view.findViewById(R.id.text_tip_body)
            val category: TextView = view.findViewById(R.id.text_tip_category)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_health_tip, parent, false)
            return TipVH(v)
        }

        override fun onBindViewHolder(holder: TipVH, position: Int) {
            val tip = list[position]
            holder.emoji.text = tip.emoji
            holder.title.text = tip.title
            holder.body.text = tip.body
            holder.category.text = tip.category
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
