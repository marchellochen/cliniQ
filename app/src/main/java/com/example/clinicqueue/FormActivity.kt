package com.example.clinicqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.clinicqueue.databinding.ActivityFormBinding

class FormActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Health form")

        val doctorId = intent.getIntExtra("doctorId", -1)
        val doctor = ClinicRepo.doctors.first { it.id == doctorId }
        binding.tvDoctor.text = "Seeing ${doctor.name}"

        // Pre-fill from the saved profile
        binding.etAge.setText(ClinicRepo.profile.age)
        binding.etAllergies.setText(ClinicRepo.profile.allergies)

        binding.btnSubmit.setOnClickListener {
            val age = binding.etAge.text.toString().trim()
            val symptoms = binding.etSymptoms.text.toString().trim()
            if (age.isEmpty() || symptoms.isEmpty()) {
                Toast.makeText(this, "Please fill in age and your health problem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ClinicRepo.book(
                doctor, symptoms, binding.etDuration.text.toString().trim(),
                Profile(age, binding.etAllergies.text.toString().trim()),
            )
            startActivity(Intent(this, TicketActivity::class.java))
            finish()
        }
    }
}
