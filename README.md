# Flip 7 · Le compteur

Application web progressive en français pour téléphone et tablette, installable sur l’écran d’accueil et utilisable hors connexion après un premier chargement réussi (indicateur « Prêt hors connexion »).

## Utilisation

1. Ajouter les joueurs, choisir l’objectif (200 par défaut) et éventuellement le mode Sans pitié.
2. À la fin de chaque manche, toucher un joueur puis sélectionner ses cartes finales. Retoucher une carte pour la retirer. Le score et le bonus sont calculés immédiatement.
3. Les joueurs sans sélection marquent 0, sans validation individuelle.
4. Terminer la manche. Depuis l’historique, modifier les cartes et enregistrer ou annuler la correction.

Le calcul suit le livret français p.6 et le choix de l’utilisateur : somme des numéros, retrait des malus, minimum zéro en mode normal, division par deux arrondie à l’inférieur, puis bonus Flip 7 de 15 points. Le zéro annule la somme sans Flip 7 ; le 13 porte-bonheur autorise deux 13 qui comptent pour le bonus. Le 7 porte-malheur remplace un 7 ordinaire dans la saisie des cartes finales. Les actions de vol, échange et défausse sont résolues autour de la table avant la saisie.

Mode Sans pitié : scores négatifs, malus conservés sur un joueur ayant sauté, et choix de retirer 15 points à un adversaire à la place du bonus.

Les anciennes parties sont converties en conservant leurs scores. Les données restent locales au navigateur ; aucune synchronisation entre appareils.

## Installation

- Android / Chrome : bouton Installer ou menu → Installer l’application.
- iPhone / iPad / Safari : Partager → Sur l’écran d’accueil → Ajouter.
- Attendre « Prêt hors connexion » avant de couper Internet.

## Développement

Aucun build ni dépendance de production. Lancer `python3 -m http.server 4175 --directory dist`, puis ouvrir http://localhost:4175. Vérifier les calculs avec `node tests/scoring.mjs`.

Le service worker précharge tous les fichiers locaux et les icônes PNG. Aucune ressource externe n’est nécessaire. Incrémenter son nom de cache lors d’une nouvelle publication pour remplacer l’application de manière cohérente.

Règles : https://flip-7.com/flip7-vengeance/ . Compteur indépendant, sans affiliation avec les éditeurs.

Visuels des cartes provenant des ressources officielles de Catch Up Games : https://flip-7.com/flip7-vengeance/ . Ils sont embarqués localement et préchargés pour le mode hors connexion.
