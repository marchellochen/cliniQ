
package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.cliniq.databinding.ActivityDoctorsBinding
import com.example.cliniq.databinding.ItemDoctorBinding

class DoctorsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get selected specialty
        val selectedSpecialty =
            intent.getStringExtra("specialty")?.trim().orEmpty()

        // Get booking flow from previous screen
        val bookingFlow = intent.getStringExtra(EXTRA_BOOKING_FLOW)
            ?: FLOW_QUEUE

        // English toolbar title
        setupBar(
            if (selectedSpecialty.isNotBlank()) {
                "Doctors: $selectedSpecialty"
            } else {
                "All Doctors"
            }
        )

        // RecyclerView setup
        binding.rvDoctors.layoutManager =
            LinearLayoutManager(this)

        binding.rvDoctors.setHasFixedSize(false)

        // Load doctors from Supabase
        launchSafely {
            ClinicRepo.loadDoctors()

            // Filter doctors by selected specialty
            val filteredDoctors = if (selectedSpecialty.isNotBlank()) {
                ClinicRepo.doctors.filter {
                    it.specialty.trim().equals(
                        selectedSpecialty,
                        ignoreCase = true
                    )
                }
            } else {
                ClinicRepo.doctors
            }

            // Display doctors
            binding.rvDoctors.adapter = DoctorAdapter(
                filteredDoctors
            ) { doctor ->

                // Choose form based on booking flow
                val targetActivity =
                    if (bookingFlow == FLOW_APPOINTMENT) {
                        AppointmentFormActivity::class.java
                    } else {
                        FormActivity::class.java
                    }

                // Open the selected form
                val formIntent = Intent(
                    this,
                    targetActivity
                ).apply {
                    putExtra("doctorId", doctor.id)
                }

                startActivity(formIntent)
            }

            // Empty state message
            if (filteredDoctors.isEmpty()) {
                Toast.makeText(
                    this,
                    "No doctors are available in this specialty yet.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val EXTRA_BOOKING_FLOW = "booking_flow"

        const val FLOW_APPOINTMENT = "appointment"
        const val FLOW_QUEUE = "queue"
    }
}


// =====================================
// DOCTOR ADAPTER
// =====================================

class DoctorAdapter(
    private val items: List<Doctor>,
    private val onClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    class ViewHolder(
        val binding: ItemDoctorBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ViewHolder(
        ItemDoctorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val doctor = items[position]
        val binding = holder.binding

        binding.tvInitial.text = doctor.name
            .removePrefix("dr. ")
            .firstOrNull()
            ?.uppercase() ?: "D"

        binding.tvName.text = doctor.name
        binding.tvSpecialty.text = doctor.specialty
        binding.tvRoom.text = doctor.room

        binding.root.setOnClickListener {
            onClick(doctor)
        }
    }
}
