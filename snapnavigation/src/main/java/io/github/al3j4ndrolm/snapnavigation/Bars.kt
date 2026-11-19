package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max

// The drawing of each TabBar style. The public, customizable side is in TabBar.kt.

/**
 * A rounded bar with the selected item in a floating circle. The bar's top edge bends down
 * around the circle, and the bend slides along when the selection changes.
 */
@Composable
internal fun NotchedBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    space: Dp = 10.dp,                   // the gap between the circle and the bent edge
    barColor: Color = Color(0xFF534AB7),
    circleColor: Color = Color(0xFFEF9F27),
    iconColor: Color = Color.White,
) {
    val density = LocalDensity.current
    val circle = 52.dp
    val barHeight = 64.dp
    val inset = 24.dp  // keeps the bend away from the rounded corners

    BoxWithConstraints(modifier.fillMaxWidth().height(barHeight + circle / 2 + 6.dp)) {
        val slot = (maxWidth - inset * 2) / items.size
        val center by animateDpAsState(inset + slot * selected + slot / 2, label = "notch")
        val shape = with(density) {
            NotchedBarShape(
                notchX = center.toPx(),
                notchRadius = (circle / 2 + space).toPx(),
                corner = 22.dp.toPx(),
                smooth = 14.dp.toPx(),
            )
        }

        // The bar, bent around the circle
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(barHeight)
                .clip(shape)
                .background(barColor)
                .padding(horizontal = inset),
        ) {
            items.forEachIndexed { index, item ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab, onClickLabel = item.label) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (index != selected) Icon(item.icon, contentDescription = item.label, tint = iconColor)
                }
            }
        }

        // The circle, centered on the bar's top edge
        Box(
            Modifier
                .offset(x = center - circle / 2, y = maxHeight - barHeight - circle / 2)
                .size(circle)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(items[selected].icon, contentDescription = items[selected].label, tint = iconColor)
        }
    }
}

/**
 * A rounded bar with a big action button in the middle. The bar bends around the button,
 * and the items sit on both sides of it: half on the left, half on the right.
 */
@Composable
internal fun ActionBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    space: Dp = 8.dp,
    barColor: Color = Color(0xFF534AB7),
    actionColor: Color = Color(0xFFEF9F27),
    iconColor: Color = Color.White,
) {
    val density = LocalDensity.current
    val button = 68.dp
    val barHeight = 64.dp
    val left = items.take((items.size + 1) / 2)
    val right = items.drop(left.size)

    BoxWithConstraints(modifier.fillMaxWidth().height(barHeight + button / 2 + 6.dp)) {
        val shape = with(density) {
            NotchedBarShape(
                notchX = (maxWidth / 2).toPx(),
                notchRadius = (button / 2 + space).toPx(),
                corner = 22.dp.toPx(),
                smooth = 14.dp.toPx(),
            )
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(barHeight)
                .clip(shape)
                .background(barColor)
                .padding(horizontal = 8.dp),
        ) {
            @Composable
            fun Item(index: Int, item: Tab) {
                val isSelected = index == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab, onClickLabel = item.label) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) iconColor else iconColor.copy(alpha = 0.55f),
                    )
                    // A small dot under the selected item
                    if (isSelected) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .size(5.dp)
                                .background(actionColor, CircleShape),
                        )
                    }
                }
            }
            left.forEachIndexed { i, item -> Item(i, item) }
            Spacer(Modifier.weight(1.3f))  // room for the button
            right.forEachIndexed { i, item -> Item(left.size + i, item) }
        }

        // The big button, centered on the bar's top edge
        val press = remember { MutableInteractionSource() }
        val pressed by press.collectIsPressedAsState()
        val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "press")
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight - barHeight - button / 2)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }  // a little squeeze when pressed
                .size(button)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(actionColor)
                .clickable(interactionSource = press, indication = null, role = Role.Button, onClickLabel = actionLabel, onClick = onAction),
            contentAlignment = Alignment.Center,
        ) {
            Icon(actionIcon, contentDescription = actionLabel, tint = iconColor, modifier = Modifier.size(32.dp))
        }
    }
}

/** A rounded rectangle whose top edge dips into a U around a circle centered at [notchX]. */
internal class NotchedBarShape(
    private val notchX: Float,
    private val notchRadius: Float,
    private val corner: Float,
    private val smooth: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val cx = notchX
        val r = notchRadius
        val s = smooth
        val path = Path().apply {
            moveTo(corner, 0f)
            lineTo((cx - r - s).coerceAtLeast(corner), 0f)
            // The bend: down into a U under the circle, then back up.
            cubicTo(cx - r + s * 0.3f, 0f, cx - r, r, cx, r)
            cubicTo(cx + r, r, cx + r - s * 0.3f, 0f, (cx + r + s).coerceAtMost(w - corner), 0f)
            // The rest is a plain rounded rectangle.
            lineTo(w - corner, 0f)
            quadraticTo(w, 0f, w, corner)
            lineTo(w, h - corner)
            quadraticTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticTo(0f, h, 0f, h - corner)
            lineTo(0f, corner)
            quadraticTo(0f, 0f, corner, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A dark pill with a light blob behind the selected icon. The blob's front edge moves fast and
 * its back edge slow, so it stretches like a drop of liquid on its way to the next item.
 */
@Composable
internal fun LiquidBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF26215C),
    blobColor: Color = Color(0xFFAFA9EC),
    iconColor: Color = Color.White,
    selectedIconColor: Color = Color(0xFF26215C),
) {
    // Which way the blob is going decides which edge leads. A plain array: no recomposition needed.
    val motion = remember { intArrayOf(selected, 1) }
    if (selected != motion[0]) {
        motion[1] = if (selected > motion[0]) 1 else -1
        motion[0] = selected
    }
    val goingRight = motion[1] > 0

    BoxWithConstraints(
        modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(barColor),
    ) {
        val pad = 8.dp
        val slot = (maxWidth - pad * 2) / items.size
        val start = pad + slot * selected
        val fast = spring<Dp>(dampingRatio = 0.75f, stiffness = 900f)
        val slow = spring<Dp>(dampingRatio = 0.75f, stiffness = 160f)
        val left by animateDpAsState(start + 4.dp, if (goingRight) slow else fast, label = "blob left")
        val right by animateDpAsState(start + slot - 4.dp, if (goingRight) fast else slow, label = "blob right")

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = left)
                .width((right - left).coerceAtLeast(0.dp))
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(blobColor),
        )
        Row(Modifier.fillMaxSize().padding(horizontal = pad)) {
            items.forEachIndexed { index, item ->
                val tint by animateColorAsState(
                    if (index == selected) selectedIconColor else iconColor.copy(alpha = 0.7f),
                    label = "tint",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab, onClickLabel = item.label) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint)
                }
            }
        }
    }
}

/**
 * The opposite of the notch: the bar's top edge bulges up over the selected item, which rises
 * into the bump and shows its label. The bump springs from item to item.
 */
@Composable
internal fun BumpBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF0F6E56),
    accent: Color = Color(0xFF9FE1CB),
    iconColor: Color = Color.White,
) {
    val density = LocalDensity.current
    val bump = 18.dp
    val barHeight = 64.dp
    val inset = 20.dp

    BoxWithConstraints(modifier.fillMaxWidth().height(barHeight + bump)) {
        val slot = (maxWidth - inset * 2) / items.size
        val center by animateDpAsState(
            inset + slot * selected + slot / 2,
            spring(dampingRatio = 0.65f, stiffness = 300f),
            label = "bump",
        )
        val shape = with(density) {
            BumpBarShape(
                bumpX = center.toPx(),
                bumpHeight = bump.toPx(),
                bumpRadius = 30.dp.toPx(),
                corner = 22.dp.toPx(),
                smooth = 16.dp.toPx(),
            )
        }
        Row(Modifier.fillMaxSize().clip(shape).background(barColor).padding(horizontal = inset)) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selected
                val lift by animateDpAsState(
                    if (isSelected) (-8).dp else 0.dp,
                    spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "lift",
                )
                val size by animateDpAsState(if (isSelected) 28.dp else 24.dp, label = "size")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab, onClickLabel = item.label) { onSelect(index) }
                        .padding(top = bump),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) accent else iconColor.copy(alpha = 0.7f),
                        modifier = Modifier.offset(y = lift).size(size),
                    )
                    AnimatedVisibility(isSelected, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Text(
                            item.label,
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.offset(y = lift),
                        )
                    }
                }
            }
        }
    }
}

/** A rounded rectangle whose top edge bulges up into a hill centered at [bumpX]. */
internal class BumpBarShape(
    private val bumpX: Float,
    private val bumpHeight: Float,
    private val bumpRadius: Float,
    private val corner: Float,
    private val smooth: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val top = bumpHeight  // the flat part of the top edge; the hill rises above it to 0
        val cx = bumpX
        val r = bumpRadius
        val s = smooth
        val path = Path().apply {
            moveTo(corner, top)
            lineTo((cx - r - s).coerceAtLeast(corner), top)
            // The hill: up to the peak, then back down.
            cubicTo(cx - r + s * 0.5f, top, cx - r * 0.6f, 0f, cx, 0f)
            cubicTo(cx + r * 0.6f, 0f, cx + r - s * 0.5f, top, (cx + r + s).coerceAtMost(w - corner), top)
            lineTo(w - corner, top)
            quadraticTo(w, top, w, top + corner)
            lineTo(w, h - corner)
            quadraticTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticTo(0f, h, 0f, h - corner)
            lineTo(0f, top + corner)
            quadraticTo(0f, top, corner, top)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Icons only, except the selected item, which grows into a colored pill with its label. */
@Composable
internal fun ExpandingBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    pillColor: Color = Color(0xFFF5C4B3),
    pillContentColor: Color = Color(0xFF712B13),
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(barColor)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selected
            val background by animateColorAsState(if (isSelected) pillColor else Color.Transparent, label = "pill")
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(background)
                    .clickable(interactionSource = null, indication = null, role = Role.Tab, onClickLabel = item.label) { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, contentDescription = item.label, tint = if (isSelected) pillContentColor else iconColor)
                AnimatedVisibility(
                    isSelected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Text(
                        item.label,
                        color = pillContentColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * A dark bar with a lamp on its top edge. The lamp slides to the selected item and shines a
 * cone of light down onto it; the other icons stay in the dark.
 */
@Composable
internal fun SpotlightBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF1E1E1E),
    light: Color = Color(0xFFFAC775),
    iconColor: Color = Color.White,
) {
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(barColor)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
    ) {
        val pad = 12.dp
        val slot = (maxWidth - pad * 2) / items.size
        val center by animateDpAsState(
            pad + slot * selected + slot / 2,
            spring(dampingRatio = 0.75f, stiffness = 350f),
            label = "lamp",
        )

        Canvas(Modifier.fillMaxSize()) {
            val cx = center.toPx()
            val lamp = 28.dp.toPx()
            val floor = 72.dp.toPx()
            val beam = Path().apply {
                moveTo(cx - lamp / 2, 0f)
                lineTo(cx + lamp / 2, 0f)
                lineTo(cx + floor / 2, size.height)
                lineTo(cx - floor / 2, size.height)
                close()
            }
            drawPath(beam, Brush.verticalGradient(listOf(light.copy(alpha = 0.45f), light.copy(alpha = 0f))))
            drawLine(
                light,
                start = Offset(cx - lamp / 2, 1.5.dp.toPx()),
                end = Offset(cx + lamp / 2, 1.5.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Row(Modifier.fillMaxSize().padding(horizontal = pad)) {
            items.forEachIndexed { index, item ->
                val tint by animateColorAsState(
                    if (index == selected) light else iconColor.copy(alpha = 0.4f),
                    label = "tint",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab, onClickLabel = item.label) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint)
                }
            }
        }
    }
}

/** Evenly spaced item slots inside a bar: where each item's center is. */
private fun slotCenter(width: Dp, pad: Dp, count: Int, index: Int): Dp {
    val slot = (width - pad * 2) / count
    return pad + slot * index + slot / 2
}

/** One row of tappable item areas, laid over whatever the bar draws. */
@Composable
private fun ItemRow(
    items: List<Tab>,
    onSelect: (Int) -> Unit,
    pad: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int, item: Tab) -> Unit,
) {
    Row(modifier.fillMaxSize().padding(horizontal = pad)) {
        items.forEachIndexed { index, item ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = null,
                        indication = null,  // no gray tap square: the bar's own animation is the feedback
                        role = Role.Tab,
                        onClickLabel = item.label,
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) { content(index, item) }
        }
    }
}

/**
 * Every item has its own color. Picking one floods the bar with that color in a circle that
 * grows out from the icon you tapped.
 */
@Composable
internal fun RevealBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF534AB7), Color(0xFF0F6E56), Color(0xFF993C1D), Color(0xFF993556)),
    iconColor: Color = Color.White,
) {
    var shown by remember { mutableIntStateOf(selected) }
    var under by remember { mutableIntStateOf(selected) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected != shown) {
            under = shown
            shown = selected
            progress.snapTo(0f)
            progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }
    val pad = 8.dp
    BoxWithConstraints(modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp))) {
        val center = slotCenter(maxWidth, pad, items.size, shown)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors[under % colors.size])
            val cx = center.toPx()
            val farthest = max(hypot(cx, size.height), hypot(size.width - cx, size.height))
            drawCircle(colors[shown % colors.size], radius = farthest * progress.value, center = Offset(cx, size.height / 2))
        }
        ItemRow(items, onSelect, pad) { index, item ->
            val scale by animateFloatAsState(if (index == selected) 1.2f else 1f, spring(0.5f, 400f), label = "scale")
            Icon(
                item.icon,
                contentDescription = item.label,
                tint = if (index == selected) iconColor else iconColor.copy(alpha = 0.6f),
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

/**
 * A thin line runs under the icons. Under the selected one it rises into a smooth crest with a
 * dot on top, and the crest glides along the line when the selection changes.
 */
@Composable
internal fun WaveBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    lineColor: Color = Color(0xFF5DCAA5),
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val pad = 12.dp
    BoxWithConstraints(modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(24.dp)).background(barColor)) {
        val slot = (maxWidth - pad * 2) / items.size
        val crest by animateDpAsState(
            slotCenter(maxWidth, pad, items.size, selected),
            spring(dampingRatio = 0.7f, stiffness = 220f),
            label = "crest",
        )
        Canvas(Modifier.fillMaxSize()) {
            val base = size.height - 14.dp.toPx()
            val height = 12.dp.toPx()
            val cx = crest.toPx()
            val spread = slot.toPx() * 0.38f
            val line = Path()
            var x = 16.dp.toPx()
            line.moveTo(x, base)
            while (x <= size.width - 16.dp.toPx()) {
                val d = (x - cx) / spread
                line.lineTo(x, base - height * exp(-d * d))
                x += 3f
            }
            drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(cx, base - height))
        }
        ItemRow(items, onSelect, pad) { index, item ->
            val isSelected = index == selected
            val lift by animateDpAsState(if (isSelected) (-10).dp else (-4).dp, spring(0.55f, 380f), label = "lift")
            val tint by animateColorAsState(if (isSelected) lineColor else iconColor, label = "tint")
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.offset(y = lift))
        }
    }
}

/**
 * Like a desktop dock: colorful app-style tiles on a glassy shelf. The selected tile swells
 * and its neighbors swell a little, so a wave rolls across the dock as the selection moves.
 */
@Composable
internal fun DockBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tileColors: List<Color> = listOf(Color(0xFF7F77DD), Color(0xFF1D9E75), Color(0xFFD85A30), Color(0xFFD4537E)),
    shelfColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
    iconColor: Color = Color.White,
) {
    val pad = 16.dp
    val tile = 44.dp
    BoxWithConstraints(modifier.fillMaxWidth().height(96.dp)) {
        val width = maxWidth
        val slot = (width - pad * 2) / items.size
        val focus by animateDpAsState(
            slotCenter(maxWidth, pad, items.size, selected),
            spring(dampingRatio = 0.8f, stiffness = 180f),
            label = "focus",
        )
        val shelfShape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .clip(shelfShape)
                .background(shelfColor)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), shelfShape),
        )
        Row(Modifier.fillMaxSize().padding(horizontal = pad)) {
            items.forEachIndexed { index, item ->
                val center = slotCenter(width, pad, items.size, index)
                val d = (center - focus) / slot
                val scale = 1f + 0.45f * exp(-d * d * 1.6f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                        interactionSource = null,
                        indication = null,  // no gray tap square: the bar's own animation is the feedback
                        role = Role.Tab,
                        onClickLabel = item.label,
                    ) { onSelect(index) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 12.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0.5f, 1f)  // grow upward, off the shelf
                            }
                            .size(tile)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tileColors[index % tileColors.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = iconColor)
                    }
                    if (index == selected) {
                        Box(
                            Modifier
                                .padding(bottom = 4.dp)
                                .size(4.dp)
                                .background(MaterialTheme.colorScheme.onSurface, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

/** Each item is a little card. The selected one flips over in 3D to show its name on the back. */
@Composable
internal fun FlipBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF26215C),
    backColor: Color = Color(0xFFEF9F27),
    backTextColor: Color = Color(0xFF412402),
    iconColor: Color = Color.White,
) {
    Box(modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).background(barColor)) {
        ItemRow(items, onSelect, pad = 8.dp) { index, item ->
            val rotation by animateFloatAsState(
                if (index == selected) 180f else 0f,
                spring(dampingRatio = 0.6f, stiffness = 180f),
                label = "flip",
            )
            Box(
                Modifier.graphicsLayer {
                    rotationX = rotation
                    cameraDistance = 14f * density
                },
                contentAlignment = Alignment.Center,
            ) {
                if (rotation <= 90f) {
                    // Front: the icon
                    Icon(item.icon, contentDescription = item.label, tint = iconColor.copy(alpha = 0.75f))
                } else {
                    // Back: the label, turned so it reads the right way up
                    Text(
                        item.label,
                        color = backTextColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier
                            .graphicsLayer { rotationX = 180f }
                            .clip(RoundedCornerShape(10.dp))
                            .background(backColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * A dark pill with a slowly turning rainbow border, like the northern lights, and a soft glow
 * that floats behind the selected icon.
 */
@Composable
internal fun AuroraBar(
    items: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF15131C),
    auroraColors: List<Color> = listOf(
        Color(0xFF5DCAA5), Color(0xFF7F77DD), Color(0xFFED93B1), Color(0xFFEF9F27), Color(0xFF5DCAA5),
    ),
    iconColor: Color = Color.White,
) {
    val turning = rememberInfiniteTransition(label = "aurora")
    val angle by turning.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val pill = RoundedCornerShape(32.dp)
    val pad = 10.dp
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(pill)
            .drawBehind {
                // A big turning rainbow; the dark pill on top leaves only a thin rim of it showing.
                rotate(angle) { drawCircle(Brush.sweepGradient(auroraColors), radius = size.maxDimension) }
            }
            .padding(1.5.dp)
            .clip(pill)
            .background(barColor),
    ) {
        val glowX by animateDpAsState(
            slotCenter(maxWidth, pad, items.size, selected),
            spring(dampingRatio = 0.75f, stiffness = 260f),
            label = "glow",
        )
        val glowColor = auroraColors[(selected + 1) % (auroraColors.size - 1)]
        val glow by animateColorAsState(glowColor, tween(400), label = "glow color")
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(glowX.toPx(), size.height / 2)
            drawCircle(
                Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0f)), center = c, radius = 34.dp.toPx()),
                radius = 34.dp.toPx(),
                center = c,
            )
        }
        ItemRow(items, onSelect, pad) { index, item ->
            val tint by animateColorAsState(
                if (index == selected) iconColor else iconColor.copy(alpha = 0.45f),
                label = "tint",
            )
            Icon(item.icon, contentDescription = item.label, tint = tint)
        }
    }
}
