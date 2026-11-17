package com.example.cliniq.finddoctor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.LocalHospital

val specialties = listOf(
    Specialty("General Practitioner", "General", Icons.Filled.MedicalServices),
    Specialty("Internal Medicine", "Internal Med.", Icons.Filled.MonitorHeart),
    Specialty("Pediatrics", "Pediatrics", Icons.Filled.ChildCare),
    Specialty("Cardiology", "Cardiology", Icons.Filled.Favorite),
    Specialty("Obstetrics & Gynecology", "OB/GYN", Icons.Filled.PregnantWoman),
    Specialty("Dentistry", "Dentistry", Icons.Filled.LocalHospital),
    Specialty("Neurology", "Neurology", Icons.Filled.Psychology),
    Specialty("ENT (Ear, Nose & Throat)", "ENT", Icons.Filled.Hearing),
    Specialty("General Surgery", "Surgery", Icons.Filled.Healing),
)
