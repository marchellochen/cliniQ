package com.example.cliniq.finddoctor

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    val id: Int,
    val name: String,
    val address: String? = null,
    val city: String? = null,
)

@Serializable
data class DoctorSchedule(
    @SerialName("schedule_id") val scheduleId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("branch_id") val branchId: Int,
    @SerialName("branch_name") val branchName: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val quota: Int? = null,
    val session: String = "",
)

data class Specialty(
    val value: String,
    val label: String,
    val icon: ImageVector,
)
