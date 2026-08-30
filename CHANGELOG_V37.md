# Anarchie Clavier — changements v37

Cette version corrige trois points relevés lors de la relecture de la v36.

## Mode privé : apprentissage et actions volontaires séparés

Le mode privé bloquait toutes les fonctions en ligne. Or refuser une traduction
que l'utilisateur demande lui-même est une gêne, alors que ne rien mémoriser
reste indispensable.

- Le mode privé continue de **toujours** bloquer : apprentissage des mots,
  enchaînements, suggestions et historique du presse-papiers.
- L'IA, la traduction, la correction et les GIF restent **disponibles** :
  ce sont des actions déclenchées volontairement, et rien n'en est mémorisé.
- Nouvelle option « En mode privé, bloquer aussi IA, traduction et GIF »
  pour retrouver le comportement strict de la v36 (désactivée par défaut).

## Frappe instantanée : deux régressions corrigées

- Le glissement du doigt vers une touche voisine corrige de nouveau la lettre.
  Auparavant la lettre envoyée au contact restait, ce qui empêchait de rattraper
  une visée approximative.
- Les effets visuels de frappe (flash et onde) sont de nouveau rendus.

## Performance

- La clé GIF, chiffrée via Keystore, est déchiffrée hors du thread principal.

## Compatibilité

- `versionCode 37`, `versionName 37.0`
- `minSdk 24` et `targetSdk 34` inchangés
- Aucun changement d'indice de thème ni de format de préférences
