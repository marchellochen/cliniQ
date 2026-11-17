package com.example.cliniq

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.example.cliniq.databinding.ActivityAppointmentFormBinding
import com.google.android.material.card.MaterialCardView

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppointmentFormActivity : BaseActivity() {

    private lateinit var binding: ActivityAppointmentFormBinding

    private var selectedDoctor: Doctor? = null


    private data class ConsultationSessionOption(
        val code: String,
        val name: String,
        val timeRange: String,
        val available: Boolean
    )

    private data class AppointmentDateOption(
        val date: Calendar,
        val available: Boolean,
        val sessions: List<ConsultationSessionOption>
    )

    private var availableDates: List<AppointmentDateOption> = emptyList()

    private var selectedDate: AppointmentDateOption? = null
    private var selectedSession: ConsultationSessionOption? = null




    private var selectedPaymentMethod = "BPJS"

    private var pendingBookingRequest: AppointmentBookingRequest? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppointmentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBar("Book Appointment")

        // Get selected doctor.
        val doctorId = intent.getIntExtra(EXTRA_DOCTOR_ID, -1)

        selectedDoctor = ClinicRepo.doctorFor(doctorId)

        if (selectedDoctor == null) {
            showMessage("Doctor information not found. Please select again.")
            finish()
            return
        }

        displayDoctorInfo()

        setupPaymentListeners()
        setupSymptomsCounter()
        setupConfirmButton()

        // Temporary schedule preview.
        // Later, load dates and sessions from Supabase for this doctor.
        loadSchedulePreview()

        // Default payment.
        selectPaymentMethod("BPJS")
    }




    private fun displayDoctorInfo() {

        val doctor = selectedDoctor ?: return

        binding.tvDoctorInitial.text = doctor.name
            .trim()
            .replaceFirst(Regex("(?i)^dr\\.?\\s*"), "")
            .firstOrNull()
            ?.uppercase() ?: "D"

        binding.tvDoctorName.text = doctor.name
        binding.tvDoctorSpecialty.text = doctor.specialty
        binding.tvDoctorRoom.text = doctor.room
    }




    private fun loadSchedulePreview() {


        val morning = ConsultationSessionOption(
            code = "MORNING",
            name = "Morning",
            timeRange = "09:00 - 11:00",
            available = true
        )

        val afternoon = ConsultationSessionOption(
            code = "AFTERNOON",
            name = "Afternoon",
            timeRange = "13:00 - 15:00",
            available = true
        )

        val evening = ConsultationSessionOption(
            code = "EVENING",
            name = "Evening",
            timeRange = "16:00 - 18:00",
            available = true
        )

        // Different dates can have different sessions.
        val sessionTemplates = listOf(
            listOf(morning, afternoon, evening),
            listOf(morning, evening),
            listOf(afternoon, evening)
        )

        val today = Calendar.getInstance()

        // Display only three date options.
        availableDates = (0..2).map { offset ->

            val calendar = Calendar.getInstance().apply {
                timeInMillis = today.timeInMillis

                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                add(Calendar.DAY_OF_MONTH, offset)
            }

            AppointmentDateOption(
                date = calendar,
                available = true,
                sessions = sessionTemplates[offset]
            )
        }

        selectedDate = availableDates.firstOrNull {
            it.available
        }

        selectedSession = selectedDate?.sessions?.firstOrNull {
            it.available
        }

        renderDateOptions()
        renderSessionOptions()
    }



    private fun renderDateOptions() {

        val container = binding.layoutDateOptions

        container.removeAllViews()

        val selected = selectedDate

        binding.tvScheduleMonth.text = selected?.let {
            SimpleDateFormat(
                "MMMM yyyy",
                Locale.ENGLISH
            ).format(it.date.time)
        } ?: "No available dates"

        binding.tvSelectedDate.text = selected?.let {
            "Selected: " + SimpleDateFormat(
                "EEEE, dd MMMM yyyy",
                Locale.ENGLISH
            ).format(it.date.time)
        } ?: "No date selected"


        availableDates.forEach { option ->

            val isSelected = option == selected

            val dayName = SimpleDateFormat(
                "EEE",
                Locale.ENGLISH
            ).format(option.date.time)

            val dayNumber = SimpleDateFormat(
                "dd",
                Locale.ENGLISH
            ).format(option.date.time)

            val today = Calendar.getInstance()

            val isToday =
                today.get(Calendar.YEAR) == option.date.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == option.date.get(Calendar.DAY_OF_YEAR)

            val status = when {
                !option.available -> "Full"
                isToday -> "Today"
                else -> "Available"
            }


            // Equal-width date card.
            val card = MaterialCardView(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dp(94),
                    1f
                ).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }

                radius = dp(12).toFloat()
                cardElevation = 0f

                setCardBackgroundColor(
                    if (isSelected) {
                        Color.parseColor("#006493")
                    } else {
                        Color.WHITE
                    }
                )

                setStrokeColor(
                    ColorStateList.valueOf(
                        Color.parseColor(
                            if (isSelected) "#006493" else "#E0E7EF"
                        )
                    )
                )

                strokeWidth = dp(1)

                isClickable = option.available
                isFocusable = option.available
            }


            val content = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                setPadding(
                    dp(4),
                    dp(8),
                    dp(4),
                    dp(8)
                )
            }


            val dayLabel = TextView(this).apply {

                text = dayName
                textSize = 10f
                gravity = Gravity.CENTER

                setTextColor(
                    if (isSelected) Color.WHITE else Color.DKGRAY
                )
            }


            val dayValue = TextView(this).apply {

                text = dayNumber
                textSize = 23f
                gravity = Gravity.CENTER

                setTypeface(null, Typeface.BOLD)

                setTextColor(
                    if (isSelected) {
                        Color.WHITE
                    } else {
                        Color.parseColor("#17243A")
                    }
                )
            }


            val statusLabel = TextView(this).apply {

                text = status
                textSize = 9f
                gravity = Gravity.CENTER

                setTextColor(
                    if (isSelected) {
                        Color.WHITE
                    } else {
                        Color.parseColor("#64748B")
                    }
                )
            }


            content.addView(dayLabel)
            content.addView(dayValue)
            content.addView(statusLabel)

            card.addView(content)


            card.setOnClickListener {

                if (!option.available) {
                    return@setOnClickListener
                }

                selectedDate = option

                // Update available sessions for this date.
                selectedSession = option.sessions.firstOrNull {
                    it.available
                }

                renderDateOptions()
                renderSessionOptions()
            }

            container.addView(card)
        }
    }



    private fun renderSessionOptions() {

        val container = binding.layoutSessionOptions

        container.removeAllViews()

        val date = selectedDate

        if (date == null || !date.available) {

            selectedSession = null

            binding.tvNoSessions.visibility = View.VISIBLE
            binding.tvSelectedSession.text = "No session selected"

            return
        }

        val availableSessions = date.sessions.filter {
            it.available
        }

        if (availableSessions.isEmpty()) {

            selectedSession = null

            binding.tvNoSessions.visibility = View.VISIBLE
            binding.tvSelectedSession.text = "No session selected"

            return
        }

        binding.tvNoSessions.visibility = View.GONE


        availableSessions.forEach { session ->

            val isSelected = session == selectedSession


            // Equal-width session card.
            val card = MaterialCardView(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dp(68),
                    1f
                ).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }

                radius = dp(11).toFloat()
                cardElevation = 0f

                setCardBackgroundColor(
                    if (isSelected) {
                        Color.parseColor("#006493")
                    } else {
                        Color.WHITE
                    }
                )

                setStrokeColor(
                    ColorStateList.valueOf(
                        Color.parseColor(
                            if (isSelected) "#006493" else "#E0E7EF"
                        )
                    )
                )

                strokeWidth = dp(1)

                isClickable = true
                isFocusable = true
            }


            val content = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                setPadding(
                    dp(3),
                    dp(5),
                    dp(3),
                    dp(5)
                )
            }


            val title = TextView(this).apply {

                text = session.name
                textSize = 11f
                gravity = Gravity.CENTER

                setTypeface(null, Typeface.BOLD)

                setTextColor(
                    if (isSelected) {
                        Color.WHITE
                    } else {
                        Color.parseColor("#17243A")
                    }
                )
            }


            val time = TextView(this).apply {

                text = session.timeRange
                textSize = 9f
                gravity = Gravity.CENTER

                setTextColor(
                    if (isSelected) {
                        Color.WHITE
                    } else {
                        Color.parseColor("#64748B")
                    }
                )
            }


            content.addView(title)
            content.addView(time)

            card.addView(content)


            card.setOnClickListener {

                selectedSession = session

                renderSessionOptions()
            }

            container.addView(card)
        }


        binding.tvSelectedSession.text = selectedSession?.let {
            "Selected: ${it.name} (${it.timeRange})"
        } ?: "Select consultation session"
    }



    private fun setupPaymentListeners() {

        binding.cardBpjs.setOnClickListener {
            selectPaymentMethod("BPJS")
        }

        binding.rbBpjs.setOnClickListener {
            selectPaymentMethod("BPJS")
        }

        binding.cardSelfPay.setOnClickListener {
            selectPaymentMethod("SELF_PAY")
        }

        binding.rbSelfPay.setOnClickListener {
            selectPaymentMethod("SELF_PAY")
        }

        binding.cardPrivateInsurance.setOnClickListener {
            selectPaymentMethod("PRIVATE_INSURANCE")
        }

        binding.rbPrivateInsurance.setOnClickListener {
            selectPaymentMethod("PRIVATE_INSURANCE")
        }
    }




    private fun selectPaymentMethod(method: String) {

        selectedPaymentMethod = method

        binding.rbBpjs.isChecked = method == "BPJS"

        binding.rbSelfPay.isChecked =
            method == "SELF_PAY"

        binding.rbPrivateInsurance.isChecked =
            method == "PRIVATE_INSURANCE"


        binding.layoutBpjsDetails.visibility =
            if (method == "BPJS") View.VISIBLE else View.GONE

        binding.layoutSelfPayDetails.visibility =
            if (method == "SELF_PAY") View.VISIBLE else View.GONE

        binding.layoutInsuranceDetails.visibility =
            if (method == "PRIVATE_INSURANCE") View.VISIBLE else View.GONE


        updatePaymentCardStyle()


        val paymentLabel = when (method) {
            "BPJS" -> "BPJS Kesehatan"
            "SELF_PAY" -> "Personal / Self-Pay"
            "PRIVATE_INSURANCE" -> "Private Insurance / Corporate"
            else -> method
        }


        binding.tvPaymentCoverage.text = when (method) {
            "BPJS" -> "BPJS selected"
            "SELF_PAY" -> "Self-pay selected"
            "PRIVATE_INSURANCE" -> "Insurance selected"
            else -> ""
        }

        binding.tvSelectedPaymentSummary.text = paymentLabel

        binding.tvSelectedCoverageSummary.text = when (method) {
            "BPJS" -> "Enter your BPJS membership number"
            "SELF_PAY" -> "Payment at hospital cashier"
            "PRIVATE_INSURANCE" -> "Enter insurance policy details"
            else -> ""
        }
    }




    private fun updatePaymentCardStyle() {

        stylePaymentCard(
            binding.cardBpjs,
            selectedPaymentMethod == "BPJS"
        )

        stylePaymentCard(
            binding.cardSelfPay,
            selectedPaymentMethod == "SELF_PAY"
        )

        stylePaymentCard(
            binding.cardPrivateInsurance,
            selectedPaymentMethod == "PRIVATE_INSURANCE"
        )
    }


    private fun stylePaymentCard(
        card: MaterialCardView,
        selected: Boolean
    ) {

        card.strokeWidth = dp(if (selected) 2 else 1)

        card.setStrokeColor(
            ColorStateList.valueOf(
                Color.parseColor(
                    if (selected) "#006493" else "#E0E7EF"
                )
            )
        )

        card.setCardBackgroundColor(Color.WHITE)
    }




    private fun setupSymptomsCounter() {

        binding.etSymptoms.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                binding.tvSymptomsCounter.text =
                    "${text?.length ?: 0}/150"
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })
    }




    private fun setupConfirmButton() {

        binding.btnConfirmAppointment.setOnClickListener {
            submitAppointment()
        }
    }



    private fun submitAppointment() {

        val doctor = selectedDoctor ?: return
        val date = selectedDate
        val session = selectedSession


        // Validate date.
        if (date == null || !date.available) {

            showMessage("Please select an available appointment date.")

            return
        }


        // Validate session.
        if (session == null || !session.available) {

            showMessage("Please select an available consultation session.")

            return
        }


        // Payment details.
        val bpjsNumber =
            binding.etBpjsNumber.text.toString().trim()

        val insuranceProvider =
            binding.etInsuranceProvider.text.toString().trim()

        val insuranceNumber =
            binding.etInsuranceNumber.text.toString().trim()


        // Validate payment details.
        when (selectedPaymentMethod) {

            "BPJS" -> {

                if (bpjsNumber.isEmpty()) {

                    showMessage("Please enter your BPJS number.")

                    return
                }
            }

            "PRIVATE_INSURANCE" -> {

                if (
                    insuranceProvider.isEmpty() ||
                    insuranceNumber.isEmpty()
                ) {

                    showMessage("Please complete your insurance details.")

                    return
                }
            }
        }


        // Complaint is optional.
        val symptoms =
            binding.etSymptoms.text.toString().trim()


        // Format date for future database use.
        val databaseDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(date.date.time)


        // Prepare request object from Data.kt.
        pendingBookingRequest = AppointmentBookingRequest(

            doctorId = doctor.id,

            appointmentDate = databaseDate,

            session = session.code,

            paymentMethod = selectedPaymentMethod,

            bpjsNumber = if (selectedPaymentMethod == "BPJS") {
                bpjsNumber
            } else {
                null
            },

            insuranceProvider = if (
                selectedPaymentMethod == "PRIVATE_INSURANCE"
            ) {
                insuranceProvider
            } else {
                null
            },

            insuranceNumber = if (
                selectedPaymentMethod == "PRIVATE_INSURANCE"
            ) {
                insuranceNumber
            } else {
                null
            },

            symptoms = symptoms
        )


        // TODO:
        // Send pendingBookingRequest to Supabase
        // after the backend endpoint and schedule schema are confirmed.

        showMessage(
            "Booking details are ready. Database integration is not connected yet."
        )
    }



    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }


    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }


    companion object {
        const val EXTRA_DOCTOR_ID = "doctorId"
    }
}
