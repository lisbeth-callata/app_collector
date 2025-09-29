package com.ecocollet.collector.utils

import android.content.Context
import android.content.SharedPreferences

class AuthManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "EcoColletCollectorPrefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_USERNAME = "user_username"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_LASTNAME = "user_lastname"
        private const val KEY_USER_ROLE = "user_role"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveAuthData(
        token: String,
        userId: Long,
        email: String,
        name: String,
        username: String,
        phone: String,
        lastname: String,
        role: String
    ) {
        println("DEBUG - Guardando datos en AuthManager:")
        println("DEBUG - Rol a guardar: $role")
        println("DEBUG - UserId a guardar: $userId")

        with(sharedPreferences.edit()) {
            putString(KEY_TOKEN, token)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_USERNAME, username)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_LASTNAME, lastname)
            putString(KEY_USER_ROLE, role)
            val success = commit()
            println("DEBUG - Datos guardados exitosamente: $success")
        }
    }

    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)
    fun getUserId(): Long = sharedPreferences.getLong(KEY_USER_ID, -1L)
    fun getUserEmail(): String? = sharedPreferences.getString(KEY_USER_EMAIL, null)
    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)
    fun getUsername(): String? = sharedPreferences.getString(KEY_USER_USERNAME, null)
    fun getPhone(): String? = sharedPreferences.getString(KEY_USER_PHONE, null)
    fun getLastname(): String? = sharedPreferences.getString(KEY_USER_LASTNAME, null)
    fun getUserRole(): String? {
        val role = sharedPreferences.getString(KEY_USER_ROLE, null)
        println("DEBUG - Rol leído de SharedPreferences: $role")
        return role
    }

    fun isLoggedIn(): Boolean = getToken() != null

    // CORRECCIÓN CRÍTICA: Función corregida para validar recolectores
    fun isValidCollector(): Boolean {
        val role = getUserRole()
        android.util.Log.d("AuthManager", "Rol del usuario: $role")
        return role == "ROLE_COLLECTOR" || role == "ROLE_ADMIN" ||
                role == "COLLECTOR" || role == "ADMIN"
    }

    fun clearAuthData() {
        with(sharedPreferences.edit()) {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_USERNAME)
            remove(KEY_USER_PHONE)
            remove(KEY_USER_LASTNAME)
            remove(KEY_USER_ROLE)
            apply()
        }
    }

    fun getUserFullName(): String {
        val name = getUserName() ?: ""
        val lastname = getLastname() ?: ""
        return "$name $lastname".trim()
    }
}