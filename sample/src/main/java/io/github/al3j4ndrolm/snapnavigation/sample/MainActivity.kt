package io.github.al3j4ndrolm.snapnavigation.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.al3j4ndrolm.snapnavigation.DialogScreen
import io.github.al3j4ndrolm.snapnavigation.Screen
import io.github.al3j4ndrolm.snapnavigation.SheetScreen
import io.github.al3j4ndrolm.snapnavigation.SnapNav
import io.github.al3j4ndrolm.snapnavigation.SnapTabs
import io.github.al3j4ndrolm.snapnavigation.Tab
import io.github.al3j4ndrolm.snapnavigation.Transition
import io.github.al3j4ndrolm.snapnavigation.nav
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SnapNav(start = Login)
                }
            }
        }
    }
}

@Serializable
object Login : Screen {
    @Composable
    override fun Content() = Page("Welcome") {
        Text("A login screen. Logging in replaces it with the tabs, so back doesn't return here.")
        Button(onClick = { nav.reset(Main) }) { Text("Log in") }
    }
}

@Serializable
object Main : Screen {
    @Composable
    override fun Content() {
        SnapTabs(
            Tab(Feed, "Feed", Icons.Default.Home),
            Tab(Search, "Search", Icons.Default.Search),
            Tab(Account, "Account", Icons.Default.Person),
        )
    }
}

// region Feed tab

@Serializable
object Feed : Screen {
    @Composable
    override fun Content() = Page("Feed") {
        LazyColumn {
            items((1..30).toList()) { id ->
                Text(
                    "Post #$id",
                    Modifier.fillMaxWidth().clickable { nav.go(Post(id)) }.padding(vertical = 14.dp),
                )
                HorizontalDivider()
            }
        }
    }
}

/** Survives rotation, cleared when its Post screen is closed. */
class PostViewModel : ViewModel() {
    var likes by mutableIntStateOf(0)
}

@Serializable
data class Post(val id: Int) : Screen {
    @Composable
    override fun Content() = Page("Post #$id") {
        val model: PostViewModel = viewModel()
        val scope = rememberCoroutineScope()
        var color by rememberSaveable { mutableStateOf("none") }

        Button(onClick = { model.likes++ }) { Text("Like (${model.likes})") }
        Text("Color: $color")
        Button(onClick = {
            scope.launch { nav.goForResult<String>(ColorPicker)?.let { color = it } }
        }) { Text("Pick a color") }
        OutlinedButton(onClick = { nav.go(Post(id + 1)) }) { Text("Next post") }
        OutlinedButton(onClick = { nav.go(DeletePost(id)) }) { Text("Delete") }
        TextButton(onClick = { nav.backTo<Feed>() }) { Text("Back to feed") }
    }
}

@Serializable
object ColorPicker : SheetScreen {
    @Composable
    override fun Content() {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Pick a color", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("Red" to Color(0xFFE53935), "Green" to Color(0xFF43A047), "Blue" to Color(0xFF1E88E5)).forEach { (name, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(56.dp).background(value, CircleShape).clickable { nav.back(result = name) })
                        Text(name)
                    }
                }
            }
        }
    }
}

@Serializable
data class DeletePost(val id: Int) : DialogScreen {
    @Composable
    override fun Content() {
        AlertDialog(
            onDismissRequest = { nav.back() },
            title = { Text("Delete post #$id?") },
            confirmButton = { TextButton(onClick = { nav.backTo<Feed>() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { nav.back() }) { Text("Cancel") } },
        )
    }
}

// endregion

@Serializable
object Search : Screen {
    @Composable
    override fun Content() = Page("Search") {
        var query by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(query, { query = it }, label = { Text("Type, switch tabs, come back") })
        Button(onClick = { nav.go(Profile(query.ifBlank { "ale" })) }) { Text("Open profile") }
    }
}

@Serializable
data class Profile(val name: String) : Screen {
    @Composable
    override fun Content() = Page("@$name") {
        Text("Opened from the Search tab, so it stays in that tab.")
        Button(onClick = { nav.back() }) { Text("Back") }
    }
}

@Serializable
object Account : Screen {
    @Composable
    override fun Content() = Page("Account") {
        Button(onClick = { nav.go(Player) }) { Text("Open player (full screen)") }
        OutlinedButton(onClick = { nav.reset(Login) }) { Text("Log out") }
    }
}

@Serializable
object Player : Screen {
    override val hidesTabs get() = true
    override val transition get() = Transition.SlideUp

    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize().background(Color(0xFF111111)).safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Now playing", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = { nav.back() }) { Text("Close") }
            }
        }
    }
}

@Composable
private fun Page(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}
