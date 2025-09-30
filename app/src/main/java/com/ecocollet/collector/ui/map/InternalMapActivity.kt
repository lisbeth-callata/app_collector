package com.ecocollet.collector.ui.map

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityInternalMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class InternalMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityInternalMapBinding
    private var googleMap: GoogleMap? = null

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address: String? = null
    private var requestCode: String? = null
    private var title: String? = null

    companion object {
        private const val TAG = "InternalMapActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInternalMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "InternalMapActivity creada")

        // Obtener datos del intent
        latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        address = intent.getStringExtra("ADDRESS")
        requestCode = intent.getStringExtra("REQUEST_CODE")
        title = intent.getStringExtra("TITLE") ?: "Ubicación"

        Log.d(TAG, "Datos recibidos - Lat: $latitude, Lng: $longitude, Código: $requestCode")

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Ubicación no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        updateUI()
        setupMap()
    }

    private fun setupToolbar() {
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun updateUI() {
        try {
            binding.tvAddress.text = address ?: "Dirección no disponible"
            binding.tvCoordinates.text = "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"

            if (requestCode != null) {
                binding.tvRequestCode.text = "Código: $requestCode"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando UI: ${e.message}")
        }
    }

    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment

            if (mapFragment != null) {
                mapFragment.getMapAsync(this)
                Log.d(TAG, "MapFragment encontrado, esperando onMapReady...")
            } else {
                Log.e(TAG, "Error: No se pudo encontrar el MapFragment")
                Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando mapa: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d(TAG, "onMapReady llamado - Mapa listo")

        googleMap = map

        try {
            // Configurar mapa
            googleMap?.uiSettings?.isZoomControlsEnabled = true
            googleMap?.uiSettings?.isScrollGesturesEnabled = true
            googleMap?.uiSettings?.isRotateGesturesEnabled = true

            // Añadir marcador en la ubicación
            val location = LatLng(latitude, longitude)
            googleMap?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(requestCode ?: "Ubicación")
                    .snippet(address ?: "Sin dirección")
            )

            // Mover cámara a la ubicación
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

            Log.d(TAG, "Marcador añadido en: $location")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onMapReady: ${e.message}")
            Toast.makeText(this, "Error al mostrar el mapa", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "InternalMapActivity destruida")
    }
}