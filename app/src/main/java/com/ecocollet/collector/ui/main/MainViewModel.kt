package com.ecocollet.collector.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.network.ApiClient
import com.ecocollet.collector.utils.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _dashboardData = MutableLiveData<DashboardStats>()
    val dashboardData: LiveData<DashboardStats> = _dashboardData

    private val _nextRequest = MutableLiveData<CollectionRequest?>()
    val nextRequest: LiveData<CollectionRequest?> = _nextRequest

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val apiClient = ApiClient.getInstance(application)
    private val authManager = AuthManager(application)

    fun loadDashboard() {
        _isLoading.value = true

        val collectorService = apiClient.getCollectorService()
        collectorService.getTodayRequests().enqueue(object : Callback<List<CollectionRequest>> {
            override fun onResponse(
                call: Call<List<CollectionRequest>>,
                response: Response<List<CollectionRequest>>
            ) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val requests = response.body() ?: emptyList()
                    _dashboardData.value = calculateStats(requests)
                    _nextRequest.value = findNextRequest(requests)
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

    private fun calculateStats(requests: List<CollectionRequest>): DashboardStats {
        val total = requests.size
        val pending = requests.count { it.status == "PENDING" }
        val collected = requests.count { it.status == "COLLECTED" }

        return DashboardStats(total, pending, collected)
    }

    private fun findNextRequest(requests: List<CollectionRequest>): CollectionRequest? {
        // Encontrar la primera solicitud pendiente
        return requests.firstOrNull { it.status == "PENDING" }
    }

    fun refreshData() {
        loadDashboard()
    }
}

data class DashboardStats(
    val totalRequests: Int,
    val pendingRequests: Int,
    val collectedRequests: Int
)