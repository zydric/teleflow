package com.example.teleflow.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView

class LoginFragment : Fragment() {
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
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
        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)
        tvSignUp = view.findViewById(R.id.tvSignUp)
    }
    
    private fun setupClickListeners() {
        // Login button click
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
        
        // Forgot password click
        tvForgotPassword.setOnClickListener {
            // TODO: Navigate to forgot password screen
            Toast.makeText(context, "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Sign up click
        tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
    
    private fun setupInputValidation() {
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
        
        // Password field watcher - only clear errors
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Only clear any errors
                tilPassword.setError(null)
            }
        })
    }
    
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
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
        
        // Only check if password is empty
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.password_required))
            isValid = false
        } else {
            tilPassword.setError(null)
        }
        
        return isValid
    }
    
    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        
        // TODO: Implement actual authentication logic with backend
        
        // For now, let's simulate a successful login
        // In a real app, this would check against a database or web service
        if (email == "user@example.com" && password == "password123") {
            // Navigate to home screen upon successful login
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        } else {
            Toast.makeText(
                context, 
                "Invalid credentials. Try user@example.com / password123", 
                Toast.LENGTH_LONG
            ).show()
        }
    }
} 