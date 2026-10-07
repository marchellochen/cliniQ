package com.example.cliniq

import android.os.Bundle
import com.example.cliniq.databinding.ActivityPharmacyBinding

class PharmacyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPharmacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Pharmacy queue", showBack = false)

        binding.tvPharmacyNo.text = ClinicRepo.pharmacyQueue
        binding.btnDone.setOnClickListener {
            ClinicRepo.finish("Medicine received. Session complete!")
            goHome()
        }
    }
}
