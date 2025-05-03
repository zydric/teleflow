package com.example.teleflow

import android.os.Bundle
import android.view.MenuItem
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

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        
        // Set up NavigationView
            val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        
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
                else -> false
            }
        }
    }
    
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Set custom title based on the current destination
        when (destination.id) {
            R.id.homeFragment -> {
                supportActionBar?.title = "TeleFlow"
                
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
                handleNonHomeDestination()
            }
            R.id.profileFragment -> {
                supportActionBar?.title = "My Profile"
                handleNonHomeDestination()
            }
            R.id.recordingsFragment -> {
                supportActionBar?.title = "My Recordings"
                handleNonHomeDestination()
            }
            R.id.recordFragment -> {
                supportActionBar?.title = "Record Video"
                handleNonHomeDestination()
            }
            R.id.videoPlayerFragment -> {
                supportActionBar?.title = "Video Player"
                handleNonHomeDestination()
            }
            R.id.settingsFragment -> {
                supportActionBar?.title = "Settings"
                handleNonHomeDestination()
            }
            R.id.scriptEditorFragment -> {
                val isEditing = arguments?.getInt("scriptId", -1) != -1
                supportActionBar?.title = if (isEditing) "Edit Script" else "New Script"
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
                // Handle about click
                // For now, just close the drawer
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
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove the destination changed listener
        navController.removeOnDestinationChangedListener(this)
    }
}