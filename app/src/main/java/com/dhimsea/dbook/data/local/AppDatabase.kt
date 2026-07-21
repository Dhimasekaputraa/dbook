package com.dhimsea.dbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.entity.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}