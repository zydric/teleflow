package com.example.teleflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            onItemLongClick = { script ->
                // Navigate to script editor for editing
                val bundle = bundleOf("scriptId" to script.id)
                findNavController().navigate(
                    R.id.action_scriptSelectionFragment_to_scriptEditorFragment,
                    bundle
                )
            }
        )
        
        scriptsRecyclerView.adapter = scriptAdapter
        
        // Observe scripts from ViewModel
        scriptViewModel.allScripts.observe(viewLifecycleOwner, Observer { scripts ->
            scriptAdapter.updateData(scripts)
        })
    }
    
    private fun navigateToScriptEditor() {
        // Navigate to script editor for creating a new script
        findNavController().navigate(R.id.action_scriptSelectionFragment_to_scriptEditorFragment)
    }
} 