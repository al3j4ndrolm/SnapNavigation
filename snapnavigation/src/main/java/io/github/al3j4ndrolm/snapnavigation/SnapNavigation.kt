package io.github.al3j4ndrolm.snapnavigation

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Navigation from anywhere: composables, click handlers and ViewModels.
 *
 * ```
 * nav.go(Profile("42"))
 * nav.back()
 * ```
 */
val nav: SnapNavigation get() = SnapNavigation

object SnapNavigation {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var navigator: Navigator? = null
    private var exit: (() -> Unit)? = null
    // Calls made before SnapNav is on screen (e.g. from a ViewModel's init) run once it is.
    private val pending = mutableListOf<(Navigator) -> Unit>()

    /** The screen on top (inside the current tab, if there are tabs), or null before [SnapNav] shows. */
    @JvmStatic
    val current: Screen? get() = navigator?.current

    /** The screens of the current back stack, oldest first. */
    @JvmStatic
    val stack: List<Screen> get() = navigator?.innermost?.map { it.screen }.orEmpty()

    /** Whether [back] would stay in the app. */
    @JvmStatic
    val canGoBack: Boolean
        get() = navigator?.let { n ->
            n.root.size > 1 || n.root.last().tabs?.let { it.current.size > 1 || it.selected != 0 } == true
        } ?: false

    /** Opens [screen] on top. Opening the same screen twice in a row (a double tap) is ignored. */
    @JvmStatic
    fun go(screen: Screen) = run { it.go(screen) }

    /**
     * Goes back one screen, like the system back gesture. With tabs, it goes back within the tab,
     * then to the first tab. On the first screen it leaves the app.
     *
     * @param result handed to whoever opened this screen with [goForResult].
     */
    @JvmStatic
    @JvmOverloads
    fun back(result: Any? = null) = run { if (!it.back(result)) exit?.invoke() }

    /** Goes back until the newest [T] is on top. With [inclusive], [T] is closed too. */
    inline fun <reified T : Screen> backTo(inclusive: Boolean = false) = backTo(inclusive) { it is T }

    /** Goes back until [screen] is on top. */
    @JvmStatic
    @JvmOverloads
    fun backTo(screen: Screen, inclusive: Boolean = false) = backTo(inclusive) { it == screen }

    /** Goes back until a screen matching [predicate] is on top. Does nothing if there's none. */
    @JvmStatic
    fun backTo(inclusive: Boolean, predicate: (Screen) -> Boolean) = run { it.backTo(inclusive, predicate) }

    /** Replaces the screen on top with [screen], e.g. a login screen with the home screen. */
    @JvmStatic
    fun replace(screen: Screen) = run { it.replace(screen) }

    /** Clears every back stack (tabs too) and starts again from [screen], e.g. after logging out. */
    @JvmStatic
    fun reset(screen: Screen) = run { it.reset(screen) }

    /** Switches to tab [index] of the [SnapTabs] on screen. Selecting the current tab goes back to its first screen. */
    @JvmStatic
    fun selectTab(index: Int) = run { it.selectTab(index) }

    /**
     * Opens [screen] and waits until it closes. Returns what it passed to `nav.back(result)`, or
     * null if it was closed another way:
     *
     * ```
     * val color = nav.goForResult<Color>(ColorPicker) ?: return
     * ```
     */
    suspend fun <R> goForResult(screen: Screen): R? {
        val deferred = withContext(Dispatchers.Main.immediate) {
            checkNotNull(navigator) { "SnapNav isn't on screen yet" }.goForResult(screen)
        }
        @Suppress("UNCHECKED_CAST")
        return deferred.await() as R?
    }

    internal fun attach(navigator: Navigator, exit: () -> Unit) {
        this.navigator = navigator
        this.exit = exit
        val queued = pending.toList()
        pending.clear()
        queued.forEach { it(navigator) }
    }

    internal fun detach(navigator: Navigator) {
        if (this.navigator === navigator) {
            this.navigator = null
            exit = null
        }
    }

    private fun run(action: (Navigator) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { run(action) }
            return
        }
        navigator?.let(action) ?: pending.add(action)
    }
}
