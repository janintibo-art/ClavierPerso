# Anarchie Clavier — changements v39

## Palette des réglages plus chaleureuse

Le rouge vif employé sur tous les boutons rendait l'écran agressif. La palette
est désormais centralisée dans un objet `Palette` unique et repose sur des
teintes terre plus douces.

| Élément | Avant | Après |
| --- | --- | --- |
| Boutons principaux | `#C81D25` rouge vif | `#A8443C` terre cuite |
| Accent / sélection | `#C81D25` | `#B4453C` |
| Choix non sélectionné | `#9AA0A6` gris froid | `#EFE8E4` pastille claire, texte brun |
| Fond général | `#F2F0F0` gris | `#FAF6F3` ivoire |
| Texte principal | `#202124` quasi noir | `#2E2724` brun foncé |
| Texte secondaire | `#5F6368` gris froid | `#6E625C` gris chaud |
| Validation | `#0F9D58` vert vif | `#4E7A5B` vert sauge |
| Suppression | `#B00020` | `#9B3A34` |
| Pastilles de navigation | `#2B2525` presque noir | `#5A4A44` brun chaud |

## Lisibilité

- Les options non sélectionnées passent d'un gris foncé à une pastille claire
  à texte brun : la différence avec l'option active est immédiate, sans reposer
  uniquement sur la teinte.
- Tous les couples texte/fond ont été mesurés et atteignent au minimum le
  niveau AA (4,5:1), les textes courants étant en AAA.
- Nouvelle fonction `styleChoice` : l'apparence d'un bouton de choix est décrite
  à un seul endroit, fond et couleur de texte compris.

## Compatibilité

- `versionCode 39`, `versionName 39.0`
- Aucun changement dans les thèmes du clavier lui-même, ni dans les préférences
