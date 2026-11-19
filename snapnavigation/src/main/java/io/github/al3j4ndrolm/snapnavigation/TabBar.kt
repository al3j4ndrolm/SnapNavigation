package io.github.al3j4ndrolm.snapnavigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How the bar of [SnapTabs] looks. Pick a style, and change any of its colors if you like:
 *
 * ```
 * SnapTabs(
 *     Tab(Home, "Home", Icons.Default.Home),
 *     Tab(Search, "Search", Icons.Default.Search),
 *     bar = TabBar.Liquid(blobColor = Color(0xFFFFC107)),
 * )
 * ```
 *
 * Every style keeps what SnapTabs does: a back stack per tab, back going to the first tab,
 * tapping the current tab to go back to its first screen, and screens with `hidesTabs`.
 * To draw a bar of your own, use [TabBar.Custom].
 */
@Immutable
abstract class TabBar {

    /** Draws the bar for [tabs], with [selected] highlighted. Call [select] when a tab is tapped. */
    @Composable
    abstract fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit)

    /** The standard Material 3 bottom bar. The default. */
    object Material : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == selected,
                        onClick = { select(index) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    }

    /** The selected tab floats in a circle, and the bar bends around it. The bend slides along. */
    class Notched(
        val barColor: Color = Color(0xFF534AB7),
        val circleColor: Color = Color(0xFFEF9F27),
        val iconColor: Color = Color.White,
        /** The gap between the circle and the bent edge. */
        val space: Dp = 10.dp,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            NotchedBar(tabs, selected, select, Floating, space, barColor, circleColor, iconColor)
    }

    /**
     * A big round button in the middle for your app's main action (a new post, a scan...), with
     * the bar bent around it and the tabs on both sides:
     *
     * ```
     * bar = TabBar.CenterButton(Icons.Default.Add, "New post") { nav.go(NewPost) }
     * ```
     */
    class CenterButton(
        val icon: ImageVector,
        /** Read out by screen readers. */
        val label: String,
        val barColor: Color = Color(0xFF534AB7),
        val buttonColor: Color = Color(0xFFEF9F27),
        val iconColor: Color = Color.White,
        val space: Dp = 8.dp,
        val onClick: () -> Unit,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            ActionBar(tabs, selected, select, icon, label, onClick, Floating, space, barColor, buttonColor, iconColor)
    }

    /** A blob behind the selected icon that stretches like a drop of liquid when it moves. */
    class Liquid(
        val barColor: Color = Color(0xFF26215C),
        val blobColor: Color = Color(0xFFAFA9EC),
        val iconColor: Color = Color.White,
        val selectedIconColor: Color = Color(0xFF26215C),
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            LiquidBar(tabs, selected, select, Floating, barColor, blobColor, iconColor, selectedIconColor)
    }

    /** The bar bulges up over the selected tab, which rises into the bump and shows its label. */
    class Bump(
        val barColor: Color = Color(0xFF0F6E56),
        val accentColor: Color = Color(0xFF9FE1CB),
        val iconColor: Color = Color.White,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            BumpBar(tabs, selected, select, Floating, barColor, accentColor, iconColor)
    }

    /** Icons only, except the selected tab, which grows into a pill with its label. */
    class Expanding(
        /** Defaults to your theme's surface color. */
        val barColor: Color = Color.Unspecified,
        val pillColor: Color = Color(0xFFF5C4B3),
        val pillContentColor: Color = Color(0xFF712B13),
        /** Defaults to your theme's icon color. */
        val iconColor: Color = Color.Unspecified,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) = ExpandingBar(
            tabs, selected, select, Floating,
            barColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHigh },
            pillColor, pillContentColor,
            iconColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
        )
    }

    /** A lamp slides along the top edge and shines a cone of light on the selected tab. */
    class Spotlight(
        val barColor: Color = Color(0xFF1E1E1E),
        val lightColor: Color = Color(0xFFFAC775),
        val iconColor: Color = Color.White,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            SpotlightBar(tabs, selected, select, Floating, barColor, lightColor, iconColor)
    }

    /** Each tab has its own color, which floods the bar in a circle from the tapped icon. */
    class ColorReveal(
        /** One per tab; reused from the start if there are more tabs than colors. */
        val colors: List<Color> = listOf(Color(0xFF534AB7), Color(0xFF0F6E56), Color(0xFF993C1D), Color(0xFF993556)),
        val iconColor: Color = Color.White,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            RevealBar(tabs, selected, select, Floating, colors, iconColor)
    }

    /** A thin line under the icons, with a smooth crest that glides under the selected tab. */
    class Wave(
        /** Defaults to your theme's surface color. */
        val barColor: Color = Color.Unspecified,
        val lineColor: Color = Color(0xFF5DCAA5),
        /** Defaults to your theme's icon color. */
        val iconColor: Color = Color.Unspecified,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) = WaveBar(
            tabs, selected, select, Floating,
            barColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHigh },
            lineColor,
            iconColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
        )
    }

    /** Colorful tiles on a glassy shelf. The selected tile swells, and a wave rolls through its neighbors. */
    class Dock(
        /** One per tab; reused from the start if there are more tabs than colors. */
        val tileColors: List<Color> = listOf(Color(0xFF7F77DD), Color(0xFF1D9E75), Color(0xFFD85A30), Color(0xFFD4537E)),
        /** Defaults to a light tint of your theme's text color. */
        val shelfColor: Color = Color.Unspecified,
        val iconColor: Color = Color.White,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) = DockBar(
            tabs, selected, select, Floating, tileColors,
            shelfColor.takeOrElse { MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f) },
            iconColor,
        )
    }

    /** The selected tab flips over in 3D to show its label on the back. */
    class Flip(
        val barColor: Color = Color(0xFF26215C),
        val cardColor: Color = Color(0xFFEF9F27),
        val cardTextColor: Color = Color(0xFF412402),
        val iconColor: Color = Color.White,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            FlipBar(tabs, selected, select, Floating, barColor, cardColor, cardTextColor, iconColor)
    }

    /** A dark pill with a slowly turning rainbow rim, and a soft glow behind the selected tab. */
    class Aurora(
        val barColor: Color = Color(0xFF15131C),
        /** The rim's colors, going around. Repeat the first at the end for a seamless loop. */
        val rimColors: List<Color> = listOf(
            Color(0xFF5DCAA5), Color(0xFF7F77DD), Color(0xFFED93B1), Color(0xFFEF9F27), Color(0xFF5DCAA5),
        ),
        val iconColor: Color = Color.White,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) =
            AuroraBar(tabs, selected, select, Floating, barColor, rimColors, iconColor)
    }

    /**
     * Your own bar. SnapTabs still keeps a back stack per tab; you draw the bar and call
     * `select(index)` when a tab is tapped:
     *
     * ```
     * bar = TabBar.Custom { tabs, selected, select ->
     *     Row(Modifier.navigationBarsPadding()) {
     *         tabs.forEachIndexed { i, tab ->
     *             IconButton(onClick = { select(i) }) { Icon(tab.icon, tab.label) }
     *         }
     *     }
     * }
     * ```
     */
    class Custom(
        val content: @Composable (tabs: List<Tab>, selected: Int, select: (Int) -> Unit) -> Unit,
    ) : TabBar() {
        @Composable
        override fun Content(tabs: List<Tab>, selected: Int, select: (Int) -> Unit) = content(tabs, selected, select)
    }

    private companion object {
        /** Floating bars sit above the system navigation, with a margin around them. */
        val Floating = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
    }
}
