package com.ecocollet.collector.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityMainCollectorBinding
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.ui.login.LoginActivity
import com.ecocollet.collector.ui.map.RouteMapActivity
import com.ecocollet.collector.ui.profile.ProfileActivity
import com.ecocollet.collector.ui.requests.RequestsActivity
import com.ecocollet.collector.utils.AuthManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainCollectorBinding
    private lateinit var authManager: AuthManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainCollectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupToolbar()
        setupUI()
        setupObservers()
        setupListeners()
        loadDashboardData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_dashboard)
    }

    private fun setupUI() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)

        binding.tvWelcome.startAnimation(fadeIn)
        binding.cardStats.startAnimation(slideUp)
    }

    private fun setupObservers() {
        viewModel.dashboardData.observe(this) { stats ->
            updateStatsUI(stats)
        }

        viewModel.nextRequest.observe(this) { nextRequest ->
            updateNextRequestUI(nextRequest)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
            }
        }
    }

    private fun setupListeners() {
        binding.cardAllRequests.setOnClickListener {
            navigateToRequests()
        }

        binding.cardRouteMap.setOnClickListener {
            navigateToRouteMap()
        }

        binding.cardQuickRegister.setOnClickListener {
            showComingSoon("Registro rápido")
        }

        binding.cardPerformance.setOnClickListener {
            showComingSoon("Estadísticas de rendimiento")
        }

        binding.btnNavigateToNext.setOnClickListener {
            val nextRequest = viewModel.nextRequest.value
            nextRequest?.let { request ->
                navigateToRequest(request)
            }
        }
    }

    private fun loadDashboardData() {
        viewModel.loadDashboard()
    }

    private fun updateStatsUI(stats: DashboardStats) {
        binding.tvTotalRequests.text = stats.totalRequests.toString()
        binding.tvPendingRequests.text = stats.pendingRequests.toString()
        binding.tvCollectedRequests.text = stats.collectedRequests.toString()

        val welcomeText = "Hola, ${authManager.getUserFullName()}!"
        val daySummary = "Hoy: ${stats.totalRequests} solicitudes • ${stats.pendingRequests} pendientes"

        binding.tvWelcome.text = welcomeText
        binding.tvDaySummary.text = daySummary
    }

    private fun updateNextRequestUI(nextRequest: CollectionRequest?) {
        if (nextRequest != null) {
            binding.cardNextRequest.visibility = View.VISIBLE
            binding.tvNextAddress.text = nextRequest.address ?: "Dirección no disponible"
            binding.tvNextMaterial.text = nextRequest.material
            binding.tvNextCode.text = nextRequest.code

            // Mostrar estado de asignación (CORREGIDO)
            val assignmentStatus = nextRequest.assignmentStatus ?: "AVAILABLE"
            binding.tvNextAssignmentStatus.text = when (assignmentStatus) {
                "AVAILABLE" -> "Disponible"
                "PENDING" -> "Asignada"
                "IN_PROGRESS" -> "En progreso"
                "COMPLETED" -> "Completada"
                "EXPIRED" -> "Expirada"
                "CANCELLED" -> "Cancelada"
                else -> assignmentStatus
            }
        } else {
            binding.cardNextRequest.visibility = View.GONE
        }
    }

    private fun navigateToRequests() {
        startActivity(Intent(this, RequestsActivity::class.java))
    }

    private fun navigateToRouteMap() {
        startActivity(Intent(this, RouteMapActivity::class.java))
    }

    private fun navigateToRequest(request: CollectionRequest) {
        request.latitude?.let { lat ->
            request.longitude?.let { lng ->
                val intent = Intent(this, RouteMapActivity::class.java).apply {
                    putExtra("ZOOM_TO_LAT", lat)
                    putExtra("ZOOM_TO_LNG", lng)
                    putExtra("ZOOM_TO_REQUEST", request)
                }
                startActivity(intent)
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.tvError.postDelayed({
            binding.tvError.visibility = View.GONE
        }, 5000)
    }

    private fun showComingSoon(feature: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Próximamente")
            .setMessage("$feature estará disponible en la próxima actualización")
            .setPositiveButton("Entendido", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_collector, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                navigateToProfile()
                true
            }
            R.id.action_refresh -> {
                loadDashboardData()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> logout() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        authManager.clearAuthData()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}