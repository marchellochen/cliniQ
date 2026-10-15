package com.example.cliniq

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dobFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun parseDate(iso: String?): LocalDate? =
    if (iso == null) null else runCatching { LocalDate.parse(iso) }.getOrNull()

/** "2001-03-12" becomes the age in years (or null if there's no valid date). */
fun ageFrom(iso: String?): Int? =
    parseDate(iso)?.let { Period.between(it, LocalDate.now()).years }

/** "2001-03-12" becomes "12 Mar 2001" for display. */
fun formatDob(iso: String?): String =
    parseDate(iso)?.format(dobFormat).orEmpty()

/** Opens a calendar dialog. [onPicked] receives the date as "yyyy-MM-dd". */
fun AppCompatActivity.pickDob(currentIso: String?, onPicked: (String) -> Unit) {
    val start = (parseDate(currentIso) ?: LocalDate.of(2000, 1, 1))
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    val picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Date of birth")
        .setSelection(start)
        .setCalendarConstraints(
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())   // no future dates
                .build()
        )
        .build()

    picker.addOnPositiveButtonClickListener { millis ->
        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        onPicked(date.toString())
    }
    picker.show(supportFragmentManager, "dob")
}