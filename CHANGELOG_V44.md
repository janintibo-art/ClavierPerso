# Anarchie Clavier — changements v44

## Dictionnaire français reconstruit : 129 900 mots

Le dictionnaire n'a pas seulement grossi, il a été **reconstruit** pour corriger
des formes fausses produites par l'ancienne conjugaison automatique.

### Conjugaison enfin correcte
L'ancien générateur ignorait les alternances du français et produisait des
formes comme « gelents » ou « gelaits ». Les règles sont désormais appliquées :

| Règle | Exemple |
| --- | --- |
| e → è devant syllabe muette | geler → **gèle**, lever → **lève**, acheter → **achète** |
| é → è devant syllabe muette | céder → **cède**, préférer → **préfère**, répéter → **répète** |
| doublement de consonne | appeler → **appelle**, jeter → **jette** |
| c → ç devant a/o | commencer → **commençons** |
| g → ge devant a/o | manger → **mangeons** |
| y → i devant e muet | payer → **paie** |

Les verbes à doublement sont traités par liste fermée, cette règle n'étant pas
prédictible en français.

### Vocabulaire spécialisé
830 termes ajoutés dans dix domaines : santé, cuisine, informatique, travail,
administratif, maison, transport, loisirs, nature, gentilés.

### Séparation stricte des mots sûrs
Le dictionnaire est ordonné : **26 774 mots vérifiés** (vocabulaire courant,
domaines, infinitifs, pluriels) puis les formes conjuguées. La limite était
auparavant mal placée à 60 000, en plein milieu des formes générées.

## Dictionnaire anglais : 307 → 2 652 mots

Génération morphologique sur les verbes (avec les irréguliers : go/went/gone,
take/took/taken…), noms avec pluriels, adjectifs avec comparatifs et adverbes.

## Classement recalibré

- La pénalité de rang suivait un dictionnaire de 21 000 mots : avec 130 000,
  elle écrasait les mots spécialisés derrière des mots courants sans rapport.
  Diviseur porté de 45 à 170.
- Les mots longs sont moins pénalisés dans les complétions.
- Quand ce qui est tapé est le début d'un mot existant, les corrections passent
  après les complétions : « ratat » propose désormais **ratatouille**.

Mesures : 10/12 en complétion de mots spécialisés (contre 8/12), 10/10 en
correction orthographique.

## Compatibilité

- `versionCode 44`, `versionName 44.0`
