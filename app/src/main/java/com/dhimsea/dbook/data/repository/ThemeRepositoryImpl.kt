package com.dhimsea.dbook.data.repository

import com.dhimsea.dbook.data.local.datastore.ThemePreferences
import com.dhimsea.dbook.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

class ThemeRepositoryImpl(
    private val prefs: ThemePreferences
) : ThemeRepository {
    override val isDarkMode: Flow<Boolean> = prefs.isDarkMode
    override val isDynamicColor: Flow<Boolean> = prefs.isDynamicColor
    override suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)
    override suspend fun setDynamicColor(enabled: Boolean) = prefs.setDynamicColor(enabled)
}