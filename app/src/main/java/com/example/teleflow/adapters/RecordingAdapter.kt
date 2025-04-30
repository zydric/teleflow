package com.example.teleflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.models.Recording
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    private val recordings: MutableList<Recording>,
    private val onItemClick: (Recording) -> Unit
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
        
        // Format recording ID as a title
        holder.titleTextView.text = "Recording #${recording.id}"
        
        // Format date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(recording.date))
        holder.dateTextView.text = formattedDate
        
        holder.itemView.setOnClickListener {
            onItemClick(recording)
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