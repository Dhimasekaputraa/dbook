package com.dhimsea.dbook.data.repository

import android.net.Uri
import com.dhimsea.dbook.data.local.dao.ScanDirectoryDao
import com.dhimsea.dbook.data.local.entity.ScanDirectoryEntity
import com.dhimsea.dbook.domain.repository.ScanDirectoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanDirectoryRepositoryImpl(
    private val dao: ScanDirectoryDao
) : ScanDirectoryRepository {

    override fun getActiveDirectory(): Flow<ScanDirectoryEntity?> {
        return dao.getAllDirectories().map { it.firstOrNull() }
    }

    override suspend fun setDirectory(uri: Uri, displayName: String) {
        dao.clearAllDirectories()
        dao.insertDirectory(
            ScanDirectoryEntity(
                uriString = uri.toString(),
                displayName = displayName
            )
        )
    }

    override suspend fun clearDirectory() {
        dao.clearAllDirectories()
    }
}