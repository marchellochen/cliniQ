package com.example.cliniq

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cliniq.databinding.ActivityStaffScanBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

/** Staff only: scan a patient's QR code to move the queue forward. */
class StaffScanActivity : BaseActivity() {

    private lateinit var binding: ActivityStaffScanBinding
    private val viewModel: StaffScanViewModel by viewModels()

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        // contents is null when the staff member backed out of the camera
        result.contents?.let(viewModel::scan)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ClinicRepo.profile.isStaff != true) {
            Toast.makeText(this, "Only staff can scan QR codes", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityStaffScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("Scan QR")
        keepContentBelowActionBar(binding.root)

        binding.btnScan.setOnClickListener { openScanner() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }

        // Open the camera right away the first time
        if (savedInstanceState == null) openScanner()
    }

    private fun openScanner() {
        scanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan patient QR code")
                .setBeepEnabled(true)
                .setOrientationLocked(false)
        )
    }

    private fun render(state: StaffScanState) {
        binding.progress.visibility = if (state is StaffScanState.Loading) View.VISIBLE else View.GONE
        binding.cardResult.visibility =
            if (state is StaffScanState.Success || state is StaffScanState.Error) View.VISIBLE else View.GONE
        binding.tvIntro.visibility = if (state is StaffScanState.Idle) View.VISIBLE else View.GONE
        binding.btnScan.isEnabled = state !is StaffScanState.Loading
        binding.btnScan.text = if (state is StaffScanState.Idle) "Scan QR" else "Scan Next"

        when (state) {
            is StaffScanState.Success -> {
                val r = state.result
                binding.ivResult.setImageResource(R.drawable.ic_check_circle)
                binding.tvResultTitle.text = "Now serving"
                binding.tvResultQueue.text = r.queueNo.orEmpty()
                binding.tvResultPatient.text = r.patientName.orEmpty()
                binding.tvResultDoctor.text = r.doctorName.orEmpty()
                binding.tvResultRoom.text = r.room.orEmpty()
                setDetailsVisible(true)
            }

            is StaffScanState.Error -> {
                binding.ivResult.setImageResource(R.drawable.ic_warning)
                binding.tvResultTitle.text = state.message
                setDetailsVisible(false)
            }

            else -> Unit
        }
    }

    private fun setDetailsVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        binding.tvResultQueue.visibility = v
        binding.tvResultPatient.visibility = v
        binding.tvResultDoctor.visibility = v
        binding.tvResultRoom.visibility = v
    }
}
