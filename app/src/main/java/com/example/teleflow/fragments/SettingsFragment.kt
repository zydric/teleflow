package com.example.teleflow.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R

class SettingsFragment : Fragment() {

    // UI elements
    private lateinit var accountSettingsItem: LinearLayout
    private lateinit var changePasswordItem: LinearLayout
    private lateinit var aboutTeleflowItem: LinearLayout
    
    private lateinit var decreaseFontButton: ImageButton
    private lateinit var increaseFontButton: ImageButton
    private lateinit var fontSizeText: TextView
    
    private lateinit var colorWhite: ImageView
    private lateinit var colorOrange: ImageView
    private lateinit var colorBlue: ImageView
    private lateinit var colorGreen: ImageView
    private lateinit var colorRed: ImageView
    private val colorViews = mutableListOf<ImageView>()
    
    private lateinit var opacitySlider: SeekBar
    private lateinit var opacityValue: TextView
    
    private lateinit var scrollSpeedSlider: SeekBar
    private lateinit var scrollSpeedValue: TextView
    
    // Settings values
    private var fontSize = 18
    private var selectedColorIndex = 0 // 0=white, 1=orange, 2=blue, 3=green, 4=red
    private var opacity = 75
    private var scrollSpeed = 50
    
    // Constants
    private val MIN_FONT_SIZE = 12
    private val MAX_FONT_SIZE = 28
    
    // Shared preferences
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize shared preferences
        prefs = requireActivity().getSharedPreferences("teleflow_settings", Context.MODE_PRIVATE)
        
        // Initialize UI components
        initializeViews(view)
        
        // Load saved settings
        loadSettings()
        
        // Set up event listeners
        setupEventListeners()
    }
    
    private fun initializeViews(view: View) {
        // General section
        accountSettingsItem = view.findViewById(R.id.account_settings_item)
        changePasswordItem = view.findViewById(R.id.change_password_item)
        aboutTeleflowItem = view.findViewById(R.id.about_teleflow_item)
        
        // Font size controls
        decreaseFontButton = view.findViewById(R.id.btn_decrease_font)
        increaseFontButton = view.findViewById(R.id.btn_increase_font)
        fontSizeText = view.findViewById(R.id.tv_font_size)
        
        // Font color controls
        colorWhite = view.findViewById(R.id.color_white)
        colorOrange = view.findViewById(R.id.color_orange)
        colorBlue = view.findViewById(R.id.color_blue)
        colorGreen = view.findViewById(R.id.color_green)
        colorRed = view.findViewById(R.id.color_red)
        colorViews.addAll(listOf(colorWhite, colorOrange, colorBlue, colorGreen, colorRed))
        
        // Opacity slider
        opacitySlider = view.findViewById(R.id.opacity_slider)
        opacityValue = view.findViewById(R.id.tv_opacity_value)
        
        // Scroll speed slider
        scrollSpeedSlider = view.findViewById(R.id.scroll_speed_slider)
        scrollSpeedValue = view.findViewById(R.id.tv_scroll_speed_value)
    }
    
    private fun loadSettings() {
        // Load font size
        fontSize = prefs.getInt("font_size", 18)
        fontSizeText.text = fontSize.toString()
        
        // Load color selection
        selectedColorIndex = prefs.getInt("font_color_index", 0)
        updateColorSelection()
        
        // Load opacity
        opacity = prefs.getInt("opacity", 75)
        opacitySlider.progress = opacity
        updateOpacityText()
        
        // Load scroll speed
        scrollSpeed = prefs.getInt("scroll_speed", 50)
        scrollSpeedSlider.progress = scrollSpeed
        updateScrollSpeedText()
    }
    
    private fun setupEventListeners() {
        // Navigation items
        accountSettingsItem.setOnClickListener {
            // Navigate to Profile screen instead of Edit Profile
            findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)
        }
        
        changePasswordItem.setOnClickListener {
            // Navigate to change password
            findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
        }
        
        aboutTeleflowItem.setOnClickListener {
            // Navigate to about developers
            findNavController().navigate(R.id.action_settingsFragment_to_aboutDevelopersFragment)
        }
        
        // Font size buttons
        decreaseFontButton.setOnClickListener {
            if (fontSize > MIN_FONT_SIZE) {
                fontSize--
                fontSizeText.text = fontSize.toString()
                saveSetting("font_size", fontSize)
            }
        }
        
        increaseFontButton.setOnClickListener {
            if (fontSize < MAX_FONT_SIZE) {
                fontSize++
                fontSizeText.text = fontSize.toString()
                saveSetting("font_size", fontSize)
            }
        }
        
        // Font color selection
        for (i in colorViews.indices) {
            val colorView = colorViews[i]
            colorView.setOnClickListener {
                selectedColorIndex = i
                updateColorSelection()
                saveSetting("font_color_index", selectedColorIndex)
            }
        }
        
        // Opacity slider
        opacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                opacity = progress
                updateOpacityText()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                saveSetting("opacity", opacity)
            }
        })
        
        // Scroll speed slider
        scrollSpeedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                scrollSpeed = progress
                updateScrollSpeedText()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                saveSetting("scroll_speed", scrollSpeed)
            }
        })
    }
    
    private fun updateColorSelection() {
        // Update all colors to unselected state
        for (i in colorViews.indices) {
            colorViews[i].isSelected = (i == selectedColorIndex)
        }
    }
    
    private fun updateOpacityText() {
        opacityValue.text = "$opacity%"
    }
    
    private fun updateScrollSpeedText() {
        val speedText = when {
            scrollSpeed < 25 -> "Slow"
            scrollSpeed < 40 -> "Medium Slow"
            scrollSpeed in 40..60 -> "Normal"
            scrollSpeed < 80 -> "Medium Fast"
            else -> "Fast"
        }
        scrollSpeedValue.text = speedText
    }
    
    private fun saveSetting(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        
        // Log the saved setting to help with debugging
        android.util.Log.d("SettingsFragment", "Saved setting: $key = $value")
    }
    
    private fun getColorFromIndex(index: Int): String {
        return when (index) {
            0 -> "#FFFFFF" // White
            1 -> "#FFCC00" // Orange/Gold
            2 -> "#3B82F6" // Blue
            3 -> "#10B981" // Green
            4 -> "#EF4444" // Red
            else -> "#FFFFFF" // Default white
        }
    }
} 