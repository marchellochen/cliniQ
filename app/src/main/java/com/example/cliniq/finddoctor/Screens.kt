package com.example.cliniq.finddoctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(24.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Try again") }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

@Composable
fun SearchDoctorScreen(
    onBack: () -> Unit,
    onDoctorClick: (Int) -> Unit,
    viewModel: SearchDoctorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BackTopBar("Find a Doctor", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                DoctorSearchBar(
                    value = state.specialtyQuery,
                    onValueChange = viewModel::onSpecialtyQueryChange,
                    placeholder = "Search specialty",
                    icon = Icons.Filled.Search,
                )
            }
            item {
                DoctorSearchBar(
                    value = state.locationQuery,
                    onValueChange = viewModel::onLocationQueryChange,
                    placeholder = "Search location",
                    icon = Icons.Filled.LocationOn,
                )
            }
            item { SectionTitle("Specialties", Modifier.padding(top = 4.dp)) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(specialties, key = { it.value }) { specialty ->
                        SpecialtyCard(
                            specialty = specialty,
                            selected = state.selectedSpecialty == specialty.value,
                            onClick = { viewModel.onSpecialtyClick(specialty.value) },
                        )
                    }
                }
            }
            item { SectionTitle("Doctors", Modifier.padding(top = 4.dp)) }
            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> item {
                    ErrorState(state.error.orEmpty(), viewModel::load, Modifier.fillMaxWidth())
                }
                state.doctors.isEmpty() -> item {
                    Text(
                        "No doctors found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
                else -> items(state.doctors, key = { it.id }) { doctor ->
                    DoctorCard(doctor, onClick = { onDoctorClick(doctor.id) })
                }
            }
        }
    }
}

@Composable
fun DoctorDetailScreen(
    onBack: () -> Unit,
    onBook: (Int) -> Unit,
    viewModel: DoctorDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val doctor = state.doctor

    Scaffold(
        topBar = { BackTopBar("", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (doctor != null) {
                Button(
                    onClick = { onBook(doctor.id) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                ) { Text("Book an Appointment", fontSize = 16.sp) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null || doctor == null -> ErrorState(
                    state.error ?: "Doctor not found",
                    viewModel::load,
                    Modifier.align(Alignment.Center),
                )
                else -> DoctorDetailContent(doctor, state.schedules)
            }
        }
    }
}

@Composable
private fun DoctorDetailContent(doctor: com.example.cliniq.Doctor, schedules: List<DoctorSchedule>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        DoctorPhoto(doctor.photoUrl, 128.dp)
        Text(
            doctor.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(doctor.specialty, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(doctor.room, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            doctor.experienceYears?.let {
                Text("$it years experience", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
            if (!doctor.bio.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionTitle("About")
                        Text(
                            doctor.bio,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            SectionTitle("Schedule", Modifier.padding(top = 24.dp, bottom = 12.dp))
            if (schedules.isEmpty()) {
                Text("No schedule available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                schedules
                    .sortedWith(compareBy({ it.dayOfWeek }, { sessionOrder(it.session) }))
                    .forEach { ScheduleItem(it) }
            }
        }
    }
}

@Composable
fun BookingPlaceholderScreen(doctorId: Int, onBack: () -> Unit) {
    Scaffold(
        topBar = { BackTopBar("Book an Appointment", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Booking for doctor #$doctorId", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
