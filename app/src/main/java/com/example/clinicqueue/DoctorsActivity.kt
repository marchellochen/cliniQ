package com.example.clinicqueue

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicqueue.databinding.ActivityDoctorsBinding
import com.example.clinicqueue.databinding.ItemDoctorBinding

class DoctorsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Choose a doctor")

        binding.rvDoctors.layoutManager = LinearLayoutManager(this)
        binding.rvDoctors.adapter = DoctorAdapter(ClinicRepo.doctors) { doctor ->
            startActivity(Intent(this, FormActivity::class.java).putExtra("doctorId", doctor.id))
        }
    }
}

/** Turns the list of doctors into rows (one item_doctor.xml per doctor). */
class DoctorAdapter(
    private val items: List<Doctor>,
    private val onClick: (Doctor) -> Unit,
) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDoctorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val d = items[position]
        holder.binding.tvInitial.text = d.name.removePrefix("dr. ").first().toString()
        holder.binding.tvName.text = d.name
        holder.binding.tvSpecialty.text = d.specialty
        holder.binding.tvRoom.text = d.room
        holder.binding.root.setOnClickListener { onClick(d) }
    }
}
