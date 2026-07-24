package com.dhimsea.dbook.domain.repository

import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val isDarkMode: Flow<Boolean>
    val isDynamicColor: Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setDynamicColor(enabled: Boolean)
}