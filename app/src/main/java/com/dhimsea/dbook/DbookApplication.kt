package com.dhimsea.dbook

import android.app.Application
import androidx.room.Room
import com.dhimsea.dbook.data.local.AppDatabase
import com.dhimsea.dbook.data.repository.BookRepositoryImpl
import com.dhimsea.dbook.domain.repository.BookRepository

class DbookApplication : Application() {
//    instance database (lazy initialization)
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "dbook_database"
        ).build()
    }
    val bookRepository: BookRepository by lazy {
        BookRepositoryImpl(database.bookDao())
    }
}