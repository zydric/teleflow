package com.example.teleflow.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.adapters.RecordingAdapter
import com.example.teleflow.adapters.ScriptAdapter
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import com.example.teleflow.viewmodels.RecordingViewModel
import com.example.teleflow.viewmodels.ScriptViewModel

class HomeFragment : Fragment() {

    private lateinit var scriptsRecyclerView: RecyclerView
    private lateinit var recordingsRecyclerView: RecyclerView
    
    private lateinit var scriptAdapter: ScriptAdapter
    private lateinit var recordingAdapter: RecordingAdapter
    
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up scripts RecyclerView
        scriptsRecyclerView = view.findViewById(R.id.recyclerView_scripts)
        scriptsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        // Create script adapter with empty list (will be populated from ViewModel)
        scriptAdapter = ScriptAdapter(
            mutableListOf(),
            onItemClick = { script ->
                // When a recent script is clicked from the home screen, navigate directly to recording
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
            },
            onItemLongClick = { script, view ->
                // Show popup menu with edit and delete options
                showScriptOptionsMenu(view, script)
            }
        )
        scriptsRecyclerView.adapter = scriptAdapter

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
        
        // Observe the scripts from ViewModel
        scriptViewModel.allScripts.observe(viewLifecycleOwner, Observer { scripts ->
            // Update the adapter with the latest scripts (limit to 3 for recent scripts)
            val recentScripts = scripts.take(3)
            scriptAdapter.updateData(recentScripts)
        })
        
        // Observe the recordings from ViewModel
        recordingViewModel.allRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            // Update the adapter with the latest recordings
            recordingAdapter.updateData(recordings)
        })
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
} 