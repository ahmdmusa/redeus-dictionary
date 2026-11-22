package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WordEntity
import com.example.domain.PracticeWordScope
import com.example.ui.DictionaryViewModel
import com.example.ui.theme.LocalReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiParticle(
    val angleRad: Float,
    val speed: Float,
    val radiusDp: Float,
    val colorIndex: Int
)

/**
 * Ultra-lightweight single-shot confetti burst for milestone celebrations (Daily/Weekly Target,
 * Streak milestones, Session Complete). Runs once for ~1.3s and uses 0 CPU afterwards.
 */
@Composable
fun MilestoneConfettiBurst(
    triggerKey: Any,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotionEnabled.current
    if (reducedMotion) return

    val progress = remember(triggerKey) { Animatable(0f) }
    val particles = remember {
        List(24) { i ->
            val angle = ((i.toFloat() / 24f) * 2f * PI.toFloat()) + ((i % 3) * 0.11f)
            val speed = 0.55f + ((i * 37) % 100) / 160f
            val radius = 3.2f + (i % 4) * 1.1f
            ConfettiParticle(angle, speed, radius, i % 4)
        }
    }

    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFFFB020),
        Color(0xFF30D158)
    )

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
        )
    }

    val t = progress.value
    if (t <= 0f || t >= 1f) return

    Canvas(modifier = modifier) {
        val originX = size.width / 2f
        val originY = size.height * 0.32f
        val maxDist = size.minDimension * 0.52f
        val alpha = (1f - t).coerceIn(0f, 1f)
        val gravity = t * t * size.height * 0.30f

        particles.forEach { p ->
            val dist = maxDist * p.speed * t
            val x = originX + cos(p.angleRad) * dist
            val y = originY + sin(p.angleRad) * dist + gravity
            drawCircle(
                color = palette[p.colorIndex].copy(alpha = alpha),
                radius = p.radiusDp.dp.toPx() * (1f - t * 0.35f),
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Interactive IELTS Speaking Practice Prompt Card.
 * Suggests a ready-to-paste prompt for ChatGPT Voice / Gemini Live / Claude so the learner
 * practices today's or this week's studied vocabulary in real 3-part IELTS Speaking format.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IeltsSpeakingPromptCard(
    viewModel: DictionaryViewModel,
    todayWords: List<WordEntity>,
    weekWords: List<WordEntity>,
    defaultScope: PracticeWordScope = PracticeWordScope.TODAY,
    headline: String = "Practice Speaking with AI Agent (IELTS)",
    subtitle: String = "Copy a tailored 3-part IELTS Speaking prompt with your words and practice live with ChatGPT Voice, Gemini, or Claude.",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var selectedScope by remember(defaultScope) { mutableStateOf(defaultScope) }
    var showPreview by remember { mutableStateOf(false) }
    var copiedRecently by remember { mutableStateOf(false) }

    val activeWords = remember(selectedScope, todayWords, weekWords) {
        when (selectedScope) {
            PracticeWordScope.TODAY -> todayWords.ifEmpty { weekWords }
            PracticeWordScope.THIS_WEEK -> weekWords.ifEmpty { todayWords }
            PracticeWordScope.WEAK_WORDS -> weekWords.sortedByDescending { it.lapses }.ifEmpty { todayWords }
        }
    }

    val promptText = remember(selectedScope, activeWords) {
        viewModel.buildIeltsSpeakingPrompt(
            scope = selectedScope,
            customWords = activeWords.ifEmpty { null }
        )
    }

    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(2400L)
            copiedRecently = false
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.32f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ielts_speaking_prompt_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppleIconBadge(
                    icon = Icons.Default.RecordVoiceOver,
                    tintColor = MaterialTheme.colorScheme.tertiary,
                    size = 40.dp,
                    iconSize = 21.dp
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scope chips (Today vs This Week)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    PracticeWordScope.TODAY to "Today (${todayWords.size})",
                    PracticeWordScope.THIS_WEEK to "This Week (${weekWords.size})"
                ).forEach { (scope, label) ->
                    val isSelected = selectedScope == scope
                    Surface(
                        onClick = { selectedScope = scope },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .testTag("speaking_scope_${scope.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Target word chips included in this prompt
            if (activeWords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeWords.take(8).forEach { w ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                        ) {
                            Text(
                                text = w.word.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (activeWords.size > 8) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "+${activeWords.size - 8} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons: 1-Tap Copy + Share to AI App
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val copyInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Sada IELTS Speaking Prompt", promptText))
                        copiedRecently = true
                        viewModel.showSnackbar("Copied IELTS Speaking prompt! Paste into ChatGPT Voice or Gemini.")
                    },
                    interactionSource = copyInteraction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (copiedRecently) Color(0xFF1B8A3A) else MaterialTheme.colorScheme.tertiary,
                        contentColor = if (copiedRecently) Color.White else MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .applePressScale(copyInteraction, 0.97f)
                        .testTag("copy_ielts_speaking_prompt_button")
                ) {
                    Icon(
                        imageVector = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copiedRecently) "Prompt Copied ✓" else "Copy IELTS Prompt",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "IELTS Speaking Practice — Sada")
                                putExtra(Intent.EXTRA_TEXT, promptText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send prompt to AI Agent"))
                        } catch (_: Exception) {
                            viewModel.showSnackbar("Copy the prompt and paste it into your AI chat app.")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("share_ielts_speaking_prompt_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share prompt", modifier = Modifier.size(18.dp))
                }
            }

            // Collapsible Prompt Preview
            TextButton(
                onClick = { showPreview = !showPreview },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (showPreview) "Hide prompt preview" else "Preview prompt",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (showPreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = showPreview,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
