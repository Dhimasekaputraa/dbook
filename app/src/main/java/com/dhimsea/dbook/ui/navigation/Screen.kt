package com.dhimsea.dbook.ui.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    
    // Kita butuh meneruskan URL/Path file epub ke Reader
    object Reader : Screen("reader/{encodedUri}") {
        fun createRoute(encodedUri: String) = "reader/$encodedUri"
    }
}