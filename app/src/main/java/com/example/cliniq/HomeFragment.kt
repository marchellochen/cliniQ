package com.example.cliniq

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment

import com.example.cliniq.databinding.FragmentHomeBinding
import com.example.cliniq.finddoctor.FindDoctorActivity
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // =====================================
    // SPECIALTY DATA
    // =====================================

    private data class SpecialtyOption(
        val name: String,
        val keywords: List<String>,
        val iconRes: Int,
        val excludedKeywords: List<String> = emptyList()
    )

    private val specialtyCatalog = listOf(

        SpecialtyOption(
            "General Practice",
            listOf("dokter umum", "general practitioner", "general practice", "general medicine", "umum"),
            R.drawable.ic_specialty_general,
            listOf("surgery", "bedah")
        ),

        SpecialtyOption(
            "Internal Medicine",
            listOf("penyakit dalam", "internal medicine", "sp.pd"),
            R.drawable.ic_specialty_internal
        ),

        SpecialtyOption(
            "Pediatrics",
            listOf("anak", "pediatric", "pediatri", "sp.a"),
            R.drawable.ic_specialty_pediatrics,
            listOf("bedah anak", "pediatric surgery")
        ),

        SpecialtyOption(
            "Obstetrics & Gynecology",
            listOf("obstetri", "obstetric", "ginekologi", "gynecol", "obgyn", "kandungan", "sp.og"),
            R.drawable.ic_specialty_obgyn
        ),

        SpecialtyOption(
            "Cardiology",
            listOf("jantung", "cardiology", "cardio", "kardiologi", "sp.jp"),
            R.drawable.ic_specialty_cardiology,
            listOf("cardiothoracic", "thoracic", "bedah")
        ),

        SpecialtyOption(
            "Neurology",
            listOf("neurolog", "saraf", "sp.n"),
            R.drawable.ic_specialty_neurology,
            listOf("bedah saraf", "neurosurgery")
        ),

        SpecialtyOption(
            "Dermatology",
            listOf("kulit", "dermat", "venereolog", "sp.dv"),
            R.drawable.ic_specialty_dermatology
        ),

        SpecialtyOption(
            "Ophthalmology",
            listOf("mata", "eye", "ophthalm", "oftalm", "sp.m"),
            R.drawable.ic_specialty_ophthalmology
        ),

        SpecialtyOption(
            "ENT (Ear, Nose & Throat)",
            listOf("tht", "telinga", "hidung", "tenggorok", "otorinolaring"),
            R.drawable.ic_specialty_ent
        ),

        SpecialtyOption(
            "Dentistry",
            listOf("dokter gigi", "dentistry", "dental", "gigi", "odont"),
            R.drawable.ic_specialty_dentistry
        )
    )


    // =====================================
    // CREATE VIEW
    // =====================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }


    // =====================================
    // VIEW INITIALIZATION
    // =====================================

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Queue: Get Queue Number
        binding.btnBook.text = "Get Queue Number"

        binding.btnBook.setOnClickListener {
            startActivity(Intent(requireContext(), FindDoctorActivity::class.java))
            openSpecialties(DoctorsActivity.FLOW_QUEUE)
        }

        // Appointment: Find a Doctor
        binding.btnFindDoctor.setOnClickListener {
            openSpecialties(DoctorsActivity.FLOW_APPOINTMENT)
        }

        // Search from Home uses Queue Flow.
        binding.cardSearch.setOnClickListener {
            openSpecialties(DoctorsActivity.FLOW_QUEUE)
        }

        // View All specialties from Home uses Queue Flow.
        binding.tvSeeAll.setOnClickListener {
            openSpecialties(DoctorsActivity.FLOW_QUEUE)
        }

        // View existing queue ticket.
        binding.btnTicket.setOnClickListener {
            (requireActivity() as HomeActivity)
                .showTab(R.id.nav_queue)
        }
    }


    // =====================================
    // NAVIGATION
    // =====================================

    private fun openSpecialties(flow: String) {

        val context = requireContext()
        val host = requireActivity() as HomeActivity

        // Activate the correct bottom navigation tab.
        val targetTab = if (flow == DoctorsActivity.FLOW_QUEUE) {
            R.id.nav_queue
        } else {
            R.id.nav_appointments
        }

        host.showTab(targetTab)

        // Open specialty selection and pass the booking flow.
        val intent = Intent(
            context,
            SpecialtiesActivity::class.java
        ).apply {
            putExtra(
                DoctorsActivity.EXTRA_BOOKING_FLOW,
                flow
            )
        }

        startActivity(intent)
    }


    private fun openDoctors(
        specialty: String,
        flow: String = DoctorsActivity.FLOW_QUEUE
    ) {

        val context = requireContext()
        val host = requireActivity() as HomeActivity

        val targetTab = if (flow == DoctorsActivity.FLOW_QUEUE) {
            R.id.nav_queue
        } else {
            R.id.nav_appointments
        }

        host.showTab(targetTab)

        val intent = Intent(
            context,
            DoctorsActivity::class.java
        ).apply {
            putExtra("specialty", specialty)

            putExtra(
                DoctorsActivity.EXTRA_BOOKING_FLOW,
                flow
            )
        }

        startActivity(intent)
    }


    // =====================================
    // REFRESH HOME DATA
    // =====================================

    override fun onResume() {
        super.onResume()

        render()

        launchSafely(
            onDone = {
                if (_binding != null) {
                    render()
                }
            }
        ) {
            ClinicRepo.refreshHome()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // =====================================
    // RENDER HOME
    // =====================================

    private fun render() {

        binding.tvGreeting.text = "Welcome,"
        binding.tvPatientName.text = ClinicRepo.displayName

        binding.tvGreetingSubtitle.text =
            "Wishing you and your loved ones good health."

        // Queue button label.
        binding.btnBook.text = "Get Queue Number"

        val message = ClinicRepo.lastMessage

        binding.cardMessage.isVisible = message != null
        binding.tvMessage.text = message.orEmpty()

        val appt = ClinicRepo.appointment

        binding.cardActive.isVisible = true

        if (appt == null) {

            binding.tvQueueStatus.text = "NO ACTIVE QUEUE"

            binding.tvActive.text = "No Active Queue"

            binding.tvQueueDescription.text =
                "You don't have an active queue number today."

            binding.btnBook.isVisible = true
            binding.btnTicket.isVisible = false

        } else {

            val inPharmacy = appt.status == "awaiting_medicine"

            binding.tvQueueStatus.text = when (appt.status) {
                "waiting" -> "WAITING"
                "awaiting_medicine" -> "WAITING FOR MEDICINE"
                else -> appt.status.uppercase()
            }

            binding.tvActive.text = if (inPharmacy) {
                "Pharmacy Queue ${appt.pharmacyQueueNo.orEmpty()}"
            } else {
                "Queue Number ${appt.queueNo}"
            }

            val doctor = ClinicRepo.doctorFor(appt.doctorId)

            binding.tvQueueDescription.text = buildString {

                if (doctor != null) {
                    append(doctor.name)
                    append(" · ")
                    append(doctor.specialty)
                    append("\n")
                }

                if (!inPharmacy) {
                    append("${ClinicRepo.peopleAhead} people ahead of you.")
                } else {
                    append("Please proceed to the pharmacy counter.")
                }
            }

            binding.btnBook.isVisible = false
            binding.btnTicket.isVisible = true

            binding.btnTicket.text = if (inPharmacy) {
                "View Pharmacy Queue"
            } else {
                "View Queue Ticket"
            }
        }

        renderSpecialties()
    }


    // =====================================
    // MATCH SPECIALTY
    // =====================================

    private fun matchesSpecialty(
        doctorSpecialty: String,
        option: SpecialtyOption
    ): Boolean {

        val value = doctorSpecialty.trim()

        if (option.excludedKeywords.any {
                value.contains(it, ignoreCase = true)
            }) {
            return false
        }

        return option.keywords.any {
            value.contains(it, ignoreCase = true)
        }
    }


    // =====================================
    // RENDER SPECIALTY CARDS
    // =====================================

    private fun renderSpecialties() {

        val container = binding.layoutSpecialties

        container.removeAllViews()

        binding.tvSpecialtyEmpty.isVisible = false

        val density = resources.displayMetrics.density

        fun dp(value: Int): Int {
            return (value * density).toInt()
        }

        specialtyCatalog.forEach { option ->

            val matchingDoctors = ClinicRepo.doctors.filter {
                matchesSpecialty(it.specialty, option)
            }

            val actualSpecialty = matchingDoctors
                .firstOrNull()
                ?.specialty
                ?.trim()

            val availableDoctors = if (actualSpecialty != null) {
                matchingDoctors.filter {
                    it.specialty.trim().equals(
                        actualSpecialty,
                        ignoreCase = true
                    )
                }
            } else {
                emptyList()
            }

            // CARD
            val card = MaterialCardView(requireContext()).apply {

                layoutParams = LinearLayout.LayoutParams(
                    dp(150),
                    dp(154)
                ).apply {
                    marginEnd = dp(12)
                }

                radius = dp(15).toFloat()

                setCardBackgroundColor(Color.WHITE)

                setStrokeColor(
                    ColorStateList.valueOf(
                        Color.parseColor("#E0E7EF")
                    )
                )

                strokeWidth = dp(1)
                cardElevation = dp(1).toFloat()

                isClickable = true
                isFocusable = true
            }

            val content = LinearLayout(requireContext()).apply {

                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(12),
                    dp(8),
                    dp(10)
                )
            }

            // SPECIALTY VECTOR ICON
            val icon = AppCompatImageView(requireContext()).apply {

                setImageResource(option.iconRes)

                imageTintList = ColorStateList.valueOf(
                    Color.parseColor("#006493")
                )

                scaleType = ImageView.ScaleType.FIT_CENTER

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#E5F1FC"))
                }
            }

            content.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(50),
                    dp(50)
                )
            )

            // SPECIALTY NAME
            val title = TextView(requireContext()).apply {

                text = option.name
                textSize = 11f

                gravity = Gravity.CENTER

                setTextColor(
                    Color.parseColor("#17243A")
                )

                typeface = Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )

                maxLines = 2

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            }

            content.addView(title)

            // DOCTOR COUNT
            val count = TextView(requireContext()).apply {

                text = when (availableDoctors.size) {
                    0 -> "Coming Soon"
                    1 -> "1 Doctor"
                    else -> "${availableDoctors.size} Doctors"
                }

                textSize = 10f

                gravity = Gravity.CENTER

                setTextColor(
                    Color.parseColor("#006493")
                )

                setTypeface(null, Typeface.BOLD)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(4)
                }
            }

            content.addView(count)

            card.addView(content)

            card.setOnClickListener {

                if (actualSpecialty != null) {
                    openDoctors(
                        actualSpecialty,
                        DoctorsActivity.FLOW_QUEUE
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No doctors are available in this specialty yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            container.addView(card)
        }
    }
}
