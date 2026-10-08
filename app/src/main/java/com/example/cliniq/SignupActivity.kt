package com.example.cliniq

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cliniq.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            when {
                name.isEmpty() || email.isEmpty() -> toast("Please fill in your name and email")
                pass.length < 6 -> toast("Password must be at least 6 characters")
                else -> {
                    binding.btnSignup.isEnabled = false
                    launchSafely(onDone = { binding.btnSignup.isEnabled = true }) {
                        ClinicRepo.signUp(name, email, pass)
                        if (ClinicRepo.hasSession()) {
                            startActivity(
                                Intent(this, HomeActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                        } else {
                            // Email confirmation is switched on in Supabase
                            toast("Account created. Confirm your email, then log in.")
                            finish()
                        }
                    }
                }
            }
        }
        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
