package com.ecocollet.collector.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.ecocollet.collector.ui.main.MainActivity
import com.ecocollet.collector.R
import com.ecocollet.collector.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.ACCESS_NETWORK_STATE
                ),
                100
            )
        }

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupUI()
        setupObservers()
        setupListeners()
        setupTextWatchers()
    }

    private fun setupUI() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.root.startAnimation(fadeIn)
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            hideLoading()
            binding.btnLogin.isEnabled = true

            when (result) {
                is LoginResult.Success -> {
                    showSuccess(result.message)
                    navigateToMain()
                }
                is LoginResult.Error -> {
                    showError(result.message)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(username, password)) {
                viewModel.login(username, password)
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etUsername.addTextChangedListener { validateFields() }
        binding.etPassword.addTextChangedListener { validateFields() }
    }

    private fun validateFields() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.btnLogin.isEnabled = username.isNotEmpty() && password.isNotEmpty()
    }

    private fun validateInputs(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "Ingrese su email o usuario"
            binding.etUsername.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Ingrese su contraseña"
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
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

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}