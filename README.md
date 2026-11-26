# Pomodoro

Une application Android simple de gestion du temps basée sur la méthode
Pomodoro : alterner des sessions de concentration et des pauses courtes, avec
une pause longue toutes les N sessions.

## Fonctionnalités

- Minuteur 25 / 5 / 15 minutes par défaut, entièrement configurable.
- Anneau de progression et compte à rebours en gros caractères.
- Démarrage, pause, réinitialisation et passage à la phase suivante.
- Enchaînement automatique des phases (option).
- Compteur de pomodoros et pastilles de progression dans le cycle.
- Notification, bip et vibration à la fin de chaque phase, désactivables
  séparément.
- Décompte permanent dans la barre d'état pendant une session, avec actions
  pause / réinitialiser / passer.
- Widget du bureau et widget d'écran verrouillé (selon le launcher), avec
  démarrage, pause, réinitialisation et passage de phase.
- Minuteur fiable en arrière-plan : service de premier plan et alarme exacte.
- Application Wear OS synchronisée avec le téléphone.
- Écran maintenu allumé pendant une session.
- Thème clair / sombre PabloSystems, interface en français et police monospace.

## Design

L'interface reprend le thème PabloSystems partagé par les outils PabloVTM :
sauge profonde `#4B5A50`, ivoire chaud `#F1EEE3` et vermillon vif `#F20B0B`,
police système monospace et invite `pablo@systems:~/pomodoro` en titre.

## Stack technique

- Kotlin 2.0
- Jetpack Compose + Material 3
- `ViewModel` + `StateFlow` + coroutines
- `SharedPreferences` pour les réglages et l'état du minuteur
- `AlarmManager`, service de premier plan et `AppWidgetProvider`
- `play-services-wearable` pour la synchronisation téléphone / montre
- Compose for Wear OS pour l'application montre

## Structure du projet

```
app/src/main/java/com/pva/pomodoro/
├── MainActivity.kt                 # Point d'entrée, thème edge-to-edge
├── PomodoroViewModel.kt            # Expose l'état du moteur à Compose
├── data/SettingsRepository.kt      # Persistance des réglages
├── model/                          # Phases, réglages, cycle, état partagé
├── system/                         # Notifications, son et vibration de fin de phase
├── timer/                          # Moteur, service, alarmes, receivers
├── ui/                             # Écran principal, réglages, thème
├── widget/                         # Widgets bureau et écran verrouillé
└── sync/                           # Publication d'état vers la montre

wear/src/main/java/com/pva/pomodoro/wear/
├── MainActivity.kt                 # Point d'entrée Wear OS
├── alarm/                          # Alerte de fin de phase (vibration / bip)
├── notif/                          # Activité en cours (minuteur visible en veille)
├── sync/WearRepository.kt          # Réception d'état et envoi des commandes
├── model/                          # État reçu du téléphone
└── ui/                             # Interface ronde, ambiante et écran de veille
```

## Widgets

- **Bureau** : ajout par appui long sur l'écran d'accueil, ou via
  « Épingler le widget » dans les réglages de l'application.
- **Écran verrouillé** : à ajouter depuis la personnalisation de l'écran de
  verrouillage. La disponibilité dépend de la version d'Android et du launcher.

Les deux widgets lisent l'état persisté et se rafraîchissent à chaque action,
à chaque changement d'état et au démarrage de l'application. Le décompte est
affiché par un `Chronometer`, sans réveil périodique.

## Montre (Wear OS)

L'application montre affiche le même minuteur, synchronisé avec le téléphone.
Le téléphone reste la source de vérité : il publie la phase, l'heure de fin de
session et le compteur via Wearable Data Layer. La montre calcule le décompte
localement et envoie les commandes démarrer / pause / réinitialiser / passer.

Les deux horloges sont recalées par un aller-retour de messages : la montre
envoie son heure, le téléphone répond avec la sienne, et l'écart est appliqué à
l'heure de fin. Le décompte reste ainsi aligné à moins d'une seconde, même si
un message arrive en retard.

La montre est aussi l'appareil qui alerte lorsque la session a été lancée
depuis son écran : elle vibre et bipe à la fin de la phase, et le téléphone
reste silencieux. Une session lancée depuis le téléphone (interface, widget ou
notification) est annoncée par le téléphone. Les réglages « Son » et
« Vibration » s'appliquent aux deux appareils : couper le son rend aussi la
montre silencieuse et la montre vibre uniquement si la vibration est activée.
L'alerte de la montre est programmée par une alarme exacte : elle fonctionne
même écran éteint ou application en arrière-plan.

L'interface de la montre place les commandes réinitialiser et passer en petits
boutons-icônes de part et d'autre du minuteur, afin de rester lisibles sur
l'écran rond.

Quand la montre passe en veille, elle reste sur un écran ambiant minimal
(fond noir, compteur mm:ss, phase et statut, sans boutons) au lieu de revenir
au cadran. L'écran est rafraîchi toutes les 15 secondes, en complément du
rafraîchissement système (environ une fois par minute).

Prérequis :

- même identifiant `com.pva.pomodoro` installé sur les deux appareils ;
- Google Play services sur le téléphone (ou un bac à sable Play services) et sur la montre ;
- montre et téléphone connectés en Bluetooth ou sur le même réseau Wi-Fi.

```bash
./gradlew :wear:assembleDebug
adb -s <montre> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

La montre doit d'abord être appairée en ADB sans fil depuis ses options
développeur, puis :

```bash
adb pair <ip>:<port-appairage> <code>
adb connect <ip>:<port-debogage>
```

Sans Play services sur le téléphone, l'application téléphone continue de
fonctionner, mais la montre reste sur « En attente du téléphone ».

## Compiler et installer

Prérequis : JDK 17 et le SDK Android (API 35).

```bash
export JAVA_HOME=<chemin-vers-le-JDK-17>
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ou ouvrir le dossier dans Android Studio et lancer la configuration `app`.

## Tests

```bash
./gradlew testDebugUnitTest lintDebug
```

Les tests unitaires couvrent les règles d'enchaînement des phases et la
conversion des durées.

## Limites connues

- L'alarme exacte et le service de premier plan maintiennent le décompte en
  arrière-plan. Certains constructeurs peuvent malgré tout retarder les
  alarmes si l'application est exclue de l'optimisation de batterie.
- Le widget d'écran verrouillé n'est proposé que si le launcher expose les
  widgets d'écran verrouillé.
- La synchronisation de la montre nécessite Google Play services sur le
  téléphone ; elle est donc indisponible sur un profil sans Play
  services sandboxés.
- Une session lancée depuis la montre n'est annoncée que par celle-ci : si la
  montre est éteinte, déchargée ou déconnectée, le téléphone reste silencieux.
- La notification persistante ne peut pas être supprimée manuellement pendant
  une session : c'est une exigence d'Android pour les services de premier plan.
  Le réglage « Notifications » coupe les alertes et le service permanent.