package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MaterialType
import com.example.ui.theme.PrimaryOrange

/**
 * Horizontal picker for [MaterialType], used to convert a measured volume
 * into estimated tons ("Reporte en toneladas"). Colors are passed explicitly
 * so the same row reads well both on a light AlertDialog and on the dark AR
 * camera overlay.
 */
@Composable
fun MaterialSelectorRow(
    selected: MaterialType,
    onSelect: (MaterialType) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = PrimaryOrange,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContentColor: Color = Color.White
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MaterialType.values().toList()) { material ->
            val isSelected = material == selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) selectedColor else unselectedContainerColor,
                modifier = Modifier.clickable { onSelect(material) }
            ) {
                Text(
                    text = material.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) selectedContentColor else unselectedContentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
