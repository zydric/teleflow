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
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
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
    
    // Password strength indicators
    private lateinit var strengthIndicator1: View
    private lateinit var strengthIndicator2: View
    private lateinit var strengthIndicator3: View
    private lateinit var strengthIndicator4: View
    private lateinit var passwordStrengthText: TextView

    // In a real app, this would come from authentication state
    private val MOCK_CURRENT_PASSWORD = "password123"
    
    // Password requirements
    private val MIN_PASSWORD_LENGTH = 8
    private val REQUIRES_SPECIAL_CHAR = true
    private val REQUIRES_NUMBER = true
    private val REQUIRES_UPPERCASE = true
    
    // Colors for strength indicators
    private val COLOR_WEAK = R.color.logout_color  // Red color for weak passwords
    private val COLOR_FAIR = R.color.yellow_darker // Darker yellow for fair
    private val COLOR_GOOD = R.color.accent_color  // Yellow for good
    private val COLOR_STRONG = R.color.primary_button_color // Green for strong

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

        // Initialize UI components
        initializeViews(view)
        setupClickListeners()
        setupPasswordStrengthIndicator()
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
        
        // Password strength indicators
        strengthIndicator1 = view.findViewById(R.id.strength_indicator_1)
        strengthIndicator2 = view.findViewById(R.id.strength_indicator_2)
        strengthIndicator3 = view.findViewById(R.id.strength_indicator_3)
        strengthIndicator4 = view.findViewById(R.id.strength_indicator_4)
        passwordStrengthText = view.findViewById(R.id.password_strength_text)
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
    
    private fun setupPasswordStrengthIndicator() {
        // Add text watcher to the current password field to validate in real-time
        currentPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateCurrentPassword(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Add text watcher to the new password field
        newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrengthIndicator(s.toString())
                
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
    
    private fun updatePasswordStrengthIndicator(password: String) {
        if (password.isEmpty()) {
            resetStrengthIndicators()
            passwordStrengthText.text = "Enter a password"
            return
        }
        
        val strength = calculatePasswordStrength(password)
        
        // Reset all indicators to inactive state
        resetStrengthIndicators()
        
        when (strength) {
            PasswordStrength.WEAK -> {
                strengthIndicator1.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_WEAK))
                strengthIndicator1.alpha = 1f
                passwordStrengthText.text = "Weak: Needs to meet more requirements"
                passwordStrengthText.setTextColor(ContextCompat.getColor(requireContext(), COLOR_WEAK))
            }
            PasswordStrength.FAIR -> {
                strengthIndicator1.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_FAIR))
                strengthIndicator2.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_FAIR))
                strengthIndicator1.alpha = 1f
                strengthIndicator2.alpha = 1f
                passwordStrengthText.text = "Fair: Almost there"
                passwordStrengthText.setTextColor(ContextCompat.getColor(requireContext(), COLOR_FAIR))
            }
            PasswordStrength.GOOD -> {
                strengthIndicator1.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_GOOD))
                strengthIndicator2.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_GOOD))
                strengthIndicator3.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_GOOD))
                strengthIndicator1.alpha = 1f
                strengthIndicator2.alpha = 1f
                strengthIndicator3.alpha = 1f
                passwordStrengthText.text = "Good: Meets all requirements"
                passwordStrengthText.setTextColor(ContextCompat.getColor(requireContext(), COLOR_GOOD))
            }
            PasswordStrength.STRONG -> {
                strengthIndicator1.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_STRONG))
                strengthIndicator2.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_STRONG))
                strengthIndicator3.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_STRONG))
                strengthIndicator4.setBackgroundColor(ContextCompat.getColor(requireContext(), COLOR_STRONG))
                strengthIndicator1.alpha = 1f
                strengthIndicator2.alpha = 1f
                strengthIndicator3.alpha = 1f
                strengthIndicator4.alpha = 1f
                passwordStrengthText.text = "Strong: Excellent password"
                passwordStrengthText.setTextColor(ContextCompat.getColor(requireContext(), COLOR_STRONG))
            }
        }
        
        // Update helper text with requirements
        updatePasswordHelperText(password)
    }
    
    private fun resetStrengthIndicators() {
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_color)
        strengthIndicator1.setBackgroundColor(inactiveColor)
        strengthIndicator2.setBackgroundColor(inactiveColor)
        strengthIndicator3.setBackgroundColor(inactiveColor)
        strengthIndicator4.setBackgroundColor(inactiveColor)
        strengthIndicator1.alpha = 0.3f
        strengthIndicator2.alpha = 0.3f
        strengthIndicator3.alpha = 0.3f
        strengthIndicator4.alpha = 0.3f
        passwordStrengthText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
    }
    
    private fun updatePasswordHelperText(password: String) {
        val missingRequirements = getMissingRequirements(password)
        if (missingRequirements.isEmpty()) {
            newPasswordLayout.helperText = "All requirements met"
        } else {
            newPasswordLayout.helperText = missingRequirements.joinToString("\n")
        }
    }
    
    private fun getMissingRequirements(password: String): List<String> {
        val missingRequirements = mutableListOf<String>()
        
        if (password.length < MIN_PASSWORD_LENGTH) {
            missingRequirements.add("• At least $MIN_PASSWORD_LENGTH characters")
        }
        
        if (REQUIRES_UPPERCASE && !password.any { it.isUpperCase() }) {
            missingRequirements.add("• Include uppercase letter")
        }
        
        if (REQUIRES_NUMBER && !password.any { it.isDigit() }) {
            missingRequirements.add("• Include number")
        }
        
        if (REQUIRES_SPECIAL_CHAR && !password.any { !it.isLetterOrDigit() }) {
            missingRequirements.add("• Include special character")
        }
        
        return missingRequirements
    }
    
    private fun calculatePasswordStrength(password: String): PasswordStrength {
        var score = 0
        
        // Length check
        when {
            password.length >= 12 -> score += 2
            password.length >= MIN_PASSWORD_LENGTH -> score += 1
        }
        
        // Uppercase check
        if (password.any { it.isUpperCase() }) score += 1
        
        // Number check
        if (password.any { it.isDigit() }) score += 1
        
        // Special character check
        if (password.any { !it.isLetterOrDigit() }) score += 1
        
        // Additional strength factors
        if (password.length > 14) score += 1
        if (password.count { it.isDigit() } > 2) score += 1
        if (password.count { !it.isLetterOrDigit() } > 1) score += 1
        
        return when {
            score >= 6 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.GOOD
            score >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Reset all error states
        currentPasswordLayout.error = null
        newPasswordLayout.error = null
        confirmPasswordLayout.error = null

        // Get input values
        val currentPassword = currentPasswordInput.text.toString()
        val newPassword = newPasswordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        // Validate current password
        if (currentPassword.isEmpty()) {
            currentPasswordLayout.error = "Current password cannot be empty"
            isValid = false
        } else if (currentPassword != MOCK_CURRENT_PASSWORD) {
            currentPasswordLayout.error = "Current password is incorrect"
            isValid = false
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            newPasswordLayout.error = "New password cannot be empty"
            isValid = false
        } else if (!isPasswordStrong(newPassword)) {
            // Show detailed password requirements error
            newPasswordLayout.error = getPasswordRequirementsMessage()
            isValid = false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your new password"
            isValid = false
        } else if (newPassword != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            isValid = false
        }
        
        // Check that new password is different from current password
        if (isValid && currentPassword == newPassword) {
            newPasswordLayout.error = "New password must be different from current password"
            isValid = false
        }

        return isValid
    }

    private fun isPasswordStrong(password: String): Boolean {
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
        
        // Check for uppercase letter
        if (REQUIRES_UPPERCASE && !password.any { it.isUpperCase() }) {
            return false
        }
        
        return true
    }
    
    private fun getPasswordRequirementsMessage(): String {
        return "Password must be at least $MIN_PASSWORD_LENGTH characters" +
                (if (REQUIRES_UPPERCASE) ", include an uppercase letter" else "") +
                (if (REQUIRES_NUMBER) ", include a number" else "") +
                (if (REQUIRES_SPECIAL_CHAR) ", include a special character" else "")
    }

    private fun changePassword() {
        // Show loading state
        showLoading(true)
        
        // Simulate API call delay
        saveButton.postDelayed({
            // In a real app, this would make an API call to change the password
            
            // Reset form and show success message
            resetForm()
            showLoading(false)
            Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
            
            // Navigate back to profile
            findNavController().popBackStack()
        }, 2000) // Longer delay to show loading state
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
        resetStrengthIndicators()
    }
    
    private fun validatePasswordMatch(newPassword: String, confirmPassword: String) {
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = null
            return
        }
        
        if (newPassword != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
        } else {
            confirmPasswordLayout.error = null
            confirmPasswordLayout.helperText = "Passwords match ✓"
        }
    }
    
    private fun validateCurrentPassword(password: String) {
        if (password.isEmpty()) {
            currentPasswordLayout.error = null
            return
        }
        
        if (password != MOCK_CURRENT_PASSWORD) {
            currentPasswordLayout.error = "Current password is incorrect"
        } else {
            currentPasswordLayout.error = null
            currentPasswordLayout.helperText = "Password verified ✓"
        }
    }
    
    // Enum to represent password strength levels
    enum class PasswordStrength {
        WEAK, FAIR, GOOD, STRONG
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear sensitive data when leaving the screen
        resetForm()
    }
} 