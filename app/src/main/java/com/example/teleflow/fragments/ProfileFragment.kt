package com.example.teleflow.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R

class ProfileFragment : Fragment() {

    private lateinit var usernameLabel: TextView
    private lateinit var emailLabel: TextView
    private lateinit var editProfileItem: LinearLayout
    private lateinit var changePasswordItem: LinearLayout
    private lateinit var logoutItem: LinearLayout
    private lateinit var aboutItem: LinearLayout
    private lateinit var versionItem: LinearLayout
    private lateinit var appVersion: TextView

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
        usernameLabel = view.findViewById(R.id.username_label)
        emailLabel = view.findViewById(R.id.email_label)
        editProfileItem = view.findViewById(R.id.edit_profile_item)
        changePasswordItem = view.findViewById(R.id.change_password_item)
        logoutItem = view.findViewById(R.id.logout_item)
        aboutItem = view.findViewById(R.id.about_item)
        versionItem = view.findViewById(R.id.version_item)
        appVersion = view.findViewById(R.id.app_version)

        // Set placeholder user data
        usernameLabel.text = "John Anderson"
        emailLabel.text = "john.anderson@email.com"
        
        // Set app version
        appVersion.text = "2.1.0" // Hardcoded for now, would use dynamic versioning in production

        // Set up click listeners
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // Edit Profile
        editProfileItem.setOnClickListener {
            // Navigate to the Edit Profile screen
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        
        // Change Password
        changePasswordItem.setOnClickListener {
            // This would navigate to password change screen in a real app
            Toast.makeText(requireContext(), "Change Password coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Logout
        logoutItem.setOnClickListener {
            showLogoutConfirmation()
        }
        
        // About TeleFlow
        aboutItem.setOnClickListener {
            // This would navigate to an about screen in a real app
            Toast.makeText(requireContext(), "About TeleFlow coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Version (no action, just display)
        versionItem.setOnClickListener {
            // Could show build details or check for updates in a real app
            Toast.makeText(requireContext(), "App version: ${appVersion.text}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Would perform actual logout in a real app
                Toast.makeText(requireContext(), "Logout successful", Toast.LENGTH_SHORT).show()
                // Normally would navigate to login screen
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 