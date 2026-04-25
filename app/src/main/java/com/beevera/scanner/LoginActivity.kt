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
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("beevera_prefs", MODE_PRIVATE)

        // 1. Inicializamos Firebase
        auth = FirebaseAuth.getInstance()

        // ── Flujo de navegación ───────────────────────────────────────
        // Si Firebase detecta que ya hay una sesión abierta → ir directo al main
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        // ── Iniciar sesión ────────────────────────────────────────────
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            when {
                !esCorreoValido(email) -> {
                    binding.etEmail.error = "Ingresa un correo válido"
                }
                password.isEmpty() -> {
                    binding.etPassword.error = "Ingresa tu contraseña"
                }
                else -> {
                    // Bloqueamos el botón
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "Iniciando..."

                    // 2. ─── INICIAR SESIÓN EN LA NUBE CON FIREBASE ───
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid

                                if (userId != null) {
                                    // Vamos a Firestore a buscar el nombre real antes de entrar a la app
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("usuarios").document(userId).get()
                                        .addOnSuccessListener { document ->
                                            val nombreReal = document.getString("nombre_usuario") ?: "Usuario"

                                            // Ahora sí, guardamos TODO en preferencias
                                            prefs.edit()
                                                .putString("user_email", email)
                                                .putString("user_name", nombreReal) // ¡Aquí está la clave!
                                                .apply()

                                            goToMain()
                                        }
                                        .addOnFailureListener {
                                            // Si falla Firestore, al menos entramos con el email
                                            prefs.edit().putString("user_email", email).apply()
                                            goToMain()
                                        }
                                }
                            } else {
                                // Falló el login
                                binding.btnLogin.isEnabled = true
                                binding.btnLogin.text = "INICIAR SESIÓN"
                                Toast.makeText(this, "Error: Correo o contraseña incorrectos", Toast.LENGTH_LONG).show()
                            }
                        }
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
                .setMessage("Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña.")
                .setView(input)
                .setPositiveButton("Enviar correo") { _, _ ->
                    val correo = input.text.toString().trim()

                    if (esCorreoValido(correo)) {
                        // 3. ─── FIREBASE ENVÍA EL CORREO DE RECUPERACIÓN ───
                        auth.sendPasswordResetEmail(correo)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "¡Correo enviado! Revisa tu bandeja de entrada o SPAM.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Error al enviar el correo.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
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

    private fun esCorreoValido(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        return email.matches(emailRegex.toRegex())
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}