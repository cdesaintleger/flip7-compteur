package io.github.cdesaintleger.flip7compteur

import java.util.Locale

val numberIds = (0..13).map(Int::toString)
val specialIds = listOf("unlucky7", "lucky13")
val penaltyIds = listOf("minus2", "minus4", "minus6", "minus8", "minus10", "half")
val allCardIds = numberIds + specialIds + penaltyIds
private val values = (0..13).associate { it.toString() to it } + mapOf("unlucky7" to 7, "lucky13" to 13)

data class Hand(val cards: Set<String> = emptySet(), val bust: Boolean = false, val target: Int? = null)
data class Score(val points: Int, val count: Int, val sum: Int, val penalty: Int, val flip: Boolean, val bonus: Int)

object Scoring {
    fun details(hand: Hand, brutal: Boolean = false): Score {
        val numbers = hand.cards.mapNotNull(values::get)
        val count = numbers.size
        val sum = numbers.sum()
        val penalty = hand.cards.filter { it.startsWith("minus") }.sumOf { it.removePrefix("minus").toInt() }
        val flip = count == 7 && !hand.bust
        var base = if (hand.bust || "0" in hand.cards && !flip) 0 else sum
        if (!hand.bust || brutal) base -= penalty
        if (!brutal) base = base.coerceAtLeast(0)
        if ("half" in hand.cards) base = Math.floorDiv(base, 2)
        val bonus = if (flip && (!brutal || hand.target == null)) 15 else 0
        return Score(base + bonus, count, sum, penalty, flip, bonus)
    }

    fun round(hands: List<Hand>, brutal: Boolean = false): List<Int> {
        val points = hands.map { details(it, brutal).points }.toMutableList()
        if (brutal) hands.forEachIndexed { i, hand ->
            hand.target?.let { target ->
                if (details(hand, true).flip && target in hands.indices && target != i) points[target] -= 15
            }
        }
        return points
    }

    fun toggle(hand: Hand, id: String): Hand {
        require(id in allCardIds) { "Carte inconnue." }
        var cards = hand.cards
        if (id in cards) cards = cards - id else {
            val other = when(id) { "7" -> "unlucky7"; "unlucky7" -> "7"; else -> null }
            val count = cards.count { it in values }
            require(id !in values || count < 7 || (other != null && other in cards)) { "Un Flip 7 contient au maximum 7 cartes Numéro." }
            cards = (cards - setOfNotNull(other)) + id
        }
        val result = hand.copy(cards = cards)
        return if (!details(result).flip) result.copy(target = null) else result
    }
}

data class Setup(val names: List<String> = listOf("", "", ""), val target: String = "200", val brutal: Boolean = false)
data class Game(
    val players: List<String>, val target: Int, val brutal: Boolean,
    val rounds: List<List<Hand>> = emptyList(),
    val draft: List<Hand> = players.map { Hand() },
    val editing: Int? = null, val editDraft: List<Hand>? = null
) {
    fun hands() = if (editing == null) draft else requireNotNull(editDraft)
    fun totals() = players.indices.map { i -> rounds.sumOf { Scoring.round(it, brutal)[i] } }
    fun winners(): List<Int> {
        val totals = totals(); val max = totals.maxOrNull() ?: 0
        return if (max < target) emptyList() else players.indices.filter { totals[it] == max }
    }
    fun changeHand(index: Int, transform: (Hand) -> Hand): Game {
        require(index in players.indices)
        val changed = hands().mapIndexed { i, hand -> if (i == index) transform(hand) else hand }
        return if (editing == null) copy(draft = changed) else copy(editDraft = changed)
    }
    fun finish(): Game {
        require(hands().count { Scoring.details(it, brutal).flip } <= 1) { "Un seul Flip 7 est possible dans une manche." }
        return if (editing == null) copy(rounds = rounds + listOf(draft), draft = players.map { Hand() })
        else copy(rounds = rounds.mapIndexed { i, round -> if (i == editing) hands() else round }, editing = null, editDraft = null)
    }
    fun edit(index: Int): Game {
        require(editing == null || editing == index) { "Terminez la correction en cours." }
        require(index in rounds.indices)
        return copy(editing = index, editDraft = rounds[index])
    }
    fun cancelEdit() = copy(editing = null, editDraft = null)
    fun undo(): Game {
        require(editing == null) { "Terminez la correction en cours." }
        require(rounds.isNotEmpty())
        return copy(draft = rounds.last(), rounds = rounds.dropLast(1))
    }
}
data class AppState(val setup: Setup = Setup(), val game: Game? = null, val selected: Int? = null) {
    fun start(): AppState {
        val target = setup.target.toIntOrNull()
        require(target != null && target in 1..9999) { "Choisissez un objectif entre 1 et 9999." }
        val names = setup.names.mapIndexed { i, n -> n.trim().ifEmpty { "Joueur ${i + 1}" } }
        require(names.size in 1..18)
        require(names.map { it.lowercase(Locale.ROOT) }.distinct().size == names.size) { "Chaque joueur doit avoir un nom différent." }
        return copy(game = Game(names, target, setup.brutal), selected = null)
    }
}
