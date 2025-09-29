package com.ecocollet.collector.ui.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ecocollet.collector.model.UpdateRequestResponse
import com.ecocollet.collector.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateRequestViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _updateResult = MutableLiveData<UpdateRequestResponse?>()
    val updateResult: LiveData<UpdateRequestResponse?> = _updateResult

    private val apiClient = ApiClient.getInstance(application)

    // Método de actualización con manejo de errores
    fun updateRequest(requestId: Long, weight: Double?, status: String, notes: String? = null) {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        val collectorService = apiClient.getCollectorService()

        collectorService.updateRequest(requestId, weight, status).enqueue(object : Callback<UpdateRequestResponse> {
            override fun onResponse(call: Call<UpdateRequestResponse>, response: Response<UpdateRequestResponse>) {
                _isLoading.value = false

                when {
                    response.isSuccessful -> {
                        val result = response.body()
                        _updateResult.value = result
                        _successMessage.value = result?.message ?: "Solicitud actualizada exitosamente"
                    }
                    response.code() == 400 -> {
                        _errorMessage.value = "Error: Datos inválidos. Verifique la información ingresada."
                    }
                    response.code() == 404 -> {
                        _errorMessage.value = "Error: La solicitud no fue encontrada en el sistema."
                    }
                    response.code() == 500 -> {
                        _errorMessage.value = "Error interno del servidor. Intente nuevamente."
                    }
                    else -> {
                        _errorMessage.value = "Error ${response.code()}: ${response.message()}"
                    }
                }
            }

            override fun onFailure(call: Call<UpdateRequestResponse>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = when {
                    t.message?.contains("timeout", true) == true ->
                        "Error: Tiempo de conexión agotado. Verifique su conexión a internet."
                    t.message?.contains("unable to resolve host", true) == true ->
                        "Error: No se puede conectar al servidor. Verifique su conexión."
                    else ->
                        "Error de conexión: ${t.message ?: "Desconocido"}"
                }
            }
        })
    }

    // ✅ NUEVO: Validar datos antes de enviar
    fun validateUpdateData(weight: Double?, status: String): ValidationResult {
        return when {
            status == "COLLECTED" && weight == null ->
                ValidationResult.Error("Debe ingresar el peso para marcar como recolectado")
            status == "COLLECTED" && (weight ?: 0.0) <= 0 ->
                ValidationResult.Error("El peso debe ser mayor a 0")
            weight != null && weight < 0 ->
                ValidationResult.Error("El peso no puede ser negativo")
            else -> ValidationResult.Success
        }
    }

    // ✅ NUEVO: Limpiar mensajes
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}