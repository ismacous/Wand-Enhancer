# DayByDay

Application Android privée pour noter chaque journée d'une couleur, tenir un
journal, y attacher des photos / vidéos, et suivre son évolution semaine par
semaine, mois par mois et année par année.

Tout reste sur le téléphone : **l'application ne demande aucune permission
Internet**, elle est donc techniquement incapable d'envoyer quoi que ce soit
sur un réseau.

## Fonctionnalités

- **Calendrier mensuel** : chaque jour se colorie en vert, orange, rouge ou noir.
  Une pastille indique la présence d'un texte, une autre celle de médias.
- **Note de la semaine** : à droite de chaque ligne du calendrier, une case
  colorée résume la semaine (moyenne des jours notés) avec son numéro.
- **Bilan du mois** : moyenne sur 3, répartition des couleurs, nombre de jours notés.
- **Vue année** : les 12 mois en miniature, chacun avec sa moyenne ; on tape sur
  un mois pour l'ouvrir, sur un jour pour l'éditer.
- **Journal du jour** : titre + texte libre, sauvegarde automatique, navigation
  jour par jour.
- **Photos et vidéos** : ajoutées depuis le sélecteur Android (aucune permission
  d'accès à la galerie n'est nécessaire), copiées dans l'espace privé de l'app,
  visibles en plein écran avec lecture vidéo intégrée.
- **Statistiques** : moyenne annuelle, mois par mois, meilleure semaine et
  semaine la plus dure, séries de jours notés.
- **Verrouillage** : code à 4–8 chiffres (stocké haché en PBKDF2, jamais en
  clair) + empreinte / reconnaissance faciale, re-verrouillage après 15 s en
  arrière-plan, blocage des captures d'écran.
- **Sauvegarde** : export `.zip` complet (journées + notes + médias) et
  restauration, vers l'emplacement de ton choix.
- **Résumé annuel en texte** : export d'une année entière (titres, notes,
  statistiques) pour préparer la vidéo de fin d'année.

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

L'APK est signé avec la clé de debug : c'est suffisant pour un usage personnel,
mais une mise à jour ne s'installera par-dessus que si elle vient du même
workflow (même clé).

## Construire localement

Nécessite le SDK Android (plateforme 35) et un JDK 17 :

```bash
cd DayByDay
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Où sont les données

- Base de données : `/data/data/com.ismael.daybyday/databases/daybyday.db`
- Médias : `/data/data/com.ismael.daybyday/files/media/<année>/<mois>/`

Ces dossiers sont privés à l'application (sandbox Android + chiffrement du
stockage du téléphone) et invisibles pour la galerie et les autres applis.
La sauvegarde cloud automatique d'Android est désactivée (`allowBackup=false`).

**Désinstaller l'application efface toutes les données.** Fais un export `.zip`
régulièrement.
