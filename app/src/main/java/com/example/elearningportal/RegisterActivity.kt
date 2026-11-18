package com.example.elearningportal

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.elearningportal.database.AppDatabase
import com.example.elearningportal.database.Gender
import com.example.elearningportal.database.Type
import com.example.elearningportal.database.Course
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerFullName: EditText
    private lateinit var registerCourse: EditText
    private lateinit var registerAge: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var studyModeGroup: RadioGroup
    private lateinit var yearSeekBar: SeekBar
    private lateinit var yearValue: TextView
    private lateinit var termsCheckBox: CheckBox
    private lateinit var registerStudentsButton: Button
    private lateinit var viewStudentsButton: Button
    private lateinit var registerMenuButton: ImageButton

    private lateinit var db: AppDatabase

    // Background database operations
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_register)

        // Initialize database
        db = AppDatabase.getInstance(this)

        // Initialize views
        registerFullName = findViewById(R.id.registerFullName)
        registerCourse = findViewById(R.id.registerCourse)
        registerAge = findViewById(R.id.registerAge)
        genderSpinner = findViewById(R.id.genderSpinner)
        studyModeGroup = findViewById(R.id.studyModeGroup)
        yearSeekBar = findViewById(R.id.yearSeekBar)
        yearValue = findViewById(R.id.yearValue)
        termsCheckBox = findViewById(R.id.termsCheckbox)
        registerStudentsButton = findViewById(R.id.registerStudentButton)
        viewStudentsButton = findViewById(R.id.viewStudentsButton)
        registerMenuButton = findViewById(R.id.registerMenuButton)

        // Handle year selection
        yearSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    yearValue.text = "Year ${progress + 1}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        // Register button
        registerStudentsButton.setOnClickListener {
            registerStudent()
        }

        // Popup menu
        setupPopupMenu()

        // View registered students
        viewStudentsButton.setOnClickListener {
            Toast.makeText(
                this,
                "Students screen not implemented yet",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun registerStudent() {

        val fullName = registerFullName.text.toString().trim()
        val courseText = registerCourse.text.toString().trim()
        val ageText = registerAge.text.toString().trim()

        // Validate name
        if (fullName.isEmpty()) {
            registerFullName.error = "Enter your full name"
            registerFullName.requestFocus()
            return
        }

        // Validate course
        if (courseText.isEmpty()) {
            registerCourse.error = "Enter your course"
            registerCourse.requestFocus()
            return
        }

        // Validate course against database enum
        val course = try {
            Course.valueOf(courseText.uppercase())
        } catch (e: IllegalArgumentException) {
            registerCourse.error =
                "Enter BSE, BCS, BED, BSCIT or BCF"
            registerCourse.requestFocus()
            return
        }

        // Validate age
        val age = ageText.toIntOrNull()

        if (age == null || age !in 1..120) {
            registerAge.error = "Enter a valid age"
            registerAge.requestFocus()
            return
        }

        // Get gender
        val genderText =
            genderSpinner.selectedItem?.toString() ?: ""

        val gender = when (genderText) {
            "Male" -> Gender.Male
            "Female" -> Gender.Female
            "Other" -> Gender.Other
            else -> {
                Toast.makeText(
                    this,
                    "Please select a valid gender",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Get study mode using RadioButton IDs
        val selectedStudyMode =
            studyModeGroup.checkedRadioButtonId

        if (selectedStudyMode == -1) {
            Toast.makeText(
                this,
                "Select your study mode",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val studyMode = when (selectedStudyMode) {

            R.id.fullTime -> Type.FullTime

            R.id.partTime -> Type.PartTime

            R.id.online -> Type.Online

            else -> {
                Toast.makeText(
                    this,
                    "Invalid study mode",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Get selected year
        val year = yearSeekBar.progress + 1

        // Validate terms
        if (!termsCheckBox.isChecked) {
            termsCheckBox.error =
                "Please accept the terms and conditions"
            return
        }

        // Prevent multiple submissions
        registerStudentsButton.isEnabled = false

        // Insert student on background thread
        executor.execute {

            val result = try {
                db.insert(
                    fullName,
                    year.toString(),
                    studyMode,
                    age,
                    gender,
                    course
                )
            } catch (e: Exception) {
                -1L
            }

            // Update UI on main thread
            runOnUiThread {

                registerStudentsButton.isEnabled = true

                if (result != -1L) {

                    Toast.makeText(
                        this,
                        "Student registered successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    val serviceIntent = Intent(
                        this,
                        RegistrationService::class.java
                    )

                    startService(serviceIntent)

                    clearForm()

                } else {

                    Toast.makeText(
                        this,
                        "Registration failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Clear registration form
    private fun clearForm() {
        registerFullName.text.clear()
        registerCourse.text.clear()
        registerAge.text.clear()
        studyModeGroup.clearCheck()
        yearSeekBar.progress = 0
        termsCheckBox.isChecked = false
        genderSpinner.setSelection(0)
    }
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Notifications are disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Popup menu implementation
    private fun setupPopupMenu() {

        registerMenuButton.setOnClickListener { view ->

            val popup = PopupMenu(this, view)

            popup.menu.add("Clear Form")
            popup.menu.add("Load Sample Data")

            popup.setOnMenuItemClickListener { item ->

                when (item.title.toString()) {

                    "Clear Form" -> {

                        clearForm()

                        Toast.makeText(
                            this,
                            "Form cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    "Load Sample Data" -> {

                        registerFullName.setText("John Doe")
                        registerCourse.setText("BSE")
                        registerAge.setText("21")
                        termsCheckBox.isChecked = true

                        Toast.makeText(
                            this,
                            "Sample data loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                true
            }

            popup.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        executor.shutdown()
    }

}
