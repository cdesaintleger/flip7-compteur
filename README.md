# Flip 7 · Le compteur

Application web statique en français pour téléphone et tablette. Les scores finaux sont saisis par manche ; les malus et les effets des cartes sont résolus autour de la table. Le bouton Flip 7 ajoute ou retire le bonus de 15 points au score saisi.

- 1 à 18 joueurs, objectif configurable (200 par défaut).
- Classement, égalités, victoire, corrections et annulation de manche.
- Sauvegarde locale au navigateur ; aucune synchronisation entre appareils.
- Service worker pour une réouverture hors connexion après un premier chargement réussi.

Lancer `python3 -m http.server 4175 --directory dist`, puis ouvrir http://localhost:4175.

Les fichiers à héberger sont dans `dist/`. Aucun build ni dépendance requis.

Les règles de référence : https://flip-7.com/flip7-vengeance/ . Compteur indépendant, sans affiliation avec les éditeurs.
