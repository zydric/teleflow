package com.example.teleflow

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.example.teleflow.fragments.RecordFragment
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.widget.ImageView

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    
    // Add these views for the custom drawer
    private lateinit var recordingsMenuItem: View
    private lateinit var settingsMenuItem: View
    private lateinit var aboutMenuItem: View
    private lateinit var logoutMenuItem: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        
        // Set drawer listener to refresh profile image when opened
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Not needed
            }

            override fun onDrawerOpened(drawerView: View) {
                // Refresh profile image when drawer is opened
                updateUserProfile()
            }

            override fun onDrawerClosed(drawerView: View) {
                // Not needed
            }

            override fun onDrawerStateChanged(newState: Int) {
                // Not needed
            }
        })
        
        // Set up custom drawer menu item clicks
        setupCustomDrawer()
        
        // Properly set up NavController using NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Set up bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        
        // Define only HomeFragment as top-level destination (with hamburger menu)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment),
            drawerLayout
        )
        
        // Set up ActionBar with NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Create ActionBarDrawerToggle for the hamburger icon
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        
        // Change toggle color to white
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.heading_color, theme)
        
        // Listen for destination changes to configure drawer toggle visibility
        navController.addOnDestinationChangedListener(this)
        
        // Set up bottom navigation with basic configuration
        bottomNavigationView.setupWithNavController(navController)
        
        // Add a manual listener to ensure navigation works from any destination
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Navigate to home fragment
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.scriptsFragment -> {
                    // Navigate to scripts fragment
                    navController.navigate(R.id.scriptsFragment)
                    true
                }
                R.id.profileFragment -> {
                    // Navigate to profile fragment
                    navController.navigate(R.id.profileFragment)
                    true
                }
                R.id.nav_about -> {
                    // Navigate to about developers fragment
                    navController.navigate(R.id.aboutDevelopersFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    // Setup custom drawer navigation
    private fun setupCustomDrawer() {
        // Initialize menu items
        recordingsMenuItem = findViewById(R.id.nav_recordings_item)
        settingsMenuItem = findViewById(R.id.nav_settings_item)
        aboutMenuItem = findViewById(R.id.nav_about_item)
        logoutMenuItem = findViewById(R.id.nav_logout_item)
        
        // Set up header click to navigate to profile
        val headerLayout = findViewById<View>(R.id.nav_header)
        headerLayout.setOnClickListener {
            navController.navigate(R.id.profileFragment)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        // Set click listeners for menu items
        recordingsMenuItem.setOnClickListener {
            navController.navigate(R.id.recordingsFragment)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        settingsMenuItem.setOnClickListener {
            navController.navigate(R.id.settingsFragment)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        aboutMenuItem.setOnClickListener {
            navController.navigate(R.id.aboutDevelopersFragment)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        logoutMenuItem.setOnClickListener {
            // Handle logout functionality
            handleLogout()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        // Update user profile in drawer header
        updateUserProfile()
    }
    
    // Update user profile information in the drawer header
    private fun updateUserProfile() {
        val userNameTextView = findViewById<TextView>(R.id.user_name)
        val userEmailTextView = findViewById<TextView>(R.id.user_email)
        val profileImageView = findViewById<ImageView>(R.id.profile_image)
        
        // In a real app, you would get this information from your user data repository
        // For now, we'll just use hardcoded values as specified in the requirements
        userNameTextView?.text = "John Anderson"
        userEmailTextView?.text = "john.anderson@example.com"
        
        // Load profile image using ImageUtils
        if (profileImageView != null) {
            com.example.teleflow.utils.ImageUtils.loadProfileImage(this, profileImageView)
            // Remove any padding that would be present for the icon
            profileImageView.setPadding(0, 0, 0, 0)
        }
    }
    
    // Handle logout
    private fun handleLogout() {
        // Show confirmation dialog before logging out
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Implementation of logout functionality
                // For example, clear user data and navigate to login screen
                
                // Navigate to login fragment
                navController.navigate(R.id.loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Set custom title based on the current destination
        when (destination.id) {
            R.id.loginFragment, R.id.registerFragment -> {
                // Hide action bar and bottom navigation on authentication screens
                supportActionBar?.hide()
                bottomNavigationView.visibility = View.GONE
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
            R.id.homeFragment -> {
                // Restore original theme if needed
                setTheme(R.style.Theme_TeleFlow)
                
                supportActionBar?.title = "TeleFlow"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                
                // Enable drawer toggle only on home fragment
                if (!::toggle.isInitialized) return
                
                // Set up drawer toggle for home screen
                drawerLayout.removeDrawerListener(toggle)
                drawerLayout.addDrawerListener(toggle)
                toggle.syncState()
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
            R.id.scriptsFragment -> {
                supportActionBar?.title = "My Scripts"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
            R.id.profileFragment -> {
                supportActionBar?.title = "My Profile"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
            R.id.recordingsFragment -> {
                supportActionBar?.title = "My Recordings"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
            R.id.recordFragment -> {
                // Apply NoActionBar theme for RecordFragment
                supportActionBar?.hide()
                bottomNavigationView.visibility = View.GONE
                
                // Enable immersive mode
                enableImmersiveMode()
                
                handleNonHomeDestination()
            }
            R.id.videoPlayerFragment -> {
                supportActionBar?.title = "Video Player"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
            R.id.settingsFragment -> {
                supportActionBar?.title = "Settings"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
            R.id.scriptEditorFragment -> {
                val isEditing = arguments?.getInt("scriptId", -1) != -1
                supportActionBar?.title = if (isEditing) "Edit Script" else "New Script"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
            R.id.aboutDevelopersFragment -> {
                supportActionBar?.title = "About Developers"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                handleNonHomeDestination()
            }
        }
    }
    
    private fun handleNonHomeDestination() {
        // For non-home destinations, disable the drawer toggle and lock the drawer
        if (!::toggle.isInitialized) return
        
        drawerLayout.removeDrawerListener(toggle)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(null) // Use default back arrow
        
        // Set back arrow color to white (use navigation_icon)
        val navigationIconId = resources.getIdentifier("home", "id", "android")
        if (navigationIconId != 0) {
            val navigationIcon = findViewById<View>(navigationIconId)
            navigationIcon?.post {
                val drawable = (supportActionBar?.themedContext?.getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material))
                drawable?.setTint(resources.getColor(R.color.heading_color, theme))
                supportActionBar?.setHomeAsUpIndicator(drawable)
            }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation drawer item clicks
        when (item.itemId) {
            R.id.nav_recordings -> {
                // Navigate to recordings fragment
                navController.navigate(R.id.recordingsFragment)
            }
            R.id.nav_settings -> {
                // Navigate to settings fragment
                navController.navigate(R.id.settingsFragment)
            }
            R.id.nav_about -> {
                // Navigate to about developers fragment
                navController.navigate(R.id.aboutDevelopersFragment)
            }
        }
        
        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks
        val currentDestination = navController.currentDestination?.id
        
        if (currentDestination == R.id.homeFragment && toggle.onOptionsItemSelected(item)) {
            // Let the drawer toggle handle it (only on home screen)
            return true
        } else if (item.itemId == android.R.id.home) {
            // Handle the back button press
            onBackPressed()
            return true
        }
        
        return super.onOptionsItemSelected(item)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onBackPressed() {
        // Close drawer if open, otherwise handle normal back button press
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Special handling for RecordFragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?.childFragmentManager
                ?.fragments
                ?.firstOrNull()
                
            if (currentFragment is RecordFragment) {
                currentFragment.handleBackPressed()
            } else {
                super.onBackPressed()
                
                // Check if we're returning from RecordFragment by looking at the current destination
                val currentDestId = navController.currentDestination?.id
                if (currentDestId != R.id.recordFragment) {
                    // If we're no longer on the record fragment, disable immersive mode
                    disableImmersiveMode()
                }
            }
        }
    }
    
    private fun enableImmersiveMode() {
        // For immersive fullscreen experience on RecordFragment
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
    
    private fun disableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove the destination changed listener
        navController.removeOnDestinationChangedListener(this)
    }
}