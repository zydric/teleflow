package com.example.teleflow.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.viewmodels.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {
    
    private lateinit var btnBack: ImageButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLogin: TextView
    
    private lateinit var tilFullName: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    
    // ViewModel for authentication
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        initViews(view)
        
        // Ensure action bar is hidden
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        setupClickListeners()
        setupInputValidation()
    }
    
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnRegister = view.findViewById(R.id.btnRegister)
        tvLogin = view.findViewById(R.id.tvLogin)
        
        tilFullName = view.findViewById(R.id.tilFullName)
        etFullName = view.findViewById(R.id.etFullName)
        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etPassword = view.findViewById(R.id.etPassword)
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
    }
    
    private fun setupClickListeners() {
        // Back button click
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Register button click
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegistration()
            }
        }
        
        // Login link click
        tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupInputValidation() {
        // Name validation
        etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isEmpty()) {
                    tilFullName.setError(getString(R.string.name_required))
                } else {
                    tilFullName.setError(null)
                }
            }
        })
        
        // Email validation
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty() && !isValidEmail(s.toString())) {
                    tilEmail.setError(getString(R.string.invalid_email))
                } else {
                    tilEmail.setError(null)
                }
            }
        })
        
        // Password validation
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isNotEmpty() && !isValidPassword(password)) {
                    tilPassword.setError(getString(R.string.password_criteria))
                } else {
                    tilPassword.setError(null)
                }
                
                // Also check confirm password if it's not empty
                val confirmPassword = etConfirmPassword.text.toString()
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    tilConfirmPassword.setError(getString(R.string.passwords_dont_match))
                } else if (confirmPassword.isNotEmpty()) {
                    tilConfirmPassword.setError(null)
                }
            }
        })
        
        // Confirm password validation
        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val confirmPassword = s.toString()
                val password = etPassword.text.toString()
                
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    tilConfirmPassword.setError(getString(R.string.passwords_dont_match))
                } else {
                    tilConfirmPassword.setError(null)
                }
            }
        })
    }
    
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun isValidPassword(password: String): Boolean {
        // Password must be at least 6 characters with at least one number and one special character
        val hasNumber = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return password.length >= 6 && hasNumber && hasSpecialChar
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate name
        val name = etFullName.text.toString().trim()
        if (name.isEmpty()) {
            tilFullName.setError(getString(R.string.name_required))
            isValid = false
        } else {
            tilFullName.setError(null)
        }
        
        // Validate email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.email_required))
            isValid = false
        } else if (!isValidEmail(email)) {
            tilEmail.setError(getString(R.string.invalid_email))
            isValid = false
        } else {
            tilEmail.setError(null)
        }
        
        // Validate password
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.password_required))
            isValid = false
        } else if (!isValidPassword(password)) {
            tilPassword.setError(getString(R.string.password_criteria))
            isValid = false
        } else {
            tilPassword.setError(null)
        }
        
        // Validate confirm password
        val confirmPassword = etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(getString(R.string.password_required))
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.setError(getString(R.string.passwords_dont_match))
            isValid = false
        } else {
            tilConfirmPassword.setError(null)
        }
        
        return isValid
    }
    
    private fun performRegistration() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        
        // Show loading indicator
        btnRegister.isEnabled = false
        btnRegister.text = getString(R.string.registering)
        
        // Use the AuthViewModel to register
        authViewModel.register(email, fullName, password) { result ->
            // Hide loading indicator
            btnRegister.isEnabled = true
            btnRegister.text = getString(R.string.register_button)
            
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            
            if (result.success) {
                // Registration successful, navigate to home screen
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
        }
    }
} 