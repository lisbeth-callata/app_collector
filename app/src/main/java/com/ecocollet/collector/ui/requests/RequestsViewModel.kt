package com.ecocollet.collector.ui.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ecocollet.collector.model.AssignmentResponse
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.model.UpdateRequestResponse
import com.ecocollet.collector.network.ApiClient
import com.ecocollet.collector.utils.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class RequestsViewModel(application: Application) : AndroidViewModel(application) {

    private val _allRequests = MutableLiveData<List<CollectionRequest>>()
    private val _filteredRequests = MutableLiveData<List<CollectionRequest>>()
    val requests: LiveData<List<CollectionRequest>> = _filteredRequests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val apiClient = ApiClient.getInstance(application)
    private val authManager = AuthManager(application)

    // ✅ SIMPLIFICADO: Solo 3 tipos de filtro
    enum class FilterType {
        ALL, MY_ASSIGNMENTS, COLLECTED
    }

    private var currentFilter: FilterType = FilterType.ALL

    fun loadRequests(filterType: FilterType = FilterType.ALL) {
        _isLoading.value = true
        currentFilter = filterType

        val collectorService = apiClient.getCollectorService()
        collectorService.getTodayRequests().enqueue(object : Callback<List<CollectionRequest>> {
            override fun onResponse(
                call: Call<List<CollectionRequest>>,
                response: Response<List<CollectionRequest>>
            ) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val allRequests = response.body() ?: emptyList()
                    _allRequests.value = allRequests
                    applyFilter(filterType)
                    _successMessage.value = when (filterType) {
                        FilterType.ALL -> "Todas las solicitudes cargadas"
                        FilterType.MY_ASSIGNMENTS -> "Mis asignaciones cargadas"
                        FilterType.COLLECTED -> "Solicitudes recolectadas cargadas"
                    }
                } else {
                    _errorMessage.value = "Error ${response.code()}: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<List<CollectionRequest>>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }

    // Aplicar filtros simplificados
    private fun applyFilter(filterType: FilterType) {
        val allRequests = _allRequests.value ?: emptyList()
        val currentUserId = authManager.getUserId()

        val filtered = when (filterType) {
            FilterType.ALL -> {
                // Priorizar PENDING + AVAILABLE, luego completadas
                allRequests.sortedWith(compareBy(
                    { it.status != "PENDING" || it.assignmentStatus != "AVAILABLE" }, // PENDING+AVAILABLE primero
                    { it.status == "COLLECTED" }, // COLLECTED después
                    { it.assignmentStatus == "COMPLETED" } // COMPLETED al final
                ))
            }
            FilterType.MY_ASSIGNMENTS -> {
                // ✅ CORREGIDO: Solo solicitudes asignadas al usuario actual con estado PENDING en asignación
                allRequests.filter { request ->
                    request.isAssignedTo(currentUserId) &&
                            (request.assignmentStatus == "PENDING" || request.assignmentStatus == "IN_PROGRESS") &&
                            request.status == "PENDING" // ✅ AÑADIDO: Solo si el estado del pedido es PENDING
                }
            }
            FilterType.COLLECTED -> {
                // Solicitudes completadas o recolectadas
                allRequests.filter { request ->
                    request.status == "COLLECTED" || request.assignmentStatus == "COMPLETED"
                }.sortedByDescending { it.updatedAt } // Más recientes primero
            }
        }

        _filteredRequests.value = filtered
    }

    //  Cargar con filtro específico
    fun loadAllRequests() {
        loadRequests(FilterType.ALL)
    }

    fun loadMyAssignments() {
        loadRequests(FilterType.MY_ASSIGNMENTS)
    }

    fun loadCollectedRequests() {
        loadRequests(FilterType.COLLECTED)
    }

    // Actualización rápida con manejo de errores mejorado
    fun quickUpdateRequest(requestId: Long, weight: Double?, status: String, notes: String? = null) {
        _isLoading.value = true

        val collectorService = apiClient.getCollectorService()

        collectorService.updateRequest(requestId, weight, status).enqueue(object : Callback<UpdateRequestResponse> {
            override fun onResponse(call: Call<UpdateRequestResponse>, response: Response<UpdateRequestResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _successMessage.value = "Solicitud actualizada correctamente"
                    // ✅ FORZAR RECARGA COMPLETA para actualizar la UI
                    loadRequests(currentFilter)
                } else {
                    _errorMessage.value = when (response.code()) {
                        400 -> "Datos inválidos en la solicitud"
                        404 -> "Solicitud no encontrada"
                        500 -> "Error interno del servidor"
                        else -> "Error ${response.code()}: ${response.message()}"
                    }
                }
            }

            override fun onFailure(call: Call<UpdateRequestResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message ?: "Desconocido"}"
            }
        })
    }

    //  Completar solicitud con validación
    fun completeRequestWithWeight(requestId: Long, weight: Double, collectorId: Long) {
        _isLoading.value = true

        // Primero actualizar el estado de asignación a COMPLETED
        val assignmentService = apiClient.getAssignmentService()
        assignmentService.completeRequest(requestId).enqueue(object : Callback<AssignmentResponse> {
            override fun onResponse(call: Call<AssignmentResponse>, response: Response<AssignmentResponse>) {
                if (response.isSuccessful) {
                    // Luego actualizar el peso y estado del pedido a COLLECTED
                    quickUpdateRequest(requestId, weight, "COLLECTED")
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Error al completar la asignación: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<AssignmentResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }

    // Buscar solicitudes
    fun searchRequests(searchTerm: String) {
        if (searchTerm.length < 2) {
            _errorMessage.value = "Ingrese al menos 2 caracteres para buscar"
            return
        }

        _isLoading.value = true
        val collectorService = apiClient.getCollectorService()

        collectorService.searchRequests(searchTerm).enqueue(object : Callback<List<CollectionRequest>> {
            override fun onResponse(
                call: Call<List<CollectionRequest>>,
                response: Response<List<CollectionRequest>>
            ) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val searchResults = response.body() ?: emptyList()
                    _allRequests.value = searchResults

                    // Aplicar filtro actual a los resultados de búsqueda
                    val currentUserId = authManager.getUserId()
                    val filteredResults = when (currentFilter) {
                        FilterType.ALL -> searchResults
                        FilterType.MY_ASSIGNMENTS -> searchResults.filter { it.isAssignedTo(currentUserId) && it.assignmentStatus == "PENDING" }
                        FilterType.COLLECTED -> searchResults.filter { it.status == "COLLECTED" || it.assignmentStatus == "COMPLETED" }
                    }

                    _filteredRequests.value = filteredResults
                    _successMessage.value = "${filteredResults.size} resultados encontrados"
                } else {
                    _errorMessage.value = "Error en la búsqueda: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<CollectionRequest>>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión en la búsqueda: ${t.message}"
            }
        })
    }

    // Obtener estadísticas
    fun getRequestStats(): Map<String, Int> {
        val requests = _allRequests.value ?: emptyList()
        val currentUserId = authManager.getUserId()

        return mapOf(
            "total" to requests.size,
            "myAssignments" to requests.count { it.isAssignedTo(currentUserId) && it.assignmentStatus == "PENDING" },
            "collected" to requests.count { it.status == "COLLECTED" || it.assignmentStatus == "COMPLETED" },
            "available" to requests.count { it.status == "PENDING" && it.assignmentStatus == "AVAILABLE" }
        )
    }

    fun getCurrentFilter(): FilterType {
        return currentFilter
    }
}