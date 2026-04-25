package com.beevera.scanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beevera.scanner.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    // 1. Declaramos la variable de Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Inicializamos Firebase
        auth = FirebaseAuth.getInstance()

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
                !esCorreoValido(email) -> {
                    binding.etEmail.error = "Ingresa un correo válido (ej. usuario@dominio.com)"
                }
                pass.length < 8 -> {
                    binding.etPassword.error = "La contraseña debe tener al menos 8 caracteres"
                }
                pass != confirm -> {
                    binding.etConfirm.error = "Las contraseñas no coinciden"
                }
                else -> {
                    // Bloqueamos el botón temporalmente para que no le den doble clic
                    binding.btnRegistrar.isEnabled = false
                    binding.btnRegistrar.text = "Registrando..."

                    val nombreFinal = nombre.replaceFirstChar { it.uppercase() }

                    // 3. ─── CREAR USUARIO EN LA NUBE CON FIREBASE ───
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // ¡Éxito! Se guardó en Firebase. Ahora guardamos localmente.
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
                            } else {
                                // Falló (ej. el correo ya existe, contraseña muy débil o no hay internet)
                                binding.btnRegistrar.isEnabled = true
                                binding.btnRegistrar.text = "REGISTRARSE"

                                // Mostramos el error que nos devuelve Google
                                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        }
    }

    private fun esCorreoValido(email: String): Boolean {
        // Exige formato estricto: texto @ texto . texto (de 2 a 6 letras al final)
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        return email.matches(emailRegex.toRegex())
    }
}