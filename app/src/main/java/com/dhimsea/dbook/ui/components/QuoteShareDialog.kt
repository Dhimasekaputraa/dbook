package com.dhimsea.dbook.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dhimsea.dbook.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import com.dhimsea.dbook.core.utils.ImageShareUtils
import kotlinx.coroutines.launch

enum class QuoteThemeOption {
    LIGHT, DARK, PRIMARY, MATERIAL_DYNAMIC
}

@Composable
fun QuoteShareDialog(
    quoteText: String,
    bookTitle: String,
    bookAuthor: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTheme by remember { mutableStateOf(QuoteThemeOption.PRIMARY) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val (cardBgColor, textColor, subTextColor) = when (selectedTheme) {
        QuoteThemeOption.LIGHT -> Triple(Color.White, Color(0xFF1A1A1A), Color(0xFF666666))
        QuoteThemeOption.DARK -> Triple(Color(0xFF121212), Color(0xFFE0E0E0), Color(0xFFA0A0A0))
        QuoteThemeOption.PRIMARY -> Triple(primaryColor, Color.White, Color.White.copy(alpha = 0.8f))
        QuoteThemeOption.MATERIAL_DYNAMIC -> Triple(primaryContainer, onPrimaryContainer, onPrimaryContainer.copy(alpha = 0.7f))
    }

    val graphicsLayer = rememberGraphicsLayer()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Image Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .drawWithContent {
                            
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawContent()
                        }
                ) {
                    QuoteCardContent(
                        quoteText = quoteText,
                        bookTitle = bookTitle,
                        bookAuthor = bookAuthor,
                        cardBgColor = cardBgColor,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Color Theme ---
                Text(
                    text = "Choose Color Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorPickerOption(
                        color = Color.White,
                        isSelected = selectedTheme == QuoteThemeOption.LIGHT,
                        onClick = { selectedTheme = QuoteThemeOption.LIGHT }
                    )
                    ColorPickerOption(
                        color = Color(0xFF121212),
                        isSelected = selectedTheme == QuoteThemeOption.DARK,
                        onClick = { selectedTheme = QuoteThemeOption.DARK }
                    )
                    ColorPickerOption(
                        color = primaryColor,
                        isSelected = selectedTheme == QuoteThemeOption.PRIMARY,
                        onClick = { selectedTheme = QuoteThemeOption.PRIMARY }
                    )
                    ColorPickerOption(
                        color = primaryContainer,
                        isSelected = selectedTheme == QuoteThemeOption.MATERIAL_DYNAMIC,
                        onClick = { selectedTheme = QuoteThemeOption.MATERIAL_DYNAMIC }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                   
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    val androidBitmap = imageBitmap.asAndroidBitmap()
                                    ImageShareUtils.shareBitmap(context, androidBitmap)
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteCardContent(
    quoteText: String,
    bookTitle: String,
    bookAuthor: String,
    cardBgColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val isDarkMode = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            
            Text(
                text = bookTitle.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = subTextColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = "by $bookAuthor",
                style = MaterialTheme.typography.labelSmall,
                color = subTextColor.copy(alpha = 0.8f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "“$quoteText”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

        }
    }
}

@Composable
fun ColorPickerOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}