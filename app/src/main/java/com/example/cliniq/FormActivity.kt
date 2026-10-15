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

        val doctor = ClinicRepo.doctors.firstOrNull {
            it.id == intent.getIntExtra("doctorId", -1)
        }
        if (doctor == null) {
            finish()
            return
        }
        binding.tvDoctor.text = "Seeing ${doctor.name}"

        // Pre-fill from the profile row in Supabase
        var dob: String? = ClinicRepo.profile.dateOfBirth
        binding.etDob.setText(formatDob(dob))
        binding.etAllergies.setText(ClinicRepo.profile.allergies.orEmpty())

        binding.etDob.setOnClickListener {
            pickDob(dob) { picked ->
                dob = picked
                binding.etDob.setText(formatDob(picked))
            }
        }

        binding.btnSubmit.setOnClickListener {
            val dateOfBirth = dob
            val symptoms = binding.etSymptoms.text.toString().trim()

            if (dateOfBirth == null || symptoms.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill in your date of birth and your health problem",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            binding.btnSubmit.isEnabled = false

            launchSafely(onDone = { binding.btnSubmit.isEnabled = true }) {
                ClinicRepo.book(
                    doctor,
                    symptoms,
                    binding.etDuration.text.toString().trim(),
                    dateOfBirth,
                    binding.etAllergies.text.toString().trim(),
                )
                startActivity(Intent(this, TicketActivity::class.java))
                finish()
            }
        }
    }
}