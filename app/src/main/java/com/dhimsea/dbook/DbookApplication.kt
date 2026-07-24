package com.dhimsea.dbook

import android.app.Application
import androidx.room.Room
import com.dhimsea.dbook.data.local.AppDatabase
import com.dhimsea.dbook.data.local.datastore.ThemePreferences
import com.dhimsea.dbook.data.repository.BookRepositoryImpl
import com.dhimsea.dbook.data.repository.ScanDirectoryRepositoryImpl
import com.dhimsea.dbook.data.repository.ThemeRepositoryImpl 
import com.dhimsea.dbook.domain.repository.BookRepository
import com.dhimsea.dbook.domain.repository.ScanDirectoryRepository
import com.dhimsea.dbook.domain.repository.ThemeRepository

class DbookApplication : Application() {
//    instance database (lazy initialization)
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "dbook_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }
    val bookRepository: BookRepository by lazy {
        BookRepositoryImpl(database.bookDao())
    }

    val scanDirectoryRepository: ScanDirectoryRepository by lazy {
        ScanDirectoryRepositoryImpl(database.scanDirectoryDao())
    }

     val themeRepository: ThemeRepository by lazy {
        ThemeRepositoryImpl(ThemePreferences(applicationContext))
    }
}