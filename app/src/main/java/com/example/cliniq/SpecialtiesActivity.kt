package com.example.cliniq
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.cliniq.databinding.ActivitySpecialtiesBinding
import com.google.android.material.card.MaterialCardView

class SpecialtiesActivity : BaseActivity() {

    private lateinit var binding: ActivitySpecialtiesBinding

    // =====================================
    // BOOKING FLOW
    // =====================================

    private val bookingFlow: String
        get() = when (
            intent.getStringExtra(DoctorsActivity.EXTRA_BOOKING_FLOW)
        ) {
            DoctorsActivity.FLOW_APPOINTMENT ->
                DoctorsActivity.FLOW_APPOINTMENT

            else -> DoctorsActivity.FLOW_QUEUE
        }


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
            listOf(
                "dokter umum",
                "general practitioner",
                "general practice",
                "general medicine",
                "umum"
            ),
            R.drawable.ic_specialty_general,
            listOf("surgery", "bedah")
        ),

        SpecialtyOption(
            "Internal Medicine",
            listOf(
                "penyakit dalam",
                "internal medicine",
                "sp.pd"
            ),
            R.drawable.ic_specialty_internal
        ),

        SpecialtyOption(
            "Pediatrics",
            listOf(
                "anak",
                "pediatric",
                "pediatri",
                "sp.a"
            ),
            R.drawable.ic_specialty_pediatrics,
            listOf("bedah anak", "pediatric surgery")
        ),

        SpecialtyOption(
            "Obstetrics & Gynecology",
            listOf(
                "obstetri",
                "obstetric",
                "ginekologi",
                "gynecol",
                "obgyn",
                "kandungan",
                "sp.og"
            ),
            R.drawable.ic_specialty_obgyn
        ),

        SpecialtyOption(
            "Cardiology",
            listOf(
                "jantung",
                "cardiology",
                "cardio",
                "kardiologi",
                "sp.jp"
            ),
            R.drawable.ic_specialty_cardiology,
            listOf("cardiothoracic", "thoracic", "bedah")
        ),

        SpecialtyOption(
            "Neurology",
            listOf(
                "neurolog",
                "saraf",
                "sp.n"
            ),
            R.drawable.ic_specialty_neurology,
            listOf("bedah saraf", "neurosurgery")
        ),

        SpecialtyOption(
            "Dermatology",
            listOf(
                "kulit",
                "dermat",
                "venereolog",
                "sp.dv"
            ),
            R.drawable.ic_specialty_dermatology
        ),

        SpecialtyOption(
            "Ophthalmology",
            listOf(
                "mata",
                "eye",
                "ophthalm",
                "oftalm",
                "sp.m"
            ),
            R.drawable.ic_specialty_ophthalmology
        ),

        SpecialtyOption(
            "ENT (Ear, Nose & Throat)",
            listOf(
                "tht",
                "telinga",
                "hidung",
                "tenggorok",
                "otorinolaring"
            ),
            R.drawable.ic_specialty_ent
        ),

        SpecialtyOption(
            "Dentistry",
            listOf(
                "dokter gigi",
                "dentistry",
                "dental",
                "gigi",
                "odont"
            ),
            R.drawable.ic_specialty_dentistry
        )
    )


    // =====================================
    // CREATE ACTIVITY
    // =====================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySpecialtiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBar("Medical Specialties")

        // Set specialty grid to two columns.

        binding.tvEmpty.visibility = View.GONE

        // Load doctors from Supabase.
        binding.rvResults.layoutManager = LinearLayoutManager(this)

        binding.etSearchDoctor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applySearch()
        })

        launchSafely {
            ClinicRepo.loadDoctors()
            renderSpecialties()
            applySearch()
        }

        // Branches come from the schedule view; the dropdown stays usable even if this fails.
        launchSafely {
            val schedules = DoctorProfileRepository().getAllSchedules()
            setupBranchDropdown(schedules)
        }
    }


    // =====================================
    // SEARCH (DOCTOR NAME + BRANCH)
    // =====================================

    private var selectedBranchId: Int? = null
    private var doctorIdsByBranch: Map<Int, Set<Int>> = emptyMap()

    /** Adapter that always lists every branch, even after one was selected. */
    private class AllItemsAdapter(
        context: android.content.Context,
        private val items: List<String>
    ) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, items) {
        override fun getFilter() = object : Filter() {
            override fun performFiltering(c: CharSequence?) =
                FilterResults().also { it.values = items; it.count = items.size }

            override fun publishResults(c: CharSequence?, r: FilterResults?) =
                notifyDataSetChanged()
        }
    }

    private fun setupBranchDropdown(schedules: List<DoctorSchedule>) {
        val branches = schedules
            .filter { it.branchId != null && !it.branchName.isNullOrBlank() }
            .distinctBy { it.branchId }
            .sortedBy { it.branchName }

        doctorIdsByBranch = schedules
            .filter { it.branchId != null }
            .groupBy({ it.branchId!! }, { it.doctorId })
            .mapValues { it.value.toSet() }

        val allLabel = "All branches"
        val labels = listOf(allLabel) + branches.map { it.branchName!! }

        binding.actBranch.setAdapter(AllItemsAdapter(this, labels))
        binding.actBranch.setOnItemClickListener { _, _, position, _ ->
            selectedBranchId = if (position == 0) null else branches[position - 1].branchId
            applySearch()
        }
    }

    private fun applySearch() {
        val query = binding.etSearchDoctor.text?.toString()?.trim().orEmpty()
        val branchId = selectedBranchId
        val searching = query.isNotEmpty() || branchId != null

        binding.layoutSpecialtiesGrid.visibility = if (searching) View.GONE else View.VISIBLE
        binding.tvTitle.text = if (searching) "Search results" else "All Medical Specialties"
        binding.rvResults.visibility = if (searching) View.VISIBLE else View.GONE

        if (!searching) {
            binding.tvNoResults.visibility = View.GONE
            return
        }

        val allowedIds = branchId?.let { doctorIdsByBranch[it].orEmpty() }
        val results = ClinicRepo.doctors.filter { doctor ->
            doctor.name.contains(query, ignoreCase = true) &&
                (allowedIds == null || doctor.id in allowedIds)
        }

        binding.tvNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        binding.rvResults.adapter = DoctorAdapter(results) { doctor ->
            startActivity(
                Intent(this, DoctorProfileActivity::class.java)
                    .putExtra("doctorId", doctor.id)
                    .putExtra(DoctorsActivity.EXTRA_BOOKING_FLOW, bookingFlow)
            )
        }
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
    // RENDER SPECIALTIES
    // =====================================

    private fun renderSpecialties() {

        val container = binding.layoutSpecialtiesGrid

        container.removeAllViews()

        binding.tvEmpty.visibility = View.GONE

        val density = resources.displayMetrics.density

        fun dp(value: Int): Int {
            return (value * density).toInt()
        }

        specialtyCatalog.forEach { option ->

            // Find doctors matching the specialty.
            val matchingDoctors = ClinicRepo.doctors.filter {
                matchesSpecialty(it.specialty, option)
            }

            val actualSpecialty = matchingDoctors
                .firstOrNull()
                ?.specialty
                ?.trim()

            // Count doctors with the same exact specialty.
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


            // =====================================
            // SPECIALTY CARD
            // =====================================

            val card = MaterialCardView(this).apply {

                val params = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                )

                params.width = 0
                params.height = dp(154)

                params.setMargins(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6)
                )

                layoutParams = params

                radius = dp(15).toFloat()

                setCardBackgroundColor(Color.WHITE)

                setStrokeColor(
                    ColorStateList.valueOf(
                        Color.parseColor("#E0E7EF")
                    )
                )

                strokeWidth = dp(1)
                cardElevation = dp(2).toFloat()

                isClickable = true
                isFocusable = true
            }


            // =====================================
            // CARD CONTENT
            // =====================================

            val content = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(12),
                    dp(8),
                    dp(10)
                )
            }


            // =====================================
            // SPECIALTY ICON
            // =====================================

            val icon = AppCompatImageView(this).apply {

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


            // =====================================
            // SPECIALTY NAME
            // =====================================

            val title = TextView(this).apply {

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


            // =====================================
            // DOCTOR COUNT
            // =====================================

            val count = TextView(this).apply {

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


            // =====================================
            // CARD CLICK
            // =====================================

            card.setOnClickListener {

                if (actualSpecialty != null) {

                    openDoctors(actualSpecialty)

                } else {

                    Toast.makeText(
                        this,
                        "No doctors are available in this specialty yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            container.addView(card)
        }
    }


    // =====================================
    // OPEN DOCTORS ACTIVITY
    // =====================================

    private fun openDoctors(specialty: String) {

        val doctorIntent = Intent(
            this,
            DoctorsActivity::class.java
        ).apply {

            putExtra(
                "specialty",
                specialty
            )

            // Pass the original booking flow.
            putExtra(
                DoctorsActivity.EXTRA_BOOKING_FLOW,
                bookingFlow
            )
        }

        startActivity(doctorIntent)
    }
}
