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
        keepContentBelowActionBar(binding.root)

        val doctor = ClinicRepo.doctors.firstOrNull {
            it.id == intent.getIntExtra("doctorId", -1)
        }
        if (doctor == null) {
            finish()
            return
        }
        binding.tvDoctor.text = "Seeing ${doctor.name}"

        binding.btnSubmit.setOnClickListener {
            val symptoms = binding.etSymptoms.text.toString().trim()

            if (symptoms.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please describe your health problem",
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
                )
                goHome(R.id.nav_queue)
            }
        }
    }
}