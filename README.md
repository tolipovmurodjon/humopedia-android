<div align="center">

<img src="docs/assets/humo.png" width="112" alt="Humo, the Humopedia mascot">

# Humopedia for Android

**Learn maths and science like a game.** Lessons you play with your hands, not videos you watch.

[![build](https://github.com/tolipovmurodjon/humopedia-android/actions/workflows/build.yml/badge.svg)](https://github.com/tolipovmurodjon/humopedia-android/actions/workflows/build.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-3DC98C?logo=jetpackcompose&logoColor=white)
![minSdk 26](https://img.shields.io/badge/minSdk-26-23A06C)
![targetSdk 36](https://img.shields.io/badge/targetSdk-36-23A06C)

[humopedia.com](https://humopedia.com) · [Brand](https://design.humopedia.com) · [Google Play](https://play.google.com/store/apps/details?id=com.mcompany.humo)

</div>

---

## What this is

The Android app for Humopedia, a learning platform for children in Uzbekistan aged 10 to 12. Every lesson is a
small simulation the child plays with: a balance scale for equations, squares you cut and move for Pythagoras.

This repository is a **fresh start** (October 2026). Nothing is carried over from the old app; it is published
under the same Play listing, package `com.mcompany.humo`.

## Status

| Version | What is inside | State |
|---|---|---|
| **2.0.0** | The Humopedia web app full-screen in a Trusted Web Activity: every lesson, quiz and game | In progress |
| 3.x | Native Compose screens, replacing the web shell one screen at a time | Planned |

## Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Build | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`) |
| Backend | Firebase project `humo-uz`, shared with the web app |
| CI | GitHub Actions: unit tests and a debug build on every pull request |

## Run it

Requires Android Studio (latest stable) and JDK 17+.

```bash
git clone git@github.com:tolipovmurodjon/humopedia-android.git
cd humopedia-android
./gradlew installDebug        # builds and installs on a connected phone
```

Debug builds need nothing else. **Release builds** need the upload key: create `keystore.properties` in the
project root (it is ignored by git, never commit it):

```properties
storeFile=/path/to/humopedia_keystore.jks
storePassword=…
keyAlias=humopedia_key
keyPassword=…
```

```bash
./gradlew bundleRelease       # app/build/outputs/bundle/release/app-release.aab
```

## Project layout

```
app/
  src/main/java/com/mcompany/humo/   app code
  src/main/res/                      resources
  src/test/                          unit tests (JVM)
  src/androidTest/                   device tests
gradle/libs.versions.toml            every dependency version, in one place
.github/workflows/build.yml          CI
```

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md): branches, commits, pull requests and how a release goes out.

---

<div align="center">
<sub>© 2026 HUMOPEDIA EDU MChJ · Tashkent · All rights reserved.</sub>
</div>
