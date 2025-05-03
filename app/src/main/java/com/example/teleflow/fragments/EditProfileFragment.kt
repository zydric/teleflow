package com.example.teleflow.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
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
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R

class EditProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var cameraButton: ImageView
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: TextView

    // URI for the selected profile image
    private var selectedImageUri: Uri? = null

    // Activity result launcher for image picking
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                profileImage.setImageURI(uri)
                // Remove the icon tint when a real image is loaded
                profileImage.colorFilter = null
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

        // Initialize UI components
        profileImage = view.findViewById(R.id.profile_image)
        cameraButton = view.findViewById(R.id.camera_button)
        nameInput = view.findViewById(R.id.name_input)
        emailInput = view.findViewById(R.id.email_input)
        saveButton = view.findViewById(R.id.save_button)
        cancelButton = view.findViewById(R.id.cancel_button)

        // Set up click listeners
        setupClickListeners()
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
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
        // In a real app, this would save the user profile to a database or server
        // For now, we just show a success message and navigate back
        
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        
        // Update the user profile in the Profile fragment
        // This is just a placeholder for now since we don't have a real database
        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back to the profile page
        findNavController().popBackStack()
    }
} 