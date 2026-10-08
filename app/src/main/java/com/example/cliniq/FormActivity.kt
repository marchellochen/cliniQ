package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.cliniq.databinding.ActivityFormBinding

class FormActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Health form")

        val doctor = ClinicRepo.doctors.firstOrNull { it.id == intent.getIntExtra("doctorId", -1) }
        if (doctor == null) { finish(); return }
        binding.tvDoctor.text = "Seeing ${doctor.name}"

        // Pre-fill from the profile row in Supabase
        binding.etAge.setText(ClinicRepo.profile.age?.toString().orEmpty())
        binding.etAllergies.setText(ClinicRepo.profile.allergies.orEmpty())

        binding.btnSubmit.setOnClickListener {
            val age = binding.etAge.text.toString().trim().toIntOrNull()
            val symptoms = binding.etSymptoms.text.toString().trim()
            if (age == null || symptoms.isEmpty()) {
                Toast.makeText(this, "Please fill in age and your health problem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnSubmit.isEnabled = false
            launchSafely(onDone = { binding.btnSubmit.isEnabled = true }) {
                ClinicRepo.book(
                    doctor, symptoms,
                    binding.etDuration.text.toString().trim(),
                    age, binding.etAllergies.text.toString().trim(),
                )
                startActivity(Intent(this, TicketActivity::class.java))
                finish()
            }
        }
    }
}
