# DayByDay

Application Android privée pour noter chaque journée d'une couleur, tenir un
journal, y attacher des photos / vidéos, et suivre son évolution semaine par
semaine, mois par mois et année par année.

Tout reste sur le téléphone : **l'application ne demande aucune permission
Internet**, elle est donc techniquement incapable d'envoyer quoi que ce soit
sur un réseau.

## Fonctionnalités

- **Accueil** : la journée du jour se colorie en un tap, sans ouvrir d'écran.
- **Calendrier mensuel** : chaque jour se colorie en vert, orange, rouge ou noir.
  Des pastilles indiquent la présence d'un texte, de médias, ou d'un suivi rempli.
- **Note de la semaine** : à droite de chaque ligne du calendrier, une case
  colorée résume la semaine (moyenne des jours notés) avec son numéro.
- **Vue année** : les 12 mois en miniature, chacun avec sa moyenne.
- **Journal du jour** : titre + texte libre, sauvegarde automatique, navigation
  jour par jour.
- **Suivi quotidien** : sport (rien / un peu / vraie séance), alimentation
  (compliquée / correcte / bien mangé), sorti ou non, et poids optionnel.
- **Étiquettes** personnalisables (créées depuis le jour ou les réglages).
- **Photos et vidéos** ajoutées depuis le sélecteur Android, copiées dans
  l'espace privé de l'app, visibles en plein écran avec lecture vidéo.
- **Recherche** dans tous les titres et toutes les notes.
- **Bilan** : moyennes par mois et par an, meilleure semaine, semaine la plus
  dure, séries de jours notés, courbe de poids + IMC, et surtout
  **« Ce qui va avec tes bonnes journées »** : comparaison de l'humeur moyenne
  selon le sport, les repas, les sorties et chaque étiquette.
- **Rappel quotidien** : une notification à l'heure choisie, uniquement si la
  journée n'est pas encore notée.
- **Sauvegarde automatique quotidienne** dans un dossier choisi, en remplaçant
  le fichier précédent (pas d'accumulation).
- **Verrouillage** : code à 4–8 chiffres (stocké haché en PBKDF2, jamais en
  clair) + empreinte / reconnaissance faciale, re-verrouillage après 15 s en
  arrière-plan, blocage des captures d'écran.
- **Sauvegarde manuelle** : export `.zip` complet et restauration.
- **Résumé annuel en texte** : export d'une année entière (titres, notes,
  détails, statistiques) pour préparer la vidéo de fin d'année.

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
  (version 2 ; la migration depuis la version 1 est testée sur émulateur)
- Médias : `/data/data/com.ismael.daybyday/files/media/<année>/<mois>/`

Ces dossiers sont privés à l'application (sandbox Android + chiffrement du
stockage du téléphone) et invisibles pour la galerie et les autres applis.
La sauvegarde cloud automatique d'Android est désactivée (`allowBackup=false`).

**Désinstaller l'application efface toutes les données.** Fais un export `.zip`
régulièrement.
