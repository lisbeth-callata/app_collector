package com.ecocollet.collector.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CollectionRequest(
    val id: Long,
    val code: String,
    val material: String,
    val description: String? = null,
    val status: String,
    val assignmentStatus: String? = "AVAILABLE",
    val createdAt: String,
    val updatedAt: String? = null,
    val weight: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val userId: Long,

    // Información completa del usuario
    val userName: String? = null,
    val userLastname: String? = null,
    val userEmail: String? = null,
    val userPhone: String? = null,

    // Información de asignación
    val assignedCollectorId: Long? = null,
    val assignedCollectorName: String? = null,
    val assignedAt: String? = null,
    val assignmentExpiresAt: String? = null
) : Parcelable {

    // Validaciones para estados
    fun canBeClaimedBy(collectorId: Long): Boolean {
        return status == "PENDING" &&
                assignmentStatus == "AVAILABLE" &&
                !isAssignedToOtherCollector(collectorId)
    }

    fun canBeReleasedBy(collectorId: Long): Boolean {
        return isAssignedTo(collectorId) &&
                (assignmentStatus == "PENDING" || assignmentStatus == "IN_PROGRESS") &&
                status == "PENDING" // AÑADIDO: Solo si el pedido sigue pendiente
    }

    fun canBeCompletedBy(collectorId: Long): Boolean {
        return isAssignedTo(collectorId) &&
                assignmentStatus == "IN_PROGRESS" &&
                status == "PENDING"
    }

    fun canBeUpdatedBy(collectorId: Long): Boolean {
        return isAssignedTo(collectorId) ||
                assignmentStatus == "COMPLETED" ||
                status == "CANCELLED" // ✅ AÑADIDO: También puede actualizar canceladas
    }
    fun canBeCancelledBy(collectorId: Long): Boolean {
        return (isAssignedTo(collectorId) || assignmentStatus == "AVAILABLE") &&
                status == "PENDING" &&
                assignmentStatus != "COMPLETED" &&
                assignmentStatus != "CANCELLED"
    }

    private fun isAssignedToOtherCollector(collectorId: Long): Boolean {
        return assignedCollectorId != null && assignedCollectorId != collectorId
    }

    // FUNCIONES
    fun getSafeAssignmentStatus(): String {
        return assignmentStatus ?: "AVAILABLE"
    }
    fun getSafeUserName(): String {
        return if (!userName.isNullOrEmpty() && !userLastname.isNullOrEmpty()) {
            "$userName $userLastname"
        } else if (!userName.isNullOrEmpty()) {
            userName
        } else {
            "Usuario #$userId"
        }
    }

    fun getSafeUserPhone(): String {
        return userPhone ?: "No disponible"
    }

    fun getSafeAddress(): String {
        return address ?: "Ubicación no especificada"
    }

    fun getSafeDescription(): String {
        return description ?: "Sin descripción"
    }

    fun getSafeWeight(): String {
        return if (weight != null) "${weight} kg" else "Pendiente"
    }

    //Verificar si tiene ubicación válida
    fun hasValidLocation(): Boolean {
        return latitude != null && longitude != null
    }

    // Obtener coordenadas seguras
    fun getSafeLatitude(): Double {
        return latitude ?: 0.0
    }

    fun getSafeLongitude(): Double {
        return longitude ?: 0.0
    }

    // Verificar si está asignada al recolector actual
    fun isAssignedTo(collectorId: Long): Boolean {
        return assignedCollectorId == collectorId
    }

    // Verificar si está disponible para reclamar
    fun isAvailable(): Boolean {
        return getSafeAssignmentStatus() == "AVAILABLE"
    }

    // Verificar si está en progreso
    fun isInProgress(): Boolean {
        return getSafeAssignmentStatus() == "IN_PROGRESS"
    }

    // Verificar si está completada
    fun isCompleted(): Boolean {
        return getSafeAssignmentStatus() == "COMPLETED" || status == "COLLECTED"
    }
    // Verificar si es recolectada
    fun isCollected(): Boolean {
        return status == "COLLECTED" || assignmentStatus == "COMPLETED"
    }

    // Obtener estado combinado para mostrar
    fun getCombinedStatus(): String {
        return when {
            status == "COLLECTED" || assignmentStatus == "COMPLETED" -> "RECOLECTADA"
            status == "CANCELLED" || assignmentStatus == "CANCELLED" -> "CANCELADA"
            assignmentStatus == "IN_PROGRESS" -> "EN PROGRESO"
            assignmentStatus == "PENDING" -> "ASIGNADA"
            assignmentStatus == "EXPIRED" -> "EXPIRADA"
            assignmentStatus == "AVAILABLE" && status == "PENDING" -> "DISPONIBLE"
            status == "SCHEDULED" -> "PROGRAMADO" // ✅ AÑADIDO
            else -> status
        }
    }
}

@Parcelize
data class AssignmentRequest(
    val collectorId: Long,
    val collectorName: String,
    val timeoutMinutes: Int = 15
) : Parcelable

@Parcelize
data class AssignmentResponse(
    val requestId: Long,
    val requestCode: String,
    val assignedCollectorId: Long?,
    val assignedCollectorName: String?,
    val assignmentStatus: String,
    val assignedAt: String?,
    val expiresAt: String?,
    val message: String
) : Parcelable