package com.example.cliniq

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Same as the Activity version in BaseActivity.kt, but tied to the fragment's view:
 * if the user leaves the screen, the work is cancelled instead of touching a dead view.
 */
fun Fragment.launchSafely(
    onDone: () -> Unit = {},
    onError: (Exception) -> Unit = {
        Toast.makeText(
            requireContext(),
            it.message ?: "Something went wrong",
            Toast.LENGTH_LONG
        ).show()
    },
    block: suspend () -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e)
        }
        onDone()
    }
}
