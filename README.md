# Ⓐ Anarchie Clavier

Clavier Android entièrement personnalisable (AZERTY / QWERTY), sans publicité.

## Nouveautés v36
- **Stabilité IA / traduction renforcée** : une réponse réseau arrivée en retard ne peut plus écrire dans un autre champ de saisie.
- **Mode privé renforcé** : respecte aussi la demande Android de ne pas personnaliser l’apprentissage, bloque l’apprentissage, l’historique et les outils réseau dans les champs privés.
- **Frappe instantanée réellement active** : les lettres partent dès le contact quand l’option est activée.
- **Apprentissage plus fluide** : les écritures du modèle personnel sont déportées hors du thread de frappe.
- **Clés API protégées** avec Android Keystore et exclues des sauvegardes exportées.
- **Presse-papiers contrôlable** : activation/désactivation, expiration automatique et effacement des éléments non épinglés.
- **Barre d’outils compacte** avec menu `⋯` pour garder les fonctions sans surcharger le clavier.
- **Navigation rapide des réglages** : Apparence, Frappe, Intelligence, Outils et Confidentialité.
- **6 nouveaux thèmes** : Charbon, Béton, Terminal, Verre rouge, Industriel et Papier noir.
- **Recherche GIF fiabilisée** : les anciennes recherches ne peuvent plus écraser la plus récente et la taille des téléchargements est limitée.


## Écriture
- Suggestions de mots **avec correcteur d'orthographe** (dictionnaire de 11 000 mots)
- **Clavier intelligent** : il compte les mots que tu écris et te les propose dès la 1re ou 2e lettre
- **Amorçage du vocabulaire** : apprend d'un coup depuis tes SMS envoyés, tes contacts, un texte collé ou le dictionnaire Android
- **Correction automatique** sur espace (annulable d'un retour arrière), majuscule auto, double espace = point
- **Prédiction du mot suivant** : après un mot, il propose ce que tu écris habituellement ensuite
- Rangée de chiffres au-dessus des lettres (option)
- Accents par appui long (é, à, ù, ç, ñ…)
- Double appui sur ⇧ = verrouillage majuscule
- **Glisser sur la barre Espace = déplacer le curseur**
- **Raccourcis texte** : `slt` + espace → "Salut, ça va ?"
- **Calculatrice** : tape `12*45` puis `=` → `540`

## Outils (barre du haut)
- Barre compacte par défaut : 😀 Émojis, 📋 Coller, 🤖 IA, 🌍 Traduction et `⋯` Outils
- Le menu `⋯` donne accès aux GIF, à la correction IA, à la navigation/édition, au changement de langue et aux réglages
- GIF avec recherche (Giphy ou Klipy)
- 📋 Coller — **appui long : historique du presse-papiers avec épinglage 📌**
- ✅ **Correction IA** : corrige d'un appui les fautes d'orthographe, grammaire, conjugaison et accents
- 🌍 **Mode traduction** : choisis une langue, écris, appuie sur ➜, le message part traduit (16 langues)
- ⏎ **appui long : reformulation IA** (plus poli, pro, drôle, court, romantique, correction)
- La barre complète historique reste disponible dans les réglages

## Confort de frappe
- **Frappe instantanée** : la lettre part dès le contact du doigt
- **Sensibilité réglable** (délais d'appui long, seuil de glissement)
- **Tolérance de zone** : rattrape les appuis entre deux touches
- **Effets à la frappe** : Couleur, Onde, Zoom, Éclat, Étincelles (durée et couleur réglables)

## Assistance à l'écriture
- **Suggestions d'emojis** pendant la frappe (« amour » → ❤️)
- **Détection des codes SMS** : le code reçu est proposé dans la barre
- **Pavé de navigation** (appui long sur ?123) : flèches, annuler/rétablir, copier/couper/coller
- **Changement de langue automatique** selon ce que tu écris
- **Mode privé** : aucun apprentissage, historique presse-papiers ou outil réseau dans les champs privés ; respecte aussi `IME_FLAG_NO_PERSONALIZED_LEARNING`
- **Emojis récents et recherche** par mot-clé

## Assistant IA 🤖
- 10 modes (recherche Google, mot d'excuse, email pro, résumé, idées…)
- **Suivi** après une réponse : « Plus court », « En anglais », « Autre version »
- **Compléter ma phrase** : appui long sur 🤖
- ⌫ annule et restaure ta demande

## Personnalisation
- 26 thèmes prédéfinis, dont les nouveaux presets Charbon, Béton, Terminal, Verre rouge, Industriel et Papier noir
- Couleur libre pour chaque élément (RVB + hexadécimal)
- **Couleur et luminosité touche par touche**
- **Mode RGB animé** : Vague, Respiration, Réactif à la frappe, Cascade (vitesse et intensité réglables)
- Luminosité globale des touches
- **Image d'arrière-plan** avec assombrissement, luminosité, saturation et flou
- **8 polices** au choix pour les touches
- **7 sons de frappe** (clic, mécanique, machine à écrire, bulle, néon…) avec volume réglable
- **Thème différent par application** (sombre pour la nuit, sobre pour le travail)
- **Sauvegarde et restauration** des réglages et données choisies ; les clés API sont toujours exclues des exports
- Opacité des touches, hauteur, taille du texte
- Bulle d'aperçu au-dessus de la touche, vibration, son
- **Aperçu en direct** dans les réglages

## Confidentialité et services en ligne
- Historique du presse-papiers activable/désactivable avec expiration 1 h / 24 h / 7 jours / jamais
- Les services publics de secours pour l’IA/traduction peuvent être désactivés
- Le fournisseur réellement utilisé est indiqué après une opération réseau
- En mode privé, les fonctions réseau sont bloquées

## Clés API (réglages → « Clés IA et services »)
- **Clé IA** (OpenAI, Groq, Mistral, DeepSeek, OpenRouter…) : traduction, correction ✅ et reformulation
- **DeepL / Google Traduction** : optionnelles
- **Giphy ou Klipy** : nécessaire pour les GIF
- Les clés sont stockées via **Android Keystore**, masquées dans les réglages et exclues des sauvegardes exportées
- Chaque section a un bouton **🧪 Tester** qui indique précisément ce qui fonctionne

## Compilation (GitHub Actions)
À chaque `git push`, l'APK est compilé automatiquement :
Onglet **Actions** → dernier workflow **Build APK** → artifact **ClavierPerso-APK**.

## Installation
1. Ouvrir **Anarchie Clavier**
2. ① Activer le clavier
3. ② Choisir comme clavier par défaut
