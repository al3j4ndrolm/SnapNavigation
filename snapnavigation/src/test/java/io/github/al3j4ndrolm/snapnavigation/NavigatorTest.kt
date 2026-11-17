package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
object Home : Screen {
    @Composable override fun Content() = Unit
}

@Serializable
object Search : Screen {
    @Composable override fun Content() = Unit
}

@Serializable
object Settings : Screen {
    @Composable override fun Content() = Unit
}

@Serializable
data class Profile(val userId: String, val tab: Int = 0) : Screen {
    @Composable override fun Content() = Unit
}

/** Not @Serializable: the back stack can't be saved. */
class Unsaveable(val bitmapId: Long) : Screen {
    @Composable override fun Content() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorTest {

    private var now = 10_000L
    private val navigator = Navigator(Home) { now }

    private fun screens() = navigator.innermost.map { it.screen }

    private fun later() {
        now += 1_000
    }

    @Test
    fun `go and back`() {
        navigator.go(Profile("42"))
        later()
        navigator.go(Settings)
        assertEquals(listOf(Home, Profile("42"), Settings), screens())
        assertTrue(navigator.back())
        assertEquals(Profile("42"), navigator.current)
        assertTrue(navigator.back())
        assertFalse("nothing left: the app would close", navigator.back())
        assertEquals(listOf(Home), screens())
    }

    @Test
    fun `a double tap opens the screen once, but opening it again later works`() {
        navigator.go(Profile("42"))
        navigator.go(Profile("42"))
        assertEquals(2, screens().size)
        later()
        navigator.go(Profile("42"))
        assertEquals(3, screens().size)
        // A different screen right away is not a double tap.
        navigator.go(Profile("7"))
        assertEquals(4, screens().size)
    }

    @Test
    fun `same screen twice has separate entries`() {
        navigator.go(Profile("42"))
        later()
        navigator.go(Profile("42"))
        val ids = navigator.root.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `backTo, inclusive and missing`() {
        navigator.go(Search); later()
        navigator.go(Profile("1")); later()
        navigator.go(Settings)
        assertTrue(navigator.backTo(inclusive = false) { it == Search })
        assertEquals(listOf(Home, Search), screens())

        navigator.go(Profile("1")); later()
        assertTrue(navigator.backTo(inclusive = true) { it is Profile })
        assertEquals(listOf(Home, Search), screens())

        assertFalse(navigator.backTo(inclusive = false) { it == Settings })
        assertEquals(listOf(Home, Search), screens())

        // Inclusive never empties the stack.
        assertTrue(navigator.backTo(inclusive = true) { it == Home })
        assertEquals(listOf(Home), screens())
    }

    @Test
    fun `replace and reset`() {
        navigator.go(Search); later()
        navigator.replace(Settings)
        assertEquals(listOf(Home, Settings), screens())
        navigator.reset(Profile("9"))
        assertEquals(listOf(Profile("9")), screens())
    }

    @Test
    fun `results come back from back(result), or null otherwise`() {
        val picked = navigator.goForResult(Settings)
        navigator.back(result = "red")
        assertEquals("red", picked.getCompleted())

        later()
        val cancelled = navigator.goForResult(Search)
        navigator.back()
        assertNull(cancelled.getCompleted())

        later()
        val reset = navigator.goForResult(Profile("1"))
        navigator.reset(Home)
        assertNull(reset.getCompleted())
    }

    @Test
    fun `tabs keep separate back stacks`() {
        val tabs = navigator.tabsFor(navigator.root.last(), listOf(Home, Search, Settings))
        navigator.go(Profile("1"))
        assertEquals(listOf(Home, Profile("1")), screens())

        navigator.selectTab(1)
        later()
        navigator.go(Profile("2"))
        assertEquals(listOf(Search, Profile("2")), screens())

        navigator.selectTab(0)
        assertEquals(listOf(Home, Profile("1")), screens())
        assertEquals(listOf(Search, Profile("2")), tabs.stacks[1].map { it.screen })
    }

    @Test
    fun `back in tabs pops the tab, then goes to the first tab, then leaves`() {
        navigator.tabsFor(navigator.root.last(), listOf(Home, Search))
        navigator.selectTab(1)
        navigator.go(Profile("2"))
        assertTrue(navigator.back())
        assertEquals(Search, navigator.current)
        assertTrue(navigator.back())
        assertEquals(Home, navigator.current)
        assertFalse(navigator.back())
    }

    @Test
    fun `tapping the current tab again goes back to its first screen`() {
        navigator.tabsFor(navigator.root.last(), listOf(Home, Search))
        navigator.go(Profile("1")); later()
        navigator.go(Settings)
        navigator.selectTab(0)
        assertEquals(listOf(Home), screens())
    }

    @Test
    fun `reset leaves the tabs`() {
        navigator.tabsFor(navigator.root.last(), listOf(Home, Search))
        navigator.go(Profile("1"))
        navigator.reset(Settings)
        assertEquals(listOf(Settings), screens())
        assertNull(navigator.root.last().tabs)
    }

    @Test
    fun `saving and restoring, tabs and arguments included`() {
        navigator.tabsFor(navigator.root.last(), listOf(Home, Search))
        navigator.go(Profile("42", tab = 3))
        navigator.selectTab(1)
        val saved = checkNotNull(Saving.save(navigator))

        val restored = Navigator(Settings)
        assertTrue(Saving.restore(restored, saved))
        val tabs = checkNotNull(restored.root.single().tabs)
        assertEquals(1, tabs.selected)
        assertEquals(listOf(Home, Profile("42", tab = 3)), tabs.stacks[0].map { it.screen })
        assertEquals(listOf(Search), tabs.stacks[1].map { it.screen })
        // Ids survive, so each screen gets its own saved state back.
        assertEquals(navigator.root.single().tabs!!.stacks[0].map { it.id }, tabs.stacks[0].map { it.id })
    }

    @Test
    fun `a screen that isn't @Serializable turns saving off instead of crashing`() {
        navigator.go(Unsaveable(1))
        assertNull(Saving.save(navigator))
        val fresh = Navigator(Home)
        assertFalse(Saving.restore(fresh, "not json"))
        assertEquals(listOf(Home), fresh.root.map { it.screen })
    }
}
