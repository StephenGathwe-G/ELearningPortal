package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.elearningportal.database.AppDatabase
import com.example.elearningportal.database.Course
import com.example.elearningportal.database.Gender
import com.example.elearningportal.database.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RegisterFragment : Fragment() {

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
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted && isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Notifications are disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database using fragment context
        db = AppDatabase.getInstance(requireContext())

        // Initialize views from the inflated root view
        registerFullName = view.findViewById(R.id.registerFullName)
        registerCourse = view.findViewById(R.id.registerCourse)
        registerAge = view.findViewById(R.id.registerAge)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        studyModeGroup = view.findViewById(R.id.studyModeGroup)
        yearSeekBar = view.findViewById(R.id.yearSeekBar)
        yearValue = view.findViewById(R.id.yearValue)
        termsCheckBox = view.findViewById(R.id.termsCheckbox)
        registerStudentsButton = view.findViewById(R.id.registerStudentButton)
        viewStudentsButton = view.findViewById(R.id.viewStudentsButton)
        registerMenuButton = view.findViewById(R.id.registerMenuButton)

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
            val intent = Intent(requireContext(), StudentsActivity::class.java)
            startActivity(intent)
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
            registerCourse.error = "Enter BSE, BCS, BED, BSCIT or BCF"
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
        val genderText = genderSpinner.selectedItem?.toString() ?: ""
        val gender = when (genderText) {
            "Male" -> Gender.Male
            "Female" -> Gender.Female
            "Other" -> Gender.Other
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Please select a valid gender",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Get study mode using RadioButton IDs
        val selectedStudyMode = studyModeGroup.checkedRadioButtonId
        if (selectedStudyMode == -1) {
            Toast.makeText(
                requireContext(),
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
                    requireContext(),
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
            termsCheckBox.error = "Please accept the terms and conditions"
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

            // Update UI on main thread safely
            activity?.runOnUiThread {
                // Ensure fragment is still attached to avoid crashes
                if (!isAdded) return@runOnUiThread

                registerStudentsButton.isEnabled = true

                if (result != -1L) {
                    Toast.makeText(
                        requireContext(),
                        "Student registered successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    val serviceIntent = Intent(
                        requireContext(),
                        RegistrationService::class.java
                    )
                    requireContext().startService(serviceIntent)

                    clearForm()
                } else {
                    Toast.makeText(
                        requireContext(),
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

    // Popup menu implementation
    private fun setupPopupMenu() {
        registerMenuButton.setOnClickListener { anchorView ->
            val popup = PopupMenu(requireContext(), anchorView)

            popup.menu.add("Clear Form")
            popup.menu.add("Load Sample Data")

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Clear Form" -> {
                        clearForm()
                        Toast.makeText(
                            requireContext(),
                            "Form cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    "Load Sample Data" -> {
                        registerFullName.setText("John Doe")
                        registerCourse.setText("BSE")
                        registerAge.setText("21")
                        studyModeGroup.check(R.id.fullTime)
                        genderSpinner.setSelection(1)
                        yearSeekBar.progress=1
                        termsCheckBox.isChecked = true

                        Toast.makeText(
                            requireContext(),
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