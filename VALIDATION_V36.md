# Validation v36

## Contrôles effectués

- Vérification de la structure du projet et des fichiers Kotlin/XML modifiés.
- Contrôle XML du manifeste et des ressources ajoutées.
- Recherche des anciens appels RenderScript : supprimés.
- Recherche du doublon `learnWord` signalé dans la v35 : corrigé.
- Vérification de l'utilisation de `instantKey` dans la gestion tactile du clavier.
- Vérification de la prise en compte de `IME_FLAG_NO_PERSONALIZED_LEARNING`.
- Vérification que les clés API ne sont plus exportées dans les sauvegardes v36.
- Test Kotlin pur de la calculatrice : multiplication, priorités, parenthèses, virgule française, division Unicode, nombre négatif, division par zéro et entrée non-expression.

## À valider sur l'appareil / CI Android

Le build Android complet n'est pas exécutable dans cet environnement faute de SDK Android local. Le projet est prévu pour être replacé dans le dépôt existant et validé par son build habituel.

Tests pratiques recommandés après compilation :

1. Activer/désactiver « Frappe instantanée » et vérifier lettre simple, accent long, swipe et suppression.
2. Lancer une traduction, changer immédiatement de champ/application et confirmer que la réponse tardive n'écrit nulle part ailleurs.
3. Faire le même test avec IA, reformulation et complétion.
4. Ouvrir un champ mot de passe/no-personalized-learning et vérifier l'indication `🛡 Mode privé` ainsi que le blocage IA/GIF/traduction/historique.
5. Tester l'expiration et l'épinglage du presse-papiers.
6. Vérifier la migration des anciennes clés API et le bouton de test de chaque service.
7. Tester la barre compacte et le menu `⋯` sur petit et grand écran.
8. Tester plusieurs recherches GIF rapides successives.
9. Importer une ancienne sauvegarde v35 puis exporter une sauvegarde v36 et vérifier qu'aucune clé API n'apparaît dans le JSON.
