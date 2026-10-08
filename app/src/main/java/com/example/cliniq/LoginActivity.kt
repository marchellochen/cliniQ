package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cliniq.databinding.ActivityLoginBinding
import io.github.jan.supabase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            Toast.makeText(this, "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, then rebuild", Toast.LENGTH_LONG).show()
            return
        }

        // Already logged in from a previous run? Skip straight to Home.
        binding.root.visibility = View.INVISIBLE
        launchSafely(onDone = { binding.root.visibility = View.VISIBLE }) {
            supabase.auth.awaitInitialization()
            if (ClinicRepo.hasSession()) goHome()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnLogin.isEnabled = false
            launchSafely(onDone = { binding.btnLogin.isEnabled = true }) {
                ClinicRepo.signIn(email, pass)
                goHome()
            }
        }
        binding.tvGoSignup.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
