package com.example.teleflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.adapters.ScriptAdapter
import com.example.teleflow.viewmodels.ScriptViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ScriptSelectionFragment : Fragment() {

    private lateinit var scriptsRecyclerView: RecyclerView
    private lateinit var addScriptFab: FloatingActionButton
    private val scriptViewModel: ScriptViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_script_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the RecyclerView
        scriptsRecyclerView = view.findViewById(R.id.recyclerView_scriptsList)
        scriptsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Set up FAB
        addScriptFab = view.findViewById(R.id.fab_addScript)
        addScriptFab.setOnClickListener {
            navigateToScriptEditor()
        }
        
        // Create adapter with click and long-click handlers
        val scriptAdapter = ScriptAdapter(
            mutableListOf(),
            onItemClick = { script ->
                // Navigate to recording fragment and pass the script data
                val bundle = bundleOf(
                    "scriptTitle" to script.title,
                    "scriptContent" to script.content,
                    "scriptId" to script.id
                )
                findNavController().navigate(
                    R.id.action_scriptSelectionFragment_to_recordingFragment,
                    bundle
                )
            },
            onItemLongClick = { script, view ->
                // Show popup menu with edit and delete options
                showScriptOptionsMenu(view, script)
            }
        )
        
        scriptsRecyclerView.adapter = scriptAdapter
        
        // Observe scripts from ViewModel
        scriptViewModel.allScripts.observe(viewLifecycleOwner, Observer { scripts ->
            scriptAdapter.updateData(scripts)
        })
    }
    
    private fun showScriptOptionsMenu(view: View, script: com.example.teleflow.models.Script) {
        val anchor = view // Use the view parameter which is the long-clicked item
        val popup = PopupMenu(requireContext(), anchor)
        
        popup.menuInflater.inflate(R.menu.menu_script_options, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    // Navigate to script editor for editing
                    val bundle = bundleOf("scriptId" to script.id)
                    findNavController().navigate(
                        R.id.action_scriptSelectionFragment_to_scriptEditorFragment,
                        bundle
                    )
                    true
                }
                R.id.action_delete -> {
                    // Show confirmation dialog before deleting
                    showDeleteConfirmationDialog(script)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showDeleteConfirmationDialog(script: com.example.teleflow.models.Script) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Script")
            .setMessage("Are you sure you want to delete '${script.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the script
                scriptViewModel.deleteScript(script)
                Toast.makeText(requireContext(), "Script deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToScriptEditor() {
        // Navigate to script editor for creating a new script
        findNavController().navigate(R.id.action_scriptSelectionFragment_to_scriptEditorFragment)
    }
} 