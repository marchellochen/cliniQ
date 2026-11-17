package com.example.cliniq.finddoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class FindDoctorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CliniQTheme {
                val navController = rememberNavController()
                val doctorArg = listOf(navArgument("doctorId") { type = NavType.IntType })

                NavHost(navController, startDestination = "search") {
                    composable("search") {
                        SearchDoctorScreen(
                            onBack = { finish() },
                            onDoctorClick = { navController.navigate("doctor/$it") },
                        )
                    }
                    composable("doctor/{doctorId}", doctorArg) {
                        DoctorDetailScreen(
                            onBack = { navController.popBackStack() },
                            onBook = { navController.navigate("booking/$it") },
                        )
                    }
                    composable("booking/{doctorId}", doctorArg) { entry ->
                        BookingPlaceholderScreen(
                            doctorId = entry.arguments?.getInt("doctorId") ?: 0,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
