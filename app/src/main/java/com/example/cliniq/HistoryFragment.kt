package com.example.cliniq

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cliniq.databinding.FragmentHistoryBinding
import com.example.cliniq.databinding.ItemHistoryBinding
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())

        binding.swipeRefresh.setColorSchemeResources(R.color.teal)
        binding.swipeRefresh.setOnRefreshListener { load() }
    }

    // Reload every time this tab becomes visible, so new or cancelled bookings show up
    override fun onResume() {
        super.onResume()
        binding.swipeRefresh.isRefreshing = true
        load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun load() = launchSafely(
        onDone = { binding.swipeRefresh.isRefreshing = false },
    ) {
        // Load doctors first so each row can show the doctor's name
        ClinicRepo.loadDoctors()

        val items = ClinicRepo.loadHistory()
        binding.rvHistory.adapter = HistoryAdapter(items)
        binding.tvEmpty.isVisible = items.isEmpty()
    }
}


private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

/** Supabase sends timestamps, then shows it in phone's timezone. */
private fun formatDate(iso: String?): String = try {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(dateFormat)
} catch (e: Exception) {
    iso.orEmpty()
}


/** One row (item_history.xml) per past appointment. */
class HistoryAdapter(
    private val items: List<Appointment>,
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appt = items[position]
        val b = holder.binding
        val doctor = ClinicRepo.doctorFor(appt.doctorId)

        b.tvQueue.text = appt.queueNo
        b.tvDoctor.text = if (doctor != null)
            "${doctor.name} · ${doctor.specialty}"
        else
            "Doctor #${appt.doctorId}"
        b.tvDate.text = formatDate(appt.createdAt)
        b.tvSymptoms.text = appt.symptoms

        b.tvPharmacy.isVisible = appt.pharmacyQueueNo != null
        b.tvPharmacy.text = "Pharmacy queue: ${appt.pharmacyQueueNo}"

        // Status label and its color
        val (label, color) = when (appt.status) {
            "waiting" -> "Waiting" to R.color.status_waiting
            "awaiting_medicine" -> "Awaiting medicine" to R.color.status_pharmacy
            "completed" -> "Completed" to R.color.status_done
            else -> "Cancelled" to R.color.status_cancelled
        }
        b.tvStatus.text = label
        b.tvStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(holder.itemView.context, color)
        )
    }
}
