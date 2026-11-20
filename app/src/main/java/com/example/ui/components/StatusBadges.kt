package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.WordEntity

@Composable
fun CardStatusBadge(
    wordEntity: WordEntity,
    modifier: Modifier = Modifier
) {
    CardStatusBadge(
        repetitions = wordEntity.repetitions,
        interval = wordEntity.interval,
        distinctReviewDays = wordEntity.distinctReviewDays,
        modifier = modifier
    )
}

@Composable
fun CardStatusBadge(
    repetitions: Int,
    interval: Int,
    distinctReviewDays: Int = 0,
    modifier: Modifier = Modifier
) {
    val isNew = repetitions == 0 && distinctReviewDays == 0
    val isMastered = distinctReviewDays >= 3 && interval >= 6

    val (label, icon, container, content) = when {
        isNew -> Quadruple(
            "New",
            Icons.Default.FiberNew,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        isMastered -> Quadruple(
            "Mastered",
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> Quadruple(
            "Learning",
            Icons.Default.School,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun LeechBadge(
    lapses: Int = 0,
    showLapseCount: Boolean = true,
    modifier: Modifier = Modifier
) {
    val label = if (showLapseCount && lapses > 0) "Leech • $lapses" else "Leech"
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
