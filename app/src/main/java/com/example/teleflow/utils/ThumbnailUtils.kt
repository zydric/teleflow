package com.example.teleflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailUtils {
    private const val TAG = "ThumbnailUtils"
    
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val thumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun getThumbnail(
        context: Context, 
        videoUri: String,
        timeUs: Long = 1000000
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val cachedThumbnail = thumbnailCache.get(videoUri)
            if (cachedThumbnail != null) {
                return@withContext cachedThumbnail
            }
            
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(videoUri))
                
                var bitmap = retriever.getFrameAtTime(
                    timeUs, 
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                if (bitmap == null) {
                    bitmap = retriever.getFrameAtTime()
                }
                
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

    fun clearCache() {
        thumbnailCache.evictAll()
    }
} 