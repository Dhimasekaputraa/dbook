package com.dhimsea.dbook

import android.app.Application
import androidx.room.Room
import com.dhimsea.dbook.data.local.AppDatabase
import com.dhimsea.dbook.data.repository.BookRepositoryImpl
import com.dhimsea.dbook.domain.repository.BookRepository

class DbookApplication : Application() {

    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "dbook_database"
        )
        .fallbackToDestructiveMigrationOnDowngrade()
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8
        )
        .build()
    }
    
    val bookRepository: BookRepository by lazy {
        BookRepositoryImpl(
            bookDao = database.bookDao(),
            annotationDao = database.annotationDao(),
            shelfDao = database.shelfDao()
        )
    }
}