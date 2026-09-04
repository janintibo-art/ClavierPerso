# Anarchie Clavier — changements v43

## Le panneau GIF ne masque plus la touche Entrée

Le panneau s'affiche au-dessus du clavier, mais sa hauteur était calquée sur
celle du clavier : l'ensemble dépassait l'écran et la rangée du bas, donc la
touche Entrée, devenait inaccessible. Impossible de valider une recherche.

- La hauteur du panneau est désormais bornée par la place réellement
  disponible, en garantissant que le clavier reste entier.
- Vignettes et barre de catégories légèrement resserrées pour voir plus de GIF
  dans moins de place.
- Hauteur des panneaux réglable (35 à 80 % du clavier, 62 % par défaut).

## Le clavier prend moins de place

- **Barre de suggestions** réglable de 26 à 48 dp (42 auparavant, 34 par défaut).
- **Barre d'outils** réglable de 26 à 48 dp (40 auparavant, 34 par défaut).
- **Rangée du bas** réglable de 60 à 100 % de la hauteur des autres rangées
  (88 % par défaut) : la barre d'espace n'a pas besoin d'être aussi haute
  qu'une rangée de lettres.
- Marges internes resserrées.

Sur un clavier à cinq rangées, les valeurs par défaut font gagner environ 21 dp.

## Bouton « Réglage compact »

Un bouton applique d'un coup un profil resserré (barres à 30 dp, rangée du bas
à 82 %, touches à 50 dp, espacement réduit, panneaux à 55 %) : environ 16 % de
hauteur en moins, utile dans les applications où le clavier gêne.

## Compatibilité

- `versionCode 43`, `versionName 43.0`
