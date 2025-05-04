package com.example.teleflow.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teleflow.R
import com.example.teleflow.RecordActivity
import com.example.teleflow.adapters.ScriptAdapter
import com.example.teleflow.models.Script
import com.example.teleflow.viewmodels.AuthViewModel
import com.example.teleflow.viewmodels.ScriptViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ScriptsFragment : Fragment() {

    private lateinit var scriptsRecyclerView: RecyclerView
    private lateinit var addScriptFab: FloatingActionButton
    private lateinit var emptyStateTextView: TextView
    private val scriptViewModel: ScriptViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var scriptAdapter: ScriptAdapter
    
    private val TAG = "ScriptsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scripts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Check if user is logged in
            if (!authViewModel.isLoggedIn()) {
                // Redirect to login screen if not logged in
                findNavController().navigate(R.id.loginFragment)
                return
            }
            
            val userId = authViewModel.getCurrentUserId()
            Log.d(TAG, "Current user ID: $userId")
    
            // Set up the RecyclerView
            scriptsRecyclerView = view.findViewById(R.id.recyclerView_scriptsList)
            scriptsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            
            // Initialize empty state view
            emptyStateTextView = view.findViewById(R.id.textView_emptyScripts)
            
            // Set up FAB
            addScriptFab = view.findViewById(R.id.fab_addScript)
            addScriptFab.setOnClickListener {
                navigateToScriptEditor()
            }
            
            // Create adapter with click and long-click handlers
            scriptAdapter = ScriptAdapter(
                mutableListOf(),
                onItemClick = { script ->
                    try {
                        // Launch RecordActivity directly instead of using navigation
                        val intent = Intent(requireContext(), RecordActivity::class.java).apply {
                            putExtra("scriptTitle", script.title)
                            putExtra("scriptContent", script.content)
                            putExtra("scriptId", script.id)
                        }
                        startActivity(intent)
                        
                        // Update script last used timestamp
                        scriptViewModel.updateScriptLastUsed(script.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error launching RecordActivity", e)
                        Toast.makeText(requireContext(), "Error launching recorder", Toast.LENGTH_SHORT).show()
                    }
                },
                onItemLongClick = { script, view ->
                    // Show popup menu with edit and delete options
                    showScriptOptionsMenu(view, script)
                }
            )
            
            scriptsRecyclerView.adapter = scriptAdapter
            
            // Observe user-specific scripts from ViewModel instead of all scripts
            scriptViewModel.userScripts.observe(viewLifecycleOwner, Observer { scripts ->
                Log.d(TAG, "Received ${scripts.size} scripts from LiveData")
                scripts.forEach { script ->
                    Log.d(TAG, "Script: ${script.id}, ${script.title}, userId: ${script.userId}")
                }
                scriptAdapter.updateData(scripts)
                
                // Toggle visibility of empty state view
                if (scripts.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    scriptsRecyclerView.visibility = View.GONE
                } else {
                    emptyStateTextView.visibility = View.GONE
                    scriptsRecyclerView.visibility = View.VISIBLE
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Error loading scripts", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            // Force refresh scripts when returning to this fragment
            val userId = authViewModel.getCurrentUserId()
            if (userId != null) {
                Log.d(TAG, "onResume: Refreshing scripts for user $userId")
                // Instead of refreshing here which could crash, 
                // we'll observe the LiveData which is already set up in onViewCreated
                // This ensures we're on the main thread when working with LiveData
                Log.d(TAG, "Waiting for LiveData update with latest scripts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }
    
    private fun showScriptOptionsMenu(view: View, script: Script) {
        try {
            val anchor = view // Use the view parameter which is the long-clicked item
            val popup = PopupMenu(requireContext(), anchor)
            
            popup.menuInflater.inflate(R.menu.menu_script_options, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        // Navigate to script editor for editing
                        val bundle = bundleOf("scriptId" to script.id)
                        findNavController().navigate(
                            R.id.action_scriptsFragment_to_scriptEditorFragment,
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
        } catch (e: Exception) {
            Log.e(TAG, "Error showing script options menu", e)
        }
    }
    
    private fun showDeleteConfirmationDialog(script: Script) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete confirmation dialog", e)
        }
    }
    
    private fun navigateToScriptEditor() {
        try {
            // Navigate to script editor for creating a new script
            findNavController().navigate(R.id.action_scriptsFragment_to_scriptEditorFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to script editor", e)
            Toast.makeText(requireContext(), "Error opening script editor", Toast.LENGTH_SHORT).show()
        }
    }
} 