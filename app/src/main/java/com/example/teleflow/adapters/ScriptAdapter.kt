package com.example.teleflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.models.Script

class ScriptAdapter(
    private val scripts: MutableList<Script>,
    private val onItemClick: (Script) -> Unit,
    private val onItemLongClick: ((Script) -> Unit)? = null
) : RecyclerView.Adapter<ScriptAdapter.ScriptViewHolder>() {

    class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textView_scriptTitle)
        val contentTextView: TextView = itemView.findViewById(R.id.textView_scriptPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scripts[position]
        holder.titleTextView.text = script.title
        
        // Show a preview of the content (first 50 characters)
        val previewContent = if (script.content.length > 50) {
            script.content.substring(0, 50) + "..."
        } else {
            script.content
        }
        holder.contentTextView.text = previewContent
        
        // Set up click listener
        holder.itemView.setOnClickListener {
            onItemClick(script)
        }
        
        // Set up long-click listener if provided
        onItemLongClick?.let { longClickHandler ->
            holder.itemView.setOnLongClickListener {
                longClickHandler(script)
                true // Return true to indicate the long press was handled
            }
        }
    }

    override fun getItemCount(): Int = scripts.size
    
    // Method to update data in the adapter
    fun updateData(newScripts: List<Script>) {
        scripts.clear()
        scripts.addAll(newScripts)
        notifyDataSetChanged()
    }
} 