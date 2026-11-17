package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.example.cliniq.databinding.ActivityDoctorProfileBinding
import com.example.cliniq.databinding.ItemScheduleBinding
import com.example.cliniq.databinding.ViewDoctorHeaderBinding
import com.example.cliniq.databinding.ViewInfoItemBinding
import com.example.cliniq.databinding.ViewSessionChipBinding
import kotlinx.coroutines.launch

class DoctorProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityDoctorProfileBinding

    private val doctorId by lazy { intent.getIntExtra("doctorId", -1) }

    private val viewModel: DoctorProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                DoctorProfileViewModel(doctorId) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Doctor Profile")

        if (doctorId == -1) {
            finish()
            return
        }

        keepContentBelowActionBar(binding.root)

        binding.btnRetry.setOnClickListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: DoctorProfileState) {
        binding.progress.visibility = if (state is DoctorProfileState.Loading) View.VISIBLE else View.GONE
        binding.errorView.visibility = if (state is DoctorProfileState.Error) View.VISIBLE else View.GONE
        binding.scrollContent.visibility = if (state is DoctorProfileState.Success) View.VISIBLE else View.GONE
        binding.btnBook.isEnabled = state is DoctorProfileState.Success && state.schedules.isNotEmpty()

        when (state) {
            is DoctorProfileState.Error -> binding.tvError.text = state.message
            is DoctorProfileState.Success -> showProfile(state.doctor, state.schedules)
            DoctorProfileState.Loading -> Unit
        }
    }

    private fun showProfile(doctor: Doctor, schedules: List<DoctorSchedule>) {
        DoctorHeader(ViewDoctorHeaderBinding.bind(binding.header.root), doctor)

        binding.tvBio.text = doctor.bio?.takeIf { it.isNotBlank() } ?: "No information available."

        binding.scheduleList.removeAllViews()
        schedules.forEach { binding.scheduleList.addView(ScheduleCard(binding.scheduleList, it)) }
        binding.tvNoSchedule.visibility = if (schedules.isEmpty()) View.VISIBLE else View.GONE

        binding.btnBook.setOnClickListener {
            // Same routing as the Doctors screen used to do: the existing form for the chosen flow.
            val target =
                if (intent.getStringExtra(DoctorsActivity.EXTRA_BOOKING_FLOW) == DoctorsActivity.FLOW_APPOINTMENT) {
                    AppointmentFormActivity::class.java
                } else {
                    FormActivity::class.java
                }
            startActivity(Intent(this, target).putExtra("doctorId", doctor.id))
        }
    }

    // ----- Reusable pieces -----

    private fun DoctorHeader(header: ViewDoctorHeaderBinding, doctor: Doctor) {
        header.tvName.text = doctor.name
        header.tvSpecialty.text = doctor.specialty

        header.ivPhoto.load(doctor.photoUrl?.takeIf { it.isNotBlank() }) {
            placeholder(R.drawable.ic_doctor_placeholder)
            error(R.drawable.ic_doctor_placeholder)
            fallback(R.drawable.ic_doctor_placeholder)
            transformations(CircleCropTransformation())
        }

        header.infoRow.removeAllViews()
        // Room is stored with its own "Room" prefix (e.g. "Room 203"), so show it as-is.
        header.infoRow.addView(InfoItem(header.infoRow, R.drawable.ic_location, doctor.room))
        doctor.experienceYears?.let {
            header.infoRow.addView(
                InfoItem(header.infoRow, R.drawable.ic_work, "$it years experience").apply {
                    (layoutParams as LinearLayout.LayoutParams).marginStart = 16.dp()
                }
            )
        }
    }

    private fun InfoItem(parent: ViewGroup, icon: Int, text: String): View {
        val item = ViewInfoItemBinding.inflate(LayoutInflater.from(this), parent, false)
        item.ivIcon.setImageResource(icon)
        item.tvText.text = text
        return item.root
    }

    private fun ScheduleCard(parent: ViewGroup, schedule: DoctorSchedule): View {
        val card = ItemScheduleBinding.inflate(LayoutInflater.from(this), parent, false)
        card.tvDay.text = dayName(schedule.dayOfWeek)
        card.tvBranch.text = schedule.branchName.orEmpty()
        card.tvTime.text = "${hhmm(schedule.startTime)} – ${hhmm(schedule.endTime)}"
        SessionChip(ViewSessionChipBinding.bind(card.chip.root), schedule.session)
        return card.root
    }

    private fun SessionChip(chip: ViewSessionChipBinding, session: String) {
        val (label, icon) = when (session.lowercase()) {
            "morning" -> "Morning" to R.drawable.ic_wb_sunny
            "afternoon" -> "Afternoon" to R.drawable.ic_wb_twilight
            "night" -> "Night" to R.drawable.ic_nights_stay
            else -> session.replaceFirstChar { it.uppercase() } to R.drawable.ic_wb_sunny
        }
        chip.tvSession.text = label
        chip.ivSession.setImageResource(icon)
    }

    private fun dayName(day: Int) =
        java.time.DayOfWeek.of(day.coerceIn(1, 7))
            .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)

    /** "13:00:00" becomes "13:00". */
    private fun hhmm(time: String) = time.take(5)

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
