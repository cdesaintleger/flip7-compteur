package io.github.cdesaintleger.flip7compteur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Ink = Color(0xFF252770)
private val Paper = Color(0xFFFAF0CE)
private val Yellow = Color(0xFFF6D84F)
private val Teal = Color(0xFF2097AC)
private val Muted = Color(0xFF625D71)
private val playerColors = listOf(Yellow, Color(0xFF6DC6C6), Color(0xFFEF8F79), Color(0xFFBCA4D2), Color(0xFFADD46D), Color(0xFF90B7DD))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Ink.toArgb()), navigationBarStyle = SystemBarStyle.dark(Ink.toArgb()))
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Paper, secondary = Yellow, surface = Paper, onSurface = Ink, background = Ink)) {
                FlipApp(viewModel())
            }
        }
    }
}

@Composable
private fun FlipApp(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var help by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
    BackHandler(enabled = state?.selected != null) { vm.update { it.copy(selected = null) } }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Teal, Ink))).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FLIP 7", color = Yellow, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 24.sp)
                    Text("VENGEANCE · LE COMPTEUR", color = Paper, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                TextButton(onClick = { help = true }, modifier = Modifier.testTag("help")) { Text("Aide", color = Paper) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                state?.let { s ->
                    if (s.game == null) SetupScreen(s.setup, vm)
                    else GameScreen(s, vm, onConfirm = { confirm = it })
                } ?: Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (message == null) CircularProgressIndicator(color = Yellow)
                    else Text(message.orEmpty(), color = Paper)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Touchez. Comptez. Jouez.") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Sélectionnez les cartes finales de chaque joueur. Aucune sélection = 0 point. Une seule validation termine la manche.") }
            item { Text("Calcul du livret français : numéros − malus, minimum zéro en mode normal, puis ÷2 arrondi à l’inférieur, et enfin bonus Flip 7 de 15 points.") }
            item { Text("Le zéro impose 0 sans Flip 7. Le 13 porte-bonheur autorise deux 13 qui comptent tous deux pour le bonus. Résolvez les actions autour de la table avant la saisie.") }
            item { Text("Sans pitié : scores négatifs et malus sur un joueur ayant sauté. À sept cartes, choisissez +15 points ou −15 pour un adversaire.") }
            item { Text("La sauvegarde est automatique sur cet appareil. Vous pouvez quitter l’application, tourner l’écran et corriger une manche depuis l’historique. Tout fonctionne sans Internet, dès l’installation.") }
            item { Text("Compteur indépendant, sans affiliation avec les éditeurs. Les illustrations des cartes appartiennent à leurs ayants droit.", fontSize = 12.sp) }
        }
    }, confirmButton = { TextButton(onClick = { help = false }) { Text("Compris") } })
    confirm?.let { action ->
        val title = when(action) { "undo" -> "Annuler la dernière manche ?"; "rematch" -> "Prêts pour la revanche ?"; else -> "Nouvelle partie ?" }
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(title) }, text = {
            Text(when(action) { "undo" -> "Ses cartes remplaceront la saisie en cours et ses points seront retirés du classement."; "rematch" -> "Les mêmes joueurs repartiront de zéro."; else -> "La partie actuelle sera remplacée après confirmation." })
        }, dismissButton = { TextButton(onClick = { confirm = null }) { Text("Retour") } }, confirmButton = {
            TextButton(onClick = {
                vm.update { s ->
                    val g = requireNotNull(s.game)
                    when(action) {
                        "undo" -> s.copy(game = g.undo(), selected = null)
                        "rematch" -> s.copy(game = Game(g.players, g.target, g.brutal), selected = null)
                        else -> AppState(Setup(g.players, g.target.toString(), g.brutal))
                    }
                }
                confirm = null
            }) { Text("Confirmer") }
        })
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = Paper, contentColor = Ink, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Ink), shadowElevation = 4.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
private fun Heading(text: String) { Text(text, fontSize = 21.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic) }
@Composable
private fun Hint(text: String) { Text(text, fontSize = 12.sp, color = Muted) }
@Composable
private fun Primary(text: String, tag: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag(tag), shape = RoundedCornerShape(5.dp), border = BorderStroke(1.dp, Ink), colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
@Composable
private fun Avatar(name: String, index: Int) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(5.dp)).background(playerColors[index % playerColors.size]), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { (index + 1).toString() }, fontWeight = FontWeight.Black, color = Ink)
    }
}

@Composable
private fun SetupScreen(setup: Setup, vm: GameViewModel) {
    LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding().testTag("setup-list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Nouvelle partie", color = Paper, fontWeight = FontWeight.Bold, fontSize = 25.sp) }
        item {
            Panel {
                Heading("AUTOUR DE LA TABLE")
                setup.names.forEachIndexed { index, name ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Avatar(name, index)
                        OutlinedTextField(name, { value -> vm.update { s -> s.copy(setup = s.setup.copy(names = s.setup.names.mapIndexed { i, n -> if(i == index) value.take(24) else n })) } }, Modifier.weight(1f).testTag("name-$index"), label = { Text("Joueur ${index + 1}") }, singleLine = true)
                        TextButton(onClick = { vm.update { s -> s.copy(setup = s.setup.copy(names = s.setup.names.filterIndexed { i, _ -> i != index })) } }, enabled = setup.names.size > 1, modifier = Modifier.sizeIn(minWidth = 48.dp).semantics { contentDescription = "Retirer le joueur ${index + 1}" }) { Text("×", fontSize = 22.sp) }
                    }
                }
                OutlinedButton(onClick = { vm.update { s -> require(s.setup.names.size < 18); s.copy(setup = s.setup.copy(names = s.setup.names + "")) } }, enabled = setup.names.size < 18, modifier = Modifier.fillMaxWidth()) { Text("+ Ajouter un joueur") }
                OutlinedTextField(setup.target, { value -> vm.update { it.copy(setup = it.setup.copy(target = value.filter(Char::isDigit).take(4))) } }, Modifier.fillMaxWidth().testTag("target"), label = { Text("Objectif · points") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(Modifier.fillMaxWidth().toggleable(setup.brutal, role = Role.Checkbox, onValueChange = { value -> vm.update { it.copy(setup = it.setup.copy(brutal = value)) } }).testTag("mode-brutal").padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(setup.brutal, onCheckedChange = null)
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("Mode Sans pitié", fontWeight = FontWeight.Bold)
                        Hint("Scores négatifs, malus après avoir sauté et vengeance −15.")
                    }
                }
                Primary("C’est parti", "start-game") { vm.update(AppState::start) }
                Hint("Sauvegarde locale automatique · Hors connexion dès l’installation")
            }
        }
    }
}

@Composable
private fun GameScreen(s: AppState, vm: GameViewModel, onConfirm: (String) -> Unit) {
    val g = requireNotNull(s.game)
    val points = Scoring.round(g.hands(), g.brutal)
    val winners = g.winners()
    val ended = winners.size == 1
    LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("game-list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if(g.brutal) "VENGEANCE · SANS PITIÉ" else "VENGEANCE", color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Objectif ${g.target} · ${g.rounds.size} manche(s)", color = Paper, fontSize = 12.sp)
                }
                TextButton(onClick = { onConfirm("new") }) { Text("Nouvelle partie", color = Paper) }
            }
        }
        if(winners.isNotEmpty()) item {
            Panel {
                Heading(if(ended) "${g.players[winners.single()]} gagne !" else "Égalité en tête !")
                Text(if(ended) "${g.totals()[winners.single()]} points. Une revanche ?" else "Jouez une nouvelle manche pour vous départager.")
                if(ended) Primary("Rejouer", "rematch") { onConfirm("rematch") }
            }
        }
        if(!ended || g.editing != null) {
            item {
                Panel {
                    Heading(if(g.editing == null) "MANCHE ${g.rounds.size + 1}" else "MODIFIER LA MANCHE ${g.editing + 1}")
                    Hint("Touchez un joueur, puis ses cartes. Aucune carte = 0 point.")
                }
            }
            itemsIndexed(g.players, key = { index, _ -> "player-$index" }) { index, name ->
                Panel {
                    val hand = g.hands()[index]
                    val detail = Scoring.details(hand, g.brutal)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(if(s.selected == index) Color(0xFFFAE49B) else Paper).clickable(role = Role.Button) { vm.update { it.copy(selected = if(it.selected == index) null else index) } }.padding(9.dp).testTag("player-$index"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(name, index)
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Hint(when { hand.bust -> "Sauté"; detail.flip -> "Flip 7"; hand.cards.isEmpty() -> "Aucune carte"; else -> "${detail.count} carte(s) Numéro" })
                        }
                        Text("${points[index]} pts", Modifier.testTag("score-$index"), fontWeight = FontWeight.Black, fontSize = 23.sp)
                        Text(if(s.selected == index) "−" else "+")
                    }
                    if(s.selected == index) CardPicker(g, index, vm)
                }
            }
            item {
                Panel {
                    Primary(if(g.editing == null) "Terminer la manche" else "Enregistrer la correction", "finish-round") { vm.update { it.copy(game = requireNotNull(it.game).finish(), selected = null) } }
                    if(g.editing != null) TextButton(onClick = { vm.update { it.copy(game = requireNotNull(it.game).cancelEdit(), selected = null) } }, modifier = Modifier.testTag("cancel-edit")) { Text("Annuler la correction") }
                    Hint("Pas de validation individuelle. Les sélections sont sauvegardées au fur et à mesure.")
                }
            }
        }
        item {
            Panel {
                Heading("LE CLASSEMENT")
                val totals = g.totals()
                g.players.indices.sortedByDescending { totals[it] }.forEachIndexed { rank, index ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${rank + 1}", color = Muted)
                        Avatar(g.players[index], index)
                        Text(g.players[index], Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("${totals[index]} pts", Modifier.testTag("total-$index"), fontWeight = FontWeight.Black, fontSize = 21.sp)
                    }
                }
            }
        }
        item { Panel {
            Heading("LE FIL DE LA PARTIE")
            if(g.rounds.isEmpty()) Hint("La première manche ouvre le bal.")
            else if(g.editing == null) TextButton(onClick = { onConfirm("undo") }) { Text("Annuler la dernière manche") }
        } }
        itemsIndexed(g.rounds.reversed(), key = { index, _ -> "history-${g.rounds.lastIndex - index}" }) { index, round ->
            val roundIndex = g.rounds.lastIndex - index
            Panel {
                Text("Manche ${roundIndex + 1}", fontWeight = FontWeight.Bold)
                Text(Scoring.round(round, g.brutal).mapIndexed { i, score -> "${g.players[i]} : $score" }.joinToString(" · "), fontSize = 13.sp)
                TextButton(onClick = { vm.update { it.copy(game = requireNotNull(it.game).edit(roundIndex), selected = null) } }, modifier = Modifier.testTag("edit-$roundIndex")) { Text("Modifier") }
            }
        }
        item { Text("Sauvegardé sur cet appareil · Hors connexion", color = Paper, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}

@Composable
private fun CardPicker(g: Game, index: Int, vm: GameViewModel) {
    val hand = g.hands()[index]
    val details = Scoring.details(hand, g.brutal)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Cartes Numéro ${details.count}/7", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        TextButton(onClick = { vm.changeHand { Hand() } }) { Text("Tout effacer", fontSize = 12.sp) }
    }
    BoxWithConstraints {
        val columns = if(maxWidth < 260.dp) 4 else if(maxWidth >= 500.dp) 7 else 5
        CardGrid(numberIds, hand, columns, vm)
    }
    CardGrid(specialIds, hand, 2, vm, special = true)
    Hint("Sélectionnez les cartes présentes en fin de manche. Le 7 porte-malheur remplace le 7 ordinaire.")
    Text("Cartes Malus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    BoxWithConstraints { CardGrid(penaltyIds, hand, if(maxWidth >= 500.dp) 6 else 3, vm) }
    Row(Modifier.fillMaxWidth().toggleable(hand.bust, role = Role.Checkbox, onValueChange = { checked -> vm.changeHand { it.copy(bust = checked, target = null) } }).testTag("bust").padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(hand.bust, onCheckedChange = null)
        Text("Sauté", Modifier.padding(start = 8.dp).weight(1f), fontWeight = FontWeight.Bold)
        Hint(if(g.brutal) "Malus conservés" else "0 point")
    }
    if(g.brutal && details.flip) {
        Text("Bonus Flip 7 : gagner ou se venger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        BonusOption("Gagner 15 points", hand.target == null, "bonus-self") { vm.changeHand { it.copy(target = null) } }
        g.players.forEachIndexed { target, name -> if(target != index) {
            BonusOption("Retirer 15 à $name", hand.target == target, "bonus-target-$target") { vm.changeHand { it.copy(target = target) } }
        } }
    }
    Text(when {
        hand.bust -> "Sauté${if(g.brutal) " · malus conservés" else ""} → ${details.points} points"
        "0" in hand.cards && !details.flip -> "Zéro sans Flip 7 → ${details.points} points"
        else -> "${details.sum} (numéros) − ${details.penalty} (malus)${if("half" in hand.cards) " → ÷2 arrondi inférieur" else ""}${if(details.bonus > 0) " + 15 (Flip 7)" else ""} = ${details.points} points"
    }, Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("live-calculation"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BonusOption(text: String, checked: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(checked, role = Role.RadioButton, onClick = onClick).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(checked, onClick = null)
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun CardGrid(ids: List<String>, hand: Hand, columns: Int, vm: GameViewModel, special: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ids.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { id ->
                    val picked = id in hand.cards
                    Surface(Modifier.weight(1f).selectable(picked, role = Role.Checkbox, onClick = { vm.changeHand { Scoring.toggle(it, id) } }).semantics { contentDescription = cardLabel(id) }.testTag("card-$id"), shape = RoundedCornerShape(5.dp), color = if(picked) Yellow else Color.Transparent, border = if(picked) BorderStroke(3.dp, Teal) else null) {
                        Box(Modifier.padding(if(picked) 3.dp else 0.dp).then(if(special) Modifier.height(144.dp) else Modifier.aspectRatio(304f / 422f))) {
                            Image(painterResource(cardResource(id)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            if(picked) Text("✓", Modifier.align(Alignment.BottomEnd).background(Ink, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp), color = Yellow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
