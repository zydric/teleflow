package com.example.teleflow.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.teleflow.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    
    // Constants
    private const val PREFS_NAME = "TeleFlowPrefs"
    private const val PROFILE_IMAGE_KEY_PREFIX = "profile_image_file_user_"
    private const val IMAGES_DIR = "profile_images"
    private const val IMAGE_QUALITY = 90
    private const val MAX_IMAGE_DIMENSION = 1000
    
    /**
     * Launch the image picker to select a photo from gallery
     */
    fun pickImageFromGallery(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        launcher.launch(intent)
    }
    
    /**
     * Save the profile image to internal storage and preferences
     */
    fun saveProfileImage(context: Context, imageUri: Uri?): Boolean {
        if (imageUri == null) return false
        
        try {
            // Get current user ID
            val authManager = com.example.teleflow.auth.AuthManager(context)
            val userId = authManager.getCurrentUserId() ?: return false
            
            // Create image directory if it doesn't exist
            val userImagesDir = File(context.filesDir, "$IMAGES_DIR/$userId").apply {
                if (!exists()) mkdirs()
            }
            
            // Create a unique file name based on timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFile = File(userImagesDir, "profile_${timestamp}.jpg")
            
            // Get the bitmap from the URI
            val bitmap = getBitmapFromUri(context, imageUri) ?: return false
            
            // Save the bitmap to the file
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
            }
            
            // Save the file path to preferences with user-specific key
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("$PROFILE_IMAGE_KEY_PREFIX$userId", imageFile.absolutePath).apply()
            
            // Also update the user's database entry
            kotlinx.coroutines.runBlocking {
                val database = com.example.teleflow.data.TeleFlowDatabase.getDatabase(context)
                database.userDao().updateProfileImage(userId, imageFile.absolutePath)
            }
            
            return true
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error saving profile image", e)
            return false
        }
    }
    
    /**
     * Load the profile image and set it to the ImageView
     */
    fun loadProfileImage(context: Context, imageView: ImageView) {
        try {
            // Use AuthManager to get the current user's profile image path from the database
            val authManager = com.example.teleflow.auth.AuthManager(context)
            val userId = authManager.getCurrentUserId()
            
            if (userId != null) {
                // Get the user from the database directly since we need immediate access
                val database = com.example.teleflow.data.TeleFlowDatabase.getDatabase(context)
                val userDao = database.userDao()
                
                // Launch a coroutine to get the user data
                kotlinx.coroutines.runBlocking {
                    val user = userDao.getUserByIdSync(userId)
                    if (user != null && user.profileImagePath != null) {
                        // User has a profile image path stored in the database
                        val imageFile = File(user.profileImagePath)
                        if (imageFile.exists()) {
                            // Load and display the circular bitmap
                            val bitmap = BitmapFactory.decodeFile(user.profileImagePath)
                            val circularBitmap = getCircularBitmap(bitmap)
                            imageView.setImageBitmap(circularBitmap)
                            
                            // Clear any tint
                            imageView.clearColorFilter()
                            return@runBlocking
                        }
                    }
                    
                    // If no user profile image found in database, fall back to shared preferences with user-specific key
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val imagePath = prefs.getString("$PROFILE_IMAGE_KEY_PREFIX$userId", null)
                    
                    if (imagePath != null) {
                        // Load image from file
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            // Load and display the circular bitmap
                            val bitmap = BitmapFactory.decodeFile(imagePath)
                            val circularBitmap = getCircularBitmap(bitmap)
                            imageView.setImageBitmap(circularBitmap)
                            
                            // Also update the user record if possible
                            if (user != null) {
                                userDao.updateProfileImage(userId, imagePath)
                            }
                            
                            // Clear any tint
                            imageView.clearColorFilter()
                            return@runBlocking
                        }
                    }
                    
                    // If no saved image or error, show placeholder
                    imageView.setImageResource(R.drawable.profile_placeholder)
                    imageView.clearColorFilter()
                }
            } else {
                // No logged in user, show placeholder
                imageView.setImageResource(R.drawable.profile_placeholder)
                imageView.clearColorFilter()
            }
            
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading profile image", e)
            // Show placeholder on error
            imageView.setImageResource(R.drawable.profile_placeholder)
            imageView.clearColorFilter()
        }
    }
    
    /**
     * Load the profile image from a specified path and set it to the ImageView
     * This is used for loading user-specific profile images
     */
    fun loadProfileImage(context: Context, imageView: ImageView, customImagePath: String?) {
        try {
            if (customImagePath != null) {
                // Load image from the custom path
                val imageFile = File(customImagePath)
                if (imageFile.exists()) {
                    // Load and display the circular bitmap
                    val bitmap = BitmapFactory.decodeFile(customImagePath)
                    val circularBitmap = getCircularBitmap(bitmap)
                    imageView.setImageBitmap(circularBitmap)
                    
                    // Clear any tint
                    imageView.clearColorFilter()
                    return
                }
            }
            
            // If no custom image path provided or it doesn't exist, fall back to the default method
            loadProfileImage(context, imageView)
            
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading custom profile image", e)
            // Fall back to the default method
            loadProfileImage(context, imageView)
        }
    }
    
    /**
     * Get bitmap from URI with proper scaling
     */
    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Get bitmap dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // Calculate sample size to scale down large images
                var sampleSize = 1
                if (options.outWidth > MAX_IMAGE_DIMENSION || options.outHeight > MAX_IMAGE_DIMENSION) {
                    val widthRatio = Math.ceil(options.outWidth.toDouble() / MAX_IMAGE_DIMENSION).toInt()
                    val heightRatio = Math.ceil(options.outHeight.toDouble() / MAX_IMAGE_DIMENSION).toInt()
                    sampleSize = Math.max(widthRatio, heightRatio)
                }
                
                // Reopen stream and decode with sample size
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error getting bitmap from URI", e)
            null
        }
    }
    
    /**
     * Create a circular bitmap from a source bitmap
     */
    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(outputBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = 0xFFFFFFFF.toInt()
        }
        
        val radius = Math.min(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        
        // Apply source-in porter-duff mode
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return outputBitmap
    }
    
    /**
     * Clear saved profile image
     */
    fun clearProfileImage(context: Context) {
        // Get current user ID
        val authManager = com.example.teleflow.auth.AuthManager(context)
        val userId = authManager.getCurrentUserId() ?: return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val imagePath = prefs.getString("$PROFILE_IMAGE_KEY_PREFIX$userId", null)
        
        // Delete file if it exists
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            }
        }
        
        // Clear preference for this user only
        prefs.edit().remove("$PROFILE_IMAGE_KEY_PREFIX$userId").apply()
        
        // Also clear the database entry
        kotlinx.coroutines.runBlocking {
            val database = com.example.teleflow.data.TeleFlowDatabase.getDatabase(context)
            database.userDao().clearProfileImage(userId)
        }
    }
    
    /**
     * Create a circular bitmap directly from a URI
     * This is useful for showing preview images before saving
     */
    fun createCircularBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val bitmap = getBitmapFromUri(context, uri) ?: return null
        return getCircularBitmap(bitmap)
    }
} 