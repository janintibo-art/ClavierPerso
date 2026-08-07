# Ⓐ Anarchie Clavier

Clavier Android entièrement personnalisable (AZERTY / QWERTY), sans publicité.

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
- 😀 Émojis (8 catégories)
- GIF avec recherche (Giphy ou Klipy — l'API Tenor a fermé le 30/06/2026)
- 📋 Coller — **appui long : historique du presse-papiers avec épinglage 📌**
- ✅ **Correction IA** : corrige d'un appui toutes les fautes d'orthographe, grammaire, conjugaison et accents
- 🌍 **Mode traduction** : choisis une langue, écris, appuie sur ➜, le message part traduit (16 langues)
- ⏎ **appui long : reformulation IA** (plus poli, pro, drôle, court, romantique, correction)
- ⚙️ Réglages
- 🌐 Changement de langue (Français / English / Español)

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
- **Mode privé** : rien n'est appris dans les champs mot de passe
- **Emojis récents et recherche** par mot-clé

## Assistant IA 🤖
- 10 modes (recherche Google, mot d'excuse, email pro, résumé, idées…)
- **Suivi** après une réponse : « Plus court », « En anglais », « Autre version »
- **Compléter ma phrase** : appui long sur 🤖
- ⌫ annule et restaure ta demande

## Personnalisation
- 20 thèmes prédéfinis
- Couleur libre pour chaque élément (RVB + hexadécimal)
- **Couleur et luminosité touche par touche**
- **Mode RGB animé** : Vague, Respiration, Réactif à la frappe, Cascade (vitesse et intensité réglables)
- Luminosité globale des touches
- **Image d'arrière-plan** avec assombrissement, luminosité, saturation et flou
- **8 polices** au choix pour les touches
- **7 sons de frappe** (clic, mécanique, machine à écrire, bulle, néon…) avec volume réglable
- **Thème différent par application** (sombre pour la nuit, sobre pour le travail)
- **Sauvegarde et restauration** complète de tous les réglages
- Opacité des touches, hauteur, taille du texte
- Bulle d'aperçu au-dessus de la touche, vibration, son
- **Aperçu en direct** dans les réglages

## Clés API (réglages → « Clés IA et services »)
- **Clé IA** (OpenAI, Groq, Mistral, DeepSeek, OpenRouter…) : traduction, correction ✅ et reformulation
- **DeepL / Google Traduction** : optionnelles
- **Giphy ou Klipy** : nécessaire pour les GIF
- Chaque section a un bouton **🧪 Tester** qui indique précisément ce qui fonctionne

## Compilation (GitHub Actions)
À chaque `git push`, l'APK est compilé automatiquement :
Onglet **Actions** → dernier workflow **Build APK** → artifact **ClavierPerso-APK**.

## Installation
1. Ouvrir **Anarchie Clavier**
2. ① Activer le clavier
3. ② Choisir comme clavier par défaut
