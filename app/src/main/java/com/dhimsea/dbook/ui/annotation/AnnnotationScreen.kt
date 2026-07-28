package com.dhimsea.dbook.ui.annotation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhimsea.dbook.domain.model.Annotation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationScreen(
    bookTitle: String,
    annotations: List<Annotation>,
    onAnnotationClick: (Annotation) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onBack: () -> Unit
) {
    var selectedAnnotationForDelete by remember { mutableStateOf<Annotation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Anotasi: $bookTitle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (annotations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada anotasi atau catatan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(annotations, key = { it.id }) { annotation ->
                    AnnotationItemCard(
                        annotation = annotation,
                        onClick = { onAnnotationClick(annotation) },
                        onMoreClick = { selectedAnnotationForDelete = annotation }
                    )
                }
            }
        }

        // Modal Dialog Hapus Anotasi
        selectedAnnotationForDelete?.let { annotation ->
            AlertDialog(
                onDismissRequest = { selectedAnnotationForDelete = null },
                title = { Text("Hapus Anotasi") },
                text = { Text("Apakah Anda yakin ingin menghapus anotasi ini?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteAnnotation(annotation)
                            selectedAnnotationForDelete = null
                        }
                    ) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedAnnotationForDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun AnnotationItemCard(
    annotation: Annotation,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    // Parse warna dari hex string
    val tagColor = remember(annotation.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(annotation.colorHex))
        } catch (e: Exception) {
            Color(0xFFFFEB3B)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Simbol Warna Anotasi (Circle Dot)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${annotation.chapterName} • Hal. ${annotation.pageNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Teks yang ditandai / di-highlight
            Text(
                text = "“${annotation.text}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            // Catatan / Komentar Tambahan pengguna (jika ada)
            if (annotation.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = annotation.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}