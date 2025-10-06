package com.ecocollet.collector.ui.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityNavigationBinding

class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address: String? = null
    private var requestCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del intent
        latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        address = intent.getStringExtra("ADDRESS")
        requestCode = intent.getStringExtra("REQUEST_CODE")

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Ubicación no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupUI()
        launchGoogleMapsNavigation()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Iniciando Navegación"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        binding.tvDestination.text = address ?: "Destino"
        if (requestCode != null) {
            binding.tvRequestCode.text = "Código: $requestCode"
        }
        binding.tvStatus.text = "Abriendo Google Maps..."
    }

    private fun launchGoogleMapsNavigation() {
        try {
            // Intent para Google Maps con navegación
            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            // Verificar si Google Maps está instalado
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
                // Cerrar esta actividad después de abrir Maps
                finish()
            } else {    
                // Fallback: abrir en navegador web
                binding.tvStatus.text = "Google Maps no instalado. Abriendo en navegador..."
                openInWebBrowser()
            }
        } catch (e: Exception) {
            binding.tvStatus.text = "Error: ${e.message}"
            Toast.makeText(this, "Error al abrir navegación", Toast.LENGTH_SHORT).show()
            openInWebBrowser() // Fallback
        }
    }

    private fun openInWebBrowser() {
        try {
            val mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
            startActivity(browserIntent)
            finish()
        } catch (e: Exception) {
            binding.tvStatus.text = "No se pudo abrir navegación"
            Toast.makeText(this, "No hay aplicación de mapas disponible", Toast.LENGTH_LONG).show()
        }
    }
}