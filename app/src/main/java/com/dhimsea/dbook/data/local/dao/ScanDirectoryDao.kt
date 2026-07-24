package com.dhimsea.dbook.data.local.dao

import androidx.room.*
import com.dhimsea.dbook.data.local.entity.ScanDirectoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDirectoryDao {
    @Query("SELECT * FROM scan_directories ORDER BY addedAt DESC")
    fun getAllDirectories(): Flow<List<ScanDirectoryEntity>>

    @Query("SELECT * FROM scan_directories LIMIT 1")
    suspend fun getActiveDirectory(): ScanDirectoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectory(directory: ScanDirectoryEntity)

    @Query("DELETE FROM scan_directories")
    suspend fun clearAllDirectories()
}