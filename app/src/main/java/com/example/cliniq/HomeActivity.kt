package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.example.cliniq.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("ClinicQueue", showBack = false)

        binding.btnBook.setOnClickListener { startActivity(Intent(this, DoctorsActivity::class.java)) }
        binding.btnTicket.setOnClickListener { startActivity(Intent(this, TicketActivity::class.java)) }
        binding.btnLogout.setOnClickListener {
            ClinicRepo.logout()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    // Runs every time the screen becomes visible, so it always shows fresh data
    override fun onResume() {
        super.onResume()
        binding.tvGreeting.text = "Hello, ${ClinicRepo.userName}"

        val message = ClinicRepo.lastMessage
        binding.cardMessage.isVisible = message != null
        binding.tvMessage.text = message

        val appt = ClinicRepo.appointment
        binding.cardActive.isVisible = appt != null
        binding.btnTicket.isVisible = appt != null
        binding.btnBook.isVisible = appt == null
        if (appt != null) binding.tvActive.text = "${appt.queueNo} · ${appt.doctor.name}"
    }
}
