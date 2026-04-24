package com.beevera.scanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beevera.scanner.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("beevera_prefs", MODE_PRIVATE)

        // Botón volver — solo visible si ya existe un registro previo
        val yaRegistro = prefs.getBoolean("ya_registro", false)
        binding.btnBack.visibility = if (yaRegistro)
            android.view.View.VISIBLE else android.view.View.GONE

        binding.btnBack.setOnClickListener { finish() }

        binding.tvIniciarSesion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegistrar.setOnClickListener {
            val nombre  = binding.etNombre.text.toString().trim()
            val email   = binding.etEmail.text.toString().trim()
            val pass    = binding.etPassword.text.toString()
            val confirm = binding.etConfirm.text.toString()

            when {
                nombre.isEmpty() -> {
                    binding.etNombre.error = "Ingresa tu nombre"
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.etEmail.error = "El correo y la contraseña no son válidos"
                }
                pass.length < 8 -> {
                    binding.etPassword.error = "El correo y la contraseña no son válidos"
                }
                pass != confirm -> {
                    binding.etConfirm.error = "Las contraseñas no coinciden"
                }
                else -> {
                    val nombreFinal = nombre.replaceFirstChar { it.uppercase() }

                    // Guarda credenciales del registro y sesión activa
                    prefs.edit()
                        .putString("user_email_registrado", email)
                        .putString("user_pass_registrada", pass)
                        .putString("user_name_registrado", nombreFinal)
                        .putString("user_email", email)
                        .putString("user_name", nombreFinal)
                        .putBoolean("ya_registro", true)
                        .apply()

                    Toast.makeText(this, "¡Bienvenido, $nombreFinal!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
        }
    }
}