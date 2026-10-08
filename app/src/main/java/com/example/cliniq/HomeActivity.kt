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
        setupBar("cliniq", showBack = false)

        binding.btnBook.setOnClickListener { startActivity(Intent(this, DoctorsActivity::class.java)) }
        binding.btnTicket.setOnClickListener {
            val appt = ClinicRepo.appointment ?: return@setOnClickListener
            val target = if (appt.status == "awaiting_medicine") PharmacyActivity::class.java
                         else TicketActivity::class.java
            startActivity(Intent(this, target))
        }
        binding.btnLogout.setOnClickListener {
            launchSafely {
                ClinicRepo.logout()
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        }
    }

    // Reload from Supabase every time the screen becomes visible
    override fun onResume() {
        super.onResume()
        render()
        launchSafely(onDone = { render() }) { ClinicRepo.refreshHome() }
    }

    private fun render() {
        binding.tvGreeting.text = "Hello, ${ClinicRepo.displayName}"

        val message = ClinicRepo.lastMessage
        binding.cardMessage.isVisible = message != null
        binding.tvMessage.text = message

        val appt = ClinicRepo.appointment
        binding.cardActive.isVisible = appt != null
        binding.btnTicket.isVisible = appt != null
        binding.btnBook.isVisible = appt == null
        if (appt != null) {
            val inPharmacy = appt.status == "awaiting_medicine"
            binding.tvActive.text = if (inPharmacy) "Pharmacy ${appt.pharmacyQueueNo}"
                else "${appt.queueNo} · ${ClinicRepo.doctorFor(appt.doctorId)?.name.orEmpty()}"
            binding.btnTicket.text = if (inPharmacy) "View pharmacy queue" else "View my ticket"
        }
    }
}
