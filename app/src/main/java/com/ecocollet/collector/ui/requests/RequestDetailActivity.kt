package com.ecocollet.collector.ui.requests

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityRequestDetailBinding
import com.ecocollet.collector.ui.map.InternalMapActivity
import com.ecocollet.collector.ui.map.NavigationActivity
import android.view.View
class RequestDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestDetailBinding
    private lateinit var currentRequest: CollectionRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        getRequestData()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun getRequestData() {
        currentRequest = intent.getParcelableExtra("REQUEST") ?: run {
            Toast.makeText(this, "Error: Solicitud no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        populateRequestData()
    }

    private fun populateRequestData() {
        with(binding) {
            tvRequestCode.text = currentRequest.code
            tvUserName.text = currentRequest.userName
            tvUserPhone.text = currentRequest.userPhone ?: "No disponible"
            tvMaterial.text = currentRequest.material
            tvDescription.text = currentRequest.description ?: "Sin descripción"
            tvAddress.text = currentRequest.getFullAddress()
            tvCreatedAt.text = formatDate(currentRequest.createdAt)

            tvWeight.text = if (currentRequest.weight != null) {
                "${currentRequest.weight} kg"
            } else {
                "Pendiente"
            }

            // Configurar estado
            val (statusText, backgroundRes) = getStatusInfo(currentRequest.status)
            tvStatus.text = statusText
            tvStatus.setBackgroundResource(backgroundRes)

            // Mostrar/ocultar botones según estado
            if (currentRequest.status == "PENDING") {
                btnUpdate.visibility = android.view.View.VISIBLE
            } else {
                btnUpdate.visibility = android.view.View.GONE
            }
            setupLocationDetails()
        }
    }

    private fun setupLocationDetails() {
        val hasLocationDetails = currentRequest.hasCompleteLocationInfo()

        if (hasLocationDetails) {
            binding.layoutLocationDetails.visibility = View.VISIBLE

            with(binding) {
                tvAddressUser.text = currentRequest.addressUser ?: "No especificada"

                tvReference.text = currentRequest.reference ?: "Sin referencia"

                val locationParts = listOfNotNull(
                    currentRequest.district,
                    currentRequest.province,
                    currentRequest.region
                )
                tvAdministrativeArea.text = if (locationParts.isNotEmpty()) {
                    locationParts.joinToString(", ")
                } else {
                    "No disponible"
                }
                if (currentRequest.hasValidLocation()) {
                    tvCoordinates.text = "Lat: ${"%.6f".format(currentRequest.latitude)}, Lng: ${"%.6f".format(currentRequest.longitude)}"
                } else {
                    tvCoordinates.text = "Coordenadas no disponibles"
                }
            }
        } else {
            binding.layoutLocationDetails.visibility = View.GONE
        }
    }
    private fun setupListeners() {
        binding.btnUpdate.setOnClickListener {
            openUpdateRequest()
        }

        binding.btnCall.setOnClickListener {
            callUser()
        }

        binding.btnNavigate.setOnClickListener {
            navigateToLocationInternal()
        }

        binding.btnMap.setOnClickListener {
            openMapInternal()
        }
    }

    private fun openMapInternal() {
        currentRequest.latitude?.let { lat ->
            currentRequest.longitude?.let { lng ->
                val intent = Intent(this, InternalMapActivity::class.java).apply {
                    putExtra("LATITUDE", lat)
                    putExtra("LONGITUDE", lng)
                    putExtra("ADDRESS", currentRequest.address)
                    putExtra("REQUEST_CODE", currentRequest.code)
                    putExtra("TITLE", "Ubicación de ${currentRequest.getSafeUserName()}")
                }
                startActivity(intent)
            }
        } ?: run {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLocationInternal() {
        currentRequest.latitude?.let { lat ->
            currentRequest.longitude?.let { lng ->
                val intent = Intent(this, NavigationActivity::class.java).apply {
                    putExtra("LATITUDE", lat)
                    putExtra("LONGITUDE", lng)
                    putExtra("ADDRESS", currentRequest.address)
                    putExtra("REQUEST_CODE", currentRequest.code)
                }
                startActivity(intent)
            }
        } ?: run {
            Toast.makeText(this, "Ubicación no disponible para navegación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUpdateRequest() {
        val intent = Intent(this, UpdateRequestActivity::class.java)
        intent.putExtra("REQUEST", currentRequest)
        startActivity(intent)
    }

    private fun callUser() {
        currentRequest.userPhone?.let { phone ->
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        } ?: run {
            Toast.makeText(this, "Número de teléfono no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getStatusInfo(status: String): Pair<String, Int> {
        return when (status.uppercase()) {
            "PENDING" -> "PENDIENTE" to R.drawable.bg_status_pending
            "COLLECTED" -> "RECOLECTADO" to R.drawable.bg_status_collected
            "SCHEDULED" -> "PROGRAMADO" to R.drawable.bg_status_scheduled
            "CANCELED" -> "CANCELADO" to R.drawable.bg_status_cancelled
            else -> status to R.drawable.bg_status_pending
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            dateString.substring(0, 10) // YYYY-MM-DD
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onResume() {
        super.onResume()
        setResult(RESULT_OK)
    }
}