package io.github.al3j4ndrolm.snapnavigation

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.core.os.bundleOf
import kotlinx.serialization.Serializable

/**
 * The whole navigation setup: shows [start], then whatever `nav.go(...)` opens.
 *
 * ```
 * setContent { SnapNav(start = Home) }
 * ```
 */
@Composable
fun SnapNav(start: Screen, modifier: Modifier = Modifier) {
    val navigator = viewModel(key = "snapnavigation") { NavigatorViewModel(start, createSavedStateHandle()) }.navigator
    val activity = LocalActivity.current
    DisposableEffect(navigator) {
        // nav.back() on the first screen leaves, like the back gesture.
        SnapNavigation.attach(navigator) { activity?.finish() }
        onDispose { SnapNavigation.detach(navigator) }
    }
    // Handed down directly: the effect above runs after the first frame, which may already show tabs.
    CompositionLocalProvider(LocalNavigator provides navigator) {
        Stack(navigator.root, modifier)
    }
}

/** A tab of [SnapTabs]: its first screen, and how it looks in the bar. */
@Immutable
class Tab(val screen: Screen, val label: String, val icon: ImageVector)

/**
 * A bottom bar of tabs, each with its own back stack, so switching tabs keeps your place:
 *
 * ```
 * setContent {
 *     SnapTabs(
 *         Tab(Home, "Home", Icons.Default.Home),
 *         Tab(Search, "Search", Icons.Default.Search),
 *     )
 * }
 * ```
 *
 * Use it as the whole app, or inside a screen (e.g. after a login screen). `nav.go` opens
 * screens in the current tab; back goes back within the tab, then to the first tab.
 */
@Composable
fun SnapTabs(vararg tabs: Tab, modifier: Modifier = Modifier) {
    require(tabs.isNotEmpty()) { "SnapTabs needs at least one tab" }
    val entry = LocalEntry.current
    if (entry == null) {
        // The whole app is tabs: put them in a root screen so nav.reset(...) can still leave them.
        CompositionLocalProvider(LocalTabsForRoot provides tabs.toList()) {
            SnapNav(start = TabsRoot, modifier = modifier)
        }
    } else {
        TabsContent(entry, tabs.toList(), modifier)
    }
}

/** The root screen of an app that is all tabs. */
@Serializable
internal object TabsRoot : Screen {
    @Composable
    override fun Content() {
        TabsContent(checkNotNull(LocalEntry.current), LocalTabsForRoot.current, Modifier)
    }

    override val transition: Transition get() = Transition.None
}

@Composable
private fun TabsContent(entry: Entry, tabs: List<Tab>, modifier: Modifier) {
    val navigator = checkNotNull(LocalNavigator.current) { "SnapTabs must be shown inside SnapNav" }
    val state = remember(entry) { navigator.tabsFor(entry, tabs.map { it.screen }) }

    // At a tab's first screen, back goes to the first tab before leaving.
    BackHandler(enabled = state.selected != 0 && state.current.size == 1) { nav.back() }

    // One set of decorators for all tabs, so each tab's screens keep their state while hidden.
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<Entry>(),
        rememberViewModelStoreNavEntryDecorator<Entry>(),
    )
    val decorated = state.stacks.map { stack -> rememberDecoratedNavEntries(stack.toList(), decorators, ::navEntry) }

    Column(modifier.fillMaxSize()) {
        NavDisplay(
            entries = decorated[state.selected],
            modifier = Modifier.weight(1f).fillMaxWidth(),
            sceneStrategies = listOf(DialogSceneStrategy(), SheetSceneStrategy()),
            onBack = { nav.back() },
        )
        if (!state.current.last().screen.hidesTabs) {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == state.selected,
                        onClick = { nav.selectTab(index) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Stack(stack: List<Entry>, modifier: Modifier) {
    NavDisplay(
        backStack = stack,
        modifier = modifier,
        onBack = { nav.back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = listOf(DialogSceneStrategy(), SheetSceneStrategy()),
        entryProvider = ::navEntry,
    )
}

private fun navEntry(entry: Entry): NavEntry<Entry> =
    NavEntry(entry, contentKey = entry.id, metadata = metadataFor(entry.screen)) {
        CompositionLocalProvider(LocalEntry provides it) { it.screen.Content() }
    }

private val LocalNavigator = staticCompositionLocalOf<Navigator?> { null }

/** The back stack entry whose screen is being drawn. */
internal val LocalEntry = staticCompositionLocalOf<Entry?> { null }

private val LocalTabsForRoot = staticCompositionLocalOf<List<Tab>> { emptyList() }

/** Keeps the navigator through rotation, and saves it for when Android closes the app in the background. */
internal class NavigatorViewModel(start: Screen, handle: SavedStateHandle) : ViewModel() {

    val navigator = Navigator(start)

    init {
        handle.get<Bundle>(KEY)?.getString(KEY)?.let { Saving.restore(navigator, it) }
        handle.setSavedStateProvider(KEY) { Saving.save(navigator)?.let { bundleOf(KEY to it) } ?: bundleOf() }
    }

    private companion object {
        const val KEY = "snapnavigation"
    }
}
