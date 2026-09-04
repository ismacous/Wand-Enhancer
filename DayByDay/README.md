# DayByDay

Application Android privée pour noter chaque journée d'une couleur, tenir un
journal, y attacher des photos / vidéos, et suivre son évolution semaine par
semaine, mois par mois et année par année.

Tout reste sur le téléphone : **l'application ne demande aucune permission
Internet**, elle est donc techniquement incapable d'envoyer quoi que ce soit
sur un réseau.

## Fonctionnalités

**Noter ses journées**
- **Accueil** : la journée du jour se colorie en un tap, sans ouvrir d'écran.
- **Calendrier mensuel** : chaque jour en vert, orange, rouge ou noir. Des
  pastilles signalent un texte, des médias, ou un suivi rempli.
- **Moments de la journée** : matin, après-midi, soir et nuit se notent
  séparément ; la couleur du jour est la moyenne des moments, sauf si elle est
  choisie à la main.
- **Note de la semaine** : à droite de chaque ligne du calendrier, une case
  colorée résume la semaine avec son numéro.
- **Vue année** : les 12 mois en miniature, chacun avec sa moyenne.
- **Journal du jour** : titre + texte libre, sauvegarde automatique.
- **Photos et vidéos** copiées dans l'espace privé de l'app, visibles en plein
  écran avec lecture vidéo.
- **Recherche** dans tous les titres et toutes les notes.

**Se suivre**
- **Suivi quotidien** : sport, alimentation, sorti ou non, poids optionnel.
- **Étiquettes** personnalisables, rangées par famille (sommeil, social,
  activité, alimentation, travail, écrans, santé, autre).
- **Pas et temps d'écran** lus localement (Health Connect et statistiques
  d'usage Android), avec autorisation explicite et sans aucun réseau.
- **Argent** : solde courant, rentrées et dépenses par mois, catégories, et
  correction de solde enregistrée comme un ajustement.

**Comprendre**
- **Bilan** : moyennes par mois et par an, meilleure semaine, semaine la plus
  dure, séries de jours notés, courbe de poids et IMC, moyenne par moment de
  la journée.
- **« Ce qui va avec tes bonnes journées »** : humeur moyenne les jours avec un
  facteur contre les jours sans, pour le sport, les repas, les sorties, les
  pas, le temps d'écran et chaque étiquette — affiché seulement quand les deux
  groupes ont assez de journées.

**Garder ses données**
- **Rappel quotidien** à l'heure choisie, seulement si la journée n'est pas
  encore notée.
- **Sauvegarde automatique quotidienne** dans un dossier choisi, en remplaçant
  le fichier précédent.
- **Reprise de sauvegarde** : au premier lancement sur une app vide, DayByDay
  propose de chercher la dernière sauvegarde et annonce sa date avant de
  restaurer.
- **Sauvegarde manuelle** `.zip` et **résumé annuel en texte** pour la vidéo de
  fin d'année.
- **Verrouillage** : code haché en PBKDF2, empreinte / reconnaissance, blocage
  des captures d'écran actif tant qu'aucun code n'est défini.

## Échelle des couleurs

| Couleur | Sens | Score |
|---|---|---|
| Vert | Bonne journée | 3 |
| Orange | Journée mitigée | 2 |
| Rouge | Journée difficile | 1 |
| Noir | Journée très noire | 0 |

Les moyennes affichées (`x.x / 3`) ne comptent que les jours effectivement notés.

## Installer l'APK sur le téléphone

1. Ouvrir l'onglet **Actions** du dépôt GitHub, workflow **DayByDay Android APK**.
2. Ouvrir le dernier run vert et télécharger l'artefact `DayByDay-apk`.
3. Décompresser le `.zip` et ouvrir `DayByDay.apk` depuis le téléphone.
4. Android demandera d'autoriser l'installation depuis cette source : accepter.

L'APK est signé avec la clé fixe du dépôt (`DayByDay/keystore/daybyday.jks`),
donc chaque nouvelle version s'installe par-dessus la précédente **sans effacer
les données**. Cette clé est volontairement en clair : l'application n'est pas
distribuée et ne demande aucune permission sensible.

> La toute première version (1.0) était signée avec une clé aléatoire générée
> par le runner. Pour passer de la 1.0 à la 1.1 il faut donc exporter une
> sauvegarde, désinstaller, réinstaller, puis restaurer. Les mises à jour
> suivantes s'installent normalement.

## Construire localement

Nécessite le SDK Android (plateforme 35) et un JDK 17 :

```bash
cd DayByDay
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Où sont les données

- Base de données : `/data/data/com.ismael.daybyday/databases/daybyday.db`
  (version 4 ; les migrations depuis la version 1 sont testées sur émulateur)
- Médias : `/data/data/com.ismael.daybyday/files/media/<année>/<mois>/`

Ces dossiers sont privés à l'application (sandbox Android + chiffrement du
stockage du téléphone) et invisibles pour la galerie et les autres applis.
La sauvegarde cloud automatique d'Android est désactivée (`allowBackup=false`).

**Désinstaller l'application efface toutes les données.** Fais un export `.zip`
régulièrement.
