package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import com.example.cliniq.databinding.ActivityPrescriptionBinding

class PrescriptionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPrescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Session finished", showBack = false)

        // Disable both buttons while a request is running
        fun setBusy(busy: Boolean) {
            binding.btnYes.isEnabled = !busy
            binding.btnNo.isEnabled = !busy
        }

        // Yes: create a pharmacy queue number, then show the pharmacy screen
        binding.btnYes.setOnClickListener {
            setBusy(true)

            launchSafely(onDone = { setBusy(false) }) {
                ClinicRepo.finishSession(needsMedicine = true)
                goHome(R.id.nav_appointments)
            }
        }

        // No: the session is complete, go back to Home
        binding.btnNo.setOnClickListener {
            setBusy(true)

            launchSafely(onDone = { setBusy(false) }) {
                ClinicRepo.finishSession(needsMedicine = false)
                goHome()
            }
        }
    }
}
