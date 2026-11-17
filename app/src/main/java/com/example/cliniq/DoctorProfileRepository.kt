package com.example.cliniq

import io.github.jan.supabase.postgrest.from

/** Loads one doctor and their weekly schedules from Supabase. */
class DoctorProfileRepository {

    suspend fun getDoctor(id: Int): Doctor =
        supabase.from("doctors")
            .select {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<Doctor>()
            ?: error("Doctor not found")

    /** Every doctor schedule (used to know which doctors work at which branch). */
    suspend fun getAllSchedules(): List<DoctorSchedule> =
        supabase.from("v_doctor_schedules")
            .select()
            .decodeList<DoctorSchedule>()

    /** Sorted by day (Monday first), then session (Morning, Afternoon, Night). */
    suspend fun getSchedules(doctorId: Int): List<DoctorSchedule> =
        supabase.from("v_doctor_schedules")
            .select {
                filter { eq("doctor_id", doctorId) }
            }
            .decodeList<DoctorSchedule>()
            .sortedWith(
                compareBy<DoctorSchedule> { it.dayOfWeek }
                    .thenBy { sessionOrder(it.session) }
                    .thenBy { it.startTime }
            )

    private fun sessionOrder(session: String) =
        when (session.lowercase()) {
            "morning" -> 0
            "afternoon" -> 1
            "night" -> 2
            else -> 3
        }
}
