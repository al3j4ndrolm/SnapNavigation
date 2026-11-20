# SnapNavigation

[![JitPack](https://jitpack.io/v/al3j4ndrolm/SnapNavigation.svg)](https://jitpack.io/#al3j4ndrolm/SnapNavigation)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Jetpack Compose navigation with zero setup.** No NavHost, no routes, no NavController to pass around.

From the maker of the Facilis family: [FacilisCam](https://github.com/al3j4ndrolm/FacilisCam), [FacilisAI](https://github.com/al3j4ndrolm/FacilisAI) and more.

```kotlin
@Serializable
data class Profile(val userId: String) : Screen {
    @Composable override fun Content() {
        Text("User $userId")
        Button(onClick = { nav.back() }) { Text("Back") }
    }
}

// MainActivity: the whole setup
setContent { SnapNav(start = Home) }

// Anywhere: a click, a ViewModel, a coroutine
nav.go(Profile("42"))
```

A screen draws itself, so there's nothing to register. Arguments are typed properties: no route strings, no `Bundle`s.

## Moving around

```kotlin
nav.go(Settings)              // open on top
nav.back()                    // like the back gesture
nav.backTo<Home>()            // back until Home is on top
nav.replace(Checkout)         // swap the current screen
nav.reset(Login)              // clear everything, e.g. after logging out
```

`nav` works from composables, click handlers and ViewModels (`nav.go(...)` after a login succeeds, even before the screen is up). A double tap opens a screen once, not twice.

## Results

```kotlin
// Ask
scope.launch {
    val color = nav.goForResult<String>(ColorPicker) ?: return@launch   // null if closed without a result
}

// Answer
nav.back(result = "Green")
```

## Tabs, each with its own history

```kotlin
SnapTabs(
    Tab(Feed, "Feed", Icons.Default.Home),
    Tab(Search, "Search", Icons.Default.Search),
    Tab(Account, "Account", Icons.Default.Person),
)
```

That's a bottom bar where every tab keeps its place, like Instagram: open a profile in Search, switch to Feed and back, and the profile is still there. `nav.go` opens screens in the current tab. Back goes back within the tab, then to the first tab, then leaves. Tapping the current tab again returns to its first screen.

Use it as the whole app, or inside a screen after a login:

```kotlin
@Serializable object Main : Screen {
    @Composable override fun Content() = SnapTabs(Tab(Feed, ...), Tab(Account, ...))
}

nav.reset(Main)   // after logging in; nav.reset(Login) to log out
```

`Main` only holds the tabs. It can't be one of its own tabs, or each tab would show the tabs again; SnapNavigation stops with an error saying so.

## Tab bar styles

The bar is Material's by default. Pick another look with `bar`:

```kotlin
SnapTabs(
    Tab(Feed, "Feed", Icons.Default.Home),
    Tab(Search, "Search", Icons.Default.Search),
    Tab(Account, "Account", Icons.Default.Person),
    bar = TabBar.Liquid(),
)
```

| Style | What it does |
| --- | --- |
| `TabBar.Material` | The standard Material 3 bar (default) |
| `TabBar.Notched()` | The selected tab floats in a circle; the bar bends around it and the bend slides along |
| `TabBar.CenterButton(icon, label) { }` | A big button in the middle for your main action, with the bar bent around it |
| `TabBar.Liquid()` | A blob behind the selected icon that stretches like liquid as it moves |
| `TabBar.Bump()` | The bar bulges up and the selected tab rises into it with its label |
| `TabBar.Expanding()` | The selected tab grows into a pill with its label |
| `TabBar.Spotlight()` | A lamp slides along the top and shines on the selected tab |
| `TabBar.ColorReveal()` | Each tab has a color that floods the bar from the icon you tap |
| `TabBar.Wave()` | A crest in a thin line glides under the selected tab |
| `TabBar.Dock()` | App-style tiles on a shelf; the selected one swells, like a desktop dock |
| `TabBar.Flip()` | The selected tab flips over in 3D to show its label |
| `TabBar.Aurora()` | A turning rainbow rim and a glow behind the selected tab |

Every color is a parameter, with good defaults:

```kotlin
bar = TabBar.Notched(barColor = Color(0xFF1B1B1F), circleColor = Color(0xFF00BFA5))
bar = TabBar.ColorReveal(colors = listOf(Color.Red, Color.Blue, Color.Green))
bar = TabBar.CenterButton(Icons.Default.Add, "New post", buttonColor = Color.Black) { nav.go(NewPost) }
```

Or draw your own and keep everything else (a back stack per tab, back to the first tab, `hidesTabs`):

```kotlin
bar = TabBar.Custom { tabs, selected, select ->
    MyBar(tabs, selected, onClick = select)
}
```

`nav.selectedTab` tells you which tab is showing, from anywhere.

## Dialogs and bottom sheets

```kotlin
@Serializable data class DeletePost(val id: Int) : DialogScreen { ... }
@Serializable object Filters : SheetScreen { ... }

nav.go(Filters)   // a Material bottom sheet; back or swiping down closes it
```

## Per screen

```kotlin
@Serializable object Player : Screen {
    override val hidesTabs get() = true                 // full screen over the tab bar
    override val transition get() = Transition.SlideUp  // how it comes in and goes out
    @Composable override fun Content() { ... }
}
```

### Transitions

| `Transition.` | Looks like |
|---|---|
| `Default` | Android's standard animation |
| `Fade` | Cross-fade |
| `SlideUp` | Slides up over the previous screen, like a modal |
| `Slide` | Pushes the previous screen out to the side |
| `Parallax` | Slides in over the previous screen, which drifts back a little, like iOS |
| `Zoom` | Grows in as the previous screen grows past you and fades |
| `Rise` | Rises a little while fading in |
| `Pop` | Pops out of the center with a little bounce |
| `None` | Appears instantly |

Back plays the transition in reverse, and the back gesture previews it. Screens that slide over another get your theme's background while they move, so the screen below never shows through.

## Included

- Android's animations and the predictive back gesture (Android 14+)
- `viewModel()` inside a screen belongs to that screen, and is cleared when it closes
- `rememberSaveable` and scroll positions kept per screen, and per tab
- The back stack, tabs included, survives rotation and Android closing your app in the background

## Install

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then (min SDK 24):

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "<your Kotlin version>"
}

dependencies {
    implementation("com.github.al3j4ndrolm:SnapNavigation:1.2.0")
}
```

## Good to know

- **Mark screens `@Serializable`.** That's how the back stack is saved. A screen that isn't still works, but the back stack then starts over if Android closes your app in the background (with a log warning). Keep arguments small: ids, not whole objects.
- **ViewModels** survive rotation, not Android closing the app. Use `SavedStateHandle` in them for what must survive that.
- **`goForResult`** waits in a coroutine, which ends if Android closes the app while the other screen is open.
- **Built on** Google's [Navigation 3](https://developer.android.com/guide/navigation/navigation-3), so the back stack, animations and predictive back are the official ones.

## Contributing

```bash
./gradlew :snapnavigation:testDebugUnitTest :snapnavigation:assembleRelease :sample:assembleDebug
```

`snapnavigation/` is the library and `sample/` a demo app: a login, then tabs with a feed, post details, a color picker sheet, a delete dialog, a full-screen player, and every tab bar style to switch between on the Account tab.

## License

[MIT](LICENSE)
