package com.example.teleflow.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        setupInputValidation()
    }
    
    private fun setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
        
        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Navigate to forgot password screen
            Toast.makeText(context, "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Sign up click
        binding.tvSignUp.setOnClickListener {
            // TODO: Navigate to registration screen
            Toast.makeText(context, "Register functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupInputValidation() {
        // Email validation
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty() && !isValidEmail(s.toString())) {
                    binding.tilEmail.error = getString(R.string.invalid_email)
                } else {
                    binding.tilEmail.error = null
                }
            }
        })
        
        // Password validation
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty() && s.toString().length < 6) {
                    binding.tilPassword.error = getString(R.string.password_too_short)
                } else {
                    binding.tilPassword.error = null
                }
            }
        })
    }
    
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate email
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }
        
        // Validate password
        val password = binding.etPassword.text.toString()
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        return isValid
    }
    
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 