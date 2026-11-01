package io.github.cdesaintleger.flip7compteur

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.gameData by preferencesDataStore(name = "flip7_native_v1")
private val gameKey = stringPreferencesKey("snapshot")

object GameJson {
    private fun hand(h: Hand) = JSONObject().put("cards", JSONArray(h.cards.toList())).put("bust", h.bust).put("target", h.target ?: JSONObject.NULL)
    private fun hands(list: List<Hand>) = JSONArray(list.map(::hand))
    private fun readHand(o: JSONObject): Hand {
        val cards = o.getJSONArray("cards").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
        require(cards.all { it in allCardIds })
        return Hand(cards, o.getBoolean("bust"), if (o.isNull("target")) null else o.getInt("target"))
    }
    private fun readHands(a: JSONArray) = (0 until a.length()).map { readHand(a.getJSONObject(it)) }
    fun encode(s: AppState): String {
        val root = JSONObject().put("version", 1).put("setup", JSONObject().put("names", JSONArray(s.setup.names)).put("target", s.setup.target).put("brutal", s.setup.brutal).put("classic", s.setup.classic)).put("selected", s.selected ?: JSONObject.NULL)
        s.game?.let { g -> root.put("game", JSONObject().put("players", JSONArray(g.players)).put("target", g.target).put("brutal", g.brutal).put("classic", g.classic).put("rounds", JSONArray(g.rounds.map(::hands))).put("draft", hands(g.draft)).put("editing", g.editing ?: JSONObject.NULL).put("editDraft", g.editDraft?.let(::hands) ?: JSONObject.NULL)) }
        return root.toString()
    }
    fun decode(raw: String?): AppState {
        if (raw == null) return AppState()
        val root = JSONObject(raw); require(root.getInt("version") == 1)
        val setup = root.getJSONObject("setup")
        val names = setup.getJSONArray("names").let { a -> (0 until a.length()).map { a.getString(it) } }
        val config = Setup(names, setup.getString("target"), setup.getBoolean("brutal"), setup.optBoolean("classic", false))
        require(names.size in 1..18)
        val game = root.optJSONObject("game")?.let { o ->
            val players = o.getJSONArray("players").let { a -> (0 until a.length()).map { a.getString(it) } }
            val rounds = o.getJSONArray("rounds").let { a -> (0 until a.length()).map { readHands(a.getJSONArray(it)) } }
            val draft = readHands(o.getJSONArray("draft"))
            val editing = if(o.isNull("editing")) null else o.getInt("editing")
            val editDraft = if(o.isNull("editDraft")) null else readHands(o.getJSONArray("editDraft"))
            require(players.size in 1..18 && o.getInt("target") in 1..9999)
            require(draft.size == players.size && rounds.all { it.size == players.size })
            require(editing == null && editDraft == null || (editing != null && editing in rounds.indices && editDraft?.size == players.size))
            Game(players, o.getInt("target"), o.getBoolean("brutal"), rounds, draft, editing, editDraft, o.optBoolean("classic", false))
        }
        val selected = if (root.isNull("selected")) null else root.getInt("selected").takeIf { game != null && it in game.players.indices }
        return AppState(config, game, selected)
    }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val data = application.gameData
    val message = MutableStateFlow<String?>(null)
    val state = data.data.map { GameJson.decode(it[gameKey]) }.catch { message.value = "Impossible de lire la sauvegarde. Redémarrez l’application pour réessayer." }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun update(transform: (AppState) -> AppState) {
        viewModelScope.launch {
            try {
                data.edit { prefs -> prefs[gameKey] = GameJson.encode(transform(GameJson.decode(prefs[gameKey]))) }
            } catch (e: IllegalArgumentException) { message.value = e.message ?: "Cette action n’est pas possible." }
            catch (_: Exception) { message.value = "La sauvegarde n’a pas pu être enregistrée. Réessayez." }
        }
    }
    fun changeHand(transform: (Hand) -> Hand) = update { s ->
        val g = requireNotNull(s.game); s.copy(game = g.changeHand(requireNotNull(s.selected), transform))
    }
}
