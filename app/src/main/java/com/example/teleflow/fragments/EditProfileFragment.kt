package com.example.teleflow.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.utils.ImageUtils
import com.example.teleflow.viewmodels.AuthViewModel

class EditProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var cameraButton: ImageView
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: TextView

    // URI for the selected profile image
    private var selectedImageUri: Uri? = null
    
    // ViewModel for authentication
    private val authViewModel: AuthViewModel by viewModels()

    // Activity result launcher for image picking
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                try {
                    // Use our utility method to create and display a circular bitmap immediately
                    val circularBitmap = ImageUtils.createCircularBitmapFromUri(requireContext(), uri)
                    if (circularBitmap != null) {
                        profileImage.setImageBitmap(circularBitmap)
                        // Remove any tint and padding
                        profileImage.clearColorFilter()
                        profileImage.setPadding(0, 0, 0, 0)
                    } else {
                        Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileFragment", "Error setting image URI", e)
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set action bar title
        requireActivity().title = "Edit Profile"
        
        // Check if user is logged in
        if (!authViewModel.isLoggedIn()) {
            // Redirect to login screen if not logged in
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Initialize UI components
        profileImage = view.findViewById(R.id.profile_image)
        cameraButton = view.findViewById(R.id.camera_button)
        nameInput = view.findViewById(R.id.name_input)
        emailInput = view.findViewById(R.id.email_input)
        saveButton = view.findViewById(R.id.save_button)
        cancelButton = view.findViewById(R.id.cancel_button)

        // Load existing profile data
        loadProfileData()

        // Set up click listeners
        setupClickListeners()
    }
    
    private fun loadProfileData() {
        // Load existing profile image if available
        ImageUtils.loadProfileImage(requireContext(), profileImage)
        
        // Load user data from ViewModel
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                nameInput.setText(user.fullName)
                emailInput.setText(user.email)
            }
        }
    }

    private fun setupClickListeners() {
        // Profile image click
        profileImage.setOnClickListener {
            openImagePicker()
        }

        // Camera button click
        cameraButton.setOnClickListener {
            openImagePicker()
        }

        // Save button
        saveButton.setOnClickListener {
            if (validateInputs()) {
                saveProfileChanges()
            }
        }

        // Cancel button
        cancelButton.setOnClickListener {
            // Simply go back without saving
            findNavController().popBackStack()
        }
    }

    private fun openImagePicker() {
        // Use the improved utility method to launch the image picker
        ImageUtils.pickImageFromGallery(pickImageLauncher)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameInput.error = "Name cannot be empty"
            isValid = false
        }

        // Validate email
        val email = emailInput.text.toString().trim()
        if (email.isEmpty()) {
            emailInput.error = "Email cannot be empty"
            isValid = false
        } else if (!isValidEmail(email)) {
            emailInput.error = "Please enter a valid email address"
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun saveProfileChanges() {
        // Show loading state
        saveButton.isEnabled = false
        saveButton.text = "Saving..."
        
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        
        // Image path to save
        var imagePath: String? = null
        
        // Save the profile image
        if (selectedImageUri != null) {
            val saved = ImageUtils.saveProfileImage(requireContext(), selectedImageUri)
            if (!saved) {
                Toast.makeText(requireContext(), "Failed to save profile image", Toast.LENGTH_SHORT).show()
            } else {
                imagePath = selectedImageUri.toString()
            }
        }
        
        // Update the user profile using the AuthViewModel
        authViewModel.updateUserProfile(name, email, imagePath) { result ->
            // Restore button state
            saveButton.isEnabled = true
            saveButton.text = "Save changes"
            
            // Show result
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            
            if (result.success) {
                // Navigate back to the profile page
                findNavController().popBackStack()
            }
        }
    }
} 