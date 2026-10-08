package com.example.cliniq

import android.os.Bundle
import com.example.cliniq.databinding.ActivityPharmacyBinding

class PharmacyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPharmacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Pharmacy queue", showBack = false)

        val appt = ClinicRepo.appointment
        if (appt == null) { finish(); return }
        binding.tvPharmacyNo.text = appt.pharmacyQueueNo

        binding.btnDone.setOnClickListener {
            binding.btnDone.isEnabled = false
            launchSafely(onDone = { binding.btnDone.isEnabled = true }) {
                ClinicRepo.completePickup()
                goHome()
            }
        }
    }
}
