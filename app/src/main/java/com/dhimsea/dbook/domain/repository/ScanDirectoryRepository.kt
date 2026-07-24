package com.dhimsea.dbook.domain.repository

import android.net.Uri
import com.dhimsea.dbook.data.local.entity.ScanDirectoryEntity
import kotlinx.coroutines.flow.Flow

interface ScanDirectoryRepository {
    fun getActiveDirectory(): Flow<ScanDirectoryEntity?>
    suspend fun setDirectory(uri: Uri, displayName: String)
    suspend fun clearDirectory()
}