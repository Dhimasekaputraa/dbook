package com.dhimsea.dbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.entity.BookEntity

@Database(
    entities = [BookEntity::class], // ScanDirectoryEntity dihapus
    version = 4,                   // Naikkan versi ke 4
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao // scanDirectoryDao() dihapus

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

        // MIGRASI BARU: Menghapus tabel scan_directories saat update ke version 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS scan_directories")
            }
        }
    }
}