package com.example.elearningportal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StudentsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_students)

        supportActionBar?.title = "Registered Students"
    }
}
