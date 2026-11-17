package com.example.cliniq

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cliniq.databinding.ActivitySignupBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupPatientStatus()
        setupDatePicker()
        setupNikCounter()
        setupSignupButton()

        binding.tvGoLogin.setOnClickListener {
            finish()
        }
    }

    // PATIENT STATUS
    private fun setupPatientStatus() {

        binding.cardNewPatient.setOnClickListener {
            selectNewPatient()
        }

        binding.cardReturningPatient.setOnClickListener {
            selectReturningPatient()
        }

        binding.rbNewPatient.setOnClickListener {
            selectNewPatient()
        }

        binding.rbReturningPatient.setOnClickListener {
            selectReturningPatient()
        }
    }

    private fun selectNewPatient() {
        binding.rbNewPatient.isChecked = true
        binding.rbReturningPatient.isChecked = false
    }

    private fun selectReturningPatient() {
        binding.rbReturningPatient.isChecked = true
        binding.rbNewPatient.isChecked = false
    }

    // DATE PICKER
    private fun setupDatePicker() {

        binding.etDateOfBirth.setOnClickListener {

            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->

                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, day)

                    val formatter = SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    )

                    binding.etDateOfBirth.setText(
                        formatter.format(selectedDate.time)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }
    }

    // NIK COUNTER
    private fun setupNikCounter() {

        binding.etNik.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Not used
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                binding.tvNikCounter.text = "${s?.length ?: 0}/16"
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used
            }
        })
    }

    // SIGN UP VALIDATION
    private fun setupSignupButton() {

        binding.btnSignup.setOnClickListener {

            val name = binding.etName.text.toString().trim()
            val nik = binding.etNik.text.toString().trim()
            val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val phoneDigits = phone.filter { it.isDigit() }
            val pin = binding.etPin.text.toString().trim()

            when {

                !binding.rbNewPatient.isChecked &&
                        !binding.rbReturningPatient.isChecked -> {
                    showMessage("Please select your patient status.")
                }

                name.isEmpty() -> {
                    binding.etName.error = "Full name is required"
                }

                !nik.matches(Regex("\\d{16}")) -> {
                    binding.etNik.error = "NIK must contain exactly 16 digits"
                }

                dateOfBirth.isEmpty() -> {
                    showMessage("Please select your date of birth.")
                }

                binding.rgGender.checkedRadioButtonId == -1 -> {
                    showMessage("Please select your gender.")
                }

                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.etEmail.error = "Please enter a valid email address"
                }

                phoneDigits.length !in 9..13 -> {
                    binding.etPhone.error = "Please enter a valid WhatsApp number"
                }

                !pin.matches(Regex("\\d{6}")) -> {
                    binding.etPin.error = "PIN must contain exactly 6 digits"
                }

                !binding.cbAgreement.isChecked -> {
                    showMessage("Please agree to the Terms & Conditions.")
                }

                else -> {
                    createAccount(
                        name = name,
                        email = email,
                        pin = pin,
                        nik = nik,
                        dateOfBirth = dateOfBirth,
                        gender = if (binding.rbMale.isChecked) "Male" else "Female",
                        phoneDigits = phoneDigits,
                    )
                }
            }
        }
    }

    // CREATE ACCOUNT (Supabase Auth, then the extra details on the profile)
    private fun createAccount(
        name: String,
        email: String,
        pin: String,
        nik: String,
        dateOfBirth: String,
        gender: String,
        phoneDigits: String,
    ) {
        binding.btnSignup.isEnabled = false

        launchSafely(
            onDone = { binding.btnSignup.isEnabled = true },
            onError = { e ->
                val msg = e.message.orEmpty()
                if (msg.contains("already", ignoreCase = true)) {
                    binding.etEmail.error = "This email is already registered"
                } else {
                    showMessage(if (msg.isNotBlank()) msg else "Sign up failed")
                }
            }
        ) {
            // The database trigger creates the profile row with the full name
            ClinicRepo.signUp(name, email, pin)

            if (ClinicRepo.hasSession()) {
                // Save the rest of the details; a failure here must not undo the sign up
                runCatching {
                    ClinicRepo.saveProfile(
                        Profile(
                            fullName = name,
                            nik = nik,
                            dateOfBirth = toIsoDate(dateOfBirth),
                            phone = "+62" + phoneDigits.removePrefix("0").removePrefix("62"),
                            gender = gender,
                        )
                    )
                }

                startActivity(
                    Intent(this, HomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()
            } else {
                // Email confirmation is switched on in Supabase: no session until confirmed
                Toast.makeText(
                    this,
                    "Account created. Please confirm your email, then log in.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    /** "23/11/1990" becomes "1990-11-23". */
    private fun toIsoDate(display: String): String? =
        runCatching {
            val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(display)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed!!)
        }.getOrNull()

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
