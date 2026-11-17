package com.example.cliniq.finddoctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cliniq.Doctor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val doctors: List<Doctor> = emptyList(),
    val specialtyQuery: String = "",
    val locationQuery: String = "",
    val selectedSpecialty: String? = null,
)

class SearchDoctorViewModel : ViewModel() {

    private val repository = FindDoctorRepository

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var allDoctors: List<Doctor> = emptyList()
    private var locationsByDoctor: Map<Int, List<String>> = emptyMap()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                allDoctors = repository.getDoctors()
                val cityByBranch = repository.getBranches().associate { it.id to it.city.orEmpty() }
                locationsByDoctor = repository.getSchedules()
                    .groupBy { it.doctorId }
                    .mapValues { (_, rows) ->
                        rows.flatMap { listOf(it.branchName, cityByBranch[it.branchId].orEmpty()) }
                    }
                _state.update { it.copy(isLoading = false) }
                applyFilters()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong") }
            }
        }
    }

    fun onSpecialtyQueryChange(query: String) {
        _state.update { it.copy(specialtyQuery = query) }
        applyFilters()
    }

    fun onLocationQueryChange(query: String) {
        _state.update { it.copy(locationQuery = query) }
        applyFilters()
    }

    fun onSpecialtyClick(value: String) {
        _state.update { it.copy(selectedSpecialty = if (it.selectedSpecialty == value) null else value) }
        applyFilters()
    }

    private fun applyFilters() {
        _state.update { s ->
            val specialtyQuery = s.specialtyQuery.trim()
            val locationQuery = s.locationQuery.trim()
            val filtered = allDoctors.filter { doctor ->
                (s.selectedSpecialty == null || doctor.specialty == s.selectedSpecialty) &&
                    doctor.specialty.contains(specialtyQuery, ignoreCase = true) &&
                    (locationQuery.isEmpty() ||
                        locationsByDoctor[doctor.id].orEmpty().any { it.contains(locationQuery, ignoreCase = true) })
            }
            s.copy(doctors = filtered)
        }
    }
}
