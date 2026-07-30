package com.dhimsea.dbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhimsea.dbook.core.designsystem.DbookTheme
import com.dhimsea.dbook.ui.annotation.AnnotationScreen
import com.dhimsea.dbook.ui.annotation.AnnotationViewModel
import com.dhimsea.dbook.ui.annotation.AnnotationViewModelFactory
import com.dhimsea.dbook.ui.bookdetail.BookDetailScreen
import com.dhimsea.dbook.ui.bookdetail.BookDetailViewModel
import com.dhimsea.dbook.ui.bookdetail.BookDetailViewModelFactory
import com.dhimsea.dbook.ui.library.LibraryScreen
import com.dhimsea.dbook.ui.library.LibraryViewModel
import com.dhimsea.dbook.ui.library.LibraryViewModelFactory
import com.dhimsea.dbook.ui.navigation.Screen
import com.dhimsea.dbook.ui.reader.ReaderScreen
import com.dhimsea.dbook.ui.search.SearchResultsScreen
import com.dhimsea.dbook.ui.search.SearchViewModel
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

            val annotationViewModel: AnnotationViewModel = viewModel(
                factory = AnnotationViewModelFactory(app.bookRepository)
            )

            DbookTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController, 
                    startDestination = Screen.Library.route
                ) {
                    // --- LIBRARY SCREEN ---
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onBookClick = { book ->
                                val encodedPath = URLEncoder.encode(book.filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate(Screen.Reader.createRoute(encodedPath))
                            },
                            onAnnotationClick = { book ->
                                val encodedTitle = URLEncoder.encode(book.title, StandardCharsets.UTF_8.toString())
                                val encodedPath = URLEncoder.encode(book.filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate("annotation/${book.id}/$encodedTitle/$encodedPath")
                            },
                            onBookDetailClick = { book ->
                                navController.navigate("book_detail/${book.id}")
                            }
                        )
                    }

                    // --- READER SCREEN ---
                    composable(
                        route = Screen.Reader.route,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(250)
                            )
                        }
                    ) { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                        val decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())

                        val cfiToJump = backStackEntry.savedStateHandle.get<String>("targetCfi")
                        val searchQuery = backStackEntry.savedStateHandle.get<String>("searchQuery")

                        ReaderScreen(
                            filePath = decodedPath,
                            initialCfiToJump = cfiToJump,
                            searchQueryToHighlight = searchQuery,
                            bookRepository = app.bookRepository,
                            onOpenAnnotationScreen = { bookId ->
                                val title = "Buku"
                                val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                                val encodedPath = URLEncoder.encode(decodedPath, StandardCharsets.UTF_8.toString())
                                navController.navigate("annotation/$bookId/$encodedTitle/$encodedPath")
                            },
                            onNavigateToSearch = { bookId, queryText, resultsJson ->
                                val encodedQuery = URLEncoder.encode(queryText, StandardCharsets.UTF_8.toString())
                                val encodedPathForSearch = URLEncoder.encode(decodedPath, StandardCharsets.UTF_8.toString())
                                navController.navigate("search_results/$bookId/$encodedQuery/$encodedPathForSearch")
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("resultsJson", resultsJson)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // --- SEARCH RESULTS SCREEN (ROUTE BARU) ---
                    composable(
                        route = "search_results/{bookId}/{queryText}/{filePath}",
                        arguments = listOf(
                            navArgument("bookId") { type = NavType.LongType },
                            navArgument("queryText") { type = NavType.StringType },
                            navArgument("filePath") { type = NavType.StringType }
                        ),
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(250)
                            )
                        }
                    ) { backStackEntry ->
                        val rawQuery = backStackEntry.arguments?.getString("queryText") ?: ""
                        val decodedQuery = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8.toString())

                        val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
                        val decodedFilePath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())

                        val resultsJson = backStackEntry.savedStateHandle.get<String>("resultsJson")

                        val searchViewModel: SearchViewModel = viewModel()
                        val searchResults by searchViewModel.searchResults.collectAsState()
                        val isLoading by searchViewModel.isLoading.collectAsState()

                        LaunchedEffect(resultsJson) {
                            resultsJson?.let { searchViewModel.parseSearchResults(it) }
                        }

                        SearchResultsScreen(
                            queryText = decodedQuery,
                            searchResults = searchResults,
                            isLoading = isLoading,
                            onResultClick = { targetCfi, query ->
                                val encodedPath = URLEncoder.encode(decodedFilePath, StandardCharsets.UTF_8.toString())

                                navController.navigate(Screen.Reader.createRoute(encodedPath)) {
                                    popUpTo(Screen.Library.route)
                                }
                                navController.currentBackStackEntry?.savedStateHandle?.apply {
                                    set("targetCfi", targetCfi)
                                    set("searchQuery", query)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    // --- ANNOTATION SCREEN ---
                    composable(
                        route = "annotation/{bookId}/{bookTitle}/{filePath}",
                        arguments = listOf(
                            navArgument("bookId") { type = NavType.LongType },
                            navArgument("bookTitle") { type = NavType.StringType },
                            navArgument("filePath") { type = NavType.StringType }
                        ),
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(250)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(250)
                            )
                        }
                    ) { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                        val rawTitle = backStackEntry.arguments?.getString("bookTitle") ?: "Buku"
                        val bookTitle = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

                        val rawFilePath = backStackEntry.arguments?.getString("filePath") ?: ""
                        val decodedFilePath = URLDecoder.decode(rawFilePath, StandardCharsets.UTF_8.toString())

                        val annotations by annotationViewModel.getAnnotationsForBook(bookId).collectAsState(initial = emptyList())

                        AnnotationScreen(
                            bookTitle = bookTitle,
                            annotations = annotations,
                            onAnnotationClick = { annotation ->
                                val encodedPath = URLEncoder.encode(decodedFilePath, StandardCharsets.UTF_8.toString())
                                navController.navigate(Screen.Reader.createRoute(encodedPath)) {
                                    popUpTo(Screen.Library.route)
                                }
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("targetCfi", annotation.cfi)
                            },
                            onDeleteAnnotation = { annotation ->
                                annotationViewModel.deleteAnnotation(annotation)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // --- BOOK DETAIL SCREEN ---
                    composable(
                        route = "book_detail/{bookId}",
                        arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(250)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(250)
                            )
                        }
                    ) { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                        
                        val factory = BookDetailViewModelFactory(app.bookRepository, bookId)
                        val viewModel: BookDetailViewModel = viewModel(factory = factory)

                        BookDetailScreen(
                            viewModel = viewModel,
                            onReadClick = { filePath ->
                                val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate(Screen.Reader.createRoute(encodedPath))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}