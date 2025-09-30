package com.ecocollet.collector.ui.requests

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocollet.collector.R
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.utils.AuthManager
import com.ecocollet.collector.databinding.ActivityRequestsBinding
import com.ecocollet.collector.ui.assignments.AssignmentViewModel
import com.ecocollet.collector.ui.map.RouteMapActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import android.text.TextWatcher
import android.widget.EditText

class RequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestsBinding
    private lateinit var viewModel: RequestsViewModel
    private lateinit var assignmentViewModel: AssignmentViewModel
    private lateinit var authManager: AuthManager
    private lateinit var adapter: RequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        viewModel = ViewModelProvider(this)[RequestsViewModel::class.java]
        assignmentViewModel = ViewModelProvider(this)[AssignmentViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupSearch()

        loadRequests()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        updateToolbarTitle()
    }

    private fun updateToolbarTitle() {
        val title = when (viewModel.getCurrentFilter()) {
            RequestsViewModel.FilterType.ALL -> "Todas las Solicitudes"
            RequestsViewModel.FilterType.MY_ASSIGNMENTS -> "Mis Asignaciones"
            RequestsViewModel.FilterType.COLLECTED -> "Solicitudes Recolectadas"
        }
        binding.toolbar.title = title
    }

    private fun setupRecyclerView() {
        adapter = RequestAdapter()
        adapter.setAuthManager(authManager)

        adapter.onItemClick = { request ->
            openRequestDetail(request)
        }

        adapter.onActionClick = { request, action ->
            handleRequestAction(request, action)
        }

        adapter.onAssignmentClick = { request, action ->
            handleAssignmentAction(request, action)
        }

        // ✅ NUEVO: Manejar completar con peso
        adapter.onCompleteWithWeight = { request ->
            showCompleteWithWeightDialog(request)
        }

        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = adapter
    }

    private fun showCompleteWithWeightDialog(request: CollectionRequest) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_with_weight, null)
        val etWeight = dialogView.findViewById<EditText>(R.id.etWeight)

        MaterialAlertDialogBuilder(this)
            .setTitle("Completar Recolección")
            .setMessage("Ingrese el peso recolectado para ${request.material}")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val weightText = etWeight.text.toString()
                val weight = weightText.toDoubleOrNull()

                if (weight == null || weight <= 0) {
                    Toast.makeText(this, "Ingrese un peso válido mayor a 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Completar la solicitud con el peso ingresado
                viewModel.completeRequestWithWeight(request.id, weight, authManager.getUserId())
                Toast.makeText(this, "Completando solicitud...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchTerm = s?.toString()?.trim()
                if (!searchTerm.isNullOrEmpty() && searchTerm.length >= 2) {
                    viewModel.searchRequests(searchTerm)
                } else if (searchTerm.isNullOrEmpty()) {
                    // Si se limpia la búsqueda, recargar datos normales
                    loadRequests()
                }
            }
        })

        binding.btnSearch.setOnClickListener {
            val searchTerm = binding.etSearch.text.toString().trim()
            if (searchTerm.isNotEmpty()) {
                viewModel.searchRequests(searchTerm)
            }
        }
    }

    private fun handleAssignmentAction(request: CollectionRequest, action: AssignmentAction) {
        when (action) {
            AssignmentAction.CLAIM -> {
                if (request.canBeClaimedBy(authManager.getUserId())) {
                    val collectorName = authManager.getUserFullName()
                    if (collectorName.isNotEmpty()) {
                        assignmentViewModel.claimRequest(request.id, collectorName)
                        Toast.makeText(this, "Reclamando solicitud...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No se pudo obtener el nombre del recolector", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No puede reclamar esta solicitud", Toast.LENGTH_SHORT).show()
                }
            }
            AssignmentAction.RELEASE -> {
                if (request.canBeReleasedBy(authManager.getUserId())) {
                    assignmentViewModel.releaseRequest(request.id)
                } else {
                    Toast.makeText(this, "No puede liberar esta solicitud", Toast.LENGTH_SHORT).show()
                }
            }
            AssignmentAction.COMPLETE -> {
                // Este caso ahora se maneja con onCompleteWithWeight
            }
        }
    }

    private fun setupObservers() {
        viewModel.requests.observe(this) { requests ->
            println("DEBUG - Activity observando ${requests.size} solicitudes")
            adapter.updateRequests(requests)
            updateUI(requests)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            println("DEBUG - Loading: $isLoading")
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                println("DEBUG - Error: $it")
                showError(it)
            }
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let {
                println("DEBUG - Success: $it")
                showSuccess(it)
            }
        }
        assignmentViewModel.assignmentResult.observe(this) { result ->
            result?.let {
                println("DEBUG - Asignación completada: ${result.message}")
                showSuccess(result.message)
                viewModel.loadRequests(viewModel.getCurrentFilter())
                assignmentViewModel.clearAssignmentResult()
            }
        }

        assignmentViewModel.errorMessage.observe(this) { error ->
            error?.let {
                println("DEBUG - Error en asignación: $it")
                showError(it)
                viewModel.loadRequests(viewModel.getCurrentFilter())
                assignmentViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUI(requests: List<CollectionRequest>) {
        binding.swipeRefreshLayout.isRefreshing = false

        if (requests.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            binding.tvResultsCount.text = "${requests.size} solicitudes encontradas"
        }
    }

    private fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.rvRequests.visibility = View.GONE

        // ✅ NUEVO: Mensaje específico por filtro
        val message = when (viewModel.getCurrentFilter()) {
            RequestsViewModel.FilterType.ALL -> "No hay solicitudes para mostrar"
            RequestsViewModel.FilterType.MY_ASSIGNMENTS -> "No tienes asignaciones activas"
            RequestsViewModel.FilterType.COLLECTED -> "No hay solicitudes recolectadas"
        }
        binding.tvEmptyMessage.text = message
    }

    private fun hideEmptyState() {
        binding.layoutEmpty.visibility = View.GONE
        binding.rvRequests.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadRequests()
        }

        // ✅ SIMPLIFICADO: Solo 3 filtros
        binding.btnFilterAll.setOnClickListener {
            viewModel.loadAllRequests()
        }

        binding.btnFilterMyAssignments.setOnClickListener {
            viewModel.loadMyAssignments()
        }

        binding.btnFilterCollected.setOnClickListener {
            viewModel.loadCollectedRequests()
        }

        binding.btnRetry.setOnClickListener {
            loadRequests()
        }
    }

    private fun loadRequests() {
        viewModel.loadAllRequests()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, theme))
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.green_primary, theme))
            .show()
    }

    private fun openRequestDetail(request: CollectionRequest) {
        val intent = Intent(this, RequestDetailActivity::class.java)
        intent.putExtra("REQUEST", request)
        startActivity(intent)
    }

    private fun handleRequestAction(request: CollectionRequest, action: RequestAction) {
        when (action) {
            RequestAction.UPDATE -> openUpdateRequest(request)
            RequestAction.VIEW_MAP -> openMap(request)
            RequestAction.CALL -> callUser(request)
            RequestAction.NAVIGATE -> navigateTo(request)
        }
    }

    private fun openUpdateRequest(request: CollectionRequest) {
        if (request.canBeUpdatedBy(authManager.getUserId())) {
            val intent = Intent(this, UpdateRequestActivity::class.java)
            intent.putExtra("REQUEST", request)
            startActivity(intent)
        } else {
            Toast.makeText(this, "No puede actualizar esta solicitud", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMap(request: CollectionRequest) {
        request.latitude?.let { lat ->
            request.longitude?.let { lng ->
                val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(request.address)})"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No hay aplicación de mapas instalada", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun callUser(request: CollectionRequest) {
        if (request.isAssignedTo(authManager.getUserId())) {
            request.userPhone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No hay aplicación de teléfono instalada", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Usuario no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Solo puede llamar a solicitudes asignadas a usted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateTo(request: CollectionRequest) {
        if (request.isAssignedTo(authManager.getUserId())) {
            request.latitude?.let { lat ->
                request.longitude?.let { lng ->
                    val intent = Intent(this, RouteMapActivity::class.java).apply {
                        putExtra("ZOOM_TO_LAT", lat)
                        putExtra("ZOOM_TO_LNG", lng)
                        putExtra("ZOOM_TO_REQUEST", request)
                    }
                    startActivity(intent)
                }
            } ?: run {
                Toast.makeText(this, "Ubicación no disponible para navegación", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Solo puede navegar a solicitudes asignadas a usted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }
}