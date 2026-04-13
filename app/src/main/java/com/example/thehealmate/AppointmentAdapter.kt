package com.example.thehealmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppointmentAdapter(private val appointments: List<Appointment>) :
    RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_patient_name)
        val details: TextView = view.findViewById(R.id.text_appointment_details)
        val token: TextView = view.findViewById(R.id.text_token)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.name.text = appointment.patientName
        holder.details.text = "Date: ${appointment.date} | Time: ${appointment.time}"
        holder.token.text = "Token: ${appointment.token}"
    }

    override fun getItemCount() = appointments.size
}