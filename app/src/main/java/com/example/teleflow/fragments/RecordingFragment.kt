package com.example.teleflow.fragments

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.viewmodels.RecordingViewModel
import com.example.teleflow.viewmodels.ScriptViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordingFragment : Fragment() {

    private val TAG = "RecordingFragment"
    
    // Script data
    private var scriptTitle: String = "Untitled Script"
    private var scriptContent: String = "No content provided"
    private var scriptId: Int = -1
    
    // UI components
    private lateinit var viewFinder: PreviewView
    private lateinit var scriptOverlayTextView: TextView
    private lateinit var recordButton: Button
    private lateinit var cameraExecutor: ExecutorService
    
    // Camera components
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    
    // Recording status
    private var isRecording = false
    
    // ViewModels
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    
    // Permission handler
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions result
        val granted = permissions.entries.all { it.value }
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera and audio permissions are required for recording",
                Toast.LENGTH_LONG
            ).show()
            // Navigate back since we can't continue without permissions
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recording, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the script data from arguments
        arguments?.let { args ->
            scriptTitle = args.getString("scriptTitle") ?: "Untitled Script"
            scriptContent = args.getString("scriptContent") ?: "No content provided"
            scriptId = args.getInt("scriptId", -1)
        }

        // Initialize UI components
        viewFinder = view.findViewById(R.id.previewView)
        scriptOverlayTextView = view.findViewById(R.id.textView_scriptOverlay)
        recordButton = view.findViewById(R.id.button_recordToggle)
        
        // Display the script content in the overlay
        scriptOverlayTextView.text = "$scriptTitle\n\n$scriptContent"
        
        // Set up button click listener
        recordButton.setOnClickListener {
            toggleRecording()
        }
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Request camera and audio permissions before starting the camera
        requestCameraPermissions()
    }
    
    private fun requestCameraPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        // For Android 10 and higher, we need WRITE_EXTERNAL_STORAGE for saving videos
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        requestPermissions.launch(permissions.toTypedArray())
    }
    
    private fun startCamera() {
        // Hide the placeholder text now that we're starting the camera
        val placeholder = view?.findViewById<TextView>(R.id.textView_placeholder)
        placeholder?.visibility = View.GONE
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            // Set up the preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            
            // Set up video capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, 
                    cameraSelector, 
                    preview, 
                    videoCapture
                )
                
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(
                    requireContext(),
                    "Failed to start camera",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun toggleRecording() {
        val videoCapture = this.videoCapture ?: return
        
        if (isRecording) {
            // Stop the current recording
            recordButton.isEnabled = false
            recording?.stop()
            recording = null
            isRecording = false
            return
        }
        
        // Create a unique name for the recording
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())
        
        // Configure MediaStore output options
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TeleFlow")
            }
        }
        
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireActivity().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        
        // Prepare video capture and start recording
        recording = videoCapture.output
            .prepareRecording(requireActivity(), mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        requireContext(), 
                        Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // Recording started - update UI
                        isRecording = true
                        recordButton.text = "Stop Recording"
                    }
                    is VideoRecordEvent.Finalize -> {
                        recordButton.isEnabled = true
                        recordButton.text = "Start Recording"
                        isRecording = false
                        
                        if (!recordEvent.hasError()) {
                            // Recording completed successfully
                            val videoUri = recordEvent.outputResults.outputUri
                            
                            Log.d(TAG, "Video saved successfully at: ${videoUri}")
                            
                            // Store the recording using the ViewModel
                            recordingViewModel.addNewRecording(
                                scriptId,
                                videoUri.toString()
                            )
                            
                            // Show success message with the script title
                            scriptViewModel.getScriptById(scriptId).observe(viewLifecycleOwner, Observer { script ->
                                val scriptName = script?.title ?: "Unknown Script"
                                
                                Toast.makeText(
                                    requireContext(),
                                    "Recording saved: $scriptName",
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // Return to home fragment
                                findNavController().navigate(R.id.action_recordingFragment_to_homeFragment)
                            })
                        } else {
                            // Recording failed
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture failed: ${recordEvent.error}")
                            Toast.makeText(
                                requireContext(), 
                                "Video recording failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Shut down camera executor
        cameraExecutor.shutdown()
        
        // Make sure to stop any ongoing recording when the fragment is destroyed
        recording?.stop()
        recording = null
    }
} 