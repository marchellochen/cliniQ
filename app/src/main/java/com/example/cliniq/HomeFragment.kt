package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.cliniq.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBook.setOnClickListener {
            startActivity(Intent(requireContext(), DoctorsActivity::class.java))
        }

        binding.btnTicket.setOnClickListener {
            (requireActivity() as HomeActivity).showTab(R.id.nav_appointments)
        }
    }

    // Reload from Supabase every time this tab becomes visible
    override fun onResume() {
        super.onResume()
        render()
        launchSafely(onDone = { if (_binding != null) render() }) {
            ClinicRepo.refreshHome()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render() {
        binding.tvGreeting.text = "Hello, ${ClinicRepo.displayName}!"

        val message = ClinicRepo.lastMessage
        binding.cardMessage.isVisible = message != null
        binding.tvMessage.text = message

        val appt = ClinicRepo.appointment
        binding.cardActive.isVisible = appt != null
        binding.btnTicket.isVisible = appt != null
        binding.btnBook.isVisible = appt == null

        if (appt != null) {
            val inPharmacy = appt.status == "awaiting_medicine"

            binding.tvActive.text = if (inPharmacy)
                "Pharmacy ${appt.pharmacyQueueNo}"
            else
                "${appt.queueNo} · ${ClinicRepo.doctorFor(appt.doctorId)?.name.orEmpty()}"

            binding.btnTicket.text = if (inPharmacy)
                "View pharmacy queue"
            else
                "View my ticket"
        }
    }
}
