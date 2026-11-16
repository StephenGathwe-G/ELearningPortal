# AppDatabase Usage Guide

## Overview

`AppDatabase` is a **singleton** wrapper around `SQLiteOpenHelper` that manages all database operations for the e-learning portal. Every Activity, Fragment, or Service in the app accesses the **same** database instance through a single entry point:

```kotlin
val db = AppDatabase.getInstance(context)
```

Because it uses `applicationContext` internally, you can pass **any** `Context` (Activity, Fragment, Service, etc.) — no context will be leaked.

The database holds two tables:

|Table|Constant|Purpose|
|---|---|---|
|`tasks`|`TABLE_NAME`|Registered **students** (name, course, age, gender, study mode, year)|
|`users`|`TABLE_USERS`|**Login accounts** (name, email, salted password hash)|

> **Naming note:** the students table is still called `tasks` for historical reasons. Renaming it to `students` would require a migration, so it has been left as is.

---

## Changelog

|Version|Change|
|---|---|
|1|Initial schema: `tasks` (students) table|
|2|Added `users` table for login. `onUpgrade` is now **non-destructive**, so registered students survive the upgrade. Added `seedInitialUser()`, `insertUser()` and `authenticate()`|

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

> The database file is created **lazily** — on the first read or write call, not at `getInstance()` time. No file exists until you actually perform an operation.

---

## Database Schema

### Students table (`tasks`)

|Column|Type|Constraints|
|---|---|---|
|`id`|`INTEGER`|`PRIMARY KEY AUTOINCREMENT`|
|`name`|`TEXT`|—|
|`course`|`TEXT`|`NOT NULL`, must be one of: `BSE`, `BCS`, `BED`, `BSCIT`, `BCF`|
|`age`|`INTEGER`|—|
|`gender`|`TEXT`|`NOT NULL`, must be one of: `Male`, `Female`, `Other`|
|`type`|`TEXT`|`NOT NULL`, must be one of: `Online`, `FullTime`, `PartTime`|
|`year`|`INTEGER`|`NOT NULL`, defaults to current year if omitted|

### Users table (`users`)

|Column|Type|Constraints|
|---|---|---|
|`id`|`INTEGER`|`PRIMARY KEY AUTOINCREMENT`|
|`name`|`TEXT`|`NOT NULL`|
|`email`|`TEXT`|`NOT NULL`, `UNIQUE`, case-insensitive (`COLLATE NOCASE`)|
|`password_hash`|`TEXT`|`NOT NULL`, Base64-encoded PBKDF2 hash|
|`salt`|`TEXT`|`NOT NULL`, Base64-encoded random 16-byte salt|

Passwords are **never stored in plain text**. See [Authentication](https://claude.ai/chat/a3c02687-7ad1-4a27-a599-24fe6765fb27#authentication--user-accounts).

---

## Student CRUD Operations

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
- Note that `year` is passed as a `String` here but stored as an `INTEGER`.

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

- Returns a `Cursor` sorted by `id` ascending.
- **You must close the cursor** when done (see [Cursor Lifecycle](https://claude.ai/chat/a3c02687-7ad1-4a27-a599-24fe6765fb27#cursor-lifecycle)).

---

### 5. Query a Single Row by ID

```kotlin
val db = AppDatabase.getInstance(this)
val cursor: Cursor = db.getById(id = 1)
```

- Returns a `Cursor` containing at most one row.
- Check `cursor.moveToFirst()` before reading — it returns `false` if no row was found.

---

## Authentication & User Accounts

Login is handled entirely inside `AppDatabase`. `LoginActivity` only calls the methods below.

### Seeded default account

On first launch the login screen seeds one account so there is something to sign in with:

|Field|Constant|Value|
|---|---|---|
|Name|`AppDatabase.DEFAULT_USER_NAME`|`Admin Student`|
|Email|`AppDatabase.DEFAULT_USER_EMAIL`|`admin@elearning.com`|
|Password|`AppDatabase.DEFAULT_USER_PASSWORD`|`Admin@123`|

> **Development only.** These credentials are visible in source code. Remove the seed (and the "Load Sample Data" menu item) before releasing, and create the first account through a registration flow instead.

### 6. Seed the initial user

```kotlin
val seeded: Boolean = db.seedInitialUser()
```

- Inserts the default account **only if the `users` table is empty**.
- Returns `true` if a user was inserted, `false` if users already existed (or the insert failed).
- Safe to call on every launch.

### 7. Create a user

```kotlin
val userId: Long = db.insertUser(
    name     = "Alice Johnson",
    email    = "alice@example.com",
    password = "S3cret!pass"
)
```

- Generates a random salt, hashes the password, and stores the hash and salt.
- Returns the **row ID** (`-1` on failure, for example when the email already exists).
- Email is trimmed and compared case-insensitively.

### 8. Authenticate

```kotlin
val userName: String? = db.authenticate(
    email    = "admin@elearning.com",
    password = "Admin@123"
)

if (userName != null) {
    // Credentials are valid; userName is the stored display name
} else {
    // Unknown email or wrong password
}
```

- Returns the user's **stored name** on success, or `null` on failure.
- The same `null` is returned for an unknown email and a wrong password, so callers cannot tell which one was wrong.
- The hash comparison uses `MessageDigest.isEqual`, which runs in constant time.

### How passwords are stored

|Setting|Value|
|---|---|
|Algorithm|`PBKDF2WithHmacSHA256`|
|Iterations|120,000|
|Key length|256 bits|
|Salt|16 random bytes per user (`SecureRandom`)|
|Encoding|Base64 (`NO_WRAP`) in the `password_hash` and `salt` columns|

> `PBKDF2WithHmacSHA256` requires `minSdk 26`. On a lower `minSdk`, switch to `PBKDF2WithHmacSHA1` in `hashPassword()`.

> Hashing is intentionally slow (a few hundred milliseconds on low-end phones). Always call `authenticate()` and `insertUser()` from a **background thread**.

### Login flow in `LoginActivity`

```kotlin
// onCreate: seed on the background executor
executor.execute {
    try { db.seedInitialUser() } catch (e: Exception) { /* login will reject all credentials */ }
}

// loginUser(): authenticate on the same single-thread executor
executor.execute {
    val userName = try { db.authenticate(email, password) } catch (e: Exception) { null }

    runOnUiThread {
        if (isFinishing || isDestroyed) return@runOnUiThread
        setLoading(false)

        if (userName != null) {
            saveCredentials(remember, userName, email)
            navigateToMain(userName)
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
        }
    }
}
```

Because `LoginActivity` uses a **single-thread** executor, the seed task always finishes before any login attempt queued after it.

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

> **Warning:** If the database contains a value that doesn't match any enum constant, `valueOf` throws an `IllegalArgumentException`. Use `enumValueOf` (Kotlin 1.9+) or a `try/catch` if the data might be inconsistent.

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

### With Kotlin's `use` (auto-close)

```kotlin
db.getById(id).use { cursor ->
    if (cursor.moveToFirst()) {
        val name = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_NAME))
        // use name...
    }
}   // cursor is closed here, even if an exception is thrown
```

`AppDatabase.authenticate()` uses this pattern internally.

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

The name passed on is the one **stored in the `users` table**, not whatever was typed on the login form.

```kotlin
// In LoginActivity (userName is the value returned by db.authenticate)
val intent = Intent(this, MainActivity::class.java)
intent.putExtra("student_name", userName)
startActivity(intent)
finish()  // remove login from back stack
```

```kotlin
// In MainActivity
val studentName = intent.getStringExtra("student_name") ?: "Student"
```

---

## Database Versioning & Migration

When you add or change tables or columns, bump `DATABASE_VERSION` and handle the migration in `onUpgrade`. The current version is **2**, and the existing migration looks like this:

```kotlin
companion object {
    private const val DATABASE_VERSION = 2
    // ...
}

override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) createUsersTable(db)
    // Add future migrations below
}
```

New installs run `onCreate`, which creates both tables. Existing installs run `onUpgrade`, which only adds what is missing, so **registered students are preserved**.

### Adding a future migration

Use one `if (oldVersion < N)` block per version so that a device jumping several versions runs every step in order. For example, adding a phone column to students as version 3:

```kotlin
private const val DATABASE_VERSION = 3

override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) createUsersTable(db)
    if (oldVersion < 3) {
        db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_PHONE TEXT")
    }
}
```

> **Do NOT** use `DROP TABLE` in `onUpgrade` in production — it deletes all user data. The original version-1 implementation did this for simplicity during development; it has been replaced.

> **During development:** if the schema gets into a bad state, uninstalling the app on the test device clears the database and runs `onCreate` again.

---

## Thread Safety

- Run every database call (`insert`, `update`, `delete`, `queryAll`, `authenticate`, `insertUser`, `seedInitialUser`) **off the main thread**. Password hashing in particular is deliberately slow.
- `RegisterActivity` and `LoginActivity` each use a single-thread `ExecutorService` and update the UI with `runOnUiThread`. Shut the executor down in `onDestroy()`.
- Check `isFinishing || isDestroyed` inside `runOnUiThread` before touching views, in case the user rotated the screen or left the Activity while the task was running.
- `AppDatabase` is a singleton shared by all Activities, and SQLite serialises writes, so concurrent use from several single-thread executors is safe for a simple app like this.
- If you later add background workers or Room, move to a lifecycle-aware approach (coroutines or Room's built-in async support).

---

## Quick Reference

|Task|Method Call|
|---|---|
|Get instance|`AppDatabase.getInstance(context)`|
|Insert student|`db.insert(name, year, type, age, gender, course)`|
|Update student (partial)|`db.update(id, name = "X")`|
|Delete student|`db.delete(id)`|
|All students|`db.queryAll()`|
|Single student|`db.getById(id)`|
|Seed default user|`db.seedInitialUser()`|
|Create user|`db.insertUser(name, email, password)`|
|Log in|`db.authenticate(email, password)` (returns name or `null`)|
|Close cursor|`cursor.close()` or `cursor.use { }`|

---

## Security Notes

- Passwords are stored as salted PBKDF2 hashes; the plain text is never written to the database or to `SharedPreferences`.
- "Remember me" stores only the **name and email**, never the password.
- The seeded account and the "Load Sample Data" menu item are for development. Remove both before shipping.
- This is a local, on-device login. It protects against casual inspection of the database, but it is not a substitute for server-side authentication in a production app.