package com.ecocollet.collector.ui.requests

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.R
import com.ecocollet.collector.utils.AuthManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.let
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RequestAdapter : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    var onItemClick: ((CollectionRequest) -> Unit)? = null
    var onActionClick: ((CollectionRequest, RequestAction) -> Unit)? = null
    var onAssignmentClick: ((CollectionRequest, AssignmentAction) -> Unit)? = null
    var onCompleteWithWeight: ((CollectionRequest) -> Unit)? = null

    private var requests: MutableList<CollectionRequest> = mutableListOf()
    private lateinit var authManager: AuthManager

    fun setAuthManager(authManager: AuthManager) {
        this.authManager = authManager
    }

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRequestCode: TextView = itemView.findViewById(R.id.tvRequestCode)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvMaterial: TextView = itemView.findViewById(R.id.tvMaterial)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        val tvWeight: TextView = itemView.findViewById(R.id.tvWeight)
        val tvAssignmentStatus: TextView = itemView.findViewById(R.id.tvAssignmentStatus)
        val tvAssignedCollector: TextView = itemView.findViewById(R.id.tvAssignedCollector)
        val tvUserContact: TextView = itemView.findViewById(R.id.tvUserContact)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val layoutAssignmentButtons: View = itemView.findViewById(R.id.layoutAssignmentButtons)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request_collector, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        val currentUserId = authManager.getUserId()

        with(holder) {
            tvRequestCode.text = request.code
            tvUserName.text = request.getSafeUserName()
            tvUserContact.text = request.getSafeUserPhone()
            tvMaterial.text = request.material
            tvDescription.text = request.getSafeDescription()
            tvAddress.text = request.getShortAddress()
            tvCreatedAt.text = formatDate(request.createdAt)
            tvWeight.text = request.getSafeWeight()

            // Estado combinado
            val combinedStatus = request.getCombinedStatus()
            val (statusText, statusBackground, statusTextColor) = getCombinedStatusInfo(combinedStatus)
            tvStatus.text = statusText
            tvStatus.setBackgroundResource(statusBackground)
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusTextColor))

            // Estado de asignación (más detallado)
            val assignmentStatus = request.getSafeAssignmentStatus()
            val (assignmentText, assignmentBackground, assignmentTextColor) = getAssignmentInfo(assignmentStatus)
            tvAssignmentStatus.text = assignmentText
            tvAssignmentStatus.setBackgroundResource(assignmentBackground)
            tvAssignmentStatus.setTextColor(ContextCompat.getColor(itemView.context, assignmentTextColor))

            // Información de asignación
            if (request.assignedCollectorName != null) {
                val isAssignedToMe = request.isAssignedTo(currentUserId)
                val statusInfo = if (isAssignedToMe) {
                    "✅ Asignado a ti"
                } else {
                    "👤 ${request.assignedCollectorName}"
                }
                tvAssignedCollector.text = statusInfo
                tvAssignedCollector.visibility = View.VISIBLE
            } else {
                tvAssignedCollector.visibility = View.GONE
            }

            // Configurar visibilidad de botones con nuevas validaciones
            updateButtonVisibility(request, holder, currentUserId)

            // Configurar listeners
            setupClickListeners(request, holder)
            holder.itemView.setOnClickListener {
                showLocationDetailsDialog(request, holder.itemView)
            }
        }
    }

    private fun showLocationDetailsDialog(request: CollectionRequest, itemView: View) {
        val context = itemView.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_location_details, null)

        with(dialogView) {
            findViewById<TextView>(R.id.tvFullAddress).text = request.getFullAddress()
            findViewById<TextView>(R.id.tvCoordinates).text =
                if (request.hasValidLocation())
                    "Coordenadas: ${"%.6f".format(request.latitude)}, ${"%.6f".format(request.longitude)}"
                else
                    "Coordenadas no disponibles"

            // Mostrar/ocultar secciones según disponibilidad
            val sectionReference = findViewById<View>(R.id.sectionReference)
            val sectionDistrict = findViewById<View>(R.id.sectionDistrict)

            sectionReference.visibility = if (!request.reference.isNullOrEmpty()) View.VISIBLE else View.GONE
            sectionDistrict.visibility = if (!request.district.isNullOrEmpty()) View.VISIBLE else View.GONE

            // Llenar datos específicos
            findViewById<TextView>(R.id.tvReference).text = request.reference ?: "Sin referencia"
            val locationParts = listOfNotNull(request.district, request.province, request.region)
            findViewById<TextView>(R.id.tvDistrict).text = if (locationParts.isNotEmpty()) {
                locationParts.joinToString(", ")
            } else {
                "Ubicación no disponible"
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("📍 Ubicación Completa - ${request.code}")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Abrir en Maps") { _, _ ->
                openInMaps(request, context)
            }
            .show()
    }
    private fun openInMaps(request: CollectionRequest, context: android.content.Context) {
        request.latitude?.let { lat ->
            request.longitude?.let { lng ->
                val uri = "geo:$lat,$lng?q=${Uri.encode(request.getFullAddress())}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No hay aplicación de mapas instalada", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Toast.makeText(context, "Ubicación no disponible para maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtonVisibility(request: CollectionRequest, holder: RequestViewHolder, currentUserId: Long) {
        val canClaim = request.canBeClaimedBy(currentUserId)
        val canRelease = request.canBeReleasedBy(currentUserId)
        val canComplete = request.canBeCompletedBy(currentUserId)
        val canUpdate = request.canBeUpdatedBy(currentUserId)
        val hasValidLocation = request.hasValidLocation()
        val hasPhone = !request.userPhone.isNullOrEmpty()

        with(holder) {
            // Botones de asignación
            if (canClaim || canRelease || canComplete) {
                layoutAssignmentButtons.visibility = View.VISIBLE

                itemView.findViewById<View>(R.id.btnClaim).visibility =
                    if (canClaim) View.VISIBLE else View.GONE

                itemView.findViewById<View>(R.id.btnRelease).visibility =
                    if (canRelease) View.VISIBLE else View.GONE

                itemView.findViewById<View>(R.id.btnComplete).visibility =
                    if (canComplete) View.VISIBLE else View.GONE
            } else {
                layoutAssignmentButtons.visibility = View.GONE
            }

            // Botón de navegación solo visible si tiene ubicación válida y está asignada a mí
            itemView.findViewById<View>(R.id.btnNavigate).visibility =
                if (hasValidLocation && request.isAssignedTo(currentUserId)) View.VISIBLE else View.GONE

            // Botón de llamada solo visible si tiene teléfono y está asignada a mí
            itemView.findViewById<View>(R.id.btnCall).visibility =
                if (hasPhone && request.isAssignedTo(currentUserId)) View.VISIBLE else View.GONE

            // Botón de actualización solo visible para solicitudes asignadas a mí o completadas
            itemView.findViewById<View>(R.id.btnUpdate).visibility =
                if (canUpdate) View.VISIBLE else View.GONE

            // Botón de mapa siempre visible si tiene ubicación
            itemView.findViewById<View>(R.id.btnMap).visibility =
                if (hasValidLocation) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners(request: CollectionRequest, holder: RequestViewHolder) {
        val currentUserId = authManager.getUserId()

        holder.itemView.findViewById<View>(R.id.btnClaim).setOnClickListener {
            if (request.canBeClaimedBy(currentUserId)) {
                onAssignmentClick?.invoke(request, AssignmentAction.CLAIM)
            }
        }

        holder.itemView.findViewById<View>(R.id.btnRelease).setOnClickListener {
            if (request.canBeReleasedBy(currentUserId)) {
                onAssignmentClick?.invoke(request, AssignmentAction.RELEASE)
            }
        }

        holder.itemView.findViewById<View>(R.id.btnComplete).setOnClickListener {
            if (request.canBeCompletedBy(currentUserId)) {
                // ✅ NUEVO: En lugar de completar directamente, abrir diálogo para peso
                onCompleteWithWeight?.invoke(request)
            }
        }

        holder.itemView.findViewById<View>(R.id.btnUpdate).setOnClickListener {
            if (request.canBeUpdatedBy(currentUserId)) {
                onActionClick?.invoke(request, RequestAction.UPDATE)
            }
        }

        holder.itemView.findViewById<View>(R.id.btnMap).setOnClickListener {
            onActionClick?.invoke(request, RequestAction.VIEW_MAP)
        }

        holder.itemView.findViewById<View>(R.id.btnCall).setOnClickListener {
            if (request.isAssignedTo(currentUserId)) {
                onActionClick?.invoke(request, RequestAction.CALL)
            }
        }

        holder.itemView.findViewById<View>(R.id.btnNavigate).setOnClickListener {
            if (request.isAssignedTo(currentUserId) && request.hasValidLocation()) {
                onActionClick?.invoke(request, RequestAction.NAVIGATE)
            }
        }

        // Click en toda la tarjeta
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(request)
        }
    }

    private fun getCombinedStatusInfo(status: String): Triple<String, Int, Int> {
        return when (status) {
            "DISPONIBLE" -> Triple("DISPONIBLE", R.drawable.bg_status_available, R.color.white)
            "ASIGNADA" -> Triple("ASIGNADA", R.drawable.bg_status_pending, R.color.white)
            "EN PROGRESO" -> Triple("EN PROGRESO", R.drawable.bg_status_in_progress, R.color.white)
            "RECOLECTADA" -> Triple("RECOLECTADA", R.drawable.bg_status_collected, R.color.white)
            "CANCELADA" -> Triple("CANCELADA", R.drawable.bg_status_cancelled, R.color.white)
            "EXPIRADA" -> Triple("EXPIRADA", R.drawable.bg_status_expired, R.color.white)
            "PROGRAMADO" -> Triple("PROGRAMADO", R.drawable.bg_status_scheduled, R.color.white)
            else -> Triple(status, R.drawable.bg_status_available, R.color.white)
        }
    }

    private fun getAssignmentInfo(status: String): Triple<String, Int, Int> {
        return when (status) {
            "AVAILABLE" -> Triple("DISPONIBLE", R.drawable.bg_status_available, R.color.white)
            "PENDING" -> Triple("ASIGNADA", R.drawable.bg_status_pending, R.color.white)
            "IN_PROGRESS" -> Triple("EN PROGRESO", R.drawable.bg_status_in_progress, R.color.white)
            "COMPLETED" -> Triple("COMPLETADA", R.drawable.bg_status_collected, R.color.white)
            "CANCELLED" -> Triple("CANCELADA", R.drawable.bg_status_cancelled, R.color.white)
            "EXPIRED" -> Triple("EXPIRADA", R.drawable.bg_status_expired, R.color.white)
            else -> Triple(status, R.drawable.bg_status_available, R.color.white)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString.substring(0, 10)
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateRequest(updatedRequest: CollectionRequest) {
        val position = requests.indexOfFirst { it.id == updatedRequest.id }
        if (position != -1) {
            // ✅ CORREGIDO: Ahora funciona porque requests es MutableList
            requests[position] = updatedRequest
            notifyItemChanged(position)
            println("DEBUG - Adapter actualizado en posición: $position")
        } else {
            println("DEBUG - Solicitud no encontrada para actualizar: ${updatedRequest.id}")
        }
    }

    fun updateRequests(newRequests: List<CollectionRequest>) {
        println("DEBUG - Adapter recibiendo ${newRequests.size} solicitudes")
        // ✅ CORREGIDO: Convertir a MutableList
        requests = newRequests.toMutableList()
        notifyDataSetChanged()
    }

}

enum class RequestAction {
    UPDATE, VIEW_MAP, CALL, NAVIGATE
}

enum class AssignmentAction {
    CLAIM, RELEASE, COMPLETE
}
