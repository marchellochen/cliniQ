package com.example.cliniq

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

/** Shared helpers for every screen except Login. */
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
