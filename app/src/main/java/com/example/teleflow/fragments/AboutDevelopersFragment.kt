package com.example.teleflow.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.teleflow.R

class AboutDevelopersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about_developers, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val devImage1 = view.findViewById<ImageView>(R.id.developer_image_1)
        val devImage2 = view.findViewById<ImageView>(R.id.developer_image_2)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            devImage1.clipToOutline = true
            devImage2.clipToOutline = true
        }
    }
} 