# Anarchie Clavier — changements v45

## Prédiction sur deux mots, sans rien avoir appris

Jusqu'ici la prédiction par trigramme ne fonctionnait qu'avec les habitudes
personnelles : elle ne servait à rien avant plusieurs semaines d'usage.

- Nouveau fichier `lm3_fr.txt` : 101 suites de deux mots parmi les plus
  courantes du français, disponibles dès l'installation.
- « je suis » propose **en, désolé, au** · « il faut » propose **que** ·
  « on se » propose **voit, rappelle, retrouve** · « je te » propose
  **rappelle, laisse, remercie**.
- Les habitudes personnelles restent prioritaires ; le modèle complète.

## Modèle de langue élargi

De 290 à 385 entrées, construites sur un corpus de phrases réelles couvrant
les messages courants, les formules de politesse et le registre professionnel.

## Verbes irréguliers complets

282 formes ajoutées au bloc vérifié : être, avoir, aller, faire, pouvoir,
vouloir, devoir, savoir, voir, venir, prendre, dire, mettre, lire, écrire,
croire, boire, partir, sortir, dormir, courir, ouvrir, attendre, comprendre,
connaître, répondre, entendre, vendre, perdre, recevoir, suivre, vivre, rire,
tenir, falloir, pleuvoir, valoir — à tous les temps, subjonctif compris.

Les 32 formes testées sont présentes et classées comme vérifiées.

## Dictionnaire espagnol : 247 → 3 287 mots

Conjugaison des trois groupes (-ar, -er, -ir), verbes irréguliers, noms avec
pluriels, adjectifs accordés et adverbes en -mente.

## Bilan des dictionnaires

| Fichier | Entrées | Taille |
| --- | --- | --- |
| dict_fr.txt | 130 155 | 1 517 Ko |
| dict_es.txt | 3 287 | 28 Ko |
| dict_en.txt | 2 652 | 19 Ko |
| lm_fr.txt | 385 | 8 Ko |
| lm3_fr.txt | 101 | 2 Ko |

## Compatibilité

- `versionCode 45`, `versionName 45.0`
