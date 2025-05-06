package com.example.teleflow.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.teleflow.MainActivity
import com.example.teleflow.R
import com.example.teleflow.viewmodels.AuthViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ChangePasswordFragment : Fragment() {

    private lateinit var currentPasswordInput: TextInputEditText
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    
    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    
    private lateinit var saveButton: Button
    private lateinit var cancelButton: TextView
    private lateinit var saveButtonProgress: ProgressBar

    private lateinit var passwordStrengthText: TextView

    private val authViewModel: AuthViewModel by viewModels()

    // password requirements
    private val MIN_PASSWORD_LENGTH = 6
    private val REQUIRES_SPECIAL_CHAR = true
    private val REQUIRES_NUMBER = true

    private val COLOR_ERROR = R.color.logout_color
    private val COLOR_SUCCESS = R.color.primary_button_color

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "Change Password"

        if (!authViewModel.isLoggedIn()) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        initializeViews(view)
        setupClickListeners()
        setupPasswordValidation()

        setupConsistentHelperTextAppearance()
    }

    private fun initializeViews(view: View) {
        currentPasswordInput = view.findViewById(R.id.current_password_input)
        newPasswordInput = view.findViewById(R.id.new_password_input)
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input)

        currentPasswordLayout = view.findViewById(R.id.current_password_layout)
        newPasswordLayout = view.findViewById(R.id.new_password_layout)
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout)

        saveButton = view.findViewById(R.id.save_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        saveButtonProgress = view.findViewById(R.id.save_button_progress)

        passwordStrengthText = view.findViewById(R.id.password_strength_text)

        view.findViewById<View>(R.id.strength_indicator_1).visibility = View.GONE
        view.findViewById<View>(R.id.strength_indicator_2).visibility = View.GONE
        view.findViewById<View>(R.id.strength_indicator_3).visibility = View.GONE
        view.findViewById<View>(R.id.strength_indicator_4).visibility = View.GONE

        passwordStrengthText.visibility = View.GONE
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }

        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    
    private fun setupPasswordValidation() {
        currentPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentPasswordLayout.error = null
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (passwordStrengthText.visibility != View.VISIBLE && !s.isNullOrEmpty()) {
                    passwordStrengthText.visibility = View.VISIBLE
                }
                
                updatePasswordValidation(s.toString())

                val confirmText = confirmPasswordInput.text.toString()
                if (confirmText.isNotEmpty()) {
                    validatePasswordMatch(s.toString(), confirmText)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        confirmPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newPassword = newPasswordInput.text.toString()
                validatePasswordMatch(newPassword, s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupConsistentHelperTextAppearance() {
        currentPasswordLayout.setHelperTextTextAppearance(R.style.HelperTextAppearance)
        currentPasswordLayout.setErrorTextAppearance(R.style.HelperTextAppearance)
        
        newPasswordLayout.setHelperTextTextAppearance(R.style.HelperTextAppearance)
        newPasswordLayout.setErrorTextAppearance(R.style.HelperTextAppearance)
        
        confirmPasswordLayout.setHelperTextTextAppearance(R.style.HelperTextAppearance)
        confirmPasswordLayout.setErrorTextAppearance(R.style.HelperTextAppearance)
    }
    
    private fun updatePasswordValidation(password: String) {
        if (password.isEmpty()) {
            resetPasswordValidation()
            passwordStrengthText.visibility = View.GONE
            return
        }
        
        val isValid = isPasswordValid(password)
        val missingRequirements = getMissingRequirements(password)

        if (isValid) {
            newPasswordInput.setBackgroundResource(R.drawable.input_valid_background)

            newPasswordLayout.helperText = "Password meets requirements ✓"
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_SUCCESS))

            passwordStrengthText.visibility = View.GONE
        } else {
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)

            newPasswordLayout.helperText = missingRequirements.joinToString("\n")
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))

            passwordStrengthText.visibility = View.GONE
        }
    }
    
    private fun resetPasswordValidation() {
        newPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)

        newPasswordLayout.helperText = "Password must be at least $MIN_PASSWORD_LENGTH characters, include a special character and a number"
        newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), R.color.text_color))

        passwordStrengthText.visibility = View.GONE
    }
    
    private fun getMissingRequirements(password: String): List<String> {
        val missingRequirements = mutableListOf<String>()
        
        if (password.length < MIN_PASSWORD_LENGTH) {
            missingRequirements.add("• Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
        
        if (REQUIRES_SPECIAL_CHAR && !password.any { !it.isLetterOrDigit() }) {
            missingRequirements.add("• Password must include at least one special character")
        }
        
        if (REQUIRES_NUMBER && !password.any { it.isDigit() }) {
            missingRequirements.add("• Password must include at least one number")
        }
        
        return missingRequirements
    }

    private fun isPasswordValid(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return false
        }

        if (REQUIRES_SPECIAL_CHAR && !password.any { !it.isLetterOrDigit() }) {
            return false
        }

        if (REQUIRES_NUMBER && !password.any { it.isDigit() }) {
            return false
        }
        
        return true
    }
    
    private fun validatePasswordMatch(newPassword: String, confirmPassword: String) {
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = null
            return
        }
        
        if (newPassword != confirmPassword) {
            confirmPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            confirmPasswordLayout.error = null
            confirmPasswordLayout.helperText = "Passwords do not match"
            confirmPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
        } else {
            confirmPasswordInput.setBackgroundResource(R.drawable.input_valid_background)
            confirmPasswordLayout.error = null
            confirmPasswordLayout.helperText = "Passwords match ✓"
            confirmPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_SUCCESS))
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        currentPasswordLayout.error = null
        newPasswordLayout.error = null
        confirmPasswordLayout.error = null
        
        newPasswordLayout.helperText = null
        confirmPasswordLayout.helperText = null

        val currentPassword = currentPasswordInput.text.toString()
        val newPassword = newPasswordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        if (currentPassword.isEmpty()) {
            currentPasswordLayout.helperText = "Current password cannot be empty"
            currentPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            currentPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        } else {
            currentPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
            currentPasswordLayout.helperText = null
        }

        if (newPassword.isEmpty()) {
            newPasswordLayout.helperText = "New password cannot be empty"
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        } else if (!isPasswordValid(newPassword)) {
            newPasswordLayout.helperText = getMissingRequirements(newPassword).joinToString("\n")
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            passwordStrengthText.visibility = View.GONE
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.helperText = "Please confirm your new password"
            confirmPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            confirmPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        } else if (newPassword != confirmPassword) {
            confirmPasswordLayout.helperText = "Passwords do not match"
            confirmPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            confirmPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        }
        
        if (isValid && currentPassword == newPassword) {
            newPasswordLayout.helperText = "New password must be different from current password"
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        }

        return isValid
    }

    private fun changePassword() {
        showLoading(true)
        
        val currentPassword = currentPasswordInput.text.toString()
        val newPassword = newPasswordInput.text.toString()
        
        authViewModel.changePassword(currentPassword, newPassword) { result ->
            showLoading(false)
            
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            
            if (result.success) {
                resetForm()
                
                (requireActivity() as MainActivity).refreshNavigationDrawerUserData()
                
                findNavController().popBackStack()
            } else {
                currentPasswordLayout.helperText = result.message
                currentPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
                currentPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        saveButton.isEnabled = !isLoading
        saveButtonProgress.isVisible = isLoading
        saveButton.text = if (isLoading) "" else "Save Changes"
        cancelButton.isEnabled = !isLoading
    }
    
    private fun resetForm() {
        currentPasswordInput.text?.clear()
        newPasswordInput.text?.clear()
        confirmPasswordInput.text?.clear()
        saveButton.isEnabled = true
        saveButton.text = "Save Changes"
        saveButtonProgress.isVisible = false
        cancelButton.isEnabled = true
        resetPasswordValidation()
        
        passwordStrengthText.visibility = View.GONE
        
        currentPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
        newPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
        confirmPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        resetForm()
    }
} 