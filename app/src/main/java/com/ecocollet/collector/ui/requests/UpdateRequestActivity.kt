package com.ecocollet.collector.ui.requests

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityUpdateRequestBinding
import com.ecocollet.collector.utils.AuthManager
import com.ecocollet.collector.ui.assignments.AssignmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateRequestBinding
    private lateinit var viewModel: UpdateRequestViewModel
    private lateinit var assignmentViewModel: AssignmentViewModel // ✅ AÑADIDO
    private lateinit var authManager: AuthManager // ✅ AÑADIDO
    private lateinit var currentRequest: CollectionRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UpdateRequestViewModel::class.java]
        assignmentViewModel = ViewModelProvider(this)[AssignmentViewModel::class.java] // ✅ AÑADIDO
        authManager = AuthManager(this) // ✅ AÑADIDO

        setupToolbar()
        getRequestData()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun getRequestData() {
        currentRequest = intent.getParcelableExtra("REQUEST") ?: run {
            Toast.makeText(this, "Error: Solicitud no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        populateRequestData()
    }

    private fun populateRequestData() {
        with(binding) {
            tvRequestCode.text = currentRequest.code
            tvUserName.text = currentRequest.getSafeUserName()
            tvMaterial.text = currentRequest.material
            tvAddress.text = currentRequest.address ?: "No especificada"

            // Formatear fecha
            val dateText = try {
                currentRequest.createdAt.substring(0, 10) // YYYY-MM-DD
            } catch (e: Exception) {
                currentRequest.createdAt
            }
            tvCreatedAt.text = dateText

            // Prellenar peso si existe
            currentRequest.weight?.let {
                etWeight.setText(it.toString())
            }

            // Seleccionar estado actual con manejo de null
            val currentStatus = currentRequest.status ?: "PENDING"
            when (currentStatus.uppercase()) {
                "PENDING" -> rgStatus.check(R.id.rbPending)
                "COLLECTED" -> rgStatus.check(R.id.rbCollected)
                "SCHEDULED" -> rgStatus.check(R.id.rbScheduled)
                "CANCELLED" -> rgStatus.check(R.id.rbCancelled)
                else -> rgStatus.check(R.id.rbPending)
            }
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnUpdate.isEnabled = !isLoading
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ AÑADIDO: Observar el resultado de liberar la solicitud
        assignmentViewModel.assignmentResult.observe(this) { result ->
            result?.let {
                // Cuando se libera la asignación, proceder a actualizar el estado a CANCELLED
                viewModel.updateRequest(currentRequest.id, null, "CANCELLED", "Solicitud cancelada")
            }
        }

        assignmentViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Error al liberar asignación: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnUpdate.setOnClickListener {
            updateRequest()
        }
    }

    private fun updateRequest() {
        val weightText = binding.etWeight.text.toString()
        val weight = weightText.toDoubleOrNull()
        val status = getSelectedStatus()
        val notes = binding.etNotes.text.toString()

        // Validaciones de estados
        when {
            status == "COLLECTED" && weight == null -> {
                Toast.makeText(this, "❌ Ingrese el peso para marcar como recolectado", Toast.LENGTH_SHORT).show()
                return
            }
            status == "CANCELLED" -> {
                // Confirmar cancelación y actualizar ambos estados
                MaterialAlertDialogBuilder(this)
                    .setTitle("Confirmar Cancelación")
                    .setMessage("¿Está seguro de cancelar esta solicitud?")
                    .setPositiveButton("Sí, Cancelar") { _, _ ->
                        handleCancellation(weight, notes)
                    }
                    .setNegativeButton("No", null)
                    .show()
                return
            }
            status == "PENDING" && currentRequest.assignmentStatus == "COMPLETED" -> {
                Toast.makeText(this, "❌ No puede volver a PENDING una solicitud completada", Toast.LENGTH_SHORT).show()
                return
            }
            status == "SCHEDULED" && currentRequest.assignmentStatus == "COMPLETED" -> {
                Toast.makeText(this, "❌ No puede programar una solicitud completada", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (status == "COLLECTED") {
            // CONFIRMACIÓN PARA ESTADO "RECOLECTADO"
            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar Recolección")
                .setMessage("¿Confirmar que ha recolectado ${weight} kg de ${currentRequest.material}?")
                .setPositiveButton("Sí, Confirmar") { _, _ ->
                    viewModel.updateRequest(currentRequest.id, weight, status, notes)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            viewModel.updateRequest(currentRequest.id, weight, status, notes)
        }
    }

    private fun handleCancellation(weight: Double?, notes: String?) {
        // Si está asignada al usuario actual, liberarla primero
        if (currentRequest.isAssignedTo(authManager.getUserId()) &&
            (currentRequest.assignmentStatus == "PENDING" || currentRequest.assignmentStatus == "IN_PROGRESS")) {
            assignmentViewModel.releaseRequest(currentRequest.id)
        } else {
            // Si no está asignada o ya está completada, simplemente cancelar
            viewModel.updateRequest(currentRequest.id, weight, "CANCELLED", notes)
        }
    }

    private fun getSelectedStatus(): String {
        return when (binding.rgStatus.checkedRadioButtonId) {
            R.id.rbPending -> "PENDING"
            R.id.rbScheduled -> "SCHEDULED"
            R.id.rbCollected -> "COLLECTED"
            R.id.rbCancelled -> "CANCELLED"
            else -> currentRequest.status
        }
    }
}