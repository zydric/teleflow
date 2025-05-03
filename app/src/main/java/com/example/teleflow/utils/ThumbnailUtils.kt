package com.example.teleflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for efficient thumbnail generation from video files
 */
object ThumbnailUtils {
    private const val TAG = "ThumbnailUtils"
    
    // Cache size is 1/8th of available memory, capped at 20MB
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    // LRU Cache for thumbnails, will automatically evict least recently used items when memory is low
    private val thumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // Size in kilobytes
            return bitmap.byteCount / 1024
        }
    }
    
    /**
     * Gets a thumbnail for a video, either from cache or by generating a new one
     * 
     * @param context The context
     * @param videoUri The video URI to extract thumbnail from
     * @param timeUs The time position in microseconds (default: 1 second)
     * @return Bitmap thumbnail or null if extraction failed
     */
    suspend fun getThumbnail(
        context: Context, 
        videoUri: String,
        timeUs: Long = 1000000
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cachedThumbnail = thumbnailCache.get(videoUri)
            if (cachedThumbnail != null) {
                return@withContext cachedThumbnail
            }
            
            // Generate thumbnail
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(videoUri))
                
                // Try to get a frame at the requested position
                var bitmap = retriever.getFrameAtTime(
                    timeUs, 
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                // If that didn't work, try to get any frame
                if (bitmap == null) {
                    bitmap = retriever.getFrameAtTime()
                }
                
                // Cache the thumbnail
                bitmap?.let { 
                    thumbnailCache.put(videoUri, it)
                }
                
                return@withContext bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting thumbnail", e)
                return@withContext null
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in thumbnail extraction", e)
            return@withContext null
        }
    }
    
    /**
     * Clears the thumbnail cache
     */
    fun clearCache() {
        thumbnailCache.evictAll()
    }
} 