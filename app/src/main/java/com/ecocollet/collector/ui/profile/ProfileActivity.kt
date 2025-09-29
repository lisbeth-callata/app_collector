package com.ecocollet.collector.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ecocollet.collector.model.ProfileData
import com.ecocollet.collector.utils.AuthManager
import com.ecocollet.collector.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        setupToolbar()
        setupObservers()
        loadProfileData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.profileData.observe(this) { profile ->
            profile?.let {
                updateUI(it)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                showLocalData()
            }
        }
    }

    private fun updateUI(profile: ProfileData) {
        with(binding) {
            // Información personal
            tvUserName.text = "${profile.name} ${profile.lastname}".trim()
            tvUserEmail.text = profile.email
            tvUserPhone.text = profile.phone
            tvUserRole.text = when (profile.role) {
                "ROLE_COLLECTOR" -> "Recolector Municipal"
                "ROLE_ADMIN" -> "Administrador"
                else -> profile.role
            }

            // Estadísticas
            tvTotalRequests.text = profile.totalRequests.toString()
            tvTotalWeight.text = String.format("%.1f kg", profile.totalWeight)

            // Impacto ambiental
            val impact = calculateEnvironmentalImpact(profile.totalWeight)
            tvEnvironmentalImpact.text = impact

            // Ocultar loading
            progressBar.visibility = android.view.View.GONE
        }
    }

    private fun showLocalData() {
        with(binding) {
            tvUserName.text = authManager.getUserFullName()
            tvUserEmail.text = authManager.getUserEmail() ?: "No disponible"
            tvUserPhone.text = authManager.getPhone() ?: "No disponible"

            val role = when (authManager.getUserRole()) {
                "ROLE_COLLECTOR" -> "Recolector Municipal"
                "ROLE_ADMIN" -> "Administrador"
                else -> authManager.getUserRole() ?: "No disponible"
            }
            tvUserRole.text = role

            progressBar.visibility = android.view.View.GONE
        }
    }

    private fun calculateEnvironmentalImpact(weight: Double): String {
        val co2Reduced = weight * 1.8 // kg de CO2 reducido por kg reciclado
        val waterSaved = weight * 100 // litros de agua ahorrados
        val treesSaved = weight * 0.02 // árboles salvados

        return "Has contribuido a:\n" +
                "• Reducir ${"%.1f".format(co2Reduced)} kg de CO₂\n" +
                "• Ahorrar ${"%.0f".format(waterSaved)}L de agua\n" +
                "• Salvar ${"%.1f".format(treesSaved)} árboles"
    }

    private fun loadProfileData() {
        val userId = authManager.getUserId()
        if (userId != -1L) {
            viewModel.loadProfile(userId)
        } else {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            showLocalData()
        }
    }
}