package com.example.cliniq

import java.util.UUID

data class Doctor(val id: Int, val name: String, val specialty: String, val room: String)

data class Profile(val age: String = "", val allergies: String = "")

data class Appointment(
    val doctor: Doctor,
    val symptoms: String,
    val duration: String,
    val queueNo: String,
    val peopleAhead: Int,
    val qrPayload: String,
)

/** One shared object that holds all app data (in memory only). Every screen reads/writes here. */
object ClinicRepo {
    val doctors = listOf(
        Doctor(1, "dr. Amanda Wijaya", "General Practitioner", "Room 101"),
        Doctor(2, "dr. Budi Santoso, Sp.PD", "Internal Medicine", "Room 204"),
        Doctor(3, "dr. Citra Lestari, Sp.A", "Pediatrics", "Room 112"),
        Doctor(4, "dr. Dimas Pratama, Sp.JP", "Cardiology", "Room 305"),
        Doctor(5, "dr. Elisa Hartono, Sp.KK", "Dermatology", "Room 208"),
    )

    var userName = ""
    var profile = Profile()
    var appointment: Appointment? = null
    var pharmacyQueue: String? = null
    var lastMessage: String? = null

    private var appointmentCount = 0
    private var pharmacyCount = 0

    fun login(email: String) {
        userName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
    }

    fun logout() {
        userName = ""; appointment = null; pharmacyQueue = null; lastMessage = null
    }

    fun book(doctor: Doctor, symptoms: String, duration: String, newProfile: Profile) {
        profile = newProfile
        appointmentCount++
        val queueNo = "A-%03d".format(appointmentCount)
        appointment = Appointment(
            doctor, symptoms, duration, queueNo,
            peopleAhead = (2..8).random(),
            qrPayload = "CLINICQUEUE:$queueNo:${UUID.randomUUID()}",
        )
        lastMessage = null
    }

    fun verifyScan(content: String) = appointment?.qrPayload == content

    fun requestMedicine() {
        pharmacyCount++
        pharmacyQueue = "M-%03d".format(pharmacyCount)
    }

    fun finish(message: String) {
        appointment = null; pharmacyQueue = null; lastMessage = message
    }
}
