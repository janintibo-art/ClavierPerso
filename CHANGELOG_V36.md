# Anarchie Clavier — changements v36

Cette version est une passe de **fiabilisation, confidentialité, fluidité et finition visuelle** de la v35. Elle conserve les fonctions existantes et ne traite pas les sujets liés à la publication sur une boutique.

## Corrections importantes

- Protection de session pour les opérations IA, traduction et reformulation : une réponse réseau tardive est ignorée si l'utilisateur a changé de champ de saisie.
- Suppression du double apprentissage d'un mot lors du choix d'une suggestion.
- Implémentation réelle de la frappe instantanée : les touches alphabétiques peuvent être envoyées dès `ACTION_DOWN` tout en conservant les appuis longs et le swipe.
- Respect de `IME_FLAG_NO_PERSONALIZED_LEARNING` dans le mode privé.
- Le mode privé bloque l'apprentissage, l'historique du presse-papiers, l'IA, la traduction et les GIF.
- Lecture du dernier code SMS déplacée hors du thread principal du clavier.
- Écritures du dictionnaire personnel et des trigrammes déplacées sur un exécuteur mono-thread afin de ne pas bloquer la frappe.
- Annulation/invalidation des opérations en ligne lors d'un changement de champ ou de la destruction du service.

## IA, traduction et confidentialité

- Les clés IA, DeepL, Google Traduction et GIF sont maintenant stockées via Android Keystore (AES/GCM).
- Migration automatique des anciennes clés v35 qui étaient dans les préférences classiques.
- Les clés API sont toujours exclues des exports JSON v36.
- Les anciennes sauvegardes v35 contenant des clés peuvent être importées : les clés sont migrées directement vers le stockage sécurisé.
- Les préférences de clés sécurisées sont exclues des sauvegardes système Android.
- Ajout d'une option pour désactiver les fournisseurs publics de secours.
- Le diagnostic de traduction respecte cette option et ne contacte pas les fournisseurs publics lorsqu'ils sont désactivés.
- Le fournisseur utilisé est mémorisé et affiché après une traduction ou une réponse IA.

## Presse-papiers

- Interrupteur pour activer/désactiver l'historique.
- Expiration automatique des éléments non épinglés : 1 heure, 24 heures, 7 jours ou jamais.
- Les éléments épinglés sont conservés.
- Bouton pour effacer les éléments non épinglés.
- Aucun nouvel élément n'est enregistré en mode privé.

## GIF

- Les recherches sont numérotées : une ancienne réponse ne peut plus écraser la recherche la plus récente.
- Pool de téléchargement limité au lieu de créer un thread sans limite pour chaque image.
- Limite de taille pour les aperçus et les GIF complets afin d'éviter une consommation mémoire excessive.
- Les résultats sont ignorés si le panneau a été fermé.

## Interface et graphisme

- Accent principal des réglages harmonisé vers un rouge plus cohérent avec l'identité Anarchie.
- Nouveaux réglages de confidentialité regroupés visuellement.
- Navigation rapide en 5 familles : Apparence, Frappe, Intelligence, Outils et Confidentialité.
- Champs de clés API masqués et exclus de l'autoremplissage sur Android compatible.
- Nouvelle barre d'outils compacte : Emoji, Coller, IA, Traduction, `⋯`.
- Nouveau panneau `⋯` pour GIF, correction IA, navigation/édition, langue et réglages.
- La barre complète reste disponible via une option.
- Indication visuelle `🛡 Mode privé` directement dans la zone de suggestions.
- 6 nouveaux thèmes ajoutés sans changer les indices des 20 anciens thèmes : Charbon, Béton, Terminal, Verre rouge, Industriel, Papier noir.
- Couleurs par défaut d'une nouvelle installation orientées noir / rouge / ivoire.

## Modernisation

- Suppression de l'utilisation de RenderScript pour le flou d'arrière-plan.
- Remplacement par un flou léger basé sur réduction/rééchantillonnage, compatible avec le projet sans dépendance supplémentaire.
- Ajout d'une première suite de tests unitaires pour la calculatrice.

## Compatibilité

- `versionCode 36`
- `versionName 36.0`
- `minSdk 24` inchangé
- `targetSdk 34` inchangé
- Les 20 thèmes historiques restent dans le même ordre afin de ne pas casser les thèmes associés par application.
