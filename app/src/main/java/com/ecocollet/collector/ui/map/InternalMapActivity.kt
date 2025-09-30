package com.ecocollet.collector.ui.map

import android.os.Bundle
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
    private lateinit var googleMap: GoogleMap

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address: String? = null
    private var requestCode: String? = null
    private var title: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInternalMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del intent
        latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        address = intent.getStringExtra("ADDRESS")
        requestCode = intent.getStringExtra("REQUEST_CODE")
        title = intent.getStringExtra("TITLE") ?: "Ubicación"

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
        binding.tvAddress.text = address ?: "Dirección no disponible"
        binding.tvLocationInfo.text = "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"

        if (requestCode != null) {
            binding.tvRequestCode.text = "Código: $requestCode"
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment

        if (mapFragment != null) {
            mapFragment.getMapAsync(this)
        } else {
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configurar mapa
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true

        // Añadir marcador
        val location = LatLng(latitude, longitude)
        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(requestCode ?: "Ubicación")
                .snippet(address ?: "Sin dirección")
        )

        // Mover cámara
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }
}