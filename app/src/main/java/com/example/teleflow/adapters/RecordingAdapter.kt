package com.example.teleflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    private val recordings: MutableList<Recording>,
    private val onItemClick: (Recording) -> Unit,
    private val onItemLongClick: ((Recording, View) -> Unit)? = null,
    private val getScript: (Int) -> LiveData<Script>? = { null },
    private val lifecycleOwner: LifecycleOwner? = null
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textView_recordingTitle)
        val dateTextView: TextView = itemView.findViewById(R.id.textView_recordingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]
        
        // Set default title in case we can't load the script
        holder.titleTextView.text = "Recording"
        
        // Try to get the script title if possible
        if (lifecycleOwner != null) {
            getScript(recording.scriptId)?.observe(lifecycleOwner, Observer { script ->
                holder.titleTextView.text = script?.title ?: "Unknown Script"
            })
        }
        
        // Format date with 12-hour clock format and AM/PM
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(recording.date))
        holder.dateTextView.text = formattedDate
        
        holder.itemView.setOnClickListener {
            onItemClick(recording)
        }
        
        // Set up long-click listener if provided
        onItemLongClick?.let { longClickHandler ->
            holder.itemView.setOnLongClickListener { view ->
                longClickHandler(recording, view)
                true // Return true to indicate the long press was handled
            }
        }
    }

    override fun getItemCount(): Int = recordings.size
    
    // Method to update data in the adapter
    fun updateData(newRecordings: List<Recording>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }
} 