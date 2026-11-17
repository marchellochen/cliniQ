package com.example.cliniq.finddoctor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.example.cliniq.R

@Composable
fun CliniQTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = colorResource(R.color.teal),
        onPrimary = colorResource(R.color.white),
        primaryContainer = colorResource(R.color.mint),
        onPrimaryContainer = colorResource(R.color.teal_dark),
        secondary = colorResource(R.color.scrub_blue),
        background = colorResource(R.color.bg),
        onBackground = colorResource(R.color.teal_dark),
        surface = colorResource(R.color.white),
        onSurface = colorResource(R.color.teal_dark),
        onSurfaceVariant = colorResource(R.color.gray),
        error = colorResource(R.color.danger),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
