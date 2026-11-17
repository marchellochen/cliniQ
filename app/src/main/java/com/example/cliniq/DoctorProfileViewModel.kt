package com.example.cliniq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DoctorProfileState {
    data object Loading : DoctorProfileState
    data class Success(
        val doctor: Doctor,
        val schedules: List<DoctorSchedule>,
    ) : DoctorProfileState
    data class Error(val message: String) : DoctorProfileState
}

class DoctorProfileViewModel(
    private val doctorId: Int,
    private val repository: DoctorProfileRepository = DoctorProfileRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<DoctorProfileState>(DoctorProfileState.Loading)
    val state: StateFlow<DoctorProfileState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = DoctorProfileState.Loading
        viewModelScope.launch {
            _state.value = try {
                coroutineScope {
                    val doctor = async { repository.getDoctor(doctorId) }
                    val schedules = async { repository.getSchedules(doctorId) }
                    DoctorProfileState.Success(doctor.await(), schedules.await())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DoctorProfileState.Error(e.message ?: "Something went wrong")
            }
        }
    }
}
