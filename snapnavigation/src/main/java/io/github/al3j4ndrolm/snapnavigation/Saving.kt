package io.github.al3j4ndrolm.snapnavigation

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * Saves the back stacks as JSON, so they survive Android closing the app in the background.
 * Screens are stored by class name and restored with their own kotlinx.serialization serializer,
 * found by reflection, so apps never register them.
 */
internal object Saving {

    private const val TAG = "SnapNavigation"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    class SavedEntry(val id: String, val type: String, val data: JsonElement, val tabs: SavedTabs? = null)

    @Serializable
    class SavedTabs(val selected: Int, val stacks: List<List<SavedEntry>>)

    /** The navigator as JSON, or null if a screen isn't `@Serializable` (then nothing is saved). */
    fun save(navigator: Navigator): String? = try {
        json.encodeToString(navigator.root.map { save(it) })
    } catch (e: Exception) {
        Log.w(TAG, "Can't save the back stack, so it won't survive the app being closed in the background: ${e.message}")
        null
    }

    /** Puts saved back stacks into [navigator]. Returns false (changing nothing) if they can't be read. */
    fun restore(navigator: Navigator, saved: String): Boolean = try {
        val entries = json.decodeFromString<List<SavedEntry>>(saved).map { restore(it) }
        require(entries.isNotEmpty())
        navigator.root.clear()
        navigator.root.addAll(entries)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't restore the back stack, starting over: ${e.message}")
        false
    }

    private fun save(entry: Entry): SavedEntry {
        val type = entry.screen.javaClass.name
        val data = json.encodeToJsonElement(serializerFor(entry.screen.javaClass), entry.screen)
        val tabs = entry.tabs?.let { tabs ->
            SavedTabs(tabs.selected, tabs.stacks.map { stack -> stack.map { save(it) } })
        }
        return SavedEntry(entry.id, type, data, tabs)
    }

    private fun restore(saved: SavedEntry): Entry {
        val screen = json.decodeFromJsonElement(serializerFor(Class.forName(saved.type)), saved.data) as Screen
        return Entry(screen, saved.id).apply {
            saved.tabs?.let { savedTabs ->
                val restored = Tabs(emptyList(), 0)
                savedTabs.stacks.forEach { stack -> restored.stacks += mutableStateListOfEntries(stack.map { restore(it) }) }
                restored.selected = savedTabs.selected.coerceIn(0, maxOf(0, restored.stacks.lastIndex))
                tabs = restored
            }
        }
    }

    private fun mutableStateListOfEntries(entries: List<Entry>) =
        androidx.compose.runtime.mutableStateListOf<Entry>().apply { addAll(entries) }

    @Suppress("UNCHECKED_CAST")
    private fun serializerFor(type: Class<*>): KSerializer<Any> = try {
        serializer(type)
    } catch (e: Exception) {
        throw IllegalArgumentException("${type.simpleName} isn't @Serializable", e)
    }
}
