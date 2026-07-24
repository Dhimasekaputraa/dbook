package com.dhimsea.dbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_directories")
data class ScanDirectoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,     
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)