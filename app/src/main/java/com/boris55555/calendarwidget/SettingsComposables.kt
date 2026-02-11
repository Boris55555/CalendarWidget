package com.boris55555.calendarwidget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class AppInfo(val name: String, val packageName: String)

@Composable
fun FontSizeSlider(label: String, value: Float, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit = {}) {
    Column {
        Text(text = "$label: ${value.roundToInt()} sp")
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 6f..30f,
            steps = 23
        )
    }
}

@Composable
fun PermissionStatusIndicator(isGranted: Boolean, onRequest: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isGranted) ComposeColor(0xFFE8F5E9) else ComposeColor(0xFFFFF3E0),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) ComposeColor(0xFF2E7D32) else ComposeColor(0xFFEF6C00)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isGranted) "Calendar permission granted" else "Calendar permission missing",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isGranted) ComposeColor(0xFF2E7D32) else ComposeColor(0xFFEF6C00)
                )
                if (!isGranted) {
                    TextButton(onClick = onRequest, contentPadding = PaddingValues(0.dp)) {
                        Text("Grant permission here")
                    }
                }
            }
        }
    }
}

@Composable
fun ColorInput(label: String, value: String, onValueChange: (String) -> Unit) {
    val commonColors = listOf(
        "#000000", "#FFFFFF", "#808080", "#696969", 
        "#FF0000", "#00FF00", "#0000FF", "#FFFF00", 
        "#00FFFF", "#FF00FF", "#FFA500", "#A52A2A"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = !isValidHex(value),
            placeholder = { Text("#RRGGBB") },
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isValidHex(value)) ComposeColor(android.graphics.Color.parseColor(value)) else ComposeColor.Transparent)
                        .border(1.dp, ComposeColor.Gray, CircleShape)
                )
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Väripaletti
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonColors.take(6).forEach { colorHex ->
                ColorOption(colorHex) { onValueChange(colorHex) }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonColors.takeLast(6).forEach { colorHex ->
                ColorOption(colorHex) { onValueChange(colorHex) }
            }
        }
    }
}

@Composable
fun ColorOption(hex: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(ComposeColor(android.graphics.Color.parseColor(hex)))
            .border(1.dp, ComposeColor.LightGray, CircleShape)
            .clickable { onClick() }
    )
}

@Composable
fun UpdateFrequencySelector(currentInterval: Long, onIntervalSelected: (Long) -> Unit) {
    val options = listOf(
        3600000L to "1 hour",
        7200000L to "2 hours",
        10800000L to "3 hours",
        18000000L to "5 hours",
        86400000L to "At daily reset"
    )

    Column {
        options.forEach { (interval, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (interval == currentInterval),
                        onClick = { onIntervalSelected(interval) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (interval == currentInterval),
                    onClick = null 
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun EventCountSlider(value: Int, onValueChange: (Int) -> Unit, onValueChangeFinished: () -> Unit = {}) {
    Column {
        Text(text = "Number of events: $value")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 1f..10f,
            steps = 8 
        )
    }
}

@Composable
fun CalendarAppSelector(apps: List<AppInfo>, selectedPackage: String, onAppSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedApp = apps.find { it.packageName == selectedPackage } ?: AppInfo("None selected / No action", "")

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedApp.name,
            onValueChange = { },
            readOnly = true,
            label = { Text("Select Calendar App") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = true }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DropdownMenuItem(
                text = { Text("None selected / No action") },
                onClick = {
                    onAppSelected("")
                    expanded = false
                }
            )
            apps.forEach { app ->
                DropdownMenuItem(
                    text = { Text(app.name) },
                    onClick = {
                        onAppSelected(app.packageName)
                        expanded = false
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
    }
}