package com.example.studentpl.data

import android.util.Log
import com.example.studentpl.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

object DatabaseConnector {
    private const val TAG = "DatabaseConnector"

    // Connection config — adjust USER / PASSWORD / DB_NAME to match your MySQL setup.
    private const val USER = "root_phone"
    private const val PASSWORD = ""
    private const val DB_NAME = "testdb"
    private const val PORT = 3306

    private fun buildUrl(host: String) =
        "jdbc:mariadb://$host:$PORT/$DB_NAME" +
                "?useSSL=false" +
                "&serverTimezone=UTC" +
                "&allowPublicKeyRetrieval=true" +
                "&connectTimeout=5000" +
                "&socketTimeout=10000"

    // Try every possible way to reach the PC's MySQL, in order.
    //  10.0.2.2      → Android-emulator alias for the host PC (most reliable on emulator)
    //  10.29.14.181  → your PC's LAN IP  (for physical devices on the same WiFi)
    //  127.0.0.1     → localhost-v4      (works with `adb reverse tcp:3306 tcp:3306`)
    //  192.168.1.x   → common home-router subnet
    private val FALLBACK_HOSTS = listOf(
        "127.0.0.1",
        "10.0.2.2",
        "10.29.14.196",
        "10.31.24.249",
        "10.29.14.181",
        "192.168.1.100",
        "192.168.0.100"
    )

    init {
        try {
            Class.forName("org.mariadb.jdbc.Driver")
            Log.d(TAG, "MariaDB Driver loaded")
        } catch (e: Throwable) {
            Log.e(TAG, "FATAL: MariaDB Driver not found", e)
        }
    }

    /**
     * Try to connect using [DB_HOST] first, then each fallback.
     * Returns the connection + the host that worked, or null + the last error message.
     */
    private fun tryConnect(): Pair<Connection?, String?> {
        for (host in FALLBACK_HOSTS) {
            Log.d(TAG, "Trying $host:$PORT as $USER ...")
            try {
                val props = Properties()
                props.setProperty("user", USER)
                props.setProperty("password", PASSWORD)
                val conn = DriverManager.getConnection(buildUrl(host), props)
                if (conn != null) {
                    Log.d(TAG, "Connected via $host")
                    return conn to null
                }
            } catch (t: Throwable) {
                val msg = t.message ?: t.javaClass.simpleName
                Log.w(TAG, "$host failed: $msg")
                // If it's a password/auth issue, don't bother trying other hosts
                if (msg.contains("password: NO") || msg.contains("Access denied")) {
                    return null to "Access denied — check MySQL user '$USER' permissions"
                }
                // Continue to next host
            }
        }
        val tried = FALLBACK_HOSTS.joinToString(", ")
        return null to "Cannot reach MySQL.\nTried: $tried\n\n• Ensure MySQL is running on your PC\n• For emulator: no setup needed (10.0.2.2)\n• For physical device: use your PC's LAN IP\n• Check Windows Firewall allows port 3306"
    }

    private fun ensureSimoappTable(connection: Connection) {
        try {
            val statement = connection.createStatement()
            // Ensure basic table exists
            statement.execute("""
                CREATE TABLE IF NOT EXISTS simoapp (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) UNIQUE,
                    password VARCHAR(255)
                )
            """.trimIndent())
        } catch (e: SQLException) {
            Log.e(TAG, "Critical error ensuring table structure: ${e.message}", e)
        }
    }

    private fun logTableStructure(connection: Connection) {
        try {
            val dbm = connection.metaData
            val rs = dbm.getColumns(null, null, "simoapp", null)
            Log.d(TAG, "--- Remote MySQL Table Structure: 'simoapp' ---")
            while (rs.next()) {
                val name = rs.getString("COLUMN_NAME")
                val nullable = rs.getString("IS_NULLABLE")
                val defaultVal = rs.getString("COLUMN_DEF")
                Log.d(TAG, "Column: $name | Nullable: $nullable | Default: $defaultVal")
            }
            Log.d(TAG, "-----------------------------------------------")
        } catch (e: SQLException) {
            Log.e(TAG, "Metadata check failed: ${e.message}")
        }
    }

    suspend fun authenticate(email: String, password: String): Pair<User?, String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "authenticate() for: $email")
            val (connection, errMsg) = tryConnect()
            if (connection == null) return@withContext null to (errMsg ?: "CONNECTION_ERROR")
            try {
                ensureSimoappTable(connection)
                logTableStructure(connection)
                
                val query = "SELECT * FROM simoapp WHERE email = ? AND password = ?"
                val statement = connection.prepareStatement(query)
                statement.setString(1, email)
                statement.setString(2, password)
                
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    val user = User(
                        id = resultSet.getInt("id"),
                        email = resultSet.getString("email") ?: email
                    )
                    Log.d(TAG, "Auth Success: Found user ${user.email}")
                    return@withContext user to "SUCCESS"
                } else {
                    Log.w(TAG, "Auth Failed: Invalid credentials for $email")
                    return@withContext null to "INVALID_CREDENTIALS"
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Auth SQL Error: ${e.message} [Code: ${e.errorCode}]", e)
                return@withContext null to "DATABASE_ERROR"
            } finally {
                try { connection.close() } catch (e: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Auth Fatal Throwable: ${t.message}", t)
            return@withContext null to "DATABASE_ERROR"
        }
    }

    suspend fun register(email: String, password: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "register() for: $email")
            val (connection, errMsg) = tryConnect()
            if (connection == null) return@withContext (errMsg ?: "CONNECTION_ERROR")
            try {
                ensureSimoappTable(connection)
                
                // 1. Check if user already exists
                val checkStmt = connection.prepareStatement("SELECT id FROM simoapp WHERE email = ?")
                checkStmt.setString(1, email)
                if (checkStmt.executeQuery().next()) {
                    Log.w(TAG, "Reg Failed: Email $email already registered")
                    return@withContext "EMAIL_EXISTS"
                }

                // 2. Perform Insert with all columns to satisfy constraints
                val insertQuery = "INSERT INTO simoapp (email, password) VALUES (?, ?)"
                val statement = connection.prepareStatement(insertQuery)
                statement.setString(1, email)
                statement.setString(2, password)
                
                val success = statement.executeUpdate() > 0
                Log.d(TAG, "Reg Result for $email: $success")
                return@withContext if (success) "SUCCESS" else "FAILURE"
            } catch (e: SQLException) {
                Log.e(TAG, "Reg SQL Error: ${e.message} [Code: ${e.errorCode}]", e)
                "DATABASE_ERROR"
            } finally {
                try { connection.close() } catch (e: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Reg Fatal Throwable: ${t.message}", t)
            "DATABASE_ERROR"
        }
    }

    // Sync Tasks
    suspend fun syncTask(task: com.example.studentpl.model.Task, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext false
        try {
            connection.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    remote_id INT,
                    title VARCHAR(255),
                    description TEXT,
                    due_date VARCHAR(50),
                    completed BOOLEAN,
                    user_email VARCHAR(255),
                    reminder_enabled BOOLEAN DEFAULT TRUE,
                    reminder_days_before VARCHAR(100) DEFAULT '0,1,2',
                    reminder_time VARCHAR(10) DEFAULT '09:00',
                    UNIQUE KEY(remote_id, user_email)
                )
            """.trimIndent())
            // Add columns if table already existed without them
            try { connection.createStatement().execute("ALTER TABLE tasks ADD COLUMN reminder_enabled BOOLEAN DEFAULT TRUE") } catch (_: Exception) {}
            try { connection.createStatement().execute("ALTER TABLE tasks ADD COLUMN reminder_days_before VARCHAR(100) DEFAULT '0,1,2'") } catch (_: Exception) {}
            try { connection.createStatement().execute("ALTER TABLE tasks ADD COLUMN reminder_time VARCHAR(10) DEFAULT '09:00'") } catch (_: Exception) {}

            val statement = connection.prepareStatement("""
                INSERT INTO tasks (remote_id, title, description, due_date, completed, user_email, reminder_enabled, reminder_days_before, reminder_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                title = VALUES(title), description = VALUES(description),
                due_date = VALUES(due_date), completed = VALUES(completed),
                reminder_enabled = VALUES(reminder_enabled),
                reminder_days_before = VALUES(reminder_days_before),
                reminder_time = VALUES(reminder_time)
            """.trimIndent())
            statement.setInt(1, task.id)
            statement.setString(2, task.title)
            statement.setString(3, task.description)
            statement.setString(4, task.dueDate)
            statement.setBoolean(5, task.completed)
            statement.setString(6, userEmail)
            statement.setBoolean(7, task.reminderEnabled)
            statement.setString(8, task.reminderDaysBefore)
            statement.setString(9, task.reminderTime)
            statement.executeUpdate() > 0
        } catch (e: SQLException) {
            Log.e(TAG, "Sync task error: ${e.message}", e)
            false
        } finally {
            try { connection.close() } catch (e: SQLException) { e.printStackTrace() }
        }
    }

    suspend fun deleteTask(taskId: Int, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext false
        try {
            val statement = connection.prepareStatement("DELETE FROM tasks WHERE remote_id = ? AND user_email = ?")
            statement.setInt(1, taskId)
            statement.setString(2, userEmail)
            statement.executeUpdate() > 0
        } catch (e: SQLException) {
            Log.e(TAG, "Delete task error: ${e.message}", e)
            false
        } finally {
            try { connection.close() } catch (e: SQLException) { e.printStackTrace() }
        }
    }

    // Sync Exams
    suspend fun syncExam(exam: com.example.studentpl.model.Exam, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext false
        try {
            connection.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS exams (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    remote_id INT,
                    subject VARCHAR(255),
                    exam_date VARCHAR(50),
                    notes TEXT,
                    user_email VARCHAR(255),
                    reminder_enabled BOOLEAN DEFAULT TRUE,
                    reminder_days_before VARCHAR(100) DEFAULT '0,1,3,7',
                    reminder_time VARCHAR(10) DEFAULT '08:00',
                    UNIQUE KEY(remote_id, user_email)
                )
            """.trimIndent())
            try { connection.createStatement().execute("ALTER TABLE exams ADD COLUMN reminder_enabled BOOLEAN DEFAULT TRUE") } catch (_: Exception) {}
            try { connection.createStatement().execute("ALTER TABLE exams ADD COLUMN reminder_days_before VARCHAR(100) DEFAULT '0,1,3,7'") } catch (_: Exception) {}
            try { connection.createStatement().execute("ALTER TABLE exams ADD COLUMN reminder_time VARCHAR(10) DEFAULT '08:00'") } catch (_: Exception) {}

            val statement = connection.prepareStatement("""
                INSERT INTO exams (remote_id, subject, exam_date, notes, user_email, reminder_enabled, reminder_days_before, reminder_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                subject = VALUES(subject), exam_date = VALUES(exam_date), notes = VALUES(notes),
                reminder_enabled = VALUES(reminder_enabled),
                reminder_days_before = VALUES(reminder_days_before),
                reminder_time = VALUES(reminder_time)
            """.trimIndent())
            statement.setInt(1, exam.id)
            statement.setString(2, exam.subject)
            statement.setString(3, exam.examDate)
            statement.setString(4, exam.notes)
            statement.setString(5, userEmail)
            statement.setBoolean(6, exam.reminderEnabled)
            statement.setString(7, exam.reminderDaysBefore)
            statement.setString(8, exam.reminderTime)
            statement.executeUpdate() > 0
        } catch (e: SQLException) {
            Log.e(TAG, "Sync exam error: ${e.message}", e)
            false
        } finally {
            try { connection.close() } catch (e: SQLException) { e.printStackTrace() }
        }
    }

    suspend fun deleteExam(examId: Int, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext false
        try {
            val statement = connection.prepareStatement("DELETE FROM exams WHERE remote_id = ? AND user_email = ?")
            statement.setInt(1, examId)
            statement.setString(2, userEmail)
            statement.executeUpdate() > 0
        } catch (e: SQLException) {
            Log.e(TAG, "Delete exam error: ${e.message}", e)
            false
        } finally {
            try { connection.close() } catch (e: SQLException) { e.printStackTrace() }
        }
    }

    // Sync Subjects
    suspend fun syncSubject(subject: com.example.studentpl.model.Subject, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext false
        try {
            val createTable = """
                CREATE TABLE IF NOT EXISTS subjects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    remote_id INT,
                    name VARCHAR(255),
                    color VARCHAR(50),
                    user_email VARCHAR(255),
                    UNIQUE KEY(remote_id, user_email)
                )
            """.trimIndent()
            connection.createStatement().execute(createTable)

            val query = """
                INSERT INTO subjects (remote_id, name, color, user_email) 
                VALUES (?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                name = VALUES(name), color = VALUES(color)
            """.trimIndent()
            val statement = connection.prepareStatement(query)
            statement.setInt(1, subject.id)
            statement.setString(2, subject.name)
            statement.setString(3, subject.color)
            statement.setString(4, userEmail)
            statement.executeUpdate() > 0
        } catch (e: SQLException) {
            Log.e(TAG, "Sync subject error: ${e.message}", e)
            false
        } finally {
            try { connection.close() } catch (e: SQLException) { e.printStackTrace() }
        }
    }

    suspend fun deleteSubject(subjectId: Int, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext false
        try {
            val statement = connection.prepareStatement("DELETE FROM subjects WHERE remote_id = ? AND user_email = ?")
            statement.setInt(1, subjectId)
            statement.setString(2, userEmail)
            statement.executeUpdate() > 0
        } catch (e: SQLException) {
            Log.e(TAG, "Delete subject error: ${e.message}", e)
            false
        } finally {
            try { connection.close() } catch (e: SQLException) { e.printStackTrace() }
        }
    }

    // ── Fetch data for a user (pull from server on login) ────────────────

    suspend fun fetchTasks(userEmail: String): List<com.example.studentpl.model.Task> = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext emptyList()
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS tasks (id INT AUTO_INCREMENT PRIMARY KEY, remote_id INT, title VARCHAR(255), description TEXT, due_date VARCHAR(50), completed BOOLEAN, user_email VARCHAR(255), reminder_enabled BOOLEAN DEFAULT TRUE, reminder_days_before VARCHAR(100) DEFAULT '0,1,2', reminder_time VARCHAR(10) DEFAULT '09:00', UNIQUE KEY(remote_id, user_email))")
            val stmt = connection.prepareStatement("SELECT * FROM tasks WHERE user_email = ?")
            stmt.setString(1, userEmail)
            val rs = stmt.executeQuery()
            val list = mutableListOf<com.example.studentpl.model.Task>()
            while (rs.next()) {
                list.add(com.example.studentpl.model.Task(
                    id = 0, title = rs.getString("title") ?: "",
                    description = rs.getString("description") ?: "",
                    dueDate = rs.getString("due_date") ?: "",
                    completed = rs.getBoolean("completed"),
                    userEmail = userEmail,
                    reminderEnabled = rs.getBoolean("reminder_enabled"),
                    reminderDaysBefore = rs.getString("reminder_days_before") ?: "0,1,2",
                    reminderTime = rs.getString("reminder_time") ?: "09:00"
                ))
            }
            list
        } catch (e: SQLException) {
            Log.e(TAG, "Fetch tasks error: ${e.message}", e)
            emptyList()
        } finally {
            try { connection.close() } catch (_: Exception) {}
        }
    }

    suspend fun fetchExams(userEmail: String): List<com.example.studentpl.model.Exam> = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext emptyList()
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS exams (id INT AUTO_INCREMENT PRIMARY KEY, remote_id INT, subject VARCHAR(255), exam_date VARCHAR(50), notes TEXT, user_email VARCHAR(255), reminder_enabled BOOLEAN DEFAULT TRUE, reminder_days_before VARCHAR(100) DEFAULT '0,1,3,7', reminder_time VARCHAR(10) DEFAULT '08:00', UNIQUE KEY(remote_id, user_email))")
            val stmt = connection.prepareStatement("SELECT * FROM exams WHERE user_email = ?")
            stmt.setString(1, userEmail)
            val rs = stmt.executeQuery()
            val list = mutableListOf<com.example.studentpl.model.Exam>()
            while (rs.next()) {
                list.add(com.example.studentpl.model.Exam(
                    id = 0, subject = rs.getString("subject") ?: "",
                    examDate = rs.getString("exam_date") ?: "",
                    notes = rs.getString("notes") ?: "",
                    userEmail = userEmail,
                    reminderEnabled = rs.getBoolean("reminder_enabled"),
                    reminderDaysBefore = rs.getString("reminder_days_before") ?: "0,1,3,7",
                    reminderTime = rs.getString("reminder_time") ?: "08:00"
                ))
            }
            list
        } catch (e: SQLException) {
            Log.e(TAG, "Fetch exams error: ${e.message}", e)
            emptyList()
        } finally {
            try { connection.close() } catch (_: Exception) {}
        }
    }

    suspend fun fetchSubjects(userEmail: String): List<com.example.studentpl.model.Subject> = withContext(Dispatchers.IO) {
        val connection = tryConnect().first ?: return@withContext emptyList()
        try {
            connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS subjects (id INT AUTO_INCREMENT PRIMARY KEY, remote_id INT, name VARCHAR(255), color VARCHAR(50), user_email VARCHAR(255), UNIQUE KEY(remote_id, user_email))"
            )
            val stmt = connection.prepareStatement("SELECT * FROM subjects WHERE user_email = ?")
            stmt.setString(1, userEmail)
            val rs = stmt.executeQuery()
            val list = mutableListOf<com.example.studentpl.model.Subject>()
            while (rs.next()) {
                list.add(com.example.studentpl.model.Subject(
                    id = 0,
                    name = rs.getString("name") ?: "",
                    color = rs.getString("color") ?: "#6C5CE7",
                    userEmail = userEmail
                ))
            }
            list
        } catch (e: SQLException) {
            Log.e(TAG, "Fetch subjects error: ${e.message}", e)
            emptyList()
        } finally {
            try { connection.close() } catch (_: Exception) {}
        }
    }
}
