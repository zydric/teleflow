package com.example.teleflow

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.teleflow.fragments.RecordFragment

class RecordActivity : AppCompatActivity() {
    
    private val TAG = "RecordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_TeleFlow_NoActionBar)
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_record)
        
        supportActionBar?.hide()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        
        enableImmersiveMode()
        
        try {
            val scriptId = intent.getIntExtra("scriptId", -1)
            val scriptTitle = intent.getStringExtra("scriptTitle") ?: "Untitled Script"
            val scriptContent = intent.getStringExtra("scriptContent") ?: "No content"
            
            Log.d(TAG, "Launching RecordActivity with scriptId: $scriptId, title: $scriptTitle")
            
            val fragment = RecordFragment().apply {
                arguments = Bundle().apply {
                    putInt("scriptId", scriptId)
                    putString("scriptTitle", scriptTitle)
                    putString("scriptContent", scriptContent)
                }
            }
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error launching RecordActivity", e)
            finish()
        }
    }
    
    private fun enableImmersiveMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting immersive mode", e)
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }
    
    override fun onBackPressed() {
        try {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            
            if (currentFragment is RecordFragment) {
                currentFragment.prepareForBackNavigation()
                finishWithAnimation()
            } else {
                finishWithAnimation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onBackPressed", e)
            finish()
        }
    }
    
    fun finishWithAnimation() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
            
            overridePendingTransition(0, 0)
            
            finish()
            
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e(TAG, "Error in finishWithAnimation", e)
            finish()
        }
    }
} 