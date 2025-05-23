package com.example.teleflow.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.RecordActivity
import com.example.teleflow.adapters.RecordingAdapter
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import com.example.teleflow.viewmodels.AuthViewModel
import com.example.teleflow.viewmodels.RecordingViewModel
import com.example.teleflow.viewmodels.ScriptViewModel

class HomeFragment : Fragment() {

    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingAdapter: RecordingAdapter
    
    // Script item views
    private lateinit var scriptItem1: View
    private lateinit var scriptItem2: View
    private lateinit var scriptItem3: View
    
    // Empty state views
    private lateinit var scriptsEmptyStateLayout: ConstraintLayout
    private lateinit var recordingsEmptyStateLayout: ConstraintLayout
    
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Cache for script data
    private var scriptsList = listOf<Script>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if user is logged in
        if (!authViewModel.isLoggedIn()) {
            // Redirect to login screen if not logged in
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Find the script item views
        scriptItem1 = view.findViewById(R.id.script_item_1)
        scriptItem2 = view.findViewById(R.id.script_item_2)
        scriptItem3 = view.findViewById(R.id.script_item_3)
        
        // Find empty state layouts
        scriptsEmptyStateLayout = view.findViewById(R.id.layout_empty_scripts)
        recordingsEmptyStateLayout = view.findViewById(R.id.layout_empty_recordings)
        
        // Initially hide all script items until data is loaded
        scriptItem1.visibility = View.GONE
        scriptItem2.visibility = View.GONE
        scriptItem3.visibility = View.GONE

        // Set up recordings RecyclerView
        recordingsRecyclerView = view.findViewById(R.id.recyclerView_recordings)
        recordingsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        // Create recording adapter with empty list (will be populated from ViewModel)
        recordingAdapter = RecordingAdapter(
            mutableListOf(),
            onItemClick = { recording ->
                // Play the recording directly
                playInApp(recording)
            },
            onItemLongClick = { recording, view ->
                // Show popup menu with play and delete options
                showRecordingOptionsMenu(view, recording)
            },
            getScript = { scriptId -> 
                scriptViewModel.getScriptById(scriptId)
            },
            lifecycleOwner = viewLifecycleOwner
        )
        recordingsRecyclerView.adapter = recordingAdapter

        // Set up navigation to script selection when "Record New Video" button is clicked
        val recordNewButton = view.findViewById<Button>(R.id.button_recordNew)
        recordNewButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scriptsFragment)
        }
        
        // Set up "View All" recordings button click listener
        val viewAllRecordingsButton = view.findViewById<TextView>(R.id.textView_viewAllRecordings)
        viewAllRecordingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_recordingsFragment)
        }
        
        // Observe the recently used scripts from ViewModel for the Recent Scripts section
        scriptViewModel.userRecentScripts.observe(viewLifecycleOwner, Observer { scripts ->
            // Cache the script list
            scriptsList = scripts
            
            // Update the script items with the most recent 3 scripts
            updateScriptItems()
        })
        
        // Observe the recordings from ViewModel - use user-specific recordings
        recordingViewModel.userRecentRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            // Update the adapter with the recordings (already limited in the ViewModel)
            recordingAdapter.updateData(recordings)
            
            // Toggle visibility of empty state view
            if (recordings.isEmpty()) {
                recordingsEmptyStateLayout.visibility = View.VISIBLE
                recordingsRecyclerView.visibility = View.GONE
                
                // Make the empty state image very large
                val emptyRecordingsImage = view.findViewById<ImageView>(R.id.imageView_empty_recordings)
                emptyRecordingsImage?.let {
                    val layoutParams = it.layoutParams
                    layoutParams.height = 600 // Set explicit large height (600dp)
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    it.layoutParams = layoutParams
                    it.setPadding(0, 100, 0, 100) // Reduced padding to give more room for the image
                    it.scaleType = ImageView.ScaleType.FIT_CENTER
                    it.adjustViewBounds = true // Allow the image to maintain aspect ratio
                }
                
                // Also set minimum height for the empty state container
                recordingsEmptyStateLayout.minimumHeight = 800 // Set explicit large minimum height
            } else {
                recordingsEmptyStateLayout.visibility = View.GONE
                recordingsRecyclerView.visibility = View.VISIBLE
            }
        })
    }
    
    private fun updateScriptItems() {
        // Get the 3 most recently used scripts
        val recentScripts = scriptsList.take(3)
        
        // Clear all script items first
        scriptItem1.visibility = View.GONE
        scriptItem2.visibility = View.GONE
        scriptItem3.visibility = View.GONE
        
        // Toggle visibility of empty state layout
        if (recentScripts.isEmpty()) {
            scriptsEmptyStateLayout.visibility = View.VISIBLE
            
            // Add more space between Recent Scripts and Recent Recordings sections
            val recentRecordingsTextView = view?.findViewById<TextView>(R.id.textView_recentRecordings)
            recentRecordingsTextView?.let {
                val layoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.empty_state_section_margin)
                it.layoutParams = layoutParams
            }
            
            // Make the empty state image very large
            val emptyScriptsImage = view?.findViewById<ImageView>(R.id.imageView_empty_scripts)
            emptyScriptsImage?.let {
                val layoutParams = it.layoutParams
                layoutParams.height = 600 // Set explicit large height (600dp)
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                it.layoutParams = layoutParams
                it.setPadding(0, 100, 0, 100) // Reduced padding to give more room for the image
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.adjustViewBounds = true // Allow the image to maintain aspect ratio
            }
            
            // Also set minimum height for the empty state container
            scriptsEmptyStateLayout.minimumHeight = 800 // Set explicit large minimum height
        } else {
            scriptsEmptyStateLayout.visibility = View.GONE
            
            // Reset the margin to normal when scripts are available
            val recentRecordingsTextView = view?.findViewById<TextView>(R.id.textView_recentRecordings)
            recentRecordingsTextView?.let {
                val layoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.section_vertical_spacing)
                it.layoutParams = layoutParams
            }
            
            // Populate script items based on available data
            if (recentScripts.isNotEmpty()) {
                updateScriptItem(scriptItem1, recentScripts[0], 0)
            }
            
            if (recentScripts.size > 1) {
                updateScriptItem(scriptItem2, recentScripts[1], 1)
            }
            
            if (recentScripts.size > 2) {
                updateScriptItem(scriptItem3, recentScripts[2], 2)
            }
        }
    }
    
    private fun updateScriptItem(itemView: View, script: Script, position: Int) {
        // Set visibility
        itemView.visibility = View.VISIBLE
        
        // Find views within the item
        val titleTextView = itemView.findViewById<TextView>(R.id.textView_scriptTitle)
        val previewTextView = itemView.findViewById<TextView>(R.id.textView_scriptPreview)
        val moreOptionsImageView = itemView.findViewById<ImageView>(R.id.imageView_more)
        
        // Set data
        titleTextView.text = script.title
        previewTextView.text = script.content
        
        // Set up the more options (3-dot menu) click listener
        moreOptionsImageView.setOnClickListener { view ->
            showScriptOptionsMenu(view, script)
        }
        
        // Set click listener for the entire item
        itemView.setOnClickListener {
            // Launch RecordActivity directly
            val intent = Intent(requireContext(), RecordActivity::class.java).apply {
                putExtra("scriptTitle", script.title)
                putExtra("scriptContent", script.content)
                putExtra("scriptId", script.id)
            }
            startActivity(intent)
            
            // Update last used timestamp
            scriptViewModel.updateScriptLastUsed(script.id)
        }
        
        // Set long click listener to directly show delete option
        itemView.setOnLongClickListener {
            // For long press, just show delete confirmation directly
            showDeleteConfirmationDialog(script)
            true
        }
    }
    
    private fun showScriptOptionsMenu(view: View, script: Script) {
        val popup = PopupMenu(requireContext(), view)
        
        popup.menuInflater.inflate(R.menu.menu_script_options, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    // Navigate to script editor for editing
                    val bundle = Bundle().apply {
                        putInt("scriptId", script.id)
                    }
                    findNavController().navigate(R.id.action_homeFragment_to_scriptEditorFragment, bundle)
                    true
                }
                R.id.action_delete -> {
                    // Show confirmation dialog before deleting
                    showDeleteConfirmationDialog(script)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showRecordingOptionsMenu(view: View, recording: Recording) {
        val popup = PopupMenu(requireContext(), view)
        
        popup.menuInflater.inflate(R.menu.menu_recording_options, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_play -> {
                    playInApp(recording)
                    true
                }
                R.id.action_delete -> {
                    // Show confirmation dialog before deleting
                    showDeleteRecordingConfirmationDialog(recording)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showDeleteConfirmationDialog(script: Script) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Script")
            .setMessage("Are you sure you want to delete '${script.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the script
                scriptViewModel.deleteScript(script)
                Toast.makeText(requireContext(), "Script deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteRecordingConfirmationDialog(recording: Recording) {
        // Get script information to show in dialog
        scriptViewModel.getScriptById(recording.scriptId).observe(viewLifecycleOwner, Observer { script ->
            val scriptTitle = script?.title ?: "Unknown Script"
            
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to delete this recording of \"$scriptTitle\"? The recording will be removed from the app but will remain in your device storage.")
                .setPositiveButton("Delete") { _, _ ->
                    // Delete the recording from the database
                    recordingViewModel.deleteRecording(recording)
                    Toast.makeText(requireContext(), "Recording deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        })
    }
    
    private fun playInApp(recording: Recording) {
        // Navigate to our video player fragment
        val bundle = Bundle().apply {
            putInt("recordingId", recording.id)
            putString("videoUri", recording.videoUri)
            putInt("scriptId", recording.scriptId)
            putLong("date", recording.date)
        }
        findNavController().navigate(R.id.action_homeFragment_to_videoPlayerFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the thumbnail cache when the view is destroyed
        if (::recordingAdapter.isInitialized) {
            recordingAdapter.clearCache()
        }
    }
} 