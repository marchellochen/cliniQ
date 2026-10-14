package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cliniq.databinding.ActivityTicketBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TicketActivity : BaseActivity() {

    private lateinit var binding: ActivityTicketBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Your queue ticket")

        val appt = ClinicRepo.appointment
        if (appt == null) {
            finish()
            return
        }

        // Back always returns to Home (not to the form)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        // Fill in the ticket
        val doctor = ClinicRepo.doctorFor(appt.doctorId)
        binding.tvQueueNo.text = appt.queueNo
        binding.tvDoctorName.text = doctor?.name.orEmpty()
        binding.tvDoctorInfo.text = if (doctor != null)
            "${doctor.specialty} · ${doctor.room}"
        else
            ""
        showAhead()
        binding.ivQr.setImageBitmap(qrBitmap(appt.qrPayload))

        // Pull down to refresh the queue position
        binding.swipeRefresh.setColorSchemeResources(R.color.teal)
        binding.swipeRefresh.setOnRefreshListener {
            launchSafely(
                onDone = { binding.swipeRefresh.isRefreshing = false },
            ) {
                refresh()
            }
        }

        // ...and refresh quietly every 10 seconds while this screen is visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(10_000)
                    try {
                        refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // No toast for background refreshes; we simply try again next time
                    }
                }
            }
        }

        // Ending the session: the scanned (or simulated) code must match this ticket
        fun check(content: String) {
            if (content == appt.qrPayload) {
                startActivity(Intent(this, PrescriptionActivity::class.java))
                finish()
            } else {
                Toast.makeText(
                    this,
                    "That QR doesn't match your ticket",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Button to open Google Lens Scanner
//        binding.btnScan.setOnClickListener {
//            scanQr(
//                this,
//                { check(it) },
//                { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() },
//            )
//        }

        binding.btnCancel.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cancel appointment?")
                .setMessage("You'll lose your place in the queue (${appt.queueNo}).")
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Cancel appointment") { _, _ ->
                    binding.btnCancel.isEnabled = false
                    launchSafely(onDone = { binding.btnCancel.isEnabled = true }) {
                        ClinicRepo.cancelAppointment()
                        goHome()
                    }
                }
                .show()
        }

        // Demo only: you can't scan your own screen, so this fakes a successful scan
        binding.btnSimulate.setOnClickListener {
            check(appt.qrPayload)
        }
    }

    /** Reloads the appointment from Supabase and reacts if its status changed. */
    private suspend fun refresh() {
        ClinicRepo.refreshActive()
        val current = ClinicRepo.appointment

        when {
            current == null -> {
                Toast.makeText(
                    this,
                    "This appointment is no longer active",
                    Toast.LENGTH_SHORT
                ).show()
                goHome()
            }

            current.status == "awaiting_medicine" -> {
                startActivity(Intent(this, PharmacyActivity::class.java))
                finish()
            }

            else -> showAhead()
        }
    }

    private fun showAhead() {
        val n = ClinicRepo.peopleAhead

        binding.tvAhead.text = when (n) {
            0 -> "You're next! Please head to the room."
            1 -> "1 person ahead of you"
            else -> "$n people ahead of you"
        }
    }
}
