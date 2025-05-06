package com.example.teleflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import com.example.teleflow.utils.ThumbnailUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    private val recordings: MutableList<Recording>,
    private val onItemClick: (Recording) -> Unit,
    private val onItemLongClick: ((Recording, View) -> Unit)? = null,
    private val getScript: (Int) -> LiveData<Script>? = { null },
    private val lifecycleOwner: LifecycleOwner? = null,
    private val useGridLayout: Boolean = false
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.imageView_thumbnail)
        val titleTextView: TextView = itemView.findViewById(R.id.textView_recordingTitle)
        val dateTextView: TextView = itemView.findViewById(R.id.textView_recordingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val layoutResId = if (useGridLayout) {
            R.layout.item_recording_grid
        } else {
            R.layout.item_recording
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]

        holder.titleTextView.text = "Recording"

        if (lifecycleOwner != null) {
            getScript(recording.scriptId)?.observe(lifecycleOwner, Observer { script ->
                holder.titleTextView.text = script?.title ?: "Unknown Script"
            })
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(recording.date))
        holder.dateTextView.text = formattedDate

        loadThumbnail(recording.videoUri, holder.thumbnailImageView)
        
        holder.itemView.setOnClickListener {
            onItemClick(recording)
        }

        onItemLongClick?.let { longClickHandler ->
            holder.itemView.setOnLongClickListener { view ->
                longClickHandler(recording, view)
                true
            }
        }
    }
    
    private fun loadThumbnail(videoUri: String, imageView: ImageView) {
        imageView.setImageResource(R.drawable.ic_video_placeholder)

        coroutineScope.launch {
            try {
                val bitmap = ThumbnailUtils.getThumbnail(
                    imageView.context,
                    videoUri
                )

                bitmap?.let {
                    imageView.setImageBitmap(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = recordings.size

    fun updateData(newRecordings: List<Recording>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    fun clearCache() {
        ThumbnailUtils.clearCache()
    }
} 