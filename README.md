<p align="center">
  <img src="icon.png" width="160" alt="Santa Game icon">
</p>

# Santa Game

A Christmas game made with Kotlin and libGDX.

Help Santa catch the falling gifts before they hit the snow, and stay away from the coal. You have three lives: dropping a gift or catching coal costs one.

## Controls

- Touch and drag to move Santa
- Arrow keys or A/D also work when a keyboard is connected
- Back returns to the menu

## Project structure

- `core` — the game itself (screens, Santa, falling items, scenery)
- `android` — Android launcher, manifest and icons
- `assets` — game assets shared by every platform

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew :android:assembleDebug
```

The APK is written to `android/build/outputs/apk/debug/`.

GitHub Actions builds debug and release APKs on every push to `main`.
