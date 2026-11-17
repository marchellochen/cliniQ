package com.example.cliniq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StaffScanState {
    /** Nothing scanned yet. */
    data object Idle : StaffScanState
    data object Loading : StaffScanState
    data class Success(val result: ScanResult) : StaffScanState
    data class Error(val message: String) : StaffScanState
}

class StaffScanViewModel : ViewModel() {

    private val _state = MutableStateFlow<StaffScanState>(StaffScanState.Idle)
    val state: StateFlow<StaffScanState> = _state.asStateFlow()

    private val uuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )

    fun scan(text: String) {
        val token = text.trim()

        if (!uuidRegex.matches(token)) {
            _state.value = StaffScanState.Error("Invalid QR code")
            return
        }

        _state.value = StaffScanState.Loading
        viewModelScope.launch {
            _state.value = try {
                val result = ClinicRepo.scanQueueQr(token)
                if (result.success) {
                    StaffScanState.Success(result)
                } else {
                    StaffScanState.Error(result.message ?: "Scan failed")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                StaffScanState.Error(e.message ?: "Something went wrong")
            }
        }
    }
}
