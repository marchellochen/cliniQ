
package com.example.cliniq

import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.*
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


// ---------------------------------------------------------------------------
// Models: each class mirrors a table.
// @SerialName maps the snake_case column to the Kotlin name.
// ---------------------------------------------------------------------------

@Serializable
data class Doctor(
    val id: Int,
    val name: String,
    val specialty: String,
    val room: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val bio: String? = null,
    @SerialName("experience_years") val experienceYears: Int? = null,
)

/** One row of the v_doctor_schedules view. day_of_week: 1 = Monday ... 7 = Sunday. */
@Serializable
data class DoctorSchedule(
    @SerialName("schedule_id") val scheduleId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("branch_name") val branchName: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val quota: Int? = null,
    val session: String,
)

@Serializable
data class Profile(
    val id: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    @SerialName("blood_type") val bloodType: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val conditions: String? = null,
    val nik: String? = null,
    @SerialName("is_staff") val isStaff: Boolean? = null,
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
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("schedule_id") val scheduleId: Int? = null,
    @SerialName("appointment_date") val appointmentDate: String? = null,
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("called_at") val calledAt: String? = null,
) {

    val qrPayload: String
        get() = "CLINIQ:$id:$queueNo"
}


/** Result of the get_now_serving RPC. */
@Serializable
data class NowServing(
    @SerialName("queue_no") val queueNo: String? = null,
    @SerialName("waiting_count") val waitingCount: Int = 0,
)

/** Result of the scan_queue_qr RPC (success or failure). */
@Serializable
data class ScanResult(
    val success: Boolean = false,
    val message: String? = null,
    @SerialName("appointment_id") val appointmentId: Long? = null,
    @SerialName("queue_no") val queueNo: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    val room: String? = null,
)


// ---------------------------------------------------------------------------
// Appointment Booking Request
// Additional model for the new scheduled Appointment Booking feature.
// ---------------------------------------------------------------------------

@Serializable
data class AppointmentBookingRequest(
    @SerialName("doctor_id")
    val doctorId: Int,

    @SerialName("appointment_date")
    val appointmentDate: String,

    val session: String,

    @SerialName("payment_method")
    val paymentMethod: String,

    @SerialName("bpjs_number")
    val bpjsNumber: String? = null,

    @SerialName("insurance_provider")
    val insuranceProvider: String? = null,

    @SerialName("insurance_number")
    val insuranceNumber: String? = null,

    val symptoms: String
)


// ---------------------------------------------------------------------------
// All Supabase calls live here, so the screens never talk to the database directly.
// ---------------------------------------------------------------------------

object ClinicRepo {

    // ----- Data kept in memory while the app runs -----

    var doctors: List<Doctor> = emptyList()
        private set

    var profile = Profile()
        private set

    var appointment: Appointment? = null
        private set

    var peopleAhead = 0
        private set

    /** The ticket that was just finished (status done), so the Queue page can show "Completed". */
    var completedAppointment: Appointment? = null
        private set

    var lastMessage: String? = null

    val displayName: String
        get() = profile.fullName?.takeIf { it.isNotBlank() }
            ?: supabase.auth.currentUserOrNull()?.email?.substringBefore("@").orEmpty()

    fun doctorFor(id: Int) = doctors.firstOrNull { it.id == id }

    private fun uid(): String =
        supabase.auth.currentUserOrNull()?.id ?: error("You are not logged in")

    private fun clear() {
        profile = Profile()
        appointment = null
        completedAppointment = null
        peopleAhead = 0
        lastMessage = null
    }


    // ----- Auth -----

    fun hasSession() = supabase.auth.currentSessionOrNull() != null

    suspend fun signUp(name: String, email: String, password: String) {
        clear()

        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password

            data = buildJsonObject {
                // The database trigger copies this into the profiles table.
                put("full_name", name)
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        clear()

        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Sends a password-reset email through Supabase Auth. */
    suspend fun sendPasswordReset(email: String) {
        supabase.auth.resetPasswordForEmail(email)
    }

    suspend fun logout() {
        supabase.auth.signOut()
        clear()
    }


    // ----- Loading data -----

    suspend fun refreshHome() {
        loadDoctors()
        loadProfile()
        loadActive()
    }

    suspend fun loadDoctors() {
        doctors = supabase.from("doctors")
            .select {
                order("id", Order.ASCENDING)
            }
            .decodeList<Doctor>()
    }

    private suspend fun loadProfile() {
        val id = uid()

        profile = supabase.from("profiles")
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeSingleOrNull<Profile>()
            ?: Profile(id = id)
    }

    suspend fun saveProfile(updated: Profile) {
        val id = uid()

        supabase.from("profiles").update({
            set("full_name", updated.fullName)
            set("date_of_birth", updated.dateOfBirth)
            set("phone", updated.phone)
            set("gender", updated.gender)
            set("blood_type", updated.bloodType)
            set("allergies", updated.allergies)
            set("medications", updated.medications)
            set("conditions", updated.conditions)
            // Only written when known, so older rows / screens without NIK are left untouched
            updated.nik?.let { set("nik", it) }
        }) {
            filter {
                eq("id", id)
            }
        }

        profile = updated
    }


    /** The patient's current queue entry (waiting for the doctor, or waiting for medicine). */
    private suspend fun loadActive() {
        val id = uid()
        val previous = appointment

        appointment = supabase.from("appointments")
            .select {
                filter {
                    eq("user_id", id)
                    isIn("status", listOf("waiting", "called", "awaiting_medicine"))
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<Appointment>()
            .firstOrNull()

        // The ticket we were showing left the active list: it may have just been completed
        if (appointment == null && previous != null) {
            val latest = supabase.from("appointments")
                .select {
                    filter { eq("id", previous.id) }
                }
                .decodeSingleOrNull<Appointment>()

            if (latest?.status == "done" || latest?.status == "completed") {
                completedAppointment = latest
            }
        } else if (appointment != null) {
            completedAppointment = null
        }

        peopleAhead = appointment
            ?.takeIf { it.status == "waiting" }
            ?.let { fetchPeopleAhead(it.id) }
            ?: 0
    }

    /** Who is being served by this doctor right now, and how many patients are still waiting. */
    suspend fun getNowServing(doctorId: Int): NowServing =
        supabase.postgrest.rpc(
            "get_now_serving",
            buildJsonObject {
                put("p_doctor_id", doctorId)
            }
        ).decodeAs<NowServing>()

    /** Staff only: moves the queue forward for the patient who owns this QR token. */
    suspend fun scanQueueQr(token: String): ScanResult =
        supabase.postgrest.rpc(
            "scan_queue_qr",
            buildJsonObject {
                put("p_token", token)
            }
        ).decodeAs<ScanResult>()

    suspend fun loadSchedule(scheduleId: Int): DoctorSchedule? =
        supabase.from("v_doctor_schedules")
            .select {
                filter { eq("schedule_id", scheduleId) }
            }
            .decodeList<DoctorSchedule>()
            .firstOrNull()

    private suspend fun fetchPeopleAhead(appointmentId: Long): Int =
        supabase.postgrest.rpc(
            "people_ahead",
            buildJsonObject {
                put("p_appointment", appointmentId)
            }
        ).decodeAs<Int>()


    /** Re-reads the current queue entry. Used by the ticket screen to keep "people ahead" fresh. */
    suspend fun refreshActive() = loadActive()


    /** Every appointment of the logged-in patient, newest first (RLS limits it to their own rows). */
    suspend fun loadHistory(): List<Appointment> {
        val id = uid()

        return supabase.from("appointments")
            .select {
                filter {
                    eq("user_id", id)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Appointment>()
    }


    // ----- Booking flow -----

    suspend fun book(
        doctor: Doctor,
        symptoms: String,
        duration: String,
    ) {
        // The database hands out the queue number (see book_appointment in SQL).
        val appt = supabase.postgrest.rpc(
            "book_appointment",
            buildJsonObject {
                put("p_doctor", doctor.id)
                put("p_symptoms", symptoms)
                put("p_duration", duration)
            }
        ).decodeAs<Appointment>()

        appointment = appt
        completedAppointment = null
        lastMessage = null
        peopleAhead = fetchPeopleAhead(appt.id)
    }


    suspend fun finishSession(needsMedicine: Boolean) {
        val appt = appointment ?: return

        appointment = supabase.postgrest.rpc(
            "finish_session",
            buildJsonObject {
                put("p_appointment", appt.id)
                put("p_needs_medicine", needsMedicine)
            }
        ).decodeAs<Appointment>()

        if (!needsMedicine) {
            appointment = null
            lastMessage = "Session complete. Get well soon!"
        }
    }

    suspend fun completePickup() {
        val appt = appointment ?: return

        supabase.from("appointments").update({
            set("status", "completed")
        }) {
            filter {
                eq("id", appt.id)
            }
        }

        appointment = null
        lastMessage = "Medicine received. Session complete!"
    }


    /** Leaves the queue. Only works while the appointment is still "waiting". */
    suspend fun cancelAppointment() {
        val appt = appointment ?: return

        val updated = supabase.from("appointments")
            .update({
                set("status", "cancelled")
            }) {
                select()
                filter {
                    eq("id", appt.id)
                    eq("status", "waiting")
                }
            }
            .decodeList<Appointment>()

        if (updated.isEmpty()) error("This appointment can no longer be cancelled")

        appointment = null
        peopleAhead = 0
        lastMessage = "Your appointment was cancelled."
    }
}
