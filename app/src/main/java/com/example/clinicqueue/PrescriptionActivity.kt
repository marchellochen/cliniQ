package com.example.clinicqueue

import android.content.Intent
import android.os.Bundle
import com.example.clinicqueue.databinding.ActivityPrescriptionBinding

class PrescriptionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPrescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Session finished", showBack = false)

        binding.btnYes.setOnClickListener {
            ClinicRepo.requestMedicine()
            startActivity(Intent(this, PharmacyActivity::class.java))
            finish()
        }
        binding.btnNo.setOnClickListener {
            ClinicRepo.finish("Session complete. Get well soon!")
            goHome()
        }
    }
}
