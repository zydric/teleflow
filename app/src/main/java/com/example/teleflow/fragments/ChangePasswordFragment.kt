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
    
    // Remove the strength indicators as we're simplifying the UI
    private lateinit var passwordStrengthText: TextView
    
    // ViewModel for authentication
    private val authViewModel: AuthViewModel by viewModels()
    
    // Simplified password requirements
    private val MIN_PASSWORD_LENGTH = 6
    private val REQUIRES_SPECIAL_CHAR = true
    private val REQUIRES_NUMBER = true
    
    // Colors for input validation
    private val COLOR_ERROR = R.color.logout_color        // Red color for errors
    private val COLOR_SUCCESS = R.color.primary_button_color // Green for success

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set action bar title
        requireActivity().title = "Change Password"
        
        // Check if user is logged in
        if (!authViewModel.isLoggedIn()) {
            // Redirect to login screen if not logged in
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Initialize UI components
        initializeViews(view)
        setupClickListeners()
        setupPasswordValidation()
        
        // Ensure consistent text appearance for all helper texts
        setupConsistentHelperTextAppearance()
    }

    private fun initializeViews(view: View) {
        // Password inputs
        currentPasswordInput = view.findViewById(R.id.current_password_input)
        newPasswordInput = view.findViewById(R.id.new_password_input)
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input)
        
        // Input layouts (for showing errors)
        currentPasswordLayout = view.findViewById(R.id.current_password_layout)
        newPasswordLayout = view.findViewById(R.id.new_password_layout)
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout)
        
        // Buttons
        saveButton = view.findViewById(R.id.save_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        saveButtonProgress = view.findViewById(R.id.save_button_progress)
        
        // Password strength text
        passwordStrengthText = view.findViewById(R.id.password_strength_text)
        
        // Hide the strength indicators since we're not using them
        view.findViewById<View>(R.id.strength_indicator_1).visibility = View.GONE
        view.findViewById<View>(R.id.strength_indicator_2).visibility = View.GONE
        view.findViewById<View>(R.id.strength_indicator_3).visibility = View.GONE
        view.findViewById<View>(R.id.strength_indicator_4).visibility = View.GONE
        
        // Hide the password requirements text initially
        passwordStrengthText.visibility = View.GONE
    }

    private fun setupClickListeners() {
        // Save button
        saveButton.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }

        // Cancel button
        cancelButton.setOnClickListener {
            // Simply go back without saving
            findNavController().popBackStack()
        }
    }
    
    private fun setupPasswordValidation() {
        // Add text watcher to the current password field to validate in real-time
        currentPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Remove real-time validation of current password - will be validated when saving
                currentPasswordLayout.error = null
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Add text watcher to the new password field
        newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Show the password strength text when user starts typing
                if (passwordStrengthText.visibility != View.VISIBLE && !s.isNullOrEmpty()) {
                    passwordStrengthText.visibility = View.VISIBLE
                }
                
                updatePasswordValidation(s.toString())
                
                // Check confirm password match if confirm field is not empty
                val confirmText = confirmPasswordInput.text.toString()
                if (confirmText.isNotEmpty()) {
                    validatePasswordMatch(s.toString(), confirmText)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Add text watcher to confirm password field to check matching in real-time
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
        // Set consistent text appearance for all password fields' helper/error text
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
            // Hide the password strength text if the input is empty
            passwordStrengthText.visibility = View.GONE
            return
        }
        
        val isValid = isPasswordValid(password)
        val missingRequirements = getMissingRequirements(password)
        
        // Show requirements using helperText for consistency
        if (isValid) {
            // Set green border by applying a drawable with green stroke
            newPasswordInput.setBackgroundResource(R.drawable.input_valid_background)
            
            // Show the success message in the helperText
            newPasswordLayout.helperText = "Password meets requirements ✓"
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_SUCCESS))
            
            // Hide the separate strength text
            passwordStrengthText.visibility = View.GONE
        } else {
            // Set red border by applying a drawable with red stroke
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            
            // Show requirements in the helperText
            newPasswordLayout.helperText = missingRequirements.joinToString("\n")
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            
            // Hide the separate strength text
            passwordStrengthText.visibility = View.GONE
        }
    }
    
    private fun resetPasswordValidation() {
        // Reset to default background
        newPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
        
        // Reset helper text
        newPasswordLayout.helperText = "Password must be at least $MIN_PASSWORD_LENGTH characters, include a special character and a number"
        newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), R.color.text_color))
        
        // Hide the separate strength text
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
        // Check minimum length
        if (password.length < MIN_PASSWORD_LENGTH) {
            return false
        }
        
        // Check for special character
        if (REQUIRES_SPECIAL_CHAR && !password.any { !it.isLetterOrDigit() }) {
            return false
        }
        
        // Check for number
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
            // Set helperText instead of error to avoid the exclamation icon
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
        
        // Reset all error states
        currentPasswordLayout.error = null
        newPasswordLayout.error = null
        confirmPasswordLayout.error = null
        
        newPasswordLayout.helperText = null
        confirmPasswordLayout.helperText = null

        // Get input values
        val currentPassword = currentPasswordInput.text.toString()
        val newPassword = newPasswordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        // Validate current password
        if (currentPassword.isEmpty()) {
            // Use helperText instead of error to avoid the exclamation icon
            currentPasswordLayout.helperText = "Current password cannot be empty"
            currentPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            currentPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        } else {
            currentPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
            currentPasswordLayout.helperText = null
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            // Use helperText instead of error to avoid the exclamation icon
            newPasswordLayout.helperText = "New password cannot be empty"
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        } else if (!isPasswordValid(newPassword)) {
            // Show the requirements in helperText instead of passwordStrengthText
            newPasswordLayout.helperText = getMissingRequirements(newPassword).joinToString("\n")
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            // Hide the separate strength text since we're using helperText
            passwordStrengthText.visibility = View.GONE
            isValid = false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            // Use helperText instead of error to avoid the exclamation icon
            confirmPasswordLayout.helperText = "Please confirm your new password"
            confirmPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            confirmPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        } else if (newPassword != confirmPassword) {
            // Use helperText instead of error to avoid the exclamation icon
            confirmPasswordLayout.helperText = "Passwords do not match"
            confirmPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            confirmPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        }
        
        // Check that new password is different from current password
        if (isValid && currentPassword == newPassword) {
            // Use helperText instead of error to avoid the exclamation icon
            newPasswordLayout.helperText = "New password must be different from current password"
            newPasswordLayout.setHelperTextColor(ContextCompat.getColorStateList(requireContext(), COLOR_ERROR))
            newPasswordInput.setBackgroundResource(R.drawable.input_error_background)
            isValid = false
        }

        return isValid
    }

    private fun changePassword() {
        // Show loading state
        showLoading(true)
        
        // Get input values
        val currentPassword = currentPasswordInput.text.toString()
        val newPassword = newPasswordInput.text.toString()
        
        // Use the AuthViewModel to change the password
        authViewModel.changePassword(currentPassword, newPassword) { result ->
            // Reset loading state
            showLoading(false)
            
            // Show result message
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            
            if (result.success) {
                // If successful, reset form and navigate back
                resetForm()
                
                // Refresh the navigation drawer's user data
                (requireActivity() as MainActivity).refreshNavigationDrawerUserData()
                
                findNavController().popBackStack()
            } else {
                // If failed, show the error on the current password field
                // as that's likely where the error occurred
                // Use helperText instead of error to avoid the exclamation icon
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
        
        // Hide password strength text when form is reset
        passwordStrengthText.visibility = View.GONE
        
        // Reset input backgrounds
        currentPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
        newPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
        confirmPasswordInput.setBackgroundResource(R.drawable.edit_profile_input_background)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear sensitive data when leaving the screen
        resetForm()
    }
} 