package com.example.teleflow.fragments

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
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
import kotlin.math.roundToInt

class RecordFragment : Fragment() {

    private val TAG = "RecordFragment"
    
    // Script data
    private var scriptTitle: String = "Untitled Script"
    private var scriptContent: String = "No content provided"
    private var scriptId: Int = -1
    
    // UI components
    private lateinit var viewFinder: PreviewView
    private lateinit var scriptOverlayTextView: TextView
    private lateinit var scriptScrollView: ScrollView
    private lateinit var recordButton: FrameLayout
    private lateinit var recordButtonImage: ImageView
    private lateinit var recordButtonLabel: TextView
    private lateinit var timerTextView: TextView
    private lateinit var opacitySlider: SeekBar
    private lateinit var scrollSpeedSlider: SeekBar
    private lateinit var fontSizeIncreaseButton: Button
    private lateinit var fontSizeDecreaseButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var lastRecordingThumbnail: ImageView
    
    // Camera components
    private lateinit var cameraExecutor: ExecutorService
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    
    // Recording status
    private var isRecording = false
    
    // Timer
    private var recordingDurationSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    
    // Font size
    private var currentFontSize = 16f
    private val minFontSize = 12f
    private val maxFontSize = 24f
    
    // ViewModels
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    
    // Animation
    private lateinit var pulseAnimator: AnimatorSet
    
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
        return inflater.inflate(R.layout.fragment_record, container, false)
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
        scriptScrollView = view.findViewById(R.id.scrollView_script)
        recordButton = view.findViewById(R.id.button_record)
        recordButtonImage = view.findViewById(R.id.imageView_recordButton)
        recordButtonLabel = view.findViewById(R.id.textView_recordButtonLabel)
        timerTextView = view.findViewById(R.id.textView_timer)
        opacitySlider = view.findViewById(R.id.slider_opacity)
        scrollSpeedSlider = view.findViewById(R.id.slider_scrollSpeed)
        fontSizeIncreaseButton = view.findViewById(R.id.button_fontSizeIncrease)
        fontSizeDecreaseButton = view.findViewById(R.id.button_fontSizeDecrease)
        toolbar = view.findViewById(R.id.toolbar)
        lastRecordingThumbnail = view.findViewById(R.id.imageView_lastRecordingThumbnail)
        
        // Set up toolbar
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        
        // Display the script content in the overlay
        scriptOverlayTextView.text = "$scriptTitle\n\n$scriptContent"
        
        // Set up button click listener
        recordButton.setOnClickListener {
            toggleRecording()
        }
        
        // Set up opacity slider
        opacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Calculate opacity (from 0.2 to 0.8)
                val opacity = 0.2f + (progress / 100f * 0.6f)
                // Apply opacity to script background
                scriptScrollView.background.alpha = (opacity * 255).toInt()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Set up font size buttons
        fontSizeIncreaseButton.setOnClickListener {
            if (currentFontSize < maxFontSize) {
                currentFontSize += 2f
                scriptOverlayTextView.textSize = currentFontSize
            }
        }
        
        fontSizeDecreaseButton.setOnClickListener {
            if (currentFontSize > minFontSize) {
                currentFontSize -= 2f
                scriptOverlayTextView.textSize = currentFontSize
            }
        }
        
        // Setup scroll speed slider (autoscroll)
        scrollSpeedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Implement scroll speed logic here if needed
                // For example, set a tag with the scroll speed value
                scriptScrollView.tag = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Set up timer runnable
        timerRunnable = object : Runnable {
            override fun run() {
                recordingDurationSeconds++
                updateTimerDisplay()
                handler.postDelayed(this, 1000)
            }
        }
        
        // Set up pulse animation
        setupPulseAnimation()
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Load the last recording thumbnail if available
        loadLastRecordingThumbnail()
        
        // Set up the last recording thumbnail click listener
        lastRecordingThumbnail.setOnClickListener {
            findNavController().navigate(R.id.action_recordFragment_to_recordingsFragment)
        }
        
        // Request camera and audio permissions before starting the camera
        requestCameraPermissions()
    }
    
    private fun setupPulseAnimation() {
        // Scale X animation
        val scaleXAnimator = ObjectAnimator.ofFloat(recordButtonImage, "scaleX", 1f, 1.1f)
        scaleXAnimator.duration = 800
        scaleXAnimator.repeatCount = ObjectAnimator.INFINITE
        scaleXAnimator.repeatMode = ObjectAnimator.REVERSE
        
        // Scale Y animation
        val scaleYAnimator = ObjectAnimator.ofFloat(recordButtonImage, "scaleY", 1f, 1.1f)
        scaleYAnimator.duration = 800
        scaleYAnimator.repeatCount = ObjectAnimator.INFINITE
        scaleYAnimator.repeatMode = ObjectAnimator.REVERSE
        
        // Combine animations
        pulseAnimator = AnimatorSet()
        pulseAnimator.interpolator = AccelerateDecelerateInterpolator()
        pulseAnimator.playTogether(scaleXAnimator, scaleYAnimator)
    }
    
    private fun loadLastRecordingThumbnail() {
        recordingViewModel.allRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings.isNotEmpty()) {
                val latestRecording = recordings.maxByOrNull { it.date }
                latestRecording?.let { recording ->
                    try {
                        val uri = Uri.parse(recording.videoUri)
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(requireContext(), uri)
                        val bitmap = retriever.getFrameAtTime(0)
                        bitmap?.let {
                            lastRecordingThumbnail.setImageBitmap(bitmap)
                            lastRecordingThumbnail.visibility = View.VISIBLE
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load thumbnail: ${e.message}")
                    }
                }
            }
        })
    }
    
    private fun updateTimerDisplay() {
        val minutes = recordingDurationSeconds / 60
        val seconds = recordingDurationSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        timerTextView.text = timeString
    }
    
    private fun requestCameraPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        // For Android 10 and below, we need WRITE_EXTERNAL_STORAGE for saving videos
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
                        updateRecordingUI(true)
                    }
                    is VideoRecordEvent.Finalize -> {
                        recordButton.isEnabled = true
                        updateRecordingUI(false)
                        
                        if (!recordEvent.hasError()) {
                            // Recording completed successfully
                            val videoUri = recordEvent.outputResults.outputUri
                            
                            Log.d(TAG, "Video saved successfully at: ${videoUri}")
                            
                            // Store the recording using the ViewModel
                            recordingViewModel.addNewRecording(
                                scriptId,
                                videoUri.toString()
                            )
                            
                            // Update thumbnail with the new recording
                            try {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(requireContext(), videoUri)
                                val bitmap = retriever.getFrameAtTime(0)
                                bitmap?.let {
                                    lastRecordingThumbnail.setImageBitmap(bitmap)
                                    lastRecordingThumbnail.visibility = View.VISIBLE
                                }
                                retriever.release()
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to create thumbnail: ${e.message}")
                            }
                            
                            // Show success message with the script title
                            scriptViewModel.getScriptById(scriptId).observe(viewLifecycleOwner, Observer { script ->
                                val scriptName = script?.title ?: "Unknown Script"
                                
                                Toast.makeText(
                                    requireContext(),
                                    "Recording saved: $scriptName",
                                    Toast.LENGTH_LONG
                                ).show()
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
    
    private fun updateRecordingUI(isRecording: Boolean) {
        this.isRecording = isRecording
        
        if (isRecording) {
            // Update button appearance
            recordButtonImage.setBackgroundResource(R.drawable.record_button_recording)
            recordButtonLabel.text = "Stop Recording"
            
            // Start timer
            recordingDurationSeconds = 0
            timerTextView.visibility = View.VISIBLE
            updateTimerDisplay()
            handler.post(timerRunnable)
            
            // Start pulse animation
            pulseAnimator.start()
        } else {
            // Update button appearance
            recordButtonImage.setBackgroundResource(R.drawable.record_button_idle)
            recordButtonLabel.text = "Start Recording"
            
            // Stop timer
            handler.removeCallbacks(timerRunnable)
            timerTextView.visibility = View.GONE
            
            // Stop pulse animation
            pulseAnimator.cancel()
            recordButtonImage.scaleX = 1f
            recordButtonImage.scaleY = 1f
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop any ongoing recording if the fragment is paused
        if (isRecording) {
            recording?.stop()
            recording = null
            updateRecordingUI(false)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Shut down camera executor
        cameraExecutor.shutdown()
        
        // Make sure to stop any ongoing recording when the fragment is destroyed
        recording?.stop()
        recording = null
        
        // Remove timer callbacks
        handler.removeCallbacks(timerRunnable)
    }
} 