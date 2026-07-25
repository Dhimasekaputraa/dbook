package com.dhimsea.dbook.ui.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Reader : Screen("reader/{filePath}") {
        fun createRoute(encodedPath: String) = "reader/$encodedPath"
    }
}