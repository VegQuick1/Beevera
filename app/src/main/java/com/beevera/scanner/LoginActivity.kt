package com.beevera.scanner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.beevera.scanner.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("beevera_prefs", MODE_PRIVATE)

        // ── Flujo de navegación ───────────────────────────────────────
        // Si ya hay sesión activa → ir directo al main
        if (prefs.getString("user_email", null) != null) {
            goToMain()
            return
        }

        // Si nunca se ha registrado → mostrar registro
        if (!prefs.getBoolean("ya_registro", false)) {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        // ── Iniciar sesión ────────────────────────────────────────────
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            val emailGuardado = prefs.getString("user_email_registrado", "") ?: ""
            val passGuardada  = prefs.getString("user_pass_registrada", "") ?: ""

            when {
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.etEmail.error = "El correo y la contraseña no son válidos"
                }
                password.length < 8 -> {
                    binding.etPassword.error = "El correo y la contraseña no son válidos"
                }
                email != emailGuardado || password != passGuardada -> {
                    binding.etEmail.error = "El correo y la contraseña no son válidos"
                    binding.etPassword.error = "El correo y la contraseña no son válidos"
                }
                else -> {
                    val nombre = prefs.getString("user_name_registrado", "") ?: ""
                    prefs.edit()
                        .putString("user_email", email)
                        .putString("user_name", nombre)
                        .apply()
                    goToMain()
                }
            }
        }

        // ── Olvidaste tu contraseña ───────────────────────────────────
        binding.tvForgotPassword.setOnClickListener {
            val input = EditText(this).apply {
                hint = "Tu correo electrónico"
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                setPadding(48, 32, 48, 16)
            }
            AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Te mostraremos tu contraseña si el correo coincide con el registrado.")
                .setView(input)
                .setPositiveButton("Buscar") { _, _ ->
                    val correo = input.text.toString().trim()
                    val emailGuardado = prefs.getString("user_email_registrado", "") ?: ""
                    val passGuardada  = prefs.getString("user_pass_registrada", "") ?: ""
                    if (correo == emailGuardado) {
                        AlertDialog.Builder(this)
                            .setTitle("Tu contraseña")
                            .setMessage("La contraseña registrada es:\n\n$passGuardada")
                            .setPositiveButton("Entendido", null)
                            .show()
                    } else {
                        Toast.makeText(this, "No encontramos ese correo", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // ── Ir a registro ─────────────────────────────────────────────
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}