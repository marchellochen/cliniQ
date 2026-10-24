package com.example.cliniq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cliniq.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val genders = listOf("Male", "Female", "Prefer not to say")
    private val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Not sure")

    // Kept as "yyyy-MM-dd", the format Supabase stores
    private var dob: String? = null

    // The profile the fields were last filled from (so unsaved edits aren't overwritten)
    private var filledFrom: Profile? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.actGender.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genders)
        )
        binding.actBloodType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, bloodTypes)
        )

        binding.etDob.setOnClickListener {
            (requireActivity() as AppCompatActivity).pickDob(dob) { picked ->
                dob = picked
                showDob()
            }
        }

        binding.btnSave.setOnClickListener { save() }
    }

    // The profile may finish loading after this tab is created, so fill the fields
    // whenever the tab becomes visible and the stored profile is different
    override fun onResume() {
        super.onResume()
        fillIfChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        filledFrom = null
        _binding = null
    }

    private fun fillIfChanged() {
        val profile = ClinicRepo.profile
        if (profile == filledFrom) return
        filledFrom = profile

        dob = profile.dateOfBirth
        showDob()
        binding.etName.setText(profile.fullName.orEmpty())
        binding.etPhone.setText(profile.phone.orEmpty())
        binding.etAllergies.setText(profile.allergies.orEmpty())
        binding.etMedications.setText(profile.medications.orEmpty())
        binding.etConditions.setText(profile.conditions.orEmpty())
        binding.actGender.setText(profile.gender.orEmpty(), false)
        binding.actBloodType.setText(profile.bloodType.orEmpty(), false)
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
                    filledFrom = updated
                    hideKeyboard()
                    toast("Profile saved")
                }
            }
        }
    }

    private fun hideKeyboard() {
        binding.root.clearFocus()
        requireContext().getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    /** The trimmed text of a field, or null if it's empty. */
    private fun TextView.value(): String? = text.toString().trim().ifEmpty { null }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
