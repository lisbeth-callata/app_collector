package com.ecocollet.collector.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ecocollet.collector.model.ProfileData
import com.ecocollet.collector.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _profileData = MutableLiveData<ProfileData>()
    val profileData: LiveData<ProfileData> = _profileData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val apiClient = ApiClient.getInstance(application)

    fun loadProfile(userId: Long) {
        _isLoading.value = true

        val authService = apiClient.getAuthService()

        authService.getMyProfile().enqueue(object : Callback<ProfileData> {
            override fun onResponse(call: Call<ProfileData>, response: Response<ProfileData>) {
                if (response.isSuccessful) {
                    _profileData.value = response.body()
                    _isLoading.value = false
                } else {
                    loadProfileFallback(userId)
                }
            }

            override fun onFailure(call: Call<ProfileData>, t: Throwable) {
                loadProfileFallback(userId)
            }
        })
    }

    private fun loadProfileFallback(userId: Long) {
        val authService = apiClient.getAuthService()
        authService.getProfile(userId).enqueue(object : Callback<ProfileData> {
            override fun onResponse(call: Call<ProfileData>, response: Response<ProfileData>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _profileData.value = response.body()
                } else {
                    _errorMessage.value = "Error al cargar perfil: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<ProfileData>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Error de conexión: ${t.message}"
            }
        })
    }
}