package com.example.cliniq.finddoctor

import com.example.cliniq.Doctor
import com.example.cliniq.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object FindDoctorRepository {

    suspend fun getDoctors(): List<Doctor> =
        supabase.from("doctors")
            .select { order("name", Order.ASCENDING) }
            .decodeList()

    suspend fun getDoctor(id: Int): Doctor =
        supabase.from("doctors")
            .select { filter { eq("id", id) } }
            .decodeSingle()

    suspend fun getBranches(): List<Branch> =
        supabase.from("branches").select().decodeList()

    suspend fun getSchedules(): List<DoctorSchedule> =
        supabase.from("v_doctor_schedules").select().decodeList()

    suspend fun getSchedules(doctorId: Int): List<DoctorSchedule> =
        supabase.from("v_doctor_schedules")
            .select {
                filter { eq("doctor_id", doctorId) }
                order("day_of_week", Order.ASCENDING)
                order("start_time", Order.ASCENDING)
            }
            .decodeList()
}
