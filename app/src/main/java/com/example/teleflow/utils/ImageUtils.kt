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
    private const val PREFS_NAME = "TeleFlowPrefs"
    private const val PROFILE_IMAGE_KEY_PREFIX = "profile_image_file_user_"
    private const val IMAGES_DIR = "profile_images"
    private const val IMAGE_QUALITY = 90
    private const val MAX_IMAGE_DIMENSION = 1000

    fun pickImageFromGallery(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        launcher.launch(intent)
    }

    fun saveProfileImage(context: Context, imageUri: Uri?): Boolean {
        if (imageUri == null) return false
        
        try {
            val authManager = com.example.teleflow.auth.AuthManager(context)
            val userId = authManager.getCurrentUserId() ?: return false
            
            val userImagesDir = File(context.filesDir, "$IMAGES_DIR/$userId").apply {
                if (!exists()) mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFile = File(userImagesDir, "profile_${timestamp}.jpg")
            
            val bitmap = getBitmapFromUri(context, imageUri) ?: return false
            
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
            }
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("$PROFILE_IMAGE_KEY_PREFIX$userId", imageFile.absolutePath).apply()
            
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

    fun loadProfileImage(context: Context, imageView: ImageView) {
        try {
            val authManager = com.example.teleflow.auth.AuthManager(context)
            val userId = authManager.getCurrentUserId()
            
            if (userId != null) {
                val database = com.example.teleflow.data.TeleFlowDatabase.getDatabase(context)
                val userDao = database.userDao()
                
                kotlinx.coroutines.runBlocking {
                    val user = userDao.getUserByIdSync(userId)
                    if (user != null && user.profileImagePath != null) {
                        val imageFile = File(user.profileImagePath)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(user.profileImagePath)
                            val circularBitmap = getCircularBitmap(bitmap)
                            imageView.setImageBitmap(circularBitmap)
                            
                            imageView.clearColorFilter()
                            return@runBlocking
                        }
                    }
                    
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val imagePath = prefs.getString("$PROFILE_IMAGE_KEY_PREFIX$userId", null)
                    
                    if (imagePath != null) {
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imagePath)
                            val circularBitmap = getCircularBitmap(bitmap)
                            imageView.setImageBitmap(circularBitmap)
                            
                            if (user != null) {
                                userDao.updateProfileImage(userId, imagePath)
                            }
                            
                            imageView.clearColorFilter()
                            return@runBlocking
                        }
                    }
                    
                    imageView.setImageResource(R.drawable.profile_placeholder)
                    imageView.clearColorFilter()
                }
            } else {
                imageView.setImageResource(R.drawable.profile_placeholder)
                imageView.clearColorFilter()
            }
            
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading profile image", e)
            imageView.setImageResource(R.drawable.profile_placeholder)
            imageView.clearColorFilter()
        }
    }

    fun loadProfileImage(context: Context, imageView: ImageView, customImagePath: String?) {
        try {
            if (customImagePath != null) {
                val imageFile = File(customImagePath)
                if (imageFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(customImagePath)
                    val circularBitmap = getCircularBitmap(bitmap)
                    imageView.setImageBitmap(circularBitmap)
                    
                    imageView.clearColorFilter()
                    return
                }
            }
            
            loadProfileImage(context, imageView)
            
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading custom profile image", e)
            loadProfileImage(context, imageView)
        }
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                var sampleSize = 1
                if (options.outWidth > MAX_IMAGE_DIMENSION || options.outHeight > MAX_IMAGE_DIMENSION) {
                    val widthRatio = Math.ceil(options.outWidth.toDouble() / MAX_IMAGE_DIMENSION).toInt()
                    val heightRatio = Math.ceil(options.outHeight.toDouble() / MAX_IMAGE_DIMENSION).toInt()
                    sampleSize = Math.max(widthRatio, heightRatio)
                }
                
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
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return outputBitmap
    }

    fun clearProfileImage(context: Context) {
        val authManager = com.example.teleflow.auth.AuthManager(context)
        val userId = authManager.getCurrentUserId() ?: return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val imagePath = prefs.getString("$PROFILE_IMAGE_KEY_PREFIX$userId", null)
        
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            }
        }
        
        prefs.edit().remove("$PROFILE_IMAGE_KEY_PREFIX$userId").apply()
        
        kotlinx.coroutines.runBlocking {
            val database = com.example.teleflow.data.TeleFlowDatabase.getDatabase(context)
            database.userDao().clearProfileImage(userId)
        }
    }

    fun createCircularBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val bitmap = getBitmapFromUri(context, uri) ?: return null
        return getCircularBitmap(bitmap)
    }
} 