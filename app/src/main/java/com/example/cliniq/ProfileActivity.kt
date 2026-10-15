package com.example.cliniq

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.example.cliniq.databinding.ActivityProfileBinding

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val genders = listOf("Male", "Female", "Prefer not to say")
    private val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Not sure")

    // Kept as "yyyy-MM-dd", the format Supabase stores
    private var dob: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("My profile")

        // Fill in the current values
        val profile = ClinicRepo.profile
        dob = profile.dateOfBirth
        showDob()
        binding.etName.setText(profile.fullName.orEmpty())
        binding.etPhone.setText(profile.phone.orEmpty())
        binding.etAllergies.setText(profile.allergies.orEmpty())
        binding.etMedications.setText(profile.medications.orEmpty())
        binding.etConditions.setText(profile.conditions.orEmpty())

        binding.actGender.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)
        )
        binding.actGender.setText(profile.gender.orEmpty(), false)

        binding.actBloodType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, bloodTypes)
        )
        binding.actBloodType.setText(profile.bloodType.orEmpty(), false)

        binding.etDob.setOnClickListener {
            pickDob(dob) { picked ->
                dob = picked
                showDob()
            }
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun showDob() {
        binding.etDob.setText(formatDob(dob))
        binding.layoutDob.helperText = ageFrom(dob)?.let { "Age: $it" }
    }

    private fun save() {
        val name = binding.etName.value()
        val phone = binding.etPhone.value()

        when {
            name == null -> toast("Please enter your name")
            phone != null && phone.count { it.isDigit() } !in 7..15 ->
                toast("Please enter a valid phone number")

            else -> {
                val updated = ClinicRepo.profile.copy(
                    fullName = name,
                    dateOfBirth = dob,
                    phone = phone,
                    gender = binding.actGender.value(),
                    bloodType = binding.actBloodType.value(),
                    allergies = binding.etAllergies.value(),
                    medications = binding.etMedications.value(),
                    conditions = binding.etConditions.value(),
                )

                binding.btnSave.isEnabled = false
                launchSafely(onDone = { binding.btnSave.isEnabled = true }) {
                    ClinicRepo.saveProfile(updated)
                    toast("Profile saved")
                    finish()
                }
            }
        }
    }

    /** The trimmed text of a field, or null if it's empty. */
    private fun TextView.value(): String? = text.toString().trim().ifEmpty { null }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}