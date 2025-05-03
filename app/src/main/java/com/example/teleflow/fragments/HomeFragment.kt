package com.example.teleflow.fragments

import android.app.AlertDialog
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.adapters.RecordingAdapter
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import com.example.teleflow.viewmodels.RecordingViewModel
import com.example.teleflow.viewmodels.ScriptViewModel

class HomeFragment : Fragment() {

    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingAdapter: RecordingAdapter
    
    // Script item views
    private lateinit var scriptItem1: View
    private lateinit var scriptItem2: View
    private lateinit var scriptItem3: View
    
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()

    // Cache for script data
    private var scriptsList = listOf<Script>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the script item views
        scriptItem1 = view.findViewById(R.id.script_item_1)
        scriptItem2 = view.findViewById(R.id.script_item_2)
        scriptItem3 = view.findViewById(R.id.script_item_3)
        
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
        scriptViewModel.recentlyUsedScripts.observe(viewLifecycleOwner, Observer { scripts ->
            // Cache the script list
            scriptsList = scripts
            
            // Update the script items with the most recent 3 scripts
            updateScriptItems()
        })
        
        // Observe the recordings from ViewModel
        recordingViewModel.allRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            // Update the adapter with the latest 5 recordings
            recordingAdapter.updateData(recordings.take(5))
        })
    }
    
    private fun updateScriptItems() {
        // Get the 3 most recently used scripts
        val recentScripts = scriptsList.take(3)
        
        // Clear all script items first
        scriptItem1.visibility = View.GONE
        scriptItem2.visibility = View.GONE
        scriptItem3.visibility = View.GONE
        
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
            val bundle = Bundle().apply {
                putString("scriptTitle", script.title)
                putString("scriptContent", script.content)
                putInt("scriptId", script.id)
            }
            // Navigate directly to the recording fragment with the selected script
            findNavController().navigate(R.id.recordFragment, bundle)
            
            // Show a toast confirming the selection
            Toast.makeText(
                requireContext(),
                "Selected recent script: ${script.title}",
                Toast.LENGTH_SHORT
            ).show()
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