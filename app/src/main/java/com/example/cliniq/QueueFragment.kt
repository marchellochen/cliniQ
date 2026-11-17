package com.example.cliniq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cliniq.databinding.FragmentQueueBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueueViewModel by viewModels()

    // The QR code is only redrawn when the token changes
    private var shownQr: String? = null

    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pull down to refresh
        binding.swipeRefresh.setColorSchemeResources(R.color.teal)
        binding.swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.refresh()
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // Draw whatever the ViewModel has
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }

        // While this tab is on screen, refresh every 5 seconds.
        // Stops when the ticket is completed and pauses when the tab is left.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    viewModel.refresh()
                    val status = (viewModel.state.value as? QueueState.Success)
                        ?.data?.appointment?.status
                    if (status == "done" || status == "completed") break
                    delay(5_000)
                }
            }
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
                        viewModel.refresh()
                        toast("Appointment cancelled")
                    }
                }
                .show()
        }

        binding.btnDone.setOnClickListener {
            binding.btnDone.isEnabled = false
            launchSafely(onDone = { binding.btnDone.isEnabled = true }) {
                ClinicRepo.completePickup()
                viewModel.refresh()
                toast("Medicine received. Session complete!")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownQr = null
        _binding = null
    }

    private fun render(state: QueueState) {
        when (state) {
            QueueState.Loading -> {
                binding.tvEmpty.isVisible = false
                binding.layoutTicket.isVisible = false
                binding.layoutPharmacy.isVisible = false
            }

            is QueueState.Error -> {
                binding.tvEmpty.text = "${state.message}\nPull down to try again."
                binding.tvEmpty.isVisible = true
                binding.layoutTicket.isVisible = false
                binding.layoutPharmacy.isVisible = false
            }

            is QueueState.Success -> renderTicket(state.data)
        }
    }

    private fun renderTicket(data: QueueData) {
        val appt = data.appointment
        val status = appt?.status

        binding.tvEmpty.text = "No appointments yet. Glad to see you well!"
        binding.tvEmpty.isVisible = appt == null
        binding.layoutPharmacy.isVisible = status == "awaiting_medicine"
        binding.layoutTicket.isVisible =
            status == "waiting" || status == "called" || status == "done" || status == "completed"

        if (appt == null) {
            shownQr = null
            return
        }

        if (status == "awaiting_medicine") {
            binding.tvPharmacyNo.text = appt.pharmacyQueueNo
            return
        }

        // ----- Status badge -----
        val badge = binding.tvStatus
        when (status) {
            "called" -> {
                badge.text = "It's your turn!"
                badge.setBackgroundResource(R.drawable.bg_badge_primary)
                badge.setTextColor(requireContext().getColor(R.color.white))
            }

            "done", "completed" -> {
                badge.text = "Completed"
                badge.setBackgroundResource(R.drawable.bg_session_chip)
                badge.setTextColor(requireContext().getColor(R.color.status_done))
            }

            else -> {
                badge.text = "Waiting"
                badge.setBackgroundResource(R.drawable.bg_session_chip)
                badge.setTextColor(requireContext().getColor(R.color.teal))
            }
        }

        // ----- Ticket details -----
        binding.tvQueueNo.text = appt.queueNo
        binding.tvDoctorName.text = data.doctor?.name.orEmpty()
        binding.tvDoctorInfo.text = data.doctor?.room.orEmpty()

        val date = appt.appointmentDate
            ?.let { runCatching { LocalDate.parse(it.take(10)).format(dateFormat) }.getOrNull() }
        binding.tvSchedule.text = listOfNotNull(
            date,
            data.session?.replaceFirstChar { it.uppercase() },
        ).joinToString(" · ")
        binding.tvSchedule.isVisible = binding.tvSchedule.text.isNotEmpty()

        // ----- QR: only while the ticket is still usable -----
        val showQr = (status == "waiting" || status == "called") && !appt.qrToken.isNullOrBlank()
        binding.layoutQr.isVisible = showQr
        binding.tvQrHint.isVisible = showQr
        if (showQr && shownQr != appt.qrToken) {
            binding.ivQr.setImageBitmap(qrBitmap(appt.qrToken!!))
            shownQr = appt.qrToken
        }

        // ----- Now serving -----
        val serving = data.nowServing
        binding.cardNowServing.isVisible = serving != null
        binding.tvServingNo.text = serving?.queueNo ?: "–"
        binding.tvWaitingCount.text = (serving?.waitingCount ?: 0).toString()

        // Only a still-waiting ticket can be cancelled
        binding.btnCancel.isVisible = status == "waiting"
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
