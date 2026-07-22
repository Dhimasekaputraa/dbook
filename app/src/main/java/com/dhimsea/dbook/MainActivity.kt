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
import com.dhimsea.dbook.core.designsystem.DbookTheme
import com.dhimsea.dbook.ui.library.LibraryScreen
import com.dhimsea.dbook.ui.library.LibraryViewModel
import com.dhimsea.dbook.ui.library.LibraryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DbookApplication
        val repository = app.bookRepository

        enableEdgeToEdge()
        setContent {
            DbookTheme {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModelFactory(repository)
                )
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