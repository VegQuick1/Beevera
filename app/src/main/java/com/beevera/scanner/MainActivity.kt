package com.beevera.scanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.beevera.scanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // ── Navegación personalizada sin "memoria" ──
        binding.bottomNav.setOnItemSelectedListener { item ->
            val opciones = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(false) // 🪄 No restaura la pantalla donde te quedaste
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = false // 🪄 No guarda la pantalla (Configuración) al salir
                )
                .build()

            try {
                navController.navigate(item.itemId, null, opciones)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}