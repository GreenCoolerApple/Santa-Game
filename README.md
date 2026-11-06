<p align="center">
  <img src="icon.png" width="160" alt="Santa Game icon">
</p>

# Santa Game

A Christmas game made with Kotlin and libGDX.

## Game modes

### Catch Gifts

Help Santa catch the falling gifts before they hit the snow, and stay away from the coal. You have three lives: dropping a gift or catching coal costs one.

- Touch and drag to move Santa
- Arrow keys or A/D on a keyboard

### Stolen Gifts

Sneak into people's houses through the fireplace, steal the gifts under the Christmas tree and escape the way you came in without being seen. The family walks around with flashlights: if their light finds you, the detection meter fills up, and when it is full you are caught. Furniture blocks their view. Every house has more people, faster patrols and more gifts, and taking every gift from the tree gives a bonus.

- Touch anywhere and drag to use the virtual joystick
- WASD or arrow keys on a keyboard

## Pause

Both modes have a pause button in the top right corner. The Android back button also pauses, and the game pauses itself when the app goes to the background.

## Project structure

- `core` — the game itself (screens, Santa, falling items, scenery, houses and guards)
- `android` — Android launcher, manifest and icons
- `assets` — game assets shared by every platform

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew :android:assembleDebug
```

The APK is written to `android/build/outputs/apk/debug/`.

GitHub Actions builds `Santa-Android.apk` on every push to `main`.
