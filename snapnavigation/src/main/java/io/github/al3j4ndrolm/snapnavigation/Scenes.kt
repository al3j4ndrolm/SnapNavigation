package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.animation.EnterTransition
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
    val transitions = when (screen.transition) {
        Transition.Default -> emptyMap()
        Transition.Fade ->
            NavDisplay.transitionSpec { fadeIn() togetherWith fadeOut() } +
                NavDisplay.popTransitionSpec { fadeIn() togetherWith fadeOut() } +
                NavDisplay.predictivePopTransitionSpec { fadeIn() togetherWith fadeOut() }
        Transition.SlideUp ->
            NavDisplay.transitionSpec { slideInVertically { it } togetherWith ExitTransition.KeepUntilTransitionsFinished } +
                NavDisplay.popTransitionSpec { EnterTransition.None togetherWith slideOutVertically { it } } +
                NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith slideOutVertically { it } }
        Transition.None ->
            NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
                NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
                NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
    }
    val kind = when (screen) {
        is DialogScreen -> DialogSceneStrategy.dialog()
        is SheetScreen -> metadata { put(SheetSceneStrategy.SheetKey, true) }
        else -> emptyMap()
    }
    return transitions + kind
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
