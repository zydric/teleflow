package com.example.teleflow.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.teleflow.models.Recording
import com.example.teleflow.viewmodels.RecordingViewModel
import com.example.teleflow.viewmodels.ScriptViewModel

class RecordingsFragment : Fragment() {

    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingAdapter: RecordingAdapter
    
    private val recordingViewModel: RecordingViewModel by viewModels()
    private val scriptViewModel: ScriptViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recordings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up recordings RecyclerView
        recordingsRecyclerView = view.findViewById(R.id.recyclerView_recordings_list)
        recordingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Create recording adapter with callbacks
        recordingAdapter = RecordingAdapter(
            mutableListOf(),
            onItemClick = { recording ->
                // Play the recording
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
        
        // Observe the recordings from ViewModel
        recordingViewModel.allRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            recordingAdapter.updateData(recordings)
            view.findViewById<View>(R.id.textView_no_recordings).visibility = 
                if (recordings.isEmpty()) View.VISIBLE else View.GONE
        })
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
        findNavController().navigate(R.id.action_recordingsFragment_to_videoPlayerFragment, bundle)
    }
} 