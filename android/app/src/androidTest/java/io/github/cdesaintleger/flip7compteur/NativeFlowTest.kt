package io.github.cdesaintleger.flip7compteur

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import org.junit.Rule
import org.junit.Test
import java.io.File

class NativeFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun tap(tag: String, list: String = "game-list") {
        ui.waitUntil(15_000) { ui.onAllNodesWithTag(list).fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        val vm = ViewModelProvider(ui.activity)[GameViewModel::class.java]
        val before = vm.state.value
        ui.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        ui.waitUntil(15_000) { vm.state.value != before }
        ui.waitForIdle()
    }
    private fun score(index: Int, value: Int) {
        ui.onNodeWithTag("game-list").performScrollToNode(hasTestTag("player-$index"))
        ui.onNodeWithTag("score-$index", useUnmergedTree = true).assertTextEquals("$value pts")
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val file = File(ui.activity.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile?.mkdirs()
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        // Gradle uninstalls the app after instrumentation, removing externalFilesDir.
        // Preserve screenshots in shell-owned storage before that cleanup.
        val shell = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun command(text: String): String = ParcelFileDescriptor.AutoCloseInputStream(shell.executeShellCommand(text)).bufferedReader().use { it.readText().trim() }
        // executeShellCommand executes a process directly; shell operators are not parsed.
        command("mkdir -p /data/local/tmp/flip7-screenshots")
        command("cp ${file.absolutePath} /data/local/tmp/flip7-screenshots/$name.png")
        val copiedSize = command("stat -c %s /data/local/tmp/flip7-screenshots/$name.png").toLongOrNull()
        check(copiedSize == file.length()) { "Capture $name non copiée : taille $copiedSize au lieu de ${file.length()}" }
    }
    @Test fun cardsCorrectionsRestoreAndBrutalMode() {
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("start-game").fetchSemanticsNodes().isNotEmpty() }
        capture("native-setup")
        ui.onNodeWithTag("name-0").performTextReplacement("Alice")
        ui.onNodeWithTag("name-1").performTextReplacement("Bob")
        tap("start-game", "setup-list")
        tap("player-0")
        for(id in listOf("10", "11", "minus4", "half")) tap("card-$id")
        score(0, 8)
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("score-0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        score(0, 8)
        capture("native-cards")
        tap("finish-round")
        tap("edit-0")
        tap("player-0")
        tap("card-half")
        score(0, 17)
        tap("finish-round")
        tap("edit-0")
        tap("player-0")
        tap("card-1")
        tap("cancel-edit")
        ui.onNodeWithTag("game-list").performScrollToNode(hasTestTag("total-0"))
        ui.onNodeWithTag("total-0", useUnmergedTree = true).assertTextEquals("17 pts")
        capture("native-history")
        ui.onNodeWithTag("game-list").performScrollToIndex(0)
        ui.onNodeWithText("Nouvelle partie").performClick()
        ui.onNodeWithText("Confirmer").performClick()
        tap("mode-brutal", "setup-list")
        tap("start-game", "setup-list")
        tap("player-0")
        for(id in listOf("1", "2", "3", "4", "5", "6", "7")) tap("card-$id")
        tap("bonus-target-1")
        score(0, 28)
        score(1, -15)
        tap("bust")
        tap("card-minus10")
        score(0, -10)
        score(1, 0)
        capture("native-brutal")
    }
}
