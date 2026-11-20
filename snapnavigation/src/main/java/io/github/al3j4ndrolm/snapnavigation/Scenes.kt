package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay

/** What Navigation 3 needs to know about [screen]: dialog, sheet and its animation. */
internal fun metadataFor(screen: Screen): Map<String, Any> {
    val kind = when (screen) {
        is DialogScreen -> DialogSceneStrategy.dialog()
        is SheetScreen -> metadata { put(SheetSceneStrategy.SheetKey, true) }
        else -> emptyMap()
    }
    return transitionsFor(screen.transition) + kind
}

private typealias Spec = AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform

/** [open] when the screen comes in, [close] when it leaves, also used while dragging the back gesture. */
private fun animations(open: Spec, close: Spec): Map<String, Any> =
    NavDisplay.transitionSpec(open) + NavDisplay.popTransitionSpec(close) +
        NavDisplay.predictivePopTransitionSpec { _ -> close() }

private fun transitionsFor(transition: Transition): Map<String, Any> = when (transition) {
    Transition.Default -> emptyMap()
    Transition.Fade -> animations(
        open = { fadeIn() togetherWith fadeOut() },
        close = { fadeIn() togetherWith fadeOut() },
    )
    Transition.SlideUp -> animations(
        open = { slideInVertically(tween(MEDIUM)) { it } togetherWith ExitTransition.KeepUntilTransitionsFinished },
        close = { EnterTransition.None togetherWith slideOutVertically(tween(MEDIUM)) { it } },
    )
    Transition.Slide -> animations(
        open = { slideIntoContainer(Start, tween(MEDIUM)) togetherWith slideOutOfContainer(Start, tween(MEDIUM)) },
        close = { slideIntoContainer(End, tween(MEDIUM)) togetherWith slideOutOfContainer(End, tween(MEDIUM)) },
    )
    Transition.Parallax -> animations(
        open = {
            slideIntoContainer(Start, tween(MEDIUM)) togetherWith
                slideOutOfContainer(Start, tween(MEDIUM)) { it / 4 }
        },
        close = {
            slideIntoContainer(End, tween(MEDIUM)) { it / 4 } togetherWith
                slideOutOfContainer(End, tween(MEDIUM))
        },
    )
    Transition.Zoom -> animations(
        open = {
            (fadeIn(tween(MEDIUM - SHORT, delayMillis = SHORT)) + scaleIn(tween(MEDIUM), initialScale = 0.85f)) togetherWith
                (fadeOut(tween(SHORT)) + scaleOut(tween(MEDIUM), targetScale = 1.1f))
        },
        close = {
            (fadeIn(tween(MEDIUM - SHORT, delayMillis = SHORT)) + scaleIn(tween(MEDIUM), initialScale = 1.1f)) togetherWith
                (fadeOut(tween(SHORT)) + scaleOut(tween(MEDIUM), targetScale = 0.85f))
        },
    )
    Transition.Rise -> animations(
        open = {
            (fadeIn(tween(MEDIUM - SHORT, delayMillis = SHORT)) + slideInVertically(tween(MEDIUM)) { it / 12 }) togetherWith
                (fadeOut(tween(SHORT)) + slideOutVertically(tween(MEDIUM)) { -it / 12 })
        },
        close = {
            (fadeIn(tween(MEDIUM - SHORT, delayMillis = SHORT)) + slideInVertically(tween(MEDIUM)) { -it / 12 }) togetherWith
                (fadeOut(tween(SHORT)) + slideOutVertically(tween(MEDIUM)) { it / 12 })
        },
    )
    Transition.Pop -> animations(
        open = {
            (scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) +
                fadeIn(tween(SHORT))) togetherWith ExitTransition.KeepUntilTransitionsFinished
        },
        close = { EnterTransition.None togetherWith (scaleOut(tween(SHORT * 2), targetScale = 0.6f) + fadeOut(tween(SHORT * 2))) },
    )
    Transition.None -> animations(
        open = { EnterTransition.None togetherWith ExitTransition.None },
        close = { EnterTransition.None togetherWith ExitTransition.None },
    )
}

private const val SHORT = 90
private const val MEDIUM = 350

/**
 * Paints the theme's background behind a screen while it animates. Without it, a screen with no
 * background of its own would show the previous screen through it as it slides in over it.
 */
@Composable
internal fun Backdrop(content: @Composable () -> Unit) {
    val color = MaterialTheme.colorScheme.background
    val transition = LocalNavAnimatedContentScope.current.transition
    Box(
        Modifier.fillMaxSize().drawBehind {
            if (transition.isRunning || transition.currentState != transition.targetState) drawRect(color)
        },
    ) { content() }
}

/** Shows [SheetScreen]s in a Material bottom sheet over the screens below. */
internal class SheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull() ?: return null
        if (last.metadata[SheetKey] != true) return null
        return SheetScene(last.contentKey, last, entries.dropLast(1), onBack)
    }

    object SheetKey : NavMetadataKey<Boolean>
}

@OptIn(ExperimentalMaterial3Api::class)
private class SheetScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<T>> = previousEntries
    private var state: SheetState? = null

    override val content: @Composable () -> Unit = {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true).also { state = it }
        ModalBottomSheet(onDismissRequest = onBack, sheetState = sheetState) {
            entry.Content()
        }
    }

    // Closed with nav.back(): slide down instead of vanishing.
    override suspend fun onRemove() {
        state?.hide()
    }

    override fun equals(other: Any?): Boolean =
        other is SheetScene<*> && key == other.key && entry == other.entry && previousEntries == other.previousEntries

    override fun hashCode(): Int = (key.hashCode() * 31 + entry.hashCode()) * 31 + previousEntries.hashCode()
}
