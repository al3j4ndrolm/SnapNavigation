package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.runtime.Composable

/**
 * A screen of your app. It draws itself, so there's nothing to register:
 *
 * ```
 * @Serializable
 * data class Profile(val userId: String) : Screen {
 *     @Composable override fun Content() {
 *         Text("User $userId")
 *     }
 * }
 *
 * nav.go(Profile("42"))
 * ```
 *
 * Mark screens `@Serializable` so the back stack survives Android closing your app in the
 * background. Arguments are plain properties: keep them small (ids, not whole objects).
 */
interface Screen {

    @Composable
    fun Content()

    /** How this screen comes in and goes out. */
    val transition: Transition get() = Transition.Default

    /** Hide the [SnapTabs] bar while this screen is showing, e.g. for a full-screen player. */
    val hidesTabs: Boolean get() = false
}

/** A screen shown as a dialog over the previous one. Back or tapping outside closes it. */
interface DialogScreen : Screen

/** A screen shown as a bottom sheet over the previous one. Back or swiping down closes it. */
interface SheetScreen : Screen

/** Screen animations. */
enum class Transition {
    /** Android's standard animation, including the predictive back gesture preview. */
    Default,

    /** Cross-fade. */
    Fade,

    /** Slides up from the bottom and back down, like a modal. */
    SlideUp,

    /** Appears instantly. */
    None,
}
