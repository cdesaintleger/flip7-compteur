package io.github.cdesaintleger.flip7compteur

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class GameTest {
    private fun h(vararg cards: String) = Hand(cards.toSet())
    @Test fun frenchOrderAndNegativeRounding() {
        assertEquals(8, Scoring.details(h("10", "11", "minus4", "half")).points)
        assertEquals(0, Scoring.details(h("1", "minus10", "half")).points)
        assertEquals(-5, Scoring.details(h("1", "minus10", "half"), true).points)
    }
    @Test fun zeroAndLucky13AndBonus() {
        assertEquals(0, Scoring.details(h("0", "13")).points)
        assertEquals(36, Scoring.details(h("0", "1", "2", "3", "4", "5", "6")).points)
        assertEquals(56, Scoring.details(h("13", "lucky13", "1", "2", "3", "4", "5")).points)
        assertEquals(27, Scoring.details(h("1", "2", "3", "4", "5", "6", "7", "half", "minus4")).points)
    }
    @Test fun bustedAndVengeance() {
        assertEquals(0, Scoring.details(h("13", "minus10").copy(bust = true)).points)
        assertEquals(-10, Scoring.details(h("13", "minus10").copy(bust = true), true).points)
        val flip = h("1", "2", "3", "4", "5", "6", "7")
        assertEquals(listOf(28, -15), Scoring.round(listOf(flip.copy(target = 1), Hand()), true))
        assertEquals(listOf(43, 0), Scoring.round(listOf(flip, Hand()), true))
        assertEquals(listOf(43, 0), Scoring.round(listOf(flip.copy(target = 1), Hand()), false))
    }
    @Test fun replacementAndBonusTargetReset() {
        val hand = Scoring.toggle(h("7", "8"), "unlucky7")
        assertEquals(setOf("8", "unlucky7"), hand.cards)
        assertEquals(15, Scoring.details(hand).points)
        val flip = h("1", "2", "3", "4", "5", "6", "7").copy(target = 1)
        assertNull(Scoring.toggle(flip, "7").target)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotAddEighthNumber() {
        Scoring.toggle(h("1", "2", "3", "4", "5", "6", "7"), "8")
    }
    @Test fun correctionsKeepDraftAndCanBeCancelled() {
        val original = Game(listOf("Alice", "Bob"), 200, false).changeHand(0) { h("10") }.finish()
            .changeHand(1) { h("12") }
        val edit = original.edit(0).changeHand(0) { h("13") }
        assertEquals(original, edit.cancelEdit())
        val corrected = edit.finish()
        assertEquals(listOf(13, 0), corrected.totals())
        assertEquals(setOf("12"), corrected.draft[1].cards)
        assertEquals(listOf(0, 0), corrected.undo().totals())
        assertEquals(setOf("13"), corrected.undo().draft[0].cards)
    }
    @Test fun winnersAndTies() {
        val game = Game(listOf("Alice", "Bob"), 10, false).copy(draft = listOf(h("10"), h("10"))).finish()
        assertEquals(listOf(0, 1), game.winners())
        assertEquals(listOf(0), game.changeHand(0) { h("1") }.finish().winners())
    }
    @Test fun serializedCorrectionAndDraftRoundTrip() {
        val game = Game(listOf("Alice", "Bob"), 200, true).changeHand(0) { h("10", "minus2") }.finish().changeHand(1) { h("lucky13") }.edit(0).changeHand(1) { h("minus10").copy(bust = true) }
        val s = AppState(Setup(listOf("Alice", "Bob"), "200", true), game, 1)
        assertEquals(s, GameJson.decode(GameJson.encode(s)))
        assertEquals(AppState(), GameJson.decode(null))
    }
    @Test fun sameResultsAs96WebFixtures() {
        val data = JSONArray(requireNotNull(javaClass.classLoader?.getResource("scoring-fixtures.json")).readText())
        for(i in 0 until data.length()) {
            val row = data.getJSONObject(i); val a = row.getJSONArray("hands")
            val hands = (0 until a.length()).map { index ->
                val o = a.getJSONObject(index);val c = o.getJSONArray("cards")
                Hand((0 until c.length()).map { c.getString(it) }.toSet(), o.getBoolean("bust"), if(o.isNull("target")) null else o.getInt("target"))
            }
            val expected = row.getJSONArray("expected").let { e -> (0 until e.length()).map { e.getInt(it) } }
            assertEquals("Fixture $i", expected, Scoring.round(hands, row.getString("mode") == "brutal"))
        }
    }
}
