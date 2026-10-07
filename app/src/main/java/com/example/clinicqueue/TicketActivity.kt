package com.example.clinicqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.clinicqueue.databinding.ActivityTicketBinding

class TicketActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Your queue ticket")

        val appt = ClinicRepo.appointment
        if (appt == null) { finish(); return }

        // Back always returns to Home (not to the form)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        binding.tvQueueNo.text = appt.queueNo
        binding.tvDoctorName.text = appt.doctor.name
        binding.tvDoctorInfo.text = "${appt.doctor.specialty} · ${appt.doctor.room}"
        binding.tvAhead.text = "${appt.peopleAhead} people ahead of you"
        binding.ivQr.setImageBitmap(qrBitmap(appt.qrPayload))

        fun check(content: String) {
            if (ClinicRepo.verifyScan(content)) {
                startActivity(Intent(this, PrescriptionActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "That QR doesn't match your ticket", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnScan.setOnClickListener {
            scanQr(this, { check(it) }, { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() })
        }
        // Demo only: you can't scan your own screen, so this fakes a successful scan
        binding.btnSimulate.setOnClickListener { check(appt.qrPayload) }
    }
}
