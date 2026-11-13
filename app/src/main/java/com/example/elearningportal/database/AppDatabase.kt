package com.example.elearningportal.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

enum class Gender {
    Male,
    Female,
    Other
}

enum class Type {
    FullTime,
    Online,
    PartTime,
}

enum class Course {
    BSE,
    BCS,
    BED,
    BSCIT,
    BCF
}

class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "elearning.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "tasks"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_COURSE = "course"
        const val COLUMN_AGE = "age"
        const val COLUMN_GENDER = "gender"
        const val COLUMN_TYPE = "type"
        const val COLUMN_YEAR = "year"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE $TABLE_NAME (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, 
                    $COLUMN_NAME TEXT, 
                    $COLUMN_COURSE TEXT NOT NULL CHECK ($COLUMN_COURSE IN ('BSE', 'BCS', 'BED', 'BSCIT', 'BCF')), 
                    $COLUMN_AGE INTEGER, 
                    $COLUMN_GENDER TEXT NOT NULL CHECK ($COLUMN_GENDER IN ('Male', 'Female', 'Other')),
                    $COLUMN_TYPE TEXT NOT NULL CHECK ($COLUMN_TYPE IN ('Online', 'FullTime', 'PartTime')),
                    $COLUMN_YEAR INTEGER NOT NULL DEFAULT (CAST(strftime('%Y', 'now') AS INTEGER))
                )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // ─── CRUD ───────────────────────────────────────────────

    fun insert(name: String, year: String, type: Type, age: Int, gender: Gender, course: Course): Long {
        return writableDatabase.insert(TABLE_NAME, null, ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_YEAR, year)
            put(COLUMN_TYPE, type.name)
            put(COLUMN_AGE, age)
            put(COLUMN_GENDER, gender.name)
            put(COLUMN_COURSE, course.name)
        })
    }

    fun update(
        id: Int,
        name: String? = null,
        age: Int? = null,
        gender: Gender? = null,
        type: Type? = null,
        course: Course? = null,
        year: Int? = null
    ): Int {
        val values = ContentValues()
        name?.let { values.put(COLUMN_NAME, it) }
        age?.let { values.put(COLUMN_AGE, it) }
        gender?.let { values.put(COLUMN_GENDER, it.name) }
        type?.let { values.put(COLUMN_TYPE, it.name) }
        course?.let { values.put(COLUMN_COURSE, it.name) }
        year?.let { values.put(COLUMN_YEAR, it) }

        if (values.size() == 0) return 0

        return writableDatabase.update(
            TABLE_NAME,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun delete(id: Int): Int {
        return writableDatabase.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun queryAll(): Cursor {
        return readableDatabase.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_COURSE, COLUMN_GENDER, COLUMN_TYPE, COLUMN_AGE, COLUMN_YEAR),
            null, null, null, null, "$COLUMN_ID ASC"
        )
    }

    fun getById(id: Int): Cursor {
        return readableDatabase.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_COURSE, COLUMN_GENDER, COLUMN_TYPE, COLUMN_AGE, COLUMN_YEAR),
            "$COLUMN_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
    }
}   