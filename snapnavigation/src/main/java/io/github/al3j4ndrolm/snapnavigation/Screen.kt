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

/** Screen animations. Back plays them in reverse, and the back gesture previews them. */
enum class Transition {
    /** Android's standard animation, including the predictive back gesture preview. */
    Default,

    /** Cross-fade. */
    Fade,

    /** Slides up from the bottom over the previous screen, and back down, like a modal. */
    SlideUp,

    /** Pushes the previous screen out to the side. */
    Slide,

    /** Slides in from the side over the previous screen, which drifts back a little, like iOS. */
    Parallax,

    /** Grows in as the previous screen grows past you and fades, like moving deeper into the app. */
    Zoom,

    /** Rises a little while fading in. Subtle, good for settings and detail pages. */
    Rise,

    /** Pops out of the center with a little bounce. */
    Pop,

    /** Appears instantly. */
    None,
}
