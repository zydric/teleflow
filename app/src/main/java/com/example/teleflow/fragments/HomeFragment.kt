package com.example.teleflow.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
                findNavController().navigate(R.id.recordingFragment, bundle)
                
                // Show a toast confirming the selection
                Toast.makeText(
                    requireContext(),
                    "Selected recent script: ${script.title}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onItemLongClick = null
        )
        scriptsRecyclerView.adapter = scriptAdapter

        // Set up recordings RecyclerView
        recordingsRecyclerView = view.findViewById(R.id.recyclerView_recordings)
        recordingsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        // Create recording adapter with empty list (will be populated from ViewModel)
        recordingAdapter = RecordingAdapter(mutableListOf()) { recording ->
            showPlaybackOptions(recording)
        }
        recordingsRecyclerView.adapter = recordingAdapter

        // Set up navigation to script selection when "Record New Video" button is clicked
        val recordNewButton = view.findViewById<Button>(R.id.button_recordNew)
        recordNewButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scriptSelectionFragment)
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
    
    private fun showPlaybackOptions(recording: Recording) {
        // Use the script ViewModel to get the script details
        scriptViewModel.getScriptById(recording.scriptId).observe(viewLifecycleOwner, Observer { script ->
            val scriptTitle = script?.title ?: "Unknown Script"
            
            AlertDialog.Builder(requireContext())
                .setTitle("Play \"$scriptTitle\"")
                .setPositiveButton("Play in App") { _, _ ->
                    playInApp(recording)
                }
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