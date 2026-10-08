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

        fun setBusy(busy: Boolean) {
            binding.btnYes.isEnabled = !busy
            binding.btnNo.isEnabled = !busy
        }

        binding.btnYes.setOnClickListener {
            setBusy(true)
            launchSafely(onDone = { setBusy(false) }) {
                ClinicRepo.finishSession(needsMedicine = true)   // creates the pharmacy queue number
                startActivity(Intent(this, PharmacyActivity::class.java))
                finish()
            }
        }
        binding.btnNo.setOnClickListener {
            setBusy(true)
            launchSafely(onDone = { setBusy(false) }) {
                ClinicRepo.finishSession(needsMedicine = false)
                goHome()
            }
        }
    }
}
