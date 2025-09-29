package com.ecocollet.collector.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.ui.requests.RequestDetailActivity
import com.ecocollet.collector.ui.requests.RequestsViewModel
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityRouteMapBinding
import com.ecocollet.collector.utils.AuthManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale

class RouteMapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var binding: ActivityRouteMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: RequestsViewModel
    private lateinit var authManager: AuthManager
    private var markers: MutableMap<Marker, CollectionRequest> = mutableMapOf()
    private var currentFilter: String? = null
    private var allRequests: List<CollectionRequest> = emptyList()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(this)[RequestsViewModel::class.java]
        authManager = AuthManager(this)

        setupToolbar()
        setupObservers()
        setupListeners()

        if (checkGooglePlayServices()) {
            setupMap()
        } else {
            Toast.makeText(this, "Google Play Services requeridos", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Mapa de Ruta Optimizada"
    }

    private fun setupObservers() {
        viewModel.requests.observe(this) { requests ->
            allRequests = requests
            if (requests.isNotEmpty()) {
                displayRequestsOnMap(requests)
                applyCurrentFilter()
            } else {
                Toast.makeText(this, "No hay solicitudes para mostrar", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnMyLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.btnOptimizeRoute.setOnClickListener {
            optimizeRoute()
        }

        binding.btnZoomToAll.setOnClickListener {
            zoomToAllMarkers()
        }

        binding.btnFilterAll.setOnClickListener {
            currentFilter = null
            applyCurrentFilter()
            updateFilterButtons()
        }

        binding.btnFilterPending.setOnClickListener {
            currentFilter = "PENDING"
            applyCurrentFilter()
            updateFilterButtons()
        }

        binding.btnPopupNavigate.setOnClickListener {
            navigateToSelectedRequest()
        }

        binding.btnPopupDetails.setOnClickListener {
            showRequestDetails()
        }
    }

    private fun updateFilterButtons() {
        binding.btnFilterAll.isSelected = currentFilter == null
        binding.btnFilterPending.isSelected = currentFilter == "PENDING"
    }

    private fun applyCurrentFilter() {
        val filteredRequests = if (currentFilter == "PENDING") {
            allRequests.filter { it.status == "PENDING" }
        } else {
            allRequests
        }
        displayRequestsOnMap(filteredRequests)
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this) ?: run {
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (checkLocationPermission()) {
            googleMap.isMyLocationEnabled = true
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.setOnMarkerClickListener(this)

        if (checkLocationPermission()) {
            enableMyLocation()
            getCurrentLocation()
            setupIntentExtras()
        } else {
            requestLocationPermission()
        }

        // ✅ CORREGIDO: Usar loadAllRequests() en lugar de loadTodayRequests()
        viewModel.loadAllRequests()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permisos de ubicación denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                    }
                }
        }
    }

    private fun displayRequestsOnMap(requests: List<CollectionRequest>) {
        googleMap.clear()
        markers.clear()
        hideRequestInfo()

        val boundsBuilder = LatLngBounds.Builder()
        var hasValidLocations = false

        requests.forEach { request ->
            request.latitude?.let { lat ->
                request.longitude?.let { lng ->
                    val location = LatLng(lat, lng)

                    // ✅ USAR FUNCIÓN SEGURA PARA assignmentStatus
                    val assignmentStatus = request.getSafeAssignmentStatus()

                    val markerColor = when (assignmentStatus) {
                        "AVAILABLE" -> BitmapDescriptorFactory.HUE_GREEN
                        "PENDING" -> if (request.assignedCollectorId == authManager.getUserId()) {
                            BitmapDescriptorFactory.HUE_BLUE
                        } else {
                            BitmapDescriptorFactory.HUE_ORANGE
                        }
                        "IN_PROGRESS" -> BitmapDescriptorFactory.HUE_BLUE
                        "COMPLETED" -> BitmapDescriptorFactory.HUE_VIOLET
                        else -> BitmapDescriptorFactory.HUE_RED
                    }

                    val marker = addMarkerForRequest(location, request, markerColor)
                    markers[marker] = request
                    boundsBuilder.include(location)
                    hasValidLocations = true
                }
            }
        }

        if (hasValidLocations) {
            val bounds = boundsBuilder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    // ✅ CORREGIDO: Añadir parámetro markerColor
    private fun addMarkerForRequest(location: LatLng, request: CollectionRequest, markerColor: Float): Marker {
        // ✅ USAR FUNCIÓN SEGURA PARA userName
        val userName = request.getSafeUserName()

        val snippet = when (request.status) {
            "PENDING" -> "Pendiente • ${request.material} • $userName"
            "COLLECTED" -> "Recolectado • ${request.material} • $userName"
            "SCHEDULED" -> "Programado • ${request.material} • $userName"
            else -> "${request.status} • ${request.material} • $userName"
        }

        return googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(request.code)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
        )!!
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val request = markers[marker]
        request?.let {
            showRequestInfo(it)
        }
        return true
    }

    private fun showRequestInfo(request: CollectionRequest) {
        with(binding) {
            tvPopupCode.text = request.code
            tvPopupUser.text = request.userName
            tvPopupMaterial.text = request.material
            tvPopupAddress.text = request.address ?: "Dirección no disponible"

            // Configurar estado con color correspondiente
            val (statusText, backgroundRes) = getStatusInfo(request.status)
            tvPopupStatus.text = statusText
            tvPopupStatus.setBackgroundResource(backgroundRes)

            cardRequestInfo.visibility = View.VISIBLE
        }
    }

    private fun hideRequestInfo() {
        binding.cardRequestInfo.visibility = View.GONE
    }

    private fun getStatusInfo(status: String): Pair<String, Int> {
        return when (status) {
            "PENDING" -> "PENDIENTE" to R.drawable.bg_status_pending
            "COLLECTED" -> "RECOLECTADO" to R.drawable.bg_status_collected
            "SCHEDULED" -> "PROGRAMADO" to R.drawable.bg_status_scheduled
            "CANCELLED" -> "CANCELADO" to R.drawable.bg_status_cancelled
            else -> status to R.drawable.bg_status_pending
        }
    }

    private fun navigateToSelectedRequest() {
        val selectedMarker = markers.keys.firstOrNull { marker ->
            val request = markers[marker]
            request?.let { it.code == binding.tvPopupCode.text.toString() } ?: false
        }

        selectedMarker?.let { marker ->
            val request = markers[marker]
            request?.let {
                navigateToLocation(it.latitude, it.longitude, it.address)
            }
        }
    }

    private fun showRequestDetails() {
        val selectedRequest = markers.values.firstOrNull { it.code == binding.tvPopupCode.text.toString() }
        selectedRequest?.let {
            val intent = Intent(this, RequestDetailActivity::class.java)
            intent.putExtra("REQUEST", it)
            startActivity(intent)
        }
    }

    private fun navigateToLocation(latitude: Double?, longitude: Double?, address: String?) {
        latitude?.let { lat ->
            longitude?.let { lng ->
                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback: abrir en maps web
                    val webUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                    startActivity(webIntent)
                }
            }
        }
    }

    private fun optimizeRoute() {
        val pendingRequests = allRequests.filter { it.status == "PENDING" }
            .mapNotNull { request ->
                request.latitude?.let { lat ->
                    request.longitude?.let { lng ->
                        LatLng(lat, lng) to request
                    }
                }
            }

        if (pendingRequests.size > 1) {
            drawOptimizedRoute(pendingRequests.map { it.first })
            Toast.makeText(this, "Ruta optimizada para ${pendingRequests.size} ubicaciones pendientes", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Se necesitan al menos 2 ubicaciones pendientes para optimizar la ruta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawOptimizedRoute(route: List<LatLng>) {
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(route)
                .width(8f)
                .color(ContextCompat.getColor(this, R.color.green_primary))
                .geodesic(true)
        )
    }

    private fun zoomToAllMarkers() {
        val boundsBuilder = LatLngBounds.Builder()
        markers.keys.forEach { marker ->
            boundsBuilder.include(marker.position)
        }

        if (markers.isNotEmpty()) {
            val bounds = boundsBuilder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    private fun setupIntentExtras() {
        val zoomLat = intent.getDoubleExtra("ZOOM_TO_LAT", 0.0)
        val zoomLng = intent.getDoubleExtra("ZOOM_TO_LNG", 0.0)
        val zoomRequest = intent.getParcelableExtra<CollectionRequest>("ZOOM_TO_REQUEST")

        if (zoomLat != 0.0 && zoomLng != 0.0) {
            val location = LatLng(zoomLat, zoomLng)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))

            zoomRequest?.let { request ->
                showRequestInfo(request)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ CORREGIDO: Usar loadAllRequests() en lugar de loadTodayRequests()
        viewModel.loadAllRequests()
    }
}