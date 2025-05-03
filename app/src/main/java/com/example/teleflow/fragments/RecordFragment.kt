package com.example.teleflow.fragments

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
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
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import com.example.teleflow.RecordActivity
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
    private lateinit var timerTextView: TextView
    private lateinit var backButton: FrameLayout
    private lateinit var mediaButton: FrameLayout
    
    // Control components
    private lateinit var opacitySlider: SeekBar
    private lateinit var fontSizeIncreaseButton: ImageButton
    private lateinit var fontSizeDecreaseButton: ImageButton
    private lateinit var scrollSpeedSlider: SeekBar
    private lateinit var fontSizeTextView: TextView
    
    // Camera components
    private lateinit var cameraExecutor: ExecutorService
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var isFrontCamera = true // Track which camera is currently in use
    
    // Recording status
    private var isRecording = false
    
    // Timer
    private var recordingDurationSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    
    // Font size
    private var currentFontSize = 18f
    private val minFontSize = 12f
    private val maxFontSize = 24f
    
    // Auto scroll
    private var isAutoScrolling = false
    private var scrollSpeed = 50 // Default value
    private lateinit var autoScrollRunnable: Runnable
    
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply the NoActionBar theme
        activity?.setTheme(R.style.Theme_TeleFlow_NoActionBar)
        
        // Force hide action bar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        // Set immersive mode flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
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

        // Set window flags for immersive mode
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Ensure we have immersive mode
        setImmersiveMode()

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
        timerTextView = view.findViewById(R.id.textView_timer)
        backButton = view.findViewById(R.id.button_back)
        mediaButton = view.findViewById(R.id.button_media)
        
        // Control components
        opacitySlider = view.findViewById(R.id.slider_opacity)
        fontSizeIncreaseButton = view.findViewById(R.id.button_fontSizeIncrease)
        fontSizeDecreaseButton = view.findViewById(R.id.button_fontSizeDecrease)
        scrollSpeedSlider = view.findViewById(R.id.slider_scrollSpeed)
        
        // Set the font size text view (we'll use the current size)
        fontSizeTextView = TextView(context)
        fontSizeTextView.text = currentFontSize.toInt().toString()
        
        // Display the script content in the overlay
        scriptOverlayTextView.text = "$scriptTitle\n\n$scriptContent"
        
        // Set initial font size
        scriptOverlayTextView.textSize = currentFontSize
        
        // Set up button click listeners
        setupClickListeners()
        
        // Set up slider listeners
        setupSliderListeners()
        
        // Set up timer runnable
        timerRunnable = object : Runnable {
            override fun run() {
                recordingDurationSeconds++
                updateTimerDisplay()
                handler.postDelayed(this, 1000)
            }
        }
        
        // Set up auto-scroll runnable
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isAutoScrolling && isRecording) {
                    // Calculate scroll speed based on slider value
                    // Higher value = faster scrolling
                    val scrollAmount = (scrollSpeed / 10) + 1
                    scriptScrollView.smoothScrollBy(0, scrollAmount)
                }
                handler.postDelayed(this, 50) // Adjust timing for smoother scrolling
            }
        }
        
        // Set up pulse animation
        setupPulseAnimation()
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Request camera and audio permissions before starting the camera
        requestCameraPermissions()
    }
    
    private fun setupClickListeners() {
        // Record button click listener
        recordButton.setOnClickListener {
            toggleRecording()
        }
        
        // Back button click listener with confirmation dialog
        backButton.setOnClickListener {
            showBackConfirmationDialog()
        }
        
        // Media/Gallery button click listener
        mediaButton.setOnClickListener {
            // If we're recording, don't allow camera switch
            if (isRecording) {
                Toast.makeText(context, "Cannot switch camera while recording", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Toggle between front and back camera
            isFrontCamera = !isFrontCamera
            startCamera()
            
            Toast.makeText(
                context, 
                "Switched to ${if(isFrontCamera) "front" else "back"} camera", 
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Font size adjustment buttons
        fontSizeIncreaseButton.setOnClickListener {
            if (currentFontSize < maxFontSize) {
                currentFontSize += 2f
                scriptOverlayTextView.textSize = currentFontSize
                fontSizeTextView.text = currentFontSize.toInt().toString()
            }
        }
        
        fontSizeDecreaseButton.setOnClickListener {
            if (currentFontSize > minFontSize) {
                currentFontSize -= 2f
                scriptOverlayTextView.textSize = currentFontSize
                fontSizeTextView.text = currentFontSize.toInt().toString()
            }
        }
    }
    
    private fun setupSliderListeners() {
        // Opacity slider
        opacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Calculate opacity (from 0.1 to 0.9)
                val opacity = 0.1f + (progress / 100f * 0.8f)
                // Apply opacity to script background
                scriptScrollView.background.alpha = (opacity * 255).toInt()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Speed slider
        scrollSpeedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scrollSpeed = progress
                
                // If recording, update scroll speed in real-time
                if (isRecording && !isAutoScrolling) {
                    isAutoScrolling = true
                    handler.post(autoScrollRunnable)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun showBackConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Go Back")
            .setMessage("Are you sure you want to go back? Recording will stop.")
            .setPositiveButton("Yes") { _, _ ->
                // Stop any ongoing recording
                if (isRecording) {
                    recording?.stop()
                    recording = null
                }
                
                // Clean up resources
                prepareForBackNavigation()
                
                // If in RecordActivity, finish with animation
                if (activity is RecordActivity) {
                    (activity as RecordActivity).finishWithAnimation()
                } else {
                    // Otherwise, use regular navigation
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("No", null)
            .show()
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
            
            // Select camera based on the current state
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            
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
            recordButtonImage.isEnabled = false
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
                        recordButtonImage.isEnabled = true
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
                            
                            // Update script lastUsed time through ViewModel
                            if (scriptId != -1) {
                                scriptViewModel.updateScriptLastUsed(scriptId)
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
            
            // Start timer
            recordingDurationSeconds = 0
            timerTextView.visibility = View.VISIBLE
            updateTimerDisplay()
            handler.post(timerRunnable)
            
            // Start auto-scrolling if enabled
            isAutoScrolling = true
            handler.post(autoScrollRunnable)
            
            // Start pulse animation
            pulseAnimator.start()
        } else {
            // Update button appearance
            recordButtonImage.setBackgroundResource(R.drawable.record_button_idle)
            
            // Stop timer
            handler.removeCallbacks(timerRunnable)
            timerTextView.visibility = View.GONE
            
            // Stop auto-scrolling
            isAutoScrolling = false
            handler.removeCallbacks(autoScrollRunnable)
            
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
    
    // Method called by MainActivity when back button is pressed
    fun handleBackPressed() {
        // Show confirmation dialog
        showBackConfirmationDialog()
    }
    
    // Method to prepare fragment for back navigation
    fun prepareForBackNavigation() {
        // Stop any ongoing recording if active
        if (isRecording) {
            recording?.stop()
            recording = null
            updateRecordingUI(false)
        }
        
        // Release camera resources
        try {
            cameraExecutor.shutdown()
            
            // Remove callbacks to prevent memory leaks
            handler.removeCallbacks(timerRunnable)
            handler.removeCallbacks(autoScrollRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources: ${e.message}")
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
        handler.removeCallbacks(autoScrollRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Clear fullscreen flags when leaving
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    private fun setImmersiveMode() {
        // Make sure action bar is hidden
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        // This ensures that the fragment's layout takes advantage of the full screen
        // by setting the appropriate insets and padding
        view?.setOnApplyWindowInsetsListener { v, insets ->
            // Adjust layout for immersive mode
            val params = scriptScrollView.layoutParams as ViewGroup.MarginLayoutParams
            // Apply top inset as margin/padding to ensure the text is visible below status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.topMargin = insets.getInsets(WindowInsets.Type.systemBars()).top
            } else {
                @Suppress("DEPRECATION")
                params.topMargin = insets.systemWindowInsetTop
            }
            scriptScrollView.layoutParams = params
            
            // Return the insets
            insets
        }
        
        // Set full immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(false)
            activity?.window?.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure immersive mode is applied when the fragment resumes
        setImmersiveMode()
        
        // Make sure action bar stays hidden
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }
} 