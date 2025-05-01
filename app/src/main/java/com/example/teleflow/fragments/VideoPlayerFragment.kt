package com.example.teleflow.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.models.Recording
import com.example.teleflow.viewmodels.ScriptViewModel
import com.example.teleflow.viewmodels.RecordingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoPlayerFragment : Fragment() {

    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var scriptInfoText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var deleteButton: ImageButton
    
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    
    // Default values in case arguments are missing
    private var recordingId: Int = -1
    private var videoUri: String = ""
    private var scriptId: Int = -1
    private var date: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get recording data from arguments
        arguments?.let { args ->
            recordingId = args.getInt("recordingId", -1)
            videoUri = args.getString("videoUri", "")
            scriptId = args.getInt("scriptId", -1)
            date = args.getLong("date", System.currentTimeMillis())
        }

        // Initialize UI components
        videoView = view.findViewById(R.id.videoView)
        progressBar = view.findViewById(R.id.progressBar)
        scriptInfoText = view.findViewById(R.id.textView_scriptInfo)
        backButton = view.findViewById(R.id.button_back)
        deleteButton = view.findViewById(R.id.button_delete)
        
        // Set up back button
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Set up delete button
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        // Display script info using ViewModel
        updateScriptInfo()
        
        // Set up video playback
        setupVideoPlayback()
    }
    
    private fun updateScriptInfo() {
        scriptViewModel.getScriptById(scriptId).observe(viewLifecycleOwner, Observer { script ->
            val scriptTitle = script?.title ?: "Unknown Script"
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(date))
            
            scriptInfoText.text = "$scriptTitle - $formattedDate"
        })
    }
    
    private fun showDeleteConfirmationDialog() {
        // Get script information to show in dialog
        scriptViewModel.getScriptById(scriptId).observe(viewLifecycleOwner, Observer { script ->
            val scriptTitle = script?.title ?: "Unknown Script"
            
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to delete this recording of \"$scriptTitle\"? The recording will be removed from the app but will remain in your device storage.")
                .setPositiveButton("Delete") { _, _ ->
                    // Delete the recording from the database
                    if (recordingId != -1) {
                        recordingViewModel.getRecordingById(recordingId).observe(viewLifecycleOwner, Observer { recording ->
                            recording?.let {
                                deleteRecording(it)
                            }
                        })
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        })
    }
    
    private fun deleteRecording(recording: Recording) {
        recordingViewModel.deleteRecording(recording)
        Toast.makeText(requireContext(), "Recording deleted", Toast.LENGTH_SHORT).show()
        // Navigate back to home screen after deleting
        findNavController().popBackStack()
    }
    
    private fun setupVideoPlayback() {
        try {
            // Show loading indicator
            progressBar.visibility = View.VISIBLE
            
            // Set up video URI
            val uri = android.net.Uri.parse(videoUri)
            videoView.setVideoURI(uri)
            
            // Set up media player listeners
            videoView.setOnPreparedListener { mediaPlayer ->
                // Hide loading indicator
                progressBar.visibility = View.GONE
                
                // Start playback
                mediaPlayer.start()
                
                // Optional: Loop the video
                mediaPlayer.isLooping = false
            }
            
            videoView.setOnErrorListener { _, what, extra ->
                // Handle errors
                progressBar.visibility = View.GONE
                
                Toast.makeText(
                    requireContext(),
                    "Error playing video: $what, $extra",
                    Toast.LENGTH_SHORT
                ).show()
                
                true // Return true to indicate error handled
            }
            
            videoView.setOnCompletionListener {
                // Return to previous screen when video completes
                // findNavController().popBackStack()
            }
            
        } catch (e: Exception) {
            // Handle any exceptions
            progressBar.visibility = View.GONE
            
            Toast.makeText(
                requireContext(),
                "Error loading video: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Pause video playback when fragment is paused
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Resume video playback when fragment is resumed
        if (!videoView.isPlaying) {
            videoView.start()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Release resources
        videoView.stopPlayback()
    }
} 