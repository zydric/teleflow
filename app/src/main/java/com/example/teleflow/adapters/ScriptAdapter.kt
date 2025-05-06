package com.example.teleflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.models.Script

class ScriptAdapter(
    private val scripts: MutableList<Script>,
    private val onItemClick: (Script) -> Unit,
    private val onItemLongClick: ((Script, View) -> Unit)? = null
) : RecyclerView.Adapter<ScriptAdapter.ScriptViewHolder>() {

    class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textView_scriptTitle)
        val contentTextView: TextView = itemView.findViewById(R.id.textView_scriptPreview)
        val moreOptionsImageView: ImageView = itemView.findViewById(R.id.imageView_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scripts[position]
        holder.titleTextView.text = script.title

        val previewContent = if (script.content.length > 50) {
            script.content.substring(0, 50) + "..."
        } else {
            script.content
        }
        holder.contentTextView.text = previewContent

        holder.itemView.setOnClickListener {
            onItemClick(script)
        }

        onItemLongClick?.let { longClickHandler ->
            holder.moreOptionsImageView.setOnClickListener { view ->
                longClickHandler(script, view)
            }
        }

        onItemLongClick?.let { longClickHandler ->
            holder.itemView.setOnLongClickListener { view ->
                longClickHandler(script, view)
                true
            }
        }
    }

    override fun getItemCount(): Int = scripts.size

    fun updateData(newScripts: List<Script>) {
        scripts.clear()
        scripts.addAll(newScripts)
        notifyDataSetChanged()
    }
} 