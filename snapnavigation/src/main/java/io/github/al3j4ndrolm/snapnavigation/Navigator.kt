package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

/** One screen in a back stack. [id] keeps two copies of the same screen apart. */
internal class Entry(
    val screen: Screen,
    val id: String = UUID.randomUUID().toString(),
    val pushedAt: Long = 0,
) {
    /** Tabs shown by this screen with [SnapTabs], created the first time they're shown. */
    var tabs: Tabs? by mutableStateOf(null)

    /** Completed with the screen's result when it's removed; set by [SnapNavigation.goForResult]. */
    var result: CompletableDeferred<Any?>? = null
    var resultValue: Any? = null

    /** Called once the screen leaves the back stack: hands back its result, if anyone waits for one. */
    fun finish() {
        result?.complete(resultValue)
        result = null
        tabs?.stacks?.forEach { stack -> stack.forEach { it.finish() } }
    }

    override fun toString(): String = "Entry($screen)"
}

/** The back stacks of a [SnapTabs]: one per tab, and which tab is showing. */
internal class Tabs(roots: List<Screen>, now: Long) {
    val stacks: SnapshotStateList<SnapshotStateList<Entry>> =
        mutableStateListOf(*roots.map { mutableStateListOf(Entry(it, pushedAt = now)) }.toTypedArray())
    var selected by mutableIntStateOf(0)

    val current: SnapshotStateList<Entry> get() = stacks[selected]
}

/**
 * The app's navigation state: a root back stack whose screens may show tabs, each with its own
 * back stack. All changes happen on the main thread.
 */
internal class Navigator(
    start: Screen,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val root: SnapshotStateList<Entry> = mutableStateListOf(Entry(start, pushedAt = clock()))

    /** The back stack that `go` pushes onto: the current tab of the top screen, or the root. */
    val innermost: SnapshotStateList<Entry>
        get() = root.last().tabs?.current ?: root

    val current: Screen get() = innermost.last().screen

    /** The last screen that came in or went out, so the one below it can be drawn to match. */
    var lastMove by mutableStateOf(Move(Transition.Default, opening = true))

    fun go(screen: Screen) {
        val stack = innermost
        val top = stack.last()
        // A double tap on a button would open the same screen twice.
        if (top.screen == screen && clock() - top.pushedAt < DOUBLE_TAP_MILLIS) return
        stack += Entry(screen, pushedAt = clock())
        lastMove = Move(screen.transition, opening = true)
    }

    fun goForResult(screen: Screen): CompletableDeferred<Any?> {
        go(screen)
        val entry = innermost.last()
        return CompletableDeferred<Any?>().also { deferred ->
            if (entry.screen == screen && entry.result == null) entry.result = deferred else deferred.complete(null)
        }
    }

    /** Goes back one step. Returns false if there's nowhere to go back to (the app would close). */
    fun back(result: Any? = null): Boolean {
        val tabs = root.last().tabs
        if (tabs != null) {
            if (tabs.current.size > 1) {
                tabs.current.pop(result)
                return true
            }
            // At a tab's first screen, back goes to the first tab before leaving the tabs.
            if (tabs.selected != 0) {
                lastMove = Move(Transition.Default, opening = false)
                tabs.selected = 0
                return true
            }
        }
        if (root.size > 1) {
            root.pop(result)
            return true
        }
        return false
    }

    /** Goes back until the newest screen matching [predicate] is on top. */
    fun backTo(inclusive: Boolean, predicate: (Screen) -> Boolean): Boolean {
        for (stack in listOfNotNull(root.last().tabs?.current, root)) {
            val index = stack.indexOfLast { predicate(it.screen) }
            if (index < 0) continue
            val keep = if (inclusive) maxOf(index, 1) else index + 1
            val top = stack.last().screen
            while (stack.size > keep) stack.pop()
            // The top screen is the one seen leaving.
            if (stack.last().screen != top) lastMove = Move(top.transition, opening = false)
            return true
        }
        return false
    }

    fun replace(screen: Screen) {
        val stack = innermost
        val removed = stack.removeAt(stack.lastIndex)
        stack += Entry(screen, pushedAt = clock())
        lastMove = Move(screen.transition, opening = true)
        removed.finish()
    }

    /** Clears everything, tabs included, and starts again from [screen]. */
    fun reset(screen: Screen) {
        val removed = root.toList()
        root.clear()
        root += Entry(screen, pushedAt = clock())
        lastMove = Move(screen.transition, opening = true)
        removed.forEach { it.finish() }
    }

    fun selectTab(index: Int) {
        val tabs = root.last().tabs ?: throw IllegalStateException("The current screen doesn't show tabs")
        require(index in tabs.stacks.indices) { "There's no tab $index" }
        if (tabs.selected == index) {
            // Tapping the current tab again goes back to its first screen.
            val top = tabs.current.last().screen
            while (tabs.current.size > 1) tabs.current.pop()
            lastMove = Move(top.transition, opening = false)
        } else {
            lastMove = Move(Transition.Default, opening = true)
            tabs.selected = index
        }
    }

    /** The tabs for [entry], created with [roots] the first time (or restored). */
    fun tabsFor(entry: Entry, roots: List<Screen>): Tabs {
        checkTabs(entry, roots)
        entry.tabs?.let { if (it.stacks.size == roots.size) return it }
        return Tabs(roots, clock()).also { entry.tabs = it }
    }

    /** Catches tabs that would draw themselves forever, which otherwise ends in an OutOfMemoryError. */
    private fun checkTabs(entry: Entry, roots: List<Screen>) {
        val name = entry.screen::class.simpleName ?: entry.screen.toString()
        check(entry.screen !in roots) {
            "$name shows SnapTabs with $name as one of its tabs, so the tab would show SnapTabs again, forever. " +
                "Show the tabs from a screen of their own:\n" +
                "    @Serializable object MainTabs : Screen {\n" +
                "        @Composable override fun Content() = SnapTabs(Tab($name, ...), ...)\n" +
                "    }\n" +
                "and open that one instead, e.g. nav.reset(MainTabs)."
        }
        val inTab = root.any { screen -> screen.tabs?.stacks?.any { stack -> stack.any { it === entry } } == true }
        check(!inTab) {
            "$name shows SnapTabs, but it's already inside a tab. Tabs inside tabs aren't supported: " +
                "show SnapTabs from a screen that isn't a tab, like the one you open after logging in."
        }
    }

    private fun SnapshotStateList<Entry>.pop(result: Any? = null) {
        val entry = removeAt(lastIndex)
        lastMove = Move(entry.screen.transition, opening = false)
        if (result != null) entry.resultValue = result
        entry.finish()
    }

    companion object {
        const val DOUBLE_TAP_MILLIS = 700L
    }
}
