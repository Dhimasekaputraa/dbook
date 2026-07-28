package com.dhimsea.dbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dhimsea.dbook.core.designsystem.DbookTheme
import com.dhimsea.dbook.ui.library.LibraryScreen
import com.dhimsea.dbook.ui.library.LibraryViewModel
import com.dhimsea.dbook.ui.library.LibraryViewModelFactory
import com.dhimsea.dbook.ui.navigation.Screen
import com.dhimsea.dbook.ui.reader.ReaderScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DbookApplication

        enableEdgeToEdge()
        setContent {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModelFactory(
                    bookRepository = app.bookRepository,
                    context = applicationContext
                )
            )

            DbookTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Screen.Library.route) {
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onBookClick = { book ->
                                val encodedPath = URLEncoder.encode(book.filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate(Screen.Reader.createRoute(encodedPath))
                            }
                        )
                    }

                    composable(Screen.Reader.route) { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                        val decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())

                        ReaderScreen(
                            filePath = decodedPath,
                            bookRepository = app.bookRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}