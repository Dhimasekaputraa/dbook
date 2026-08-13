package com.dhimsea.dbook.ui.reader

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ReaderSettings(
    val fontFamily: String = "serif",
    val fontSize: Int = 18,
    val lineHeight: Float = 1.7f,
    val textAlign: String = "justify",
    val isDarkMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val isCustomNightLightEnabled: Boolean = false,
    val nightLightIntensity: Float = 0.15f
) {
    companion object {
        private const val PREF_NAME = "reader_preferences"

        fun loadFromPrefs(context: Context, isSystemDark: Boolean): ReaderSettings {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return ReaderSettings(
                fontFamily = prefs.getString("fontFamily", "serif") ?: "serif",
                fontSize = prefs.getInt("fontSize", 18),
                lineHeight = prefs.getFloat("lineHeight", 1.7f),
                textAlign = prefs.getString("textAlign", "justify") ?: "justify",
                isDarkMode = prefs.getBoolean("isDarkMode", isSystemDark),
                keepScreenOn = prefs.getBoolean("keepScreenOn", false),
                isCustomNightLightEnabled = prefs.getBoolean("isCustomNightLightEnabled", false),
                nightLightIntensity = prefs.getFloat("nightLightIntensity", 0.3f)
            )
        }

        fun saveToPrefs(context: Context, settings: ReaderSettings) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("fontFamily", settings.fontFamily)
                putInt("fontSize", settings.fontSize)
                putFloat("lineHeight", settings.lineHeight)
                putString("textAlign", settings.textAlign)
                putBoolean("isDarkMode", settings.isDarkMode)
                putBoolean("keepScreenOn", settings.keepScreenOn)
                putBoolean("isCustomNightLightEnabled", settings.isCustomNightLightEnabled)
                putFloat("nightLightIntensity", settings.nightLightIntensity)
                apply()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppearanceBottomSheet(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = settings.fontFamily == "serif",
                        onClick = { onSettingsChanged(settings.copy(fontFamily = "serif")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Serif")
                    }
                    SegmentedButton(
                        selected = settings.fontFamily == "sans",
                        onClick = { onSettingsChanged(settings.copy(fontFamily = "sans")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Sans")
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = {
                            if (settings.fontSize > 12) {
                                onSettingsChanged(settings.copy(fontSize = settings.fontSize - 1))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size")
                    }

                    Text(
                        text = "${settings.fontSize} px",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FilledTonalIconButton(
                        onClick = {
                            if (settings.fontSize < 32) {
                                onSettingsChanged(settings.copy(fontSize = settings.fontSize + 1))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Font Size")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Line Spacing", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = {
                            if (settings.lineHeight > 1.2f) {
                                val newHeight = (settings.lineHeight - 0.1f).coerceAtLeast(1.2f)
                                onSettingsChanged(settings.copy(lineHeight = (Math.round(newHeight * 10) / 10f)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Line Spacing")
                    }

                    Text(
                        text = String.format("%.1f", settings.lineHeight),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FilledTonalIconButton(
                        onClick = {
                            if (settings.lineHeight < 2.5f) {
                                val newHeight = (settings.lineHeight + 0.1f).coerceAtMost(2.5f)
                                onSettingsChanged(settings.copy(lineHeight = (Math.round(newHeight * 10) / 10f)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Line Spacing")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alignment", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = settings.textAlign == "left",
                        onClick = { onSettingsChanged(settings.copy(textAlign = "left")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Left Align")
                    }
                    SegmentedButton(
                        selected = settings.textAlign == "justify",
                        onClick = { onSettingsChanged(settings.copy(textAlign = "justify")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify Align")
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !settings.isDarkMode,
                        onClick = { onSettingsChanged(settings.copy(isDarkMode = false)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Light")
                    }
                    SegmentedButton(
                        selected = settings.isDarkMode,
                        onClick = { onSettingsChanged(settings.copy(isDarkMode = true)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Dark")
                    }
                }
            }

            HorizontalDivider()

            ReaderAppearanceSettingsContent(
                settings = settings,
                onSettingsChanged = onSettingsChanged
            )
        }
    }
}

@Composable
fun ReaderAppearanceSettingsContent(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Keep Screen On",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = settings.keepScreenOn,
                onCheckedChange = { isChecked ->
                    onSettingsChanged(settings.copy(keepScreenOn = isChecked))
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Night Light Filter",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = settings.isCustomNightLightEnabled,
                onCheckedChange = { isChecked ->
                    onSettingsChanged(settings.copy(isCustomNightLightEnabled = isChecked))
                }
            )
        }

        if (settings.isCustomNightLightEnabled) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Warmth Intensity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(settings.nightLightIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = settings.nightLightIntensity,
                    onValueChange = { newIntensity ->
                        onSettingsChanged(settings.copy(nightLightIntensity = newIntensity))
                    },
                    valueRange = 0.00f..1.00f
                )
            }
        }
    }
}