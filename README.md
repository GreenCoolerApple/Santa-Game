<p align="center">
  <img src="icon.png" width="160" alt="Santa Game icon">
</p>

# Santa Game

A Christmas game made with Kotlin and libGDX, for Android and Windows, in English and Portuguese (Brazil).

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

### Escape from the Santa City

You are a boy lost in a snowy city at night, and the green sick people are looking for you to wrap you up in a Christmas present. Find the glowing exit on the other side of the city without getting caught. The sick wander the streets, and when one of them sees you it chases you through the streets and then searches the last place it saw you. Buildings and park trees block their view. Grab candy on the way for extra points, and use the minimap and the arrow on the edge of the screen to find the exit. Every city is bigger, with more and faster sick people.

- Touch anywhere and drag to use the virtual joystick
- WASD or arrow keys on a keyboard

## Menus

- **Game Mode** — pick Catch Gifts, Stolen Gifts or Escape from the Santa City
- **Awards** — the game's achievements, with your progress
- **Shop** — spend your golden gifts on outfits and perks
- **Options** — FPS limit (30, 60, 90, 120 or Max), language (English or Português (Brasil)), haptics on/off, haptics intensity (25% to 100%), Save Game and Load Game

The game opens with a short intro (Santa's sleigh crossing the moon) that can be skipped with a tap.

The language starts as the system language and can be changed at any time.

## Golden gifts and Shop

Golden gifts are the game's coins. You earn them at the end of every game: one for every 30 seconds played, plus a bonus from your score (gifts caught, gifts stolen or city points). The Shop sells:

- **Santa suits** — Classic, Frost, Evergreen, Royal and Golden, used in Catch Gifts and Stolen Gifts
- **Boy jackets** — Blue, Orange, Green and Pink, used in Escape from the Santa City
- **Perks** — Extra Life (4 lives in Catch Gifts), Quiet Boots (families notice you 20% slower) and Fast Sneakers (8% faster in the city)

## Haptics

Vibration uses short patterns with real intensity instead of a single buzz: a light tick when you collect something or press a button, a double pulse when you get hurt, a strong sequence when you are caught and a rising pattern when you succeed. Haptics can be turned off or set to 25%, 50%, 75% or 100% in Options.

## Awards

There are 19 awards: five for each game mode and four general ones (play every mode, save to a slot, change the language, buy something in the Shop). When you earn one, a golden card slides in from the top of the screen wherever you are. Awards are stored in the save data, and loading a slot never removes awards you already earned.

## Save system

The game uses its own binary save format instead of platform preferences. Every file starts with a signature, a format version and a type, followed by the payload and a CRC32 checksum, and is written to a temporary file first and then swapped in, so an interrupted write never leaves a broken save behind. Damaged files are detected and ignored.

- `saves/autosave.sav` — best scores, stats, awards, golden gifts, Shop items and the current Stolen Gifts run, updated automatically
- `saves/slot1.sav` to `saves/slot3.sav` — manual save slots from Options
- `saves/settings.sav` — options such as the FPS limit, language and haptics, kept separate so loading a slot never changes them

On Android the saves live in the app's private storage. On Windows they live in `.santa-game/saves` inside your user folder.

## Pause

Every mode has a pause button in the top right corner. The Android back button also pauses, and the game pauses itself when the app goes to the background.

## Project structure

- `core` — the game itself (screens, modes, save system, translations)
- `android` — Android launcher, manifest and icons
- `lwjgl3` — Windows (desktop) launcher and packaging
- `assets` — game assets shared by every platform, including the bitmap font

The font is generated from Inter Bold, licensed under the SIL Open Font License 1.1 (see `assets/fonts/Inter-LICENSE.txt`).

## Building

Requires JDK 17, plus the Android SDK for the Android build.

```bash
./gradlew :android:assembleDebug
./gradlew :lwjgl3:run
./gradlew :lwjgl3:dist
```

The APK is written to `android/build/outputs/apk/debug/`, and `dist` writes a runnable `SantaGame.jar` to `lwjgl3/build/dist/`.

GitHub Actions builds the APK and the desktop jar together in one Gradle run on Linux, then a Windows runner only packages the jar with a trimmed Java runtime. Two artifacts come out of every push to `main`:

- `Santa-Android` — the Android APK
- `Santa-Windows` — a ready-to-run Windows folder with `SantaGame.exe` and a bundled Java runtime
