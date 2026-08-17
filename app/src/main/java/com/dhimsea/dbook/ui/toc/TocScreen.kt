package com.dhimsea.dbook.ui.toc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val activeChapterHref = remember(chapters, currentChapter, currentPage) {
        fun flatten(list: List<ChapterMarker>): List<ChapterMarker> {
            val result = mutableListOf<ChapterMarker>()
            list.forEach {
                result.add(it)
                result.addAll(flatten(it.subitems))
            }
            return result
        }

        val flatChapters = flatten(chapters)
        if (flatChapters.isEmpty()) return@remember ""

        val currentCleanName = currentChapter.trim()
        val targetSubLabel = if (currentCleanName.contains(": ")) {
            currentCleanName.split(": ").lastOrNull()?.trim() ?: currentCleanName
        } else currentCleanName

        val matchingIndices = flatChapters.indices.filter { idx ->
            val label = flatChapters[idx].label.trim()
            label.equals(currentCleanName, ignoreCase = true) || label.equals(targetSubLabel, ignoreCase = true)
        }

        val chosenIndex = when {
            matchingIndices.size > 1 -> {
                val candidateBelow = matchingIndices
                    .filter { idx -> flatChapters[idx].pageNum <= currentPage }
                    .maxByOrNull { idx -> flatChapters[idx].pageNum }
                candidateBelow ?: matchingIndices.minByOrNull { idx -> 
                    kotlin.math.abs(flatChapters[idx].pageNum - currentPage) 
                } ?: matchingIndices.first()
            }
            matchingIndices.size == 1 -> matchingIndices.first()
            else -> flatChapters.indexOfLast { it.pageNum <= currentPage }.coerceAtLeast(0)
        }

        flatChapters.getOrNull(chosenIndex)?.href ?: ""
    }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chapters) { chapter ->
                    TocItemNode(
                        chapter = chapter,
                        activeChapterHref = activeChapterHref,
                        onChapterClick = onChapterClick
                    )
                }
            }
        }
    }
}

@Composable
fun TocItemNode(
    chapter: ChapterMarker,
    activeChapterHref: String,
    onChapterClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasChildren = chapter.subitems.isNotEmpty()

    val isCurrent = chapter.href.isNotEmpty() && chapter.href == activeChapterHref

    LaunchedEffect(activeChapterHref) {
        if (hasChildren && hasActiveChild(chapter.subitems, activeChapterHref)) {
            isExpanded = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hasChildren && chapter.href.isEmpty()) {
                        isExpanded = !isExpanded
                    } else if (chapter.href.isNotEmpty()) {
                        onChapterClick(chapter.href)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = (16 + (chapter.level * 16)).dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasChildren) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

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

        if (hasChildren) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chapter.subitems.forEach { child ->
                        TocItemNode(
                            chapter = child.copy(level = chapter.level + 1),
                            activeChapterHref = activeChapterHref,
                            onChapterClick = onChapterClick
                        )
                    }
                }
            }
        }
    }
}

private fun hasActiveChild(items: List<ChapterMarker>, targetHref: String): Boolean {
    if (targetHref.isEmpty()) return false
    return items.any { child ->
        child.href == targetHref || hasActiveChild(child.subitems, targetHref)
    }
}