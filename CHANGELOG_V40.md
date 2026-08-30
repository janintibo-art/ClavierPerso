# Anarchie Clavier — changements v40

## Menu de variantes : deux façons de choisir

Le menu introduit en v38 exigeait de garder le doigt appuyé et de glisser.
Si l'utilisateur levait le doigt sans bouger, la première variante était insérée
sans qu'il ait vu les autres.

- **Glisser puis relâcher** : on parcourt les choix, celui qui est survolé est
  mis en évidence, et il est inséré au relâchement.
- **Lever le doigt sans bouger** : le menu **reste ouvert** avec l'indication
  « Touche ton choix ». On touche ensuite tranquillement la variante voulue.
- Un appui en dehors du menu l'annule sans rien écrire.

## Barre d'outils

- La correction IA `✅` est de nouveau visible en permanence, à côté de `🤖`
  et `🌍`, y compris en barre compacte.
- Elle n'apparaît plus en double dans le menu `⋯`.

## Réglages

- Le numéro de version est affiché en haut de l'écran, pour vérifier d'un coup
  d'œil quelle version est réellement installée.

## Compatibilité

- `versionCode 40`, `versionName 40.0`
- Aucun changement de format de préférences ni d'indice de thème

## Correction : le menu était masqué par la bulle d'aperçu

La bulle qui agrandit la lettre pressée et le menu de variantes occupent
exactement la même zone, juste au-dessus de la touche. La bulle étant dessinée
après le menu, elle le recouvrait entièrement : le menu existait mais restait
invisible, et l'accent semblait choisi au hasard.

- Le menu de variantes est désormais dessiné en dernier, au-dessus de tout.
- La bulle d'aperçu est masquée tant qu'un menu est ouvert.
- Les effets de frappe sont également suspendus pendant l'affichage du menu.
