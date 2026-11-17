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
            launchSafely(
                onDone = {
                    binding.btnLogin.isEnabled = true
                },
                onError = { e ->
                    if (e.message?.contains("invalid_credentials", ignoreCase = true) == true) {
                        binding.emailLayout.error = "Invalid email or password"
                        binding.passwordLayout.error = "Invalid email or password"
                    } else if (e.message?.contains("not_confirmed", ignoreCase = true) == true ||
                        e.message?.contains("not confirmed", ignoreCase = true) == true
                    ) {
                        Toast.makeText(
                            this,
                            "Please confirm your email first. Check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            e.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            ) {
                ClinicRepo.signIn(email, pass)
                goHome()
            }
        }
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.error = "Enter your email above first"
                return@setOnClickListener
            }
            binding.emailLayout.error = null
            binding.tvForgotPassword.isEnabled = false
            launchSafely(onDone = { binding.tvForgotPassword.isEnabled = true }) {
                ClinicRepo.sendPasswordReset(email)
                Toast.makeText(
                    this,
                    "If this email is registered, a reset link has been sent.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.tvGoSignup.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
