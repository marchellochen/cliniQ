package com.example.cliniq

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Runs network code in a coroutine (so the UI doesn't freeze) and shows any error as a Toast.
 * [onDone] always runs afterwards, e.g. to re-enable a button.
 */
fun AppCompatActivity.launchSafely(onDone: () -> Unit = {}, block: suspend () -> Unit) {
    lifecycleScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Toast.makeText(this@launchSafely, e.message ?: "Something went wrong", Toast.LENGTH_LONG).show()
        }
        onDone()
    }
}

/** Shared helpers for every screen except Login and Signup. */
open class BaseActivity : AppCompatActivity() {

    protected fun setupBar(title: String, showBack: Boolean = true) {
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    /** Jump back to Home and close everything stacked above it. */
    protected fun goHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
