package com.example.teleflow

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.teleflow.fragments.RecordFragment

class RecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply no action bar theme before onCreate
        setTheme(R.style.Theme_TeleFlow_NoActionBar)
        
        // Use a smooth entry transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        
        super.onCreate(savedInstanceState)
        
        // Set content view
        setContentView(R.layout.activity_record)
        
        // Force hide action bar and ensure it stays hidden
        supportActionBar?.hide()
        
        // Set window flags for immersive mode
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Set immersive mode
        enableImmersiveMode()
        
        // Get data from intent
        val scriptId = intent.getIntExtra("scriptId", -1)
        val scriptTitle = intent.getStringExtra("scriptTitle") ?: "Untitled Script"
        val scriptContent = intent.getStringExtra("scriptContent") ?: "No content"
        
        // Create RecordFragment with arguments
        val fragment = RecordFragment().apply {
            arguments = Bundle().apply {
                putInt("scriptId", scriptId)
                putString("scriptTitle", scriptTitle)
                putString("scriptContent", scriptContent)
            }
        }
        
        // Add fragment to container
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun enableImmersiveMode() {
        // For immersive fullscreen experience
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For earlier Android versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }
    
    override fun onBackPressed() {
        // Get the current fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        // If it's a RecordFragment, let it handle the back press
        if (currentFragment is RecordFragment) {
            // Release resources before closing
            currentFragment.prepareForBackNavigation()
            // Use a custom animation for activity finish
            finishWithAnimation()
        } else {
            finishWithAnimation()
        }
    }
    
    // Make this method public so the fragment can call it
    fun finishWithAnimation() {
        // Clean up resources
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Set flag to not animate
        overridePendingTransition(0, 0)
        
        // Finish activity
        finish()
        
        // Apply fade animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
} 