package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class RegisterFragment : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRegister = view.findViewById<Button>(R.id.viewStudentsButton)

        btnRegister.setOnClickListener {
            val intent = Intent(requireActivity(), StudentsActivity::class.java)
            startActivity(intent)
        }
    }
}
