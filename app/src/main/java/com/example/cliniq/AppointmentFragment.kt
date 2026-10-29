package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cliniq.databinding.FragmentAppointmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppointmentFragment : Fragment() {

    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!

    // The QR code is only redrawn when the ticket changes
    private var shownQr: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pull down to refresh the queue position
        binding.swipeRefresh.setColorSchemeResources(R.color.teal)
        binding.swipeRefresh.setOnRefreshListener {
            launchSafely(onDone = { binding.swipeRefresh.isRefreshing = false }) {
                ClinicRepo.refreshActive()
                render()
            }
        }

        // While this tab is the one on screen: show what we have, then re-check
        // the queue every 10 seconds. It pauses when you leave the tab.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                render()
                while (true) {
                    refreshQuietly()
                    delay(10_000)
                }
            }
        }

        // Demo only: you can't scan your own screen, so this fakes a successful scan
        binding.btnSimulate.setOnClickListener {
            val payload = ClinicRepo.appointment?.qrPayload ?: return@setOnClickListener
            checkQr(payload)
        }

        binding.btnCancel.setOnClickListener {
            val appt = ClinicRepo.appointment ?: return@setOnClickListener

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel appointment?")
                .setMessage("You'll lose your place in the queue (${appt.queueNo}).")
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Cancel appointment") { _, _ ->
                    binding.btnCancel.isEnabled = false
                    launchSafely(onDone = { binding.btnCancel.isEnabled = true }) {
                        ClinicRepo.cancelAppointment()
                        render()
                        toast("Appointment cancelled")
                    }
                }
                .show()
        }

        binding.btnDone.setOnClickListener {
            binding.btnDone.isEnabled = false
            launchSafely(onDone = { binding.btnDone.isEnabled = true }) {
                ClinicRepo.completePickup()
                render()
                toast("Medicine received. Session complete!")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownQr = null
        _binding = null
    }

    /** Reloads the appointment from Supabase and redraws. Errors are ignored (we retry in 10 s). */
    private suspend fun refreshQuietly() {
        val hadAppointment = ClinicRepo.appointment != null

        try {
            if (ClinicRepo.doctors.isEmpty()) ClinicRepo.loadDoctors()
            ClinicRepo.refreshActive()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }

        render()
        if (hadAppointment && ClinicRepo.appointment == null) {
            toast("This appointment is no longer active")
        }
    }

    /** Shows the empty message, the ticket or the pharmacy queue, depending on the status. */
    private fun render() {
        val appt = ClinicRepo.appointment

        binding.tvEmpty.isVisible = appt == null
        binding.layoutTicket.isVisible = appt?.status == "waiting"
        binding.layoutPharmacy.isVisible = appt?.status == "awaiting_medicine"

        if (appt == null) {
            shownQr = null
            return
        }

        if (appt.status == "awaiting_medicine") {
            binding.tvPharmacyNo.text = appt.pharmacyQueueNo
            return
        }

        // Waiting for the doctor
        val doctor = ClinicRepo.doctorFor(appt.doctorId)
        binding.tvQueueNo.text = appt.queueNo
        binding.tvDoctorName.text = doctor?.name.orEmpty()
        binding.tvDoctorInfo.text = if (doctor != null)
            "${doctor.specialty} · ${doctor.room}"
        else
            ""

        val n = ClinicRepo.peopleAhead
        binding.tvAhead.text = when (n) {
            0 -> "You're next! Please head to the room."
            1 -> "1 person ahead of you"
            else -> "$n people ahead of you"
        }

        if (shownQr != appt.qrPayload) {
            binding.ivQr.setImageBitmap(qrBitmap(appt.qrPayload))
            shownQr = appt.qrPayload
        }
    }

    // Ending the session: the scanned (or simulated) code must match this ticket
    private fun checkQr(content: String) {
        if (content == ClinicRepo.appointment?.qrPayload) {
            startActivity(Intent(requireContext(), PrescriptionActivity::class.java))
        } else {
            toast("That QR doesn't match your ticket")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}