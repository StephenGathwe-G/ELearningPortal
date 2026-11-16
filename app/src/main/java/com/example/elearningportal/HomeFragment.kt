package com.example.elearningportal

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Receive student name
        val studentName = arguments?.getString("STUDENT_NAME") ?: "Student"

        // Display personalised welcome message
        val welcomeText = view.findViewById<TextView>(R.id.welcomeMessage)

        welcomeText.text = "Welcome, $studentName!"
    }
}
