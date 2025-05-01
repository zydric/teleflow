package com.example.teleflow.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.teleflow.R

class SettingsFragment : Fragment() {

    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var fontSizeRadioGroup: RadioGroup
    private lateinit var videoQualitySpinner: Spinner
    private lateinit var autoSaveSwitch: SwitchCompat
    private lateinit var versionValue: TextView
    private lateinit var privacyPolicyButton: Button
    private lateinit var termsButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        darkModeSwitch = view.findViewById(R.id.dark_mode_switch)
        fontSizeRadioGroup = view.findViewById(R.id.font_size_radio_group)
        videoQualitySpinner = view.findViewById(R.id.video_quality_spinner)
        autoSaveSwitch = view.findViewById(R.id.auto_save_switch)
        versionValue = view.findViewById(R.id.version_value)
        privacyPolicyButton = view.findViewById(R.id.privacy_policy_button)
        termsButton = view.findViewById(R.id.terms_button)

        // Set up video quality spinner
        val videoQualities = arrayOf("Low (480p)", "Medium (720p)", "High (1080p)", "Ultra High (4K)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, videoQualities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        videoQualitySpinner.adapter = adapter
        videoQualitySpinner.setSelection(1) // Default to Medium

        // Set version number
        versionValue.text = "1.0.0" // Hardcoded version instead of BuildConfig.VERSION_NAME

        // Set up dark mode switch listener
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Toggle dark mode
            val mode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            Toast.makeText(
                requireContext(),
                "Dark mode ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Set up font size radio group listener
        fontSizeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = view.findViewById<RadioButton>(checkedId)
            Toast.makeText(
                requireContext(),
                "Font size set to ${radioButton.text}",
                Toast.LENGTH_SHORT
            ).show()
            // Here you would actually implement the font size change
        }

        // Set up auto-save switch listener
        autoSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                requireContext(),
                "Auto-save ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Set up privacy policy button
        privacyPolicyButton.setOnClickListener {
            openWebPage("https://www.example.com/privacy-policy")
        }

        // Set up terms of service button
        termsButton.setOnClickListener {
            openWebPage("https://www.example.com/terms-of-service")
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                requireContext(),
                "No app found to open the link",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
} 