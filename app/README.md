```markdown
# AppDatabase Usage Guide

## Overview

`AppDatabase` is a **singleton** wrapper around `SQLiteOpenHelper` that manages all database
operations for the e-learning portal. Every Activity, Fragment, or Service in the app accesses
the **same** database instance through a single entry point:

```kotlin
val db = AppDatabase.getInstance(context)
```

Because it uses `applicationContext` internally, you can pass **any** `Context` (Activity,
Fragment, Service, etc.) — no context will be leaked.

---

## File Location & Package

```
app/src/main/java/com/example/elearningportal/database/AppDatabase.kt
```

### Required Imports (in any Activity)

```kotlin
import com.example.elearningportal.database.AppDatabase
import com.example.elearningportal.database.Gender
import com.example.elearningportal.database.Type
import com.example.elearningportal.database.Course
import android.database.Cursor
```

---

## Getting the Instance

```kotlin
val db = AppDatabase.getInstance(this)  // 'this' can be any Context
```

> The database file is created **lazily** — on the first read or write call, not at
> `getInstance()` time. No file exists until you actually perform an operation.

---

## Database Schema

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` |
| `name` | `TEXT` | — |
| `course` | `TEXT` | `NOT NULL`, must be one of: `BSE`, `BCS`, `BED`, `BSCIT`, `BCF` |
| `age` | `INTEGER` | — |
| `gender` | `TEXT` | `NOT NULL`, must be one of: `Male`, `Female`, `Other` |
| `type` | `TEXT` | `NOT NULL`, must be one of: `Online`, `FullTime`, `PartTime` |
| `year` | `INTEGER` | `NOT NULL`, defaults to current year if omitted |

---

## CRUD Operations

### 1. Insert a New Row

```kotlin
val db = AppDatabase.getInstance(this)
val newRowId: Long = db.insert(
    name   = "Alice Johnson",
    year   = "2026",
    type   = Type.FullTime,
    age    = 20,
    gender = Gender.Female,
    course = Course.BCS
)
```

- Returns the **row ID** of the newly inserted record (`-1` on failure).
- All parameters are required.

---

### 2. Update an Existing Row (Partial Update)

Only the fields you pass are changed. Fields left as `null` remain untouched.

```kotlin
val db = AppDatabase.getInstance(this)

// Change only the name
db.update(id = 1, name = "Alice Smith")

// Change age and gender together
db.update(id = 1, age = 21, gender = Gender.Other)

// Change only the course
db.update(id = 1, course = Course.BSE)
```

- Returns the **number of rows affected** (`0` if nothing was changed or the ID doesn't exist).
- If all parameters are `null`, the method returns `0` without touching the database.

---

### 3. Delete a Row

```kotlin
val db = AppDatabase.getInstance(this)
val rowsDeleted: Int = db.delete(id = 1)
```

- Returns the **number of rows deleted** (`0` if the ID doesn't exist).

---

### 4. Query All Rows

```kotlin
val db = AppDatabase.getInstance(this)
val cursor: Cursor = db.queryAll()
```

- Returns a `Cursor` sorted by `name` ascending.
- **You must close the cursor** when done (see [Cursor Lifecycle](#cursor-lifecycle)).

---

### 5. Query a Single Row by ID

```kotlin
val db = AppDatabase.getInstance(this)
val cursor: Cursor = db.getById(id = 1)
```

- Returns a `Cursor` containing at most one row.
- Check `cursor.moveToFirst()` before reading — it returns `false` if no row was found.

---

## Reading Data from a Cursor

```kotlin
val cursor = db.queryAll()
if (cursor.moveToFirst()) {
    do {
        val id     = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_ID))
        val name   = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_NAME))
        val course = Course.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_COURSE)))
        val age    = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_AGE))
        val gender = Gender.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_GENDER)))
        val type   = Type.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_TYPE)))
        val year   = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_YEAR))

        // Use the values...
    } while (cursor.moveToNext())
}
cursor.close()
```

### Converting Strings Back to Enums

Since enums are stored as their `.name` string, use `valueOf` to convert back:

```kotlin
val gender = Gender.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_GENDER)))
// "Male" → Gender.Male
```

> **Warning:** If the database contains a value that doesn't match any enum constant,
> `valueOf` throws an `IllegalArgumentException`. Use `enumValueOf` (Kotlin 1.9+) or a
> `try/catch` if the data might be inconsistent.

---

## Cursor Lifecycle

Cursors hold a reference to the database and **must** be closed to prevent memory leaks.

### In an Activity

```kotlin
class StudentListActivity : AppCompatActivity() {

    private var cursor: Cursor? = null

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        cursor?.close()  // close previous cursor before re-querying
        cursor = AppDatabase.getInstance(this).queryAll()
        // bind to adapter...
    }

    override fun onDestroy() {
        super.onDestroy()
        cursor?.close()
        cursor = null
    }
}
```

### In a `try/finally` block (one-off reads)

```kotlin
val cursor = db.getById(id)
try {
    if (cursor.moveToFirst()) {
        val name = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_NAME))
        // use name...
    }
} finally {
    cursor.close()
}
```

### With `SimpleCursorAdapter` (ListView)

```kotlin
val cursor = db.queryAll()
val adapter = SimpleCursorAdapter(
    this,
    android.R.layout.simple_list_item_2,
    cursor,
    arrayOf(AppDatabase.COLUMN_NAME, AppDatabase.COLUMN_COURSE),
    intArrayOf(android.R.id.text1, android.R.id.text2),
    0
)
listView.adapter = adapter
// Close in onDestroy()
```

---

## Passing Data Between Activities

### From List → Detail (pass the row ID)

```kotlin
// In StudentListActivity
listView.setOnItemClickListener { _, _, position, _ ->
    cursor?.moveToPosition(position)
    val id = cursor?.getInt(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_ID)) ?: return@setOnItemClickListener

    val intent = Intent(this, StudentDetailActivity::class.java)
    intent.putExtra("student_id", id)
    startActivity(intent)
}
```

```kotlin
// In StudentDetailActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_student_detail)

    val id = intent.getIntExtra("student_id", -1)
    if (id == -1) { finish(); return }

    val cursor = AppDatabase.getInstance(this).getById(id)
    try {
        if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_NAME))
            // populate UI...
        }
    } finally {
        cursor.close()
    }
}
```

### From Login → Main (pass a string)

```kotlin
// In LoginActivity
val intent = Intent(this, MainActivity::class.java)
intent.putExtra("student_name", name)
startActivity(intent)
finish()  // remove login from back stack
```

```kotlin
// In MainActivity
val studentName = intent.getStringExtra("student_name") ?: "Student"
```

---

## Database Versioning & Migration

When you add or change columns, bump `DATABASE_VERSION` and handle the migration in `onUpgrade`:

```kotlin
companion object {
    private const val DATABASE_VERSION = 2  // ← bump this
    // ...
}

override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) {
        db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_PHONE TEXT")
    }
    // Add future migrations below
}
```

> **Do NOT** use `DROP TABLE` in `onUpgrade` in production — it deletes all user data.
> The current implementation does this for simplicity during development.

---

## Thread Safety

- `SQLiteOpenHelper` is **not** thread-safe for concurrent writes from multiple threads.
- For a simple app with single-threaded UI access (as in this project), this is fine.
- If you later add background workers or Room, migrate to a thread-confined approach.

---

## Quick Reference

| Task | Method Call |
|------|-------------|
| Get instance | `AppDatabase.getInstance(context)` |
| Insert | `db.insert(name, year, type, age, gender, course)` |
| Update (partial) | `db.update(id, name = "X")` |
| Delete | `db.delete(id)` |
| All rows | `db.queryAll()` |
| Single row | `db.getById(id)` |
| Close cursor | `cursor.close()` |
```

