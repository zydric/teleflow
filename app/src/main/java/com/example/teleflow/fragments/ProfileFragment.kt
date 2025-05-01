package com.example.teleflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.teleflow.R

class ProfileFragment : Fragment() {

    private lateinit var usernameLabel: TextView
    private lateinit var emailLabel: TextView
    private lateinit var editProfileButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        usernameLabel = view.findViewById(R.id.username_label)
        emailLabel = view.findViewById(R.id.email_label)
        editProfileButton = view.findViewById(R.id.edit_profile_button)

        // Set example user data (would be replaced with actual user data in a real app)
        usernameLabel.text = "TeleFlow User"
        emailLabel.text = "user@teleflow.example.com"

        // Set up edit profile button click listener
        editProfileButton.setOnClickListener {
            // This would normally open a profile editor
            Toast.makeText(requireContext(), "Edit profile functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
} 