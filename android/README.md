# Application Android native — 0.1.1

Première version Android de **Flip 7 · Le compteur**, développée en Kotlin avec Jetpack Compose. Interface, calculs et stockage sont natifs. Les 22 cartes sont incluses dans l’APK et l’application fonctionne hors connexion dès son installation.

## Fonctionnalités

- Création de partie de 1 à 18 joueurs, objectif configurable et mode Sans pitié.
- Sélection tactile des cartes finales et score immédiat ; joueurs non saisis à zéro.
- Bonus Flip 7, zéro, 13 porte-bonheur, malus et ÷2 selon l’ordre du livret français.
- Classement, égalités, victoire, revanche et historique.
- Correction d’une manche avec annulation, sans perdre la saisie de la manche courante.
- Annulation de la dernière manche avec restauration de ses cartes.
- Sauvegarde native avec **DataStore**, y compris les corrections en cours et le joueur sélectionné.
- Mise en page adaptable, marges système et commandes accessibles Android.

Aucune permission réseau, publicité, télémétrie ou connexion à un compte n’est demandée. Les données restent dans le stockage interne de l’application. La sauvegarde Android dans le cloud est désactivée. Les parties de la PWA ne sont pas importées automatiquement.

## Compiler

Ouvrir ce dossier dans Android Studio, avec **JDK 17** et le SDK Android **36** installés. Le wrapper Gradle 8.13 est inclus, avec vérification SHA-256 de sa distribution et de son JAR lors de sa préparation.

```sh
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK installable de test : `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Le minimum pris en charge est Android 8.0 (API 26). La cible est Android 16 (API 36). L’identifiant applicatif est `io.github.cdesaintleger.flip7compteur`.

## GitHub Actions

Le workflow **Android natif** compile le projet, lance les tests unitaires et lint, puis fournit :

- **flip7-android-debug** : APK signé avec une clé de débogage, pour essais.
- **flip7-android-release-unsigned** : Android App Bundle non signé, pour préparer la distribution.
- Les rapports de test, de lint et les captures du parcours sur émulateur Android.

Les artefacts se téléchargent depuis l’exécution du workflow. L’émulateur API 35 teste la saisie, la correction, la recréation de l’activité et le mode Sans pitié. Les 96 fixtures dans `app/src/test/resources/scoring-fixtures.json` proviennent du moteur de la version web ; elles vérifient la cohérence de la version Kotlin.

## Structure

```text
app/src/main/java/io/github/cdesaintleger/flip7compteur/
  Game.kt            Modèles immuables, règles et transitions de partie
  GameStore.kt       Sérialisation et sauvegarde DataStore
  MainActivity.kt    Interface Jetpack Compose et navigation
  Cards.kt           Ressources et libellés accessibles des cartes
app/src/main/res/    Cartes complètes, icône et thème
app/src/test/        Tests des règles, corrections, sauvegardes et parité web
app/src/androidTest/ Parcours Android instrumenté et captures
```

## Préparer le Google Play Store

```sh
./gradlew bundleRelease
```

Le bundle généré n’est **pas encore prêt à être envoyé au Play Store** : il reste à configurer la signature de publication et Play App Signing avec la clé de ton compte, puis la fiche et les déclarations Play Console. Ne pas utiliser l’APK de débogage pour la publication. Les clés et mots de passe ne doivent pas être ajoutés au dépôt.

Le nom et les illustrations du jeu restent la propriété de leurs ayants droit. L’autorisation de les distribuer dans une application Play Store doit être confirmée avant publication. La provenance des illustrations est documentée dans le README principal.

### Interface 0.1.1

L’interface native reprend les champs compacts, avatars, panneaux crème, boutons et proportions de la webapp. Le logo de l’en-tête est exporté depuis sa composition HTML/CSS, avec transparence et marge. L’icône adaptative Android réserve une marge de 20 % pour conserver le visuel entier dans les masques du lanceur.
