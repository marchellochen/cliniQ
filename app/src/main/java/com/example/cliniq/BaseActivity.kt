package com.example.cliniq

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Runs network code in a coroutine (so the UI doesn't freeze) and shows any error as a Toast.
 * [onDone] always runs afterwards, e.g. to re-enable a button.
 */
fun AppCompatActivity.launchSafely(
    onDone: () -> Unit = {},
    onError: (Exception) -> Unit = {
        Toast.makeText(
            this,
            it.message ?: "Something went wrong",
            Toast.LENGTH_LONG
        ).show()
    },
    block: suspend () -> Unit
) {
    lifecycleScope.launch {
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

/** Shared helpers for every screen except Login and Signup. */
open class BaseActivity : AppCompatActivity() {

    protected fun setupBar(title: String, showBack: Boolean = true) {
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)
    }

    /** Pushes [root] down if the action bar is drawn on top of it (edge-to-edge windows). */
    protected fun keepContentBelowActionBar(root: View) {
        val bar = findViewById<View>(androidx.appcompat.R.id.action_bar_container) ?: return
        val adjust = {
            val barLoc = IntArray(2).also { bar.getLocationInWindow(it) }
            val rootLoc = IntArray(2).also { root.getLocationInWindow(it) }
            val overlap = (barLoc[1] + bar.height - rootLoc[1]).coerceAtLeast(0)
            if (overlap != 0 && root.paddingTop == 0) {
                root.setPadding(root.paddingLeft, overlap, root.paddingRight, root.paddingBottom)
            }
        }
        root.post(adjust)
        root.viewTreeObserver.addOnGlobalLayoutListener(adjust)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    /** Jump back to Home (or to the given tab) and close everything stacked above it. */
    protected fun goHome(tab: Int? = null) {
        val intent = Intent(this, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (tab != null) intent.putExtra(HomeActivity.EXTRA_TAB, tab)

        startActivity(intent)
        finish()
    }
}
