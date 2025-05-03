package com.example.teleflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.teleflow.R
import com.example.teleflow.models.Script
import com.example.teleflow.viewmodels.ScriptViewModel
import java.util.Date

class ScriptEditorFragment : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button
    
    private val scriptViewModel: ScriptViewModel by viewModels()
    
    // If scriptId is -1, we're creating a new script. Otherwise, we're editing an existing one
    private var scriptId: Int = -1
    private var originalScript: Script? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_script_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        titleEditText = view.findViewById(R.id.editText_title)
        contentEditText = view.findViewById(R.id.editText_content)
        saveButton = view.findViewById(R.id.button_save)
        
        // Get script ID from arguments if it exists (for editing)
        arguments?.let { args ->
            scriptId = args.getInt("scriptId", -1)
            
            // If we're editing an existing script, load its data
            if (scriptId != -1) {
                scriptViewModel.getScriptById(scriptId).observe(viewLifecycleOwner, Observer { script ->
                    script?.let {
                        originalScript = it
                        titleEditText.setText(it.title)
                        contentEditText.setText(it.content)
                    }
                })
            }
        }
        
        // Set up save button click listener
        saveButton.setOnClickListener {
            saveScript()
        }
    }
    
    private fun saveScript() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        
        // Validate input
        if (title.isEmpty()) {
            titleEditText.error = "Title cannot be empty"
            return
        }
        
        if (content.isEmpty()) {
            contentEditText.error = "Content cannot be empty"
            return
        }
        
        // Create or update script
        val script = if (scriptId != -1) {
            // Update existing script, preserve creation date but update last modified
            Script(
                id = scriptId,
                title = title,
                content = content,
                createdAt = originalScript?.createdAt ?: Date(),
                lastUsedAt = originalScript?.lastUsedAt,
                lastModifiedAt = Date()
            )
        } else {
            // Create new script with current date
            val now = Date()
            Script(
                id = 0, // Room will auto-generate
                title = title,
                content = content,
                createdAt = now,
                lastUsedAt = null,
                lastModifiedAt = now
            )
        }
        
        // Save to database
        if (scriptId != -1) {
            // Update existing script
            scriptViewModel.updateScript(script)
            Toast.makeText(requireContext(), "Script updated", Toast.LENGTH_SHORT).show()
        } else {
            // Create new script
            scriptViewModel.insertScript(script)
            Toast.makeText(requireContext(), "Script created", Toast.LENGTH_SHORT).show()
        }
        
        // Navigate back
        findNavController().popBackStack()
    }
} 