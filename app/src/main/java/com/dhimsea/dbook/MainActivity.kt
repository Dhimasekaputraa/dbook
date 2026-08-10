package com.dhimsea.dbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhimsea.dbook.core.designsystem.DbookTheme
import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.ui.annotation.AnnotationScreen
import com.dhimsea.dbook.ui.annotation.AnnotationViewModel
import com.dhimsea.dbook.ui.annotation.AnnotationViewModelFactory
import com.dhimsea.dbook.ui.library.LibraryScreen
import com.dhimsea.dbook.ui.library.LibraryViewModel
import com.dhimsea.dbook.ui.library.LibraryViewModelFactory
import com.dhimsea.dbook.ui.navigation.Screen
import com.dhimsea.dbook.ui.reader.ChapterMarker
import com.dhimsea.dbook.ui.reader.ReaderScreen
import com.dhimsea.dbook.ui.search.SearchResultsScreen
import com.dhimsea.dbook.ui.search.SearchViewModel
import com.dhimsea.dbook.ui.toc.TocScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
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
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onBookClick = { book ->
                                val encodedPath = URLEncoder.encode(book.filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate(Screen.Reader.createRoute(encodedPath))
                            },
                            onAnnotationClick = { book ->
                                navController.navigate("annotation/${book.id}")
                            }
                        )
                    }

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
                        val targetPercent = backStackEntry.savedStateHandle.get<Float>("targetPercent")
                        val targetHref = backStackEntry.savedStateHandle.get<String>("targetHref")

                        ReaderScreen(
                            filePath = decodedPath,
                            initialCfiToJump = cfiToJump,
                            targetHrefToJump = targetHref,
                            targetPercentToJump = targetPercent,
                            searchQueryToHighlight = searchQuery,
                            bookRepository = app.bookRepository,
                            onOpenAnnotationScreen = { bookId ->
                                navController.navigate("annotation/$bookId")
                            },
                            onOpenTocScreen = { bookId, chapterList, chapterName, pageNum ->
                                navController.currentBackStackEntry?.savedStateHandle?.apply {
                                    set("chaptersList", chapterList)
                                    set("currentChapterName", chapterName)
                                    set("currentPage", pageNum)
                                }
                                navController.navigate("toc/$bookId")
                            },
                            onNavigateToSearch = { bookId, queryText, resultsJson ->
                                val encodedQuery = URLEncoder.encode(queryText, StandardCharsets.UTF_8.toString())
                                val encodedPathForSearch = URLEncoder.encode(decodedPath, StandardCharsets.UTF_8.toString())
                                navController.navigate("search_results/$bookId/$encodedQuery/$encodedPathForSearch")
                                navController.currentBackStackEntry?.savedStateHandle?.set("resultsJson", resultsJson)
                            },
                            onBack = { navController.popBackStack() },
                            onHrefJumpHandled = {
                                backStackEntry.savedStateHandle.remove<String>("targetHref")
                            }
                        )
                    }

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
                    
                    composable(
                        route = "annotation/{bookId}",
                        arguments = listOf(
                            navArgument("bookId") { type = NavType.LongType }
                        ),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
                        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) },
                        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
                    ) { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L

                        LaunchedEffect(bookId) {
                            annotationViewModel.loadBook(bookId)
                        }

                        val currentBook by annotationViewModel.book.collectAsState()
                        val annotations by annotationViewModel.getAnnotationsForBook(bookId)
                            .collectAsState(initial = emptyList<com.dhimsea.dbook.domain.model.Annotation>())

                        AnnotationScreen(
                            bookTitle = currentBook?.title ?: "Annotation",
                            bookAuthor = currentBook?.author ?: "Unknown Author",
                            annotations = annotations,
                            onAnnotationClick = { annotation ->
                                currentBook?.filePath?.let { filePath ->
                                    val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                                    navController.navigate(Screen.Reader.createRoute(encodedPath)) {
                                        popUpTo(Screen.Library.route)
                                    }
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("targetCfi", annotation.cfi)
                                }
                            },
                            onDeleteAnnotation = { annotation ->
                                annotationViewModel.deleteAnnotation(annotation)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "toc/{bookId}",
                        arguments = listOf(
                            navArgument("bookId") { type = NavType.LongType }
                        ),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
                        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) },
                        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
                    ) { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L

                        val previousBackStackEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(Screen.Reader.route)
                        }

                        LaunchedEffect(bookId) {
                            annotationViewModel.loadBook(bookId)
                        }
                        val currentBook by annotationViewModel.book.collectAsState()

                        val chapterList = previousBackStackEntry.savedStateHandle.get<List<ChapterMarker>>("chaptersList") ?: emptyList()
                        val currentChapterName = previousBackStackEntry.savedStateHandle.get<String>("currentChapterName") ?: ""

                        val currentPageNum = previousBackStackEntry.savedStateHandle.get<Int>("currentPage") ?: 1

                        TocScreen(
                            bookTitle = currentBook?.title ?: "Table of Contents",
                            chapters = chapterList,
                            currentChapter = currentChapterName,
                            currentPage = currentPageNum,
                            onChapterClick = { targetHref ->
                                previousBackStackEntry.savedStateHandle["targetHref"] = targetHref
                                navController.popBackStack() 
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}