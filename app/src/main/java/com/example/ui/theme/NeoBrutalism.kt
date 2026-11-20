package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NeoCornerRadius = 8.dp
val NeoBorderWidth = 2.5.dp
val NeoShadowOffset = 4.dp
val NeoDefaultShape = RoundedCornerShape(NeoCornerRadius)

/**
 * Draws a crisp, hard non-blurred offset drop shadow behind the composable.
 */
fun Modifier.neoShadow(
    offsetX: Dp = NeoShadowOffset,
    offsetY: Dp = NeoShadowOffset,
    shadowColor: Color = NeoBlack,
    shape: Shape = NeoDefaultShape
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(offsetX.toPx(), offsetY.toPx())
        drawPath(path, color = shadowColor)
        canvas.restore()
    }
}

/**
 * Neo-Brutalist standard card block with solid background, thick black border, and hard offset drop shadow.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoWhite,
    borderColor: Color = NeoBlack,
    borderWidth: Dp = NeoBorderWidth,
    shadowColor: Color = NeoBlack,
    shadowOffset: Dp = NeoShadowOffset,
    shape: RoundedCornerShape = NeoDefaultShape,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .neoShadow(
                offsetX = shadowOffset,
                offsetY = shadowOffset,
                shadowColor = shadowColor,
                shape = shape
            )
            .background(backgroundColor, shape = shape)
            .border(borderWidth, borderColor, shape = shape)
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(content = content)
    }
}

/**
 * Neo-Brutalist Interactive Button with solid color fill, thick black border, bold uppercase text, and hard offset shadow.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoYellow,
    contentColor: Color = NeoBlack,
    borderColor: Color = NeoBlack,
    borderWidth: Dp = NeoBorderWidth,
    shadowOffset: Dp = NeoShadowOffset,
    shape: RoundedCornerShape = NeoDefaultShape,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val actualOffset = if (isPressed) 1.dp else shadowOffset
    val translationX = if (isPressed) (shadowOffset - 1.dp) else 0.dp
    val translationY = if (isPressed) (shadowOffset - 1.dp) else 0.dp

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .offset(x = translationX, y = translationY)
            .neoShadow(
                offsetX = actualOffset,
                offsetY = actualOffset,
                shadowColor = if (enabled) NeoBlack else Color.DarkGray,
                shape = shape
            )
            .background(
                if (enabled) containerColor else Color(0xFFD1D5DB),
                shape = shape
            )
            .border(borderWidth, borderColor, shape = shape)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/**
 * Neo-Brutalist Icon Button with hard drop shadow and thick border.
 */
@Composable
fun NeoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoWhite,
    contentColor: Color = NeoBlack,
    borderColor: Color = NeoBlack,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 3.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val actualOffset = if (isPressed) 1.dp else shadowOffset
    val translationX = if (isPressed) (shadowOffset - 1.dp) else 0.dp
    val translationY = if (isPressed) (shadowOffset - 1.dp) else 0.dp

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .offset(x = translationX, y = translationY)
            .neoShadow(
                offsetX = actualOffset,
                offsetY = actualOffset,
                shadowColor = NeoBlack,
                shape = shape
            )
            .background(containerColor, shape = shape)
            .border(borderWidth, borderColor, shape = shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Neo-Brutalist Tag / Filter Chip with solid black border and hard shadow.
 */
@Composable
fun NeoChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    selectedColor: Color = NeoYellow,
    unselectedColor: Color = NeoWhite,
    contentColor: Color = NeoBlack,
    borderColor: Color = NeoBlack,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    val bg = if (selected) selectedColor else unselectedColor
    val shadowOffset = if (selected) 2.5.dp else 1.5.dp

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset, shadowColor = NeoBlack, shape = shape)
            .background(bg, shape = shape)
            .border(2.dp, borderColor, shape = shape)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = contentColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Neo-Brutalist Section Header Badge
 */
@Composable
fun NeoSectionBadge(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoYellow,
    contentColor: Color = NeoBlack
) {
    Box(
        modifier = modifier
            .padding(end = 2.dp, bottom = 2.dp)
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(4.dp))
            .background(containerColor, shape = RoundedCornerShape(4.dp))
            .border(2.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = contentColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Neo-Brutalist Status Badge for cards ("NEW", "LEARNING", "MASTERED")
 */
@Composable
fun NeoCardStatusBadge(
    repetitions: Int,
    interval: Int,
    distinctReviewDays: Int = 0,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, textColor) = when {
        repetitions == 0 && distinctReviewDays == 0 -> Triple("NEW", NeoCyan, NeoBlack)
        distinctReviewDays >= 3 && interval >= 6 -> Triple("MASTERED", NeoGreen, NeoBlack)
        else -> Triple("LEARNING", NeoYellow, NeoBlack)
    }

    Box(
        modifier = modifier
            .padding(end = 2.dp, bottom = 2.dp)
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(4.dp))
            .background(containerColor, shape = RoundedCornerShape(4.dp))
            .border(2.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
            ),
            color = textColor
        )
    }
}

/**
 * Neo-Brutalist Leech Badge for words with high lapse count ("LEECH")
 */
@Composable
fun NeoLeechBadge(
    lapses: Int = 0,
    showLapseCount: Boolean = true,
    modifier: Modifier = Modifier
) {
    val text = if (showLapseCount && lapses > 0) "LEECH • $lapses" else "LEECH"
    Box(
        modifier = modifier
            .padding(end = 2.dp, bottom = 2.dp)
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(4.dp))
            .background(NeoRed, shape = RoundedCornerShape(4.dp))
            .border(2.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
            ),
            color = NeoWhite
        )
    }
}

/**
 * Neo-Brutalist Rating Button (Again, Hard, Good, Easy)
 */
@Composable
fun NeoRatingButton(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    NeoButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = NeoBlack,
        modifier = modifier.height(52.dp),
        shadowOffset = 3.dp,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Neo-Brutalist Destructive Button (Bold Red #D93B2B with solid black border & hard shadow)
 */
@Composable
fun NeoDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    NeoButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = NeoRed,
        contentColor = NeoWhite,
        enabled = enabled,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Neo-Brutalist Secondary / Neutral Button (White/Cream with solid black border & hard shadow)
 */
@Composable
fun NeoSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoWhite,
    contentColor: Color = NeoBlack,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    NeoButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Neo-Brutalist Linear Progress Indicator
 */
@Composable
fun NeoLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = NeoYellow,
    trackColor: Color = NeoWhite,
    borderColor: Color = NeoBlack
) {
    Box(
        modifier = modifier
            .height(14.dp)
            .padding(end = 2.dp, bottom = 2.dp)
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(4.dp))
            .background(trackColor, shape = RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, shape = RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .background(barColor)
        )
    }
}
