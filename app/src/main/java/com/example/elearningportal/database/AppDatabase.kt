package com.example.elearningportal.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.DatabaseUtils
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
        private const val DATABASE_VERSION = 2

        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_NAME = "name"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_PASSWORD_HASH = "password_hash"
        const val COLUMN_USER_SALT = "salt"

        // Seeded account (development only)
        const val DEFAULT_USER_NAME = "Admin Student"
        const val DEFAULT_USER_EMAIL = "admin@elearning.com"
        const val DEFAULT_USER_PASSWORD = "Admin@123"

        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16

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
        createUsersTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createUsersTable(db)
    }

    private fun createUsersTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_EMAIL TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COLUMN_USER_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_USER_SALT TEXT NOT NULL
            )
        """.trimIndent()
        )
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

    // ─── USERS / AUTH ───────────────────────────────────────

    /** Inserts the default user only if no users exist. Safe to call on every launch. */
    fun seedInitialUser(): Boolean {
        if (DatabaseUtils.queryNumEntries(writableDatabase, TABLE_USERS) > 0) return false
        return insertUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD) != -1L
    }

    fun insertUser(name: String, email: String, password: String): Long {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)

        return writableDatabase.insert(TABLE_USERS, null, ContentValues().apply {
            put(COLUMN_USER_NAME, name)
            put(COLUMN_USER_EMAIL, email.trim())
            put(COLUMN_USER_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            put(COLUMN_USER_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        })
    }

    /** Returns the user's name if the credentials match, otherwise null. */
    fun authenticate(email: String, password: String): String? {
        readableDatabase.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_NAME, COLUMN_USER_PASSWORD_HASH, COLUMN_USER_SALT),
            "$COLUMN_USER_EMAIL = ?", arrayOf(email.trim()),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            val name = cursor.getString(0)
            val storedHash = Base64.decode(cursor.getString(1), Base64.NO_WRAP)
            val salt = Base64.decode(cursor.getString(2), Base64.NO_WRAP)

            val attempt = hashPassword(password, salt)
            return if (MessageDigest.isEqual(storedHash, attempt)) name else null
        }
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}   