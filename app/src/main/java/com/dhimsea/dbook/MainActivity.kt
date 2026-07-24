package com.dhimsea.dbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dhimsea.dbook.core.designsystem.DbookTheme
import com.dhimsea.dbook.ui.library.LibraryScreen
import com.dhimsea.dbook.ui.library.LibraryViewModel
import com.dhimsea.dbook.ui.library.LibraryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DbookApplication

        enableEdgeToEdge()
        setContent {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModelFactory(
                    bookRepository = app.bookRepository,
                    scanRepository = app.scanDirectoryRepository,
                    themeRepository = app.themeRepository,
                    context = applicationContext
                )
            )

            val isDarkMode by libraryViewModel.isDarkMode.collectAsState()
            val isDynamicColor by libraryViewModel.isDynamicColor.collectAsState()

            DbookTheme(
                darkTheme = isDarkMode,
                dynamicColor = isDynamicColor
            ) {
                LibraryScreen(viewModel = libraryViewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DbookTheme {
        Greeting("Android")
    }
}