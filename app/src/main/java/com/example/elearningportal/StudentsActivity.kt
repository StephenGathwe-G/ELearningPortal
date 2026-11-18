package com.example.elearningportal

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.elearningportal.database.AppDatabase
import com.example.elearningportal.database.Course
import com.example.elearningportal.database.Gender
import com.example.elearningportal.database.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StudentsActivity : AppCompatActivity() {

    // Lightweight model holding each loaded student's data for editing & display
    private data class StudentItem(
        val id: Int,
        val name: String,
        val course: Course,
        val age: Int,
        val gender: Gender,
        val type: Type,
        val year: Int
    ) {
        fun toDisplayString(): String =
            "$name • ${course.name} (Year $year)\nAge: $age | ${gender.name} | ${type.name}"
    }

    private lateinit var tvStudentsTitle: TextView
    private lateinit var listStudents: ListView
    private lateinit var btnDeleteAll: Button

    private lateinit var db: AppDatabase
    private lateinit var adapter: ArrayAdapter<String>

    private val studentItems = mutableListOf<StudentItem>()
    private val studentDisplayList = mutableListOf<String>()

    // Background database operations
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_students)

        // Initialize database
        db = AppDatabase.getInstance(this)

        // Initialize views
        tvStudentsTitle = findViewById(R.id.tvStudentsTitle)
        listStudents = findViewById(R.id.listStudents)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)

        // Set up ListView adapter
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            studentDisplayList
        )
        listStudents.adapter = adapter

        // Load students from database
        loadStudents()

        // Tap a row to edit name, age, or course
        listStudents.setOnItemClickListener { _, _, position, _ ->
            val selectedStudent = studentItems[position]
            showEditStudentDialog(selectedStudent)
        }

        // Long-press a row to delete a single student
        listStudents.setOnItemLongClickListener { _, _, position, _ ->
            val selectedStudent = studentItems[position]
            confirmDeleteSingle(selectedStudent)
            true
        }

        // Delete All button
        btnDeleteAll.setOnClickListener {
            if (studentItems.isEmpty()) {
                Toast.makeText(
                    this,
                    "No students to delete",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                confirmDeleteAll()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStudents()
    }

    private fun loadStudents() {
        executor.execute {
            val loadedItems = mutableListOf<StudentItem>()

            try {
                db.queryAll().use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_NAME)
                    val courseIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_COURSE)
                    val genderIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_GENDER)
                    val typeIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_TYPE)
                    val ageIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_AGE)
                    val yearIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_YEAR)

                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(idIdx)
                        val name = cursor.getString(nameIdx) ?: "Unknown"
                        val course = Course.valueOf(cursor.getString(courseIdx))
                        val gender = Gender.valueOf(cursor.getString(genderIdx))
                        val type = Type.valueOf(cursor.getString(typeIdx))
                        val age = cursor.getInt(ageIdx)
                        val year = cursor.getInt(yearIdx)

                        loadedItems.add(
                            StudentItem(id, name, course, age, gender, type, year)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                studentItems.clear()
                studentItems.addAll(loadedItems)

                studentDisplayList.clear()
                studentDisplayList.addAll(loadedItems.map { it.toDisplayString() })

                adapter.notifyDataSetChanged()
                btnDeleteAll.isEnabled = studentItems.isNotEmpty()
            }
        }
    }

    private fun showEditStudentDialog(student: StudentItem) {
        val paddingPx = (20 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx / 2, paddingPx, 0)
        }

        // Full Name input
        val nameLabel = TextView(this).apply { text = "Full Name" }
        val nameInput = EditText(this).apply {
            setText(student.name)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            setSelection(text.length)
        }

        // Age input
        val ageLabel = TextView(this).apply {
            text = "Age"
            setPadding(0, paddingPx / 2, 0, 0)
        }
        val ageInput = EditText(this).apply {
            setText(student.age.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        // Course spinner populated directly from your Course enum
        val courseLabel = TextView(this).apply {
            text = "Course"
            setPadding(0, paddingPx / 2, 0, 0)
        }
        val courses = Course.values()
        val courseSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@StudentsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                courses.map { it.name }
            )
            setSelection(courses.indexOf(student.course).coerceAtLeast(0))
        }

        container.addView(nameLabel)
        container.addView(nameInput)
        container.addView(ageLabel)
        container.addView(ageInput)
        container.addView(courseLabel)
        container.addView(courseSpinner)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Student")
            .setView(container)
            .setPositiveButton("Save", null) // Overridden below so validation errors keep the dialog open
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val updatedName = nameInput.text.toString().trim()
                val updatedAge = ageInput.text.toString().trim().toIntOrNull()
                val updatedCourse = courses[courseSpinner.selectedItemPosition]

                if (updatedName.isEmpty()) {
                    nameInput.error = "Enter a valid name"
                    nameInput.requestFocus()
                    return@setOnClickListener
                }

                if (updatedAge == null || updatedAge !in 1..120) {
                    ageInput.error = "Enter a valid age (1–120)"
                    ageInput.requestFocus()
                    return@setOnClickListener
                }

                dialog.dismiss()
                updateStudent(student.id, updatedName, updatedAge, updatedCourse)
            }
        }

        dialog.show()
    }

    private fun updateStudent(id: Int, name: String, age: Int, course: Course) {
        executor.execute {
            val rowsUpdated = try {
                // Uses your exact AppDatabase.update() method with named arguments
                db.update(
                    id = id,
                    name = name,
                    age = age,
                    course = course
                )
            } catch (e: Exception) {
                0
            }

            runOnUiThread {
                if (rowsUpdated > 0) {
                    Toast.makeText(this, "Student updated", Toast.LENGTH_SHORT).show()
                    loadStudents()
                } else {
                    Toast.makeText(this, "Failed to update student", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDeleteSingle(student: StudentItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Remove this student?\n\n${student.toDisplayString()}")
            .setPositiveButton("Delete") { _, _ ->
                executor.execute {
                    val deletedRows = db.delete(student.id)
                    runOnUiThread {
                        if (deletedRows > 0) {
                            Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show()
                            loadStudents()
                        } else {
                            Toast.makeText(this, "Could not delete student", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Students")
            .setMessage("Are you sure you want to delete all registered students?")
            .setPositiveButton("Delete All") { _, _ ->
                deleteAllStudents()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllStudents() {
        btnDeleteAll.isEnabled = false

        executor.execute {
            val deletedCount = try {
                db.writableDatabase.delete(AppDatabase.TABLE_NAME, null, null)
            } catch (e: Exception) {
                -1
            }

            runOnUiThread {
                if (deletedCount >= 0) {
                    studentItems.clear()
                    studentDisplayList.clear()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(
                        this,
                        "All students deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    btnDeleteAll.isEnabled = true
                    Toast.makeText(
                        this,
                        "Failed to delete students",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}