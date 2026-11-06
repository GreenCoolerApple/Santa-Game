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

Your Stolen Gifts run is saved after every house you rob, so you can leave and continue from the same house later.

## Menus

- **Game Mode** — pick Catch Gifts or Stolen Gifts
- **Options** — FPS limit (30, 60, 90, 120 or Max), Save Game and Load Game

## Save system

The game uses its own binary save format instead of platform preferences. Every file starts with a signature, a format version and a type, followed by the payload and a CRC32 checksum, and is written to a temporary file first and then swapped in, so an interrupted write never leaves a broken save behind. Damaged files are detected and ignored.

- `saves/autosave.sav` — best scores, stats and the current Stolen Gifts run, updated automatically
- `saves/slot1.sav` to `saves/slot3.sav` — manual save slots from Options
- `saves/settings.sav` — options such as the FPS limit, kept separate so loading a slot never changes them

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
