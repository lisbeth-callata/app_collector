package com.ecocollet.collector.ui.assignments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ecocollet.collector.model.AssignmentRequest
import com.ecocollet.collector.model.AssignmentResponse
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.network.ApiClient
import com.ecocollet.collector.utils.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val _assignments = MutableLiveData<List<CollectionRequest>>()
    val assignments: LiveData<List<CollectionRequest>> = _assignments

    private val _assignmentResult = MutableLiveData<AssignmentResponse?>()
    val assignmentResult: LiveData<AssignmentResponse?> = _assignmentResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val apiClient = ApiClient.getInstance(application)
    private val authManager = AuthManager(application)

    fun claimRequest(requestId: Long, collectorName: String, timeoutMinutes: Int = 15) {
        _isLoading.value = true

        val collectorId = authManager.getUserId()
        if (collectorId == -1L) {
            _errorMessage.value = "Usuario no identificado"
            _isLoading.value = false
            return
        }

        val assignmentRequest = AssignmentRequest(collectorId, collectorName, timeoutMinutes)
        val assignmentService = apiClient.getAssignmentService()

        assignmentService.claimRequest(requestId, assignmentRequest).enqueue(object : Callback<AssignmentResponse> {
            override fun onResponse(call: Call<AssignmentResponse>, response: Response<AssignmentResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _assignmentResult.value = response.body()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "La solicitud ya ha sido completada por otro recolector"
                        404 -> "Solicitud no encontrada"
                        else -> "Error al reclamar solicitud: ${response.code()}"
                    }
                    _errorMessage.value = errorMsg
                }
            }

            override fun onFailure(call: Call<AssignmentResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }

    fun releaseRequest(requestId: Long) {
        _isLoading.value = true
        val assignmentService = apiClient.getAssignmentService()

        assignmentService.releaseRequest(requestId).enqueue(object : Callback<AssignmentResponse> {
            override fun onResponse(call: Call<AssignmentResponse>, response: Response<AssignmentResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _assignmentResult.value = response.body()
                } else {
                    _errorMessage.value = "Error al liberar solicitud: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<AssignmentResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }

    fun completeRequest(requestId: Long) {
        _isLoading.value = true
        val assignmentService = apiClient.getAssignmentService()

        assignmentService.completeRequest(requestId).enqueue(object : Callback<AssignmentResponse> {
            override fun onResponse(call: Call<AssignmentResponse>, response: Response<AssignmentResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _assignmentResult.value = response.body()
                } else {
                    _errorMessage.value = "Error al completar solicitud: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<AssignmentResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }

    fun loadCollectorAssignments() {
        _isLoading.value = true
        val collectorId = authManager.getUserId()

        if (collectorId == -1L) {
            _errorMessage.value = "Usuario no identificado"
            _isLoading.value = false
            return
        }

        val assignmentService = apiClient.getAssignmentService()
        assignmentService.getCollectorAssignments(collectorId).enqueue(object : Callback<List<CollectionRequest>> {
            override fun onResponse(call: Call<List<CollectionRequest>>, response: Response<List<CollectionRequest>>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _assignments.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Error al cargar asignaciones: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<CollectionRequest>>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }
}