# ⛪ Wise Smart Church

Système de régie TV et diffusion Live pour l'église.

## Structure du projet

```
wisesmartchurch/
├── app-display/          ← APK pour Smart TV / Android Box / Projecteur
│   └── src/main/
│       ├── java/.../
│       │   ├── ui/display/TvDisplayActivity.java
│       │   ├── service/EliteNetworkService.java
│       │   ├── network/          (NSD, WebSocket client, Bible DL)
│       │   ├── model/Models.java
│       │   └── util/LocaleHelper.java
│       ├── res/layout/activity_tv_display.xml
│       └── AndroidManifest.xml
│
├── app-regie/            ← APK pour téléphone/tablette du régisseur
│   └── src/main/
│       ├── java/.../
│       │   ├── ui/control/ControlActivity.java
│       │   ├── service/EliteNetworkService.java
│       │   ├── network/          (NSD, WebSocket serveur, Bible DL)
│       │   ├── model/Models.java
│       │   └── util/LocaleHelper.java
│       ├── res/layout/
│       ├── assets/index.html    (interface web de régie)
│       └── AndroidManifest.xml
│
├── .github/workflows/build.yml  ← CI/CD : 2 APKs séparés
├── build.gradle
└── settings.gradle
```

## APKs générés

| APK | Cible | Installer sur |
|-----|-------|---------------|
| `WSC-Display-arm64.apk` | Android Box 64-bit | Smart TV, projecteur |
| `WSC-Display-armv7.apk` | Android Box 32-bit | Anciens boîtiers |
| `WSC-Regie-universal.apk` | Téléphone/Tablette | Console du régisseur |

## Workflow CI/CD

- **Push/PR** → build debug des 2 APKs séparément  
- **Tag `v*.*.*`** → build release signé + GitHub Release avec les 3 APKs
