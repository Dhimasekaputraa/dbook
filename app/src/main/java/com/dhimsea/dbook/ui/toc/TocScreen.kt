package com.dhimsea.dbook.ui.toc

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhimsea.dbook.ui.reader.ChapterMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocScreen(
    bookTitle: String,
    chapters: List<ChapterMarker>,
    currentChapter: String,
    currentPage: Int = 1,
    onChapterClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Table of Contents: $bookTitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chapters available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val activeChapterIndex = remember(chapters, currentChapter, currentPage) {
                if (chapters.isEmpty()) return@remember 0

                val currentCleanName = currentChapter.trim()

                val targetSubLabel = if (currentCleanName.contains(": ")) {
                    currentCleanName.split(": ").lastOrNull()?.trim() ?: currentCleanName
                } else {
                    currentCleanName
                }

                val matchingIndices = chapters.indices.filter { idx ->
                    val label = chapters[idx].label.trim()
                    label.equals(currentCleanName, ignoreCase = true) || label.equals(targetSubLabel, ignoreCase = true)
                }

                val chosenIndex = when {

                    matchingIndices.size > 1 -> {

                        val candidateBelowCurrentPage = matchingIndices
                            .filter { idx -> chapters[idx].pageNum <= currentPage }
                            .maxByOrNull { idx -> chapters[idx].pageNum }

                        candidateBelowCurrentPage ?: matchingIndices.minByOrNull { idx -> 
                            kotlin.math.abs(chapters[idx].pageNum - currentPage) 
                        } ?: matchingIndices.first()
                    }

                    matchingIndices.size == 1 -> matchingIndices.first()

                    else -> {
                        chapters.indexOfLast { it.pageNum <= currentPage }.coerceAtLeast(0)
                    }
                }

                Log.d(
                    "DBOOK_TOC_DEBUG",
                    "Page: $currentPage | Chapter: '$currentChapter' | TargetSub: '$targetSubLabel' | ChosenIdx: $chosenIndex | ChosenLabel: '${chapters.getOrNull(chosenIndex)?.label}' (Page ${chapters.getOrNull(chosenIndex)?.pageNum})"
                )

                chosenIndex
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(chapters) { index, chapter ->
                    val isCurrent = index == activeChapterIndex

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(chapter.href) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = chapter.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Page ${chapter.pageNum}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}