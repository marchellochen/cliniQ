package com.example.cliniq

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Each class mirrors a table. @SerialName maps the snake_case column to the Kotlin name.

@Serializable
data class Doctor(val id: Int, val name: String, val specialty: String, val room: String)

@Serializable
data class Profile(
    val id: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val age: Int? = null,
    val allergies: String? = null,
    @SerialName("blood_type") val bloodType: String? = null,
)

@Serializable
data class Appointment(
    val id: Long,
    @SerialName("doctor_id") val doctorId: Int,
    val symptoms: String,
    val duration: String? = null,
    @SerialName("queue_no") val queueNo: String,
    val status: String,
    @SerialName("needs_medicine") val needsMedicine: Boolean? = null,
    @SerialName("pharmacy_queue_no") val pharmacyQueueNo: String? = null,
) {
    val qrPayload: String get() = "CLINIQ:$id:$queueNo"
}

/** All Supabase calls live here, so the screens never talk to the database directly. */
object ClinicRepo {
    var doctors: List<Doctor> = emptyList(); private set
    var profile = Profile(); private set
    var appointment: Appointment? = null; private set
    var peopleAhead = 0; private set
    var lastMessage: String? = null

    val displayName: String
        get() = profile.fullName?.takeIf { it.isNotBlank() }
            ?: supabase.auth.currentUserOrNull()?.email?.substringBefore("@").orEmpty()

    fun doctorFor(id: Int) = doctors.firstOrNull { it.id == id }

    private fun uid(): String =
        supabase.auth.currentUserOrNull()?.id ?: error("You are not logged in")

    private fun clear() {
        profile = Profile(); appointment = null; peopleAhead = 0; lastMessage = null
    }

    // ---------- Auth ----------
    fun hasSession() = supabase.auth.currentSessionOrNull() != null

    suspend fun signUp(name: String, email: String, password: String) {
        clear()
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("full_name", name) }  // the DB trigger copies this into profiles
        }
    }

    suspend fun signIn(email: String, password: String) {
        clear()
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
        clear()
    }

    // ---------- Loading ----------
    suspend fun refreshHome() {
        loadDoctors(); loadProfile(); loadActive()
    }

    suspend fun loadDoctors() {
        doctors = supabase.from("doctors")
            .select { order("id", Order.ASCENDING) }
            .decodeList<Doctor>()
    }

    private suspend fun loadProfile() {
        val id = uid()
        profile = supabase.from("profiles")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Profile>() ?: Profile(id = id)
    }

    /** The patient's current queue entry (waiting for the doctor, or waiting for medicine). */
    private suspend fun loadActive() {
        val id = uid()
        appointment = supabase.from("appointments")
            .select {
                filter {
                    eq("user_id", id)
                    isIn("status", listOf("waiting", "awaiting_medicine"))
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<Appointment>().firstOrNull()
        peopleAhead = appointment?.takeIf { it.status == "waiting" }?.let { fetchPeopleAhead(it.id) } ?: 0
    }

    private suspend fun fetchPeopleAhead(appointmentId: Long): Int =
        supabase.postgrest.rpc("people_ahead", buildJsonObject {
            put("p_appointment", appointmentId)
        }).decodeAs<Int>()

    // ---------- Booking flow ----------
    suspend fun book(doctor: Doctor, symptoms: String, duration: String, age: Int, allergies: String) {
        val id = uid()
        supabase.from("profiles").update({
            set("age", age)
            set("allergies", allergies)
        }) { filter { eq("id", id) } }
        profile = profile.copy(age = age, allergies = allergies)

        // The database hands out the queue number (see book_appointment in SQL)
        val appt = supabase.postgrest.rpc("book_appointment", buildJsonObject {
            put("p_doctor", doctor.id)
            put("p_symptoms", symptoms)
            put("p_duration", duration)
        }).decodeAs<Appointment>()
        appointment = appt
        lastMessage = null
        peopleAhead = fetchPeopleAhead(appt.id)
    }

    suspend fun finishSession(needsMedicine: Boolean) {
        val appt = appointment ?: return
        appointment = supabase.postgrest.rpc("finish_session", buildJsonObject {
            put("p_appointment", appt.id)
            put("p_needs_medicine", needsMedicine)
        }).decodeAs<Appointment>()
        if (!needsMedicine) {
            appointment = null
            lastMessage = "Session complete. Get well soon!"
        }
    }

    suspend fun completePickup() {
        val appt = appointment ?: return
        supabase.from("appointments").update({ set("status", "completed") }) {
            filter { eq("id", appt.id) }
        }
        appointment = null
        lastMessage = "Medicine received. Session complete!"
    }
}
