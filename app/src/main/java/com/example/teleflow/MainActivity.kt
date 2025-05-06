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
import com.example.teleflow.TeleFlowApplication
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    
    private lateinit var recordingsMenuItem: View
    private lateinit var settingsMenuItem: View
    private lateinit var aboutMenuItem: View
    private lateinit var logoutMenuItem: View
    
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var profileImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        drawerLayout = findViewById(R.id.drawer_layout)
        
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                refreshUserData()
                updateUserProfile()
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
        
        setupCustomDrawer()
        
        observeUserProfileChanges()
        
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment),
            drawerLayout
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.heading_color, theme)
        
        navController.addOnDestinationChangedListener(this)
        
        bottomNavigationView.setupWithNavController(navController)
        
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.scriptsFragment -> {
                    navController.navigate(R.id.scriptsFragment)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                R.id.nav_about -> {
                    navController.navigate(R.id.aboutDevelopersFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupCustomDrawer() {
        // Initialize menu items
        recordingsMenuItem = findViewById(R.id.nav_recordings_item)
        settingsMenuItem = findViewById(R.id.nav_settings_item)
        aboutMenuItem = findViewById(R.id.nav_about_item)
        logoutMenuItem = findViewById(R.id.nav_logout_item)
        
        userNameTextView = findViewById(R.id.user_name)
        userEmailTextView = findViewById(R.id.user_email)
        profileImageView = findViewById(R.id.profile_image)
        
        val headerLayout = findViewById<View>(R.id.nav_header)
        headerLayout.setOnClickListener {
            navController.navigate(R.id.profileFragment)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
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
            handleLogout()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        updateUserProfile()
    }
    
    private fun observeUserProfileChanges() {
        val authManager = (application as TeleFlowApplication).authManager
        authManager.currentUser.observe(this) { user ->
            if (user != null) {
                userNameTextView.text = user.fullName
                userEmailTextView.text = user.email
                
                com.example.teleflow.utils.ImageUtils.loadProfileImage(this, profileImageView, user.profileImagePath)
                profileImageView.setPadding(0, 0, 0, 0)
            }
        }
    }
    
    private fun updateUserProfile() {
        val authManager = (application as TeleFlowApplication).authManager
        
        refreshUserData()
        
        val currentUser = authManager.currentUser.value
        
        if (currentUser != null) {
            userNameTextView.text = currentUser.fullName
            userEmailTextView.text = currentUser.email
            
            com.example.teleflow.utils.ImageUtils.loadProfileImage(this, profileImageView, currentUser.profileImagePath)
            profileImageView.setPadding(0, 0, 0, 0)
        } else {
            userNameTextView.text = "Guest User"
            userEmailTextView.text = "Not logged in"
            
            com.example.teleflow.utils.ImageUtils.loadProfileImage(this, profileImageView)
            profileImageView.setPadding(0, 0, 0, 0)
        }
    }
    
    private fun refreshUserData() {
        val authManager = (application as TeleFlowApplication).authManager
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            lifecycleScope.launch {
                authManager.refreshUserData(userId)
            }
        }
    }
    
    private fun handleLogout() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                val authManager = (application as TeleFlowApplication).authManager
                authManager.logout()
                
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
        when (destination.id) {
            R.id.loginFragment, R.id.registerFragment -> {
                supportActionBar?.hide()
                bottomNavigationView.visibility = View.GONE
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
            R.id.homeFragment -> {
                setTheme(R.style.Theme_TeleFlow)
                
                supportActionBar?.title = "TeleFlow"
                supportActionBar?.show()
                bottomNavigationView.visibility = View.VISIBLE
                
                if (!::toggle.isInitialized) return
                
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
                supportActionBar?.hide()
                bottomNavigationView.visibility = View.GONE
                
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
        if (!::toggle.isInitialized) return
        
        drawerLayout.removeDrawerListener(toggle)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(null)
        
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
        when (item.itemId) {
            R.id.nav_recordings -> {
                navController.navigate(R.id.recordingsFragment)
            }
            R.id.nav_settings -> {
                navController.navigate(R.id.settingsFragment)
            }
            R.id.nav_about -> {
                navController.navigate(R.id.aboutDevelopersFragment)
            }
        }
        
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentDestination = navController.currentDestination?.id
        
        if (currentDestination == R.id.homeFragment && toggle.onOptionsItemSelected(item)) {
            return true
        } else if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        
        return super.onOptionsItemSelected(item)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?.childFragmentManager
                ?.fragments
                ?.firstOrNull()
                
            if (currentFragment is RecordFragment) {
                currentFragment.handleBackPressed()
            } else {
                super.onBackPressed()
                
                val currentDestId = navController.currentDestination?.id
                if (currentDestId != R.id.recordFragment) {
                    disableImmersiveMode()
                }
            }
        }
    }
    
    private fun enableImmersiveMode() {
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
        navController.removeOnDestinationChangedListener(this)
    }
    
    fun refreshNavigationDrawerUserData() {
        refreshUserData()
        updateUserProfile()
    }
}