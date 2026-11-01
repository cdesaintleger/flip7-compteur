package io.github.cdesaintleger.flip7compteur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.draw.drawWithCache
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
    Box(Modifier.fillMaxSize().drawWithCache { val gradient = Brush.radialGradient(listOf(Color(0xFF28A7B9), Color(0xFF147B9C), Ink), center = Offset(size.width / 2, size.height * .13f), radius = size.height * .9f); onDrawBehind { drawRect(gradient) } }.safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.brand_web), "Flip 7 · Vengeance · Scores", Modifier.width(152.dp).height(58.dp), contentScale = ContentScale.Fit)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(33.dp)) {
                    Box(Modifier.offset(2.dp, 3.dp).size(33.dp).background(Ink, RoundedCornerShape(5.dp)))
                    Box(Modifier.size(33.dp).background(Paper, RoundedCornerShape(5.dp)).border(1.5.dp, Ink, RoundedCornerShape(5.dp)).clickable { help = true }.testTag("help").semantics { contentDescription = "Aide" }, contentAlignment = Alignment.Center) {
                        Text("?", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                    }
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = Yellow)
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
    Box(modifier.padding(end = 5.dp, bottom = 6.dp)) {
        Box(Modifier.matchParentSize().offset(5.dp, 6.dp).background(Color(0x99171951), RoundedCornerShape(7.dp)))
        Surface(color = Paper, contentColor = Ink, shape = RoundedCornerShape(7.dp), border = BorderStroke(1.5.dp, Ink)) {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
        Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(4.dp).border(.5.dp, Ink).background(Brush.horizontalGradient(listOf(Yellow, Yellow, Color(0xFFEF685A), Teal, Yellow))))
    }
}
@Composable
private fun Heading(text: String) { Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-.7).sp) }
@Composable
private fun Hint(text: String) { Text(text, fontSize = 10.sp, lineHeight = 16.sp, color = Muted) }
@Composable
private fun Primary(text: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(end = 3.dp, bottom = 4.dp)) {
        Box(Modifier.matchParentSize().offset(3.dp, 4.dp).background(Ink, RoundedCornerShape(5.dp)))
        Row(Modifier.fillMaxWidth().heightIn(min = 55.dp).background(Yellow, RoundedCornerShape(5.dp)).border(1.5.dp, Ink, RoundedCornerShape(5.dp)).clickable(role = Role.Button, onClick = onClick).testTag(tag).padding(horizontal = 22.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), fontWeight = FontWeight.Black, color = Ink)
            Text("↗", color = Ink, fontSize = 20.sp)
        }
    }
}
@Composable
private fun Avatar(name: String, index: Int) {
    Box(Modifier.size(38.dp).rotate(-4f)) {
        Box(Modifier.matchParentSize().offset(2.dp, 2.dp).background(Ink, RoundedCornerShape(5.dp)))
        Box(Modifier.fillMaxSize().background(playerColors[index % playerColors.size], RoundedCornerShape(5.dp)).border(1.dp, Ink, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
            Text(name.trim().take(1).uppercase().ifEmpty { (index + 1).toString() }, fontWeight = FontWeight.Black, color = Ink)
        }
    }
}
@Composable
private fun WebField(value: String, placeholder: String, modifier: Modifier, numeric: Boolean = false, onChange: (String) -> Unit) {
    BasicTextField(value, onChange, modifier.height(48.dp).background(Color(0xFFFFF8E3), RoundedCornerShape(4.dp)).border(1.dp, Ink, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp), singleLine = true,
        textStyle = TextStyle(color = Ink, fontSize = 16.sp), keyboardOptions = KeyboardOptions(keyboardType = if(numeric) KeyboardType.Number else KeyboardType.Text),
        decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = if(numeric) Alignment.Center else Alignment.CenterStart) { if(value.isEmpty()) Text(placeholder, color = Muted, fontSize = 16.sp); inner() } })
}
@Composable
private fun Footer() {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HorizontalDivider(color = Color(0xFF81C1C1), thickness = .5.dp)
        Text("●  Sauvegardé sur cet appareil · Hors connexion", color = Paper, fontSize = 10.sp)
    }
}
@Composable
private fun SetupScreen(setup: Setup, vm: GameViewModel) {
    LazyColumn(Modifier.widthIn(max = 660.dp).fillMaxWidth().imePadding().testTag("setup-list"), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Nouvelle partie", color = Paper, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 26.sp)
            Text("Installez-vous, on compte les points.", color = Paper, fontSize = 12.sp)
        } }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AUTOUR DE LA TABLE", Modifier.weight(1f), fontSize = 21.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-.7).sp)
                    Text("VENGEANCE", Modifier.rotate(-3f).background(Ink, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 7.dp), color = Paper, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
                Text("Qui tente sa chance aujourd’hui ?", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                setup.names.forEachIndexed { index, name ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(name, index)
                        WebField(name, "Joueur ${index + 1}", Modifier.weight(1f).testTag("name-$index")) { value -> vm.update { s -> s.copy(setup = s.setup.copy(names = s.setup.names.mapIndexed { i, n -> if(i == index) value.take(24) else n })) } }
                        Box(Modifier.size(28.dp, 48.dp).clickable(enabled = setup.names.size > 1, role = Role.Button) { vm.update { s -> s.copy(setup = s.setup.copy(names = s.setup.names.filterIndexed { i, _ -> i != index })) } }.semantics { contentDescription = "Retirer le joueur ${index + 1}" }, contentAlignment = Alignment.Center) { Text("×", fontSize = 22.sp) }
                    }
                }
                Box(Modifier.fillMaxWidth().height(46.dp).background(Color(0xFFF7E5AA), RoundedCornerShape(4.dp)).clickable(enabled = setup.names.size < 18, role = Role.Button) { vm.update { s -> s.copy(setup = s.setup.copy(names = s.setup.names + "")) } }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize()) { drawRoundRect(Ink, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))) }
                    Text("+ Ajouter un joueur", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 8.dp), color = Color(0xFFB7AB85), thickness = .5.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Objectif de la partie · points", Modifier.weight(1f), fontSize = 12.sp)
                    WebField(setup.target, "200", Modifier.width(83.dp).testTag("target"), numeric = true) { value -> vm.update { it.copy(setup = it.setup.copy(target = value.filter(Char::isDigit).take(4))) } }
                }
                Column(Modifier.fillMaxWidth().toggleable(setup.brutal, role = Role.Checkbox, onValueChange = { value -> vm.update { it.copy(setup = it.setup.copy(brutal = value)) } }).testTag("mode-brutal")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(setup.brutal, onCheckedChange = null, modifier = Modifier.size(26.dp))
                        Text("Mode Sans pitié", Modifier.padding(start = 8.dp), fontSize = 13.sp)
                    }
                    Text("Scores négatifs, malus après avoir sauté. Un Flip 7 donne +15 ou retire 15 à un adversaire.", Modifier.padding(start = 26.dp, top = 8.dp, bottom = 10.dp), color = Muted, fontSize = 10.sp, lineHeight = 18.sp)
                }
                Primary("C’est parti", "start-game") { vm.update(AppState::start) }
                Hint("Un appareil pour toute la table. Sauvegarde automatique et jeu hors connexion.")
            }
        }
        item { Footer() }
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
                    g.players.forEachIndexed { index, name ->
                    val hand = g.hands()[index]
                    val detail = Scoring.details(hand, g.brutal)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(if(s.selected == index) Color(0xFFFAE49B) else Paper).clickable(role = Role.Button) { vm.update { it.copy(selected = if(it.selected == index) null else index) } }.border(if(s.selected == index) 1.5.dp else 1.dp, if(s.selected == index) Ink else Color(0xFF9993AA), RoundedCornerShape(5.dp)).padding(12.dp).testTag("player-$index"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(name, index)
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Hint(when { hand.bust -> "Sauté"; detail.flip -> "Flip 7"; hand.cards.isEmpty() -> "Aucune carte"; else -> "${detail.count} carte(s) Numéro" })
                        }
                        Text("${points[index]} pts", Modifier.testTag("score-$index"), fontWeight = FontWeight.Black, fontSize = 23.sp)
                        Text(if(s.selected == index) "−" else "+")
                    }
                    if(s.selected == index) Surface(color = Color(0xFFF4E6BC), shape = RoundedCornerShape(5.dp), border = BorderStroke(1.dp, Ink)) {
                        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { CardPicker(g, index, vm) }
                    }
                    }
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
        item { Footer() }
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
