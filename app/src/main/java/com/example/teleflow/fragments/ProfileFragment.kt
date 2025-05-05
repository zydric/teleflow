package com.example.teleflow.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.utils.ImageUtils
import com.example.teleflow.viewmodels.AuthViewModel

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var usernameLabel: TextView
    private lateinit var emailLabel: TextView
    private lateinit var editProfileItem: LinearLayout
    private lateinit var changePasswordItem: LinearLayout
    private lateinit var logoutItem: LinearLayout
    private lateinit var aboutItem: LinearLayout
    private lateinit var versionItem: LinearLayout
    private lateinit var appVersion: TextView
    
    // ViewModel for authentication
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set action bar title
        requireActivity().title = "Profile"

        // Initialize UI components
        profileImage = view.findViewById(R.id.profile_image)
        usernameLabel = view.findViewById(R.id.username_label)
        emailLabel = view.findViewById(R.id.email_label)
        editProfileItem = view.findViewById(R.id.edit_profile_item)
        changePasswordItem = view.findViewById(R.id.change_password_item)
        logoutItem = view.findViewById(R.id.logout_item)
        aboutItem = view.findViewById(R.id.about_item)
        versionItem = view.findViewById(R.id.version_item)
        appVersion = view.findViewById(R.id.app_version)
        
        // Check if user is logged in
        if (!authViewModel.isLoggedIn()) {
            // Redirect to login screen if not logged in
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Set app version
        appVersion.text = "1.0.0" // Hardcoded for now, would use dynamic versioning in production

        // Load profile image
        loadProfileImage()
        
        // Observe current user data
        observeUserData()

        // Set up click listeners
        setupClickListeners()
    }
    
    private fun observeUserData() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Update UI with user data
                usernameLabel.text = user.fullName
                emailLabel.text = user.email
            } else {
                // Default values if user is null (should not happen)
                usernameLabel.text = "User"
                emailLabel.text = "user@example.com"
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh profile image when returning to this fragment
        loadProfileImage()
        
        // Refresh user data when returning to the fragment
        refreshUserData()
    }
    
    private fun loadProfileImage() {
        // Load profile image using the improved utility
        ImageUtils.loadProfileImage(requireContext(), profileImage)
        // Remove any padding that would be present for the icon
        profileImage.setPadding(0, 0, 0, 0)
    }
    
    private fun refreshUserData() {
        // Refresh user data from the database
        authViewModel.refreshUserData()
        
        // Also update UI with current value if available
        val currentUser = authViewModel.currentUser.value
        if (currentUser != null) {
            // Update UI with fresh user data
            usernameLabel.text = currentUser.fullName
            emailLabel.text = currentUser.email
        }
    }
    
    private fun setupClickListeners() {
        // Edit Profile
        editProfileItem.setOnClickListener {
            // Navigate to the Edit Profile screen
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        
        // Change Password
        changePasswordItem.setOnClickListener {
            // Navigate to the Change Password screen
            findNavController().navigate(R.id.action_profileFragment_to_changePasswordFragment)
        }
        
        // Logout
        logoutItem.setOnClickListener {
            showLogoutConfirmation()
        }
        
        // About TeleFlow
        aboutItem.setOnClickListener {
            // Navigate to the About Developers screen
            findNavController().navigate(R.id.action_profileFragment_to_aboutDevelopersFragment)
        }
        
        // Version (no action, just display)
        versionItem.setOnClickListener {
            // Could show build details or check for updates in a real app
            Toast.makeText(requireContext(), "App version: ${appVersion.text}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Perform logout using AuthViewModel
                authViewModel.logout()
                
                Toast.makeText(requireContext(), "Logout successful", Toast.LENGTH_SHORT).show()
                
                // Clear profile image on logout
                ImageUtils.clearProfileImage(requireContext())
                
                // Navigate to login fragment
                findNavController().navigate(R.id.loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 