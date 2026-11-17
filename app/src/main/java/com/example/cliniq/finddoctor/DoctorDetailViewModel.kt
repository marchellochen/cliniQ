package com.example.cliniq.finddoctor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cliniq.Doctor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DoctorDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val doctor: Doctor? = null,
    val schedules: List<DoctorSchedule> = emptyList(),
)

class DoctorDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = FindDoctorRepository
    private val doctorId: Int = checkNotNull(savedStateHandle["doctorId"])

    private val _state = MutableStateFlow(DoctorDetailUiState())
    val state: StateFlow<DoctorDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val doctor = repository.getDoctor(doctorId)
                val schedules = repository.getSchedules(doctorId)
                _state.value = DoctorDetailUiState(isLoading = false, doctor = doctor, schedules = schedules)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong") }
            }
        }
    }
}
