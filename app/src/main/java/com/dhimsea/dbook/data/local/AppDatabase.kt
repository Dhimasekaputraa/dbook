package com.dhimsea.dbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.dao.AnnotationDao
import com.dhimsea.dbook.data.local.dao.ShelfDao
import com.dhimsea.dbook.data.local.entity.BookEntity
import com.dhimsea.dbook.data.local.entity.AnnotationEntity
import com.dhimsea.dbook.data.local.entity.ShelfEntity
import com.dhimsea.dbook.data.local.entity.ShelfBookCrossRef

@Database(
    entities = [
        BookEntity::class, 
        AnnotationEntity::class,
        ShelfEntity::class,
        ShelfBookCrossRef::class
    ],
    version = 8,                   
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun shelfDao(): ShelfDao

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shelves` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shelf_book_cross_ref` (
                        `shelfId` INTEGER NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        PRIMARY KEY(`shelfId`, `bookId`),
                        FOREIGN KEY(`shelfId`) REFERENCES `shelves`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_shelf_book_cross_ref_bookId` 
                    ON `shelf_book_cross_ref` (`bookId`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_shelf_book_cross_ref_bookId` 
                    ON `shelf_book_cross_ref` (`bookId`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shelf_book_cross_ref ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}