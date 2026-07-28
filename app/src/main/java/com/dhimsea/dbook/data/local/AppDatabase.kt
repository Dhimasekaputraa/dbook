package com.dhimsea.dbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.dao.AnnotationDao
import com.dhimsea.dbook.data.local.entity.BookEntity
import com.dhimsea.dbook.data.local.entity.AnnotationEntity

@Database(
    entities = [
        BookEntity::class, 
        AnnotationEntity::class // <--- 1. Wajib ditambahkan di sini
    ],
    version = 5,                   
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scan_directories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uriString TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN fileSize INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS scan_directories")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `annotations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `cfi` TEXT NOT NULL,
                        `chapterName` TEXT NOT NULL,
                        `pageNumber` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `colorHex` TEXT NOT NULL DEFAULT '#FFEB3B',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}