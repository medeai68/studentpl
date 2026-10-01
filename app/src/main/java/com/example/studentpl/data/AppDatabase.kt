package com.example.studentpl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studentpl.data.dao.ExamDao
import com.example.studentpl.data.dao.SubjectDao
import com.example.studentpl.data.dao.TaskDao
import com.example.studentpl.data.dao.UserDao
import com.example.studentpl.model.Exam
import com.example.studentpl.model.Subject
import com.example.studentpl.model.Task
import com.example.studentpl.model.User

@Database(
    entities = [Subject::class, Task::class, Exam::class, User::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun examDao(): ExamDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_planner_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
