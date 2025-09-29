package com.ecocollet.collector.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ecocollet.collector.model.AuthResponse
import com.ecocollet.collector.model.LoginRequest
import com.ecocollet.collector.network.ApiClient
import com.ecocollet.collector.utils.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application.applicationContext)
    private val apiClient = ApiClient.getInstance(application)

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(username: String, password: String) {
        _isLoading.value = true

        val loginRequest = LoginRequest(username, password)
        val authService = apiClient.getAuthService()

        authService.login(loginRequest).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                _isLoading.value = false

                when {
                    response.isSuccessful -> {
                        val authResponse = response.body()
                        if (authResponse != null) {
                            // ✅ CORRECCIÓN: Llamar directamente a saveAuthData
                            saveAuthData(authResponse)
                        } else {
                            _loginResult.value = LoginResult.Error("Error en la respuesta del servidor")
                        }
                    }
                    response.code() == 401 -> {
                        _loginResult.value = LoginResult.Error("Credenciales incorrectas")
                    }
                    else -> {
                        _loginResult.value = LoginResult.Error("Error del servidor: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                _isLoading.value = false
                _loginResult.value = LoginResult.Error("Error de conexión: ${t.message}")
            }
        })
    }

    private fun saveAuthData(authResponse: AuthResponse) {
        // Validación DIRECTA sin depender de AuthManager
        val isValidRole = authResponse.role.equals("ROLE_COLLECTOR", ignoreCase = true) ||
                authResponse.role.equals("ROLE_ADMIN", ignoreCase = true) ||
                authResponse.role.equals("COLLECTOR", ignoreCase = true) ||
                authResponse.role.equals("ADMIN", ignoreCase = true)

        println("DEBUG - Rol validado: $isValidRole - ${authResponse.role}")

        if (isValidRole) {
            // Guardar datos
            authManager.saveAuthData(
                token = authResponse.token,
                userId = authResponse.userId,
                email = authResponse.email,
                name = authResponse.name,
                username = authResponse.username,
                phone = authResponse.phone,
                lastname = authResponse.lastname,
                role = authResponse.role
            )

            // ✅ ÉXITO inmediato - no validar de nuevo
            _loginResult.value = LoginResult.Success("Login exitoso")
        } else {
            _loginResult.value = LoginResult.Error("Acceso solo para recolectores y administradores")
        }
    }
}

sealed class LoginResult {
    data class Success(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}