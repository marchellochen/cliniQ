package com.example.cliniq

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Everything the Queue page needs to draw the patient's ticket. */
data class QueueData(
    val appointment: Appointment?,
    val doctor: Doctor?,
    val session: String?,
    val nowServing: NowServing?,
)

sealed interface QueueState {
    data object Loading : QueueState
    data class Success(val data: QueueData) : QueueState
    data class Error(val message: String) : QueueState
}

class QueueViewModel : ViewModel() {

    private val _state = MutableStateFlow<QueueState>(QueueState.Loading)
    val state: StateFlow<QueueState> = _state.asStateFlow()

    // scheduleId -> session name, so the lookup happens once
    private val sessions = mutableMapOf<Int, String>()

    /** Re-reads the appointment and the "now serving" info. Keeps the last good state on failure. */
    suspend fun refresh() {
        try {
            if (ClinicRepo.doctors.isEmpty()) ClinicRepo.loadDoctors()
            ClinicRepo.refreshActive()

            val appt = ClinicRepo.appointment ?: ClinicRepo.completedAppointment
            val doctor = appt?.let { ClinicRepo.doctorFor(it.doctorId) }

            val session = appt?.scheduleId?.let { id ->
                sessions[id] ?: ClinicRepo.loadSchedule(id)?.session?.also { sessions[id] = it }
            }

            val nowServing = appt
                ?.takeIf { it.status == "waiting" || it.status == "called" }
                ?.let { ClinicRepo.getNowServing(it.doctorId) }

            _state.value = QueueState.Success(QueueData(appt, doctor, session, nowServing))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (_state.value !is QueueState.Success) {
                _state.value = QueueState.Error(e.message ?: "Something went wrong")
            }
        }
    }
}
