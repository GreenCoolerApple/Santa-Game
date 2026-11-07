<p align="center">
  <img src="icon.png" width="160" alt="Santa Game icon">
</p>

# Santa Game

A Christmas game made with Kotlin and libGDX, for Android, iOS, Windows, macOS and Linux, in English and Portuguese (Brazil).

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

### Sled Race

A downhill sled race against three rivals: Comet the reindeer, Jingle the elf and Flurry the snowman. After a 3-2-1 countdown you race down a winding snowy track with candy-cane poles on the edges. Dodge rocks, trees and snowmen (a crash spins you out and almost stops you), stay out of the deep snow on the sides, which slows you down, and drive over the blue boost pads for a burst of speed. Golden gifts along the track give extra coins. Every race has a new track. The HUD shows your position, time, gifts and speed, and the bar on the right shows where every racer is. Your best time is saved.

- Hold the left or right half of the screen to steer on mobile
- A/D or the left/right arrows on a keyboard

## Menus

The main menu is fully animated: the title letters drop in and wave, Santa's hat sits on the "S", a garland of blinking Christmas lights hangs across the top, stars twinkle, Santa's sleigh flies across the sky now and then, the buttons pop in one after another with snow on top and holly on the Game Mode button, and on desktop the buttons grow and glow when you hover over them.

- **Game Mode** — pick Catch Gifts, Stolen Gifts, Escape from the Santa City or Sled Race
- **Awards** — the game's achievements, with your progress
- **Shop** — spend your golden gifts on outfits and perks
- **Options** — FPS limit (30, 60, 90, 120 or Max), language (English or Português (Brasil)), music volume, Save Game and Load Game. On Android and iOS it also has haptics on/off and haptics intensity (25% to 100%); these are hidden on desktop, which has no vibration

The game opens with a short intro (Santa's sleigh crossing the moon) that can be skipped with a tap.

The language starts as the system language and can be changed at any time.

## Desktop extras

On Windows, macOS and Linux the game uses its own Christmas cursor (a candy cane arrow with a golden star) and has a few extras:

- **Screenshots** — press **F3** to take a screenshot. The screen flashes, a camera shutter plays and the PNG is saved to `Pictures/Santa Game` in your user folder (or `.santa-game/screenshots` if there is no Pictures folder), named with the date and time.
- **Controls** — Options has a Controls screen where every action has a main key and an alternative key: Move Up, Move Down, Move Left, Move Right, Pause, Confirm and Screenshot. Click a key and press the new one; Esc cancels, Backspace clears it and Reset restores the defaults. Esc always pauses too.

| Action | Default keys |
|---|---|
| Move | W A S D / arrow keys |
| Pause | P / Esc |
| Confirm | Space / Enter |
| Screenshot | F3 |

## Music

Every part of the game has its own original music, composed and synthesized for the game:

- `intro` — a short piece that follows the intro: rising bells as the moon comes up, sleigh bells while the sleigh crosses it and a big chord when the title lands
- `menu` — a cozy Christmas waltz in F major (celesta, music box, bass, soft strings and sleigh bells)
- `catch` — "Sleigh Dash", an upbeat 144 BPM sleigh ride in C major for Catch Gifts
- `stolen` — "Tiptoe Thief", a sneaky pizzicato piece in D minor for Stolen Gifts
- `escape` — "Snowstorm Chase", a tense 150 BPM chase in E minor with drums for Escape from the Santa City
- `sled` — "Downhill Rush", a fast 168 BPM race theme in D major with driving drums, sleigh bells and a bright lead for Sled Race

All looping tracks loop seamlessly. The menu music keeps playing while you move between menu screens and crossfades into each mode's music when a game starts. The tracks are stored as OGG in `assets/music` and as M4A in `assets-ios/music`, because iOS does not play OGG. The volume (Off, 25% to 100%) is in Options.

## Easter egg: the Green Talking Apple

Every time you open the game there is a 50% chance that the Green Apple shows up on the main menu. She is the dev's mascot: a green apple with cool sunglasses and a Santa hat, and she introduces herself as the one who made the game. She picks one of eight Christmas topics (snowflakes, Santa's sleigh, apples as the first tree ornaments, cookies, the best gift, reindeer that see ultraviolet light, the green sick people and Christmas in the Brazilian summer), you answer with one of two replies and she answers back. At the end she gives you 5 golden gifts, and meeting her for the first time unlocks the Talking Apple award.

## Golden gifts and Shop

Golden gifts are the game's coins. You earn them at the end of every game: one for every 30 seconds played, plus a bonus from your score (gifts caught, gifts stolen, city points, or your place and gifts in Sled Race). The Shop sells:

- **Santa suits** — Classic, Frost, Evergreen, Royal and Golden, used in Catch Gifts, Stolen Gifts and Sled Race
- **Boy jackets** — Blue, Orange, Green and Pink, used in Escape from the Santa City
- **Perks** — Extra Life (4 lives in Catch Gifts), Quiet Boots (families notice you 20% slower) and Fast Sneakers (8% faster in the city)

## Haptics

Vibration uses short patterns with real intensity instead of a single buzz: a light tick when you collect something or press a button, a double pulse when you get hurt, a strong sequence when you are caught and a rising pattern when you succeed. Haptics can be turned off or set to 25%, 50%, 75% or 100% in Options on Android and iOS.

## Awards

There are 24 awards: five for each of the first three game modes, four for Sled Race (finish a race, win a race, finish in under 50 seconds, finish without crashing) and five general ones (play every mode, save to a slot, change the language, buy something in the Shop, meet the Green Apple). When you earn one, a golden card slides in from the top of the screen wherever you are. Awards are stored in the save data, and loading a slot never removes awards you already earned.

## Save system

The game uses its own binary save format instead of platform preferences. Every file starts with a signature, a format version and a type, followed by the payload and a CRC32 checksum, and is written to a temporary file first and then swapped in, so an interrupted write never leaves a broken save behind. Damaged files are detected and ignored.

- `saves/autosave.sav` — best scores, stats, awards, golden gifts, Shop items and the current Stolen Gifts run, updated automatically
- `saves/slot1.sav` to `saves/slot3.sav` — manual save slots from Options
- `saves/settings.sav` — options such as the FPS limit, language, music volume, haptics and key bindings, kept separate so loading a slot never changes them

On Android and iOS the saves live in the app's private storage. On Windows, macOS and Linux they live in `.santa-game/saves` inside your user folder.

## Pause

On Android and iOS every mode has a pause button (with a little Santa hat) in the top right corner; on desktop you pause with Esc or your Pause key. The Android back button also pauses, and the game pauses itself when the app goes to the background.

The pause menu is a Christmas panel that drops in with a bounce: a garland with blinking lights, holly in the corners, a red ribbon banner with a waving title and snow falling behind it. It has three buttons that pop in one after another: Resume, Restart and Menu. Resuming shows a short 3, 2, 1 countdown so you are ready when the game starts again, and the music gets quieter while the game is paused.

## Project structure

- `core` — the game itself (screens, modes, save system, translations)
- `android` — Android launcher, manifest and icons
- `ios` — iOS launcher (RoboVM / MobiVM), Info.plist, icons and launch screen
- `lwjgl3` — desktop launcher (Windows, macOS and Linux) and packaging
- `assets-ios` — iOS-only assets (M4A music)
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

GitHub Actions builds the APK and the desktop jar together in one Gradle run on Linux and packages the Linux app right there; Windows and macOS runners then only package that jar, and a separate macOS runner builds the iOS app. Every desktop build bundles a trimmed Java runtime. These artifacts come out of every push to `main`:

- `Santa-Android` — the Android APK
- `Santa-Windows` — a ready-to-run Windows folder with `SantaGame.exe` and a bundled Java runtime
- `Santa-Linux` — `Santa-Linux.tar.gz` with a ready-to-run `SantaGame` folder and a bundled Java runtime
- `Santa-macOS-AppleSilicon` and `Santa-macOS-Intel` — `SantaGame.app` zipped, for M-series and Intel Macs
- `Santa-iOS` — an unsigned `Santa-iOS.ipa`, ready to sign and sideload (AltStore, Sideloadly, TrollStore and similar)

### Running on macOS

The app is not notarized, so the first time macOS may block it. Right-click `SantaGame.app` and choose Open, or run:

```bash
xattr -dr com.apple.quarantine SantaGame.app
```

### iOS

The iOS module is only included in the Gradle build on macOS (it needs Xcode). To build it locally on a Mac:

```bash
./gradlew :ios:createIPA -Probovm.iosSkipSigning=true -Probovm.archs=arm64
```

### Running on Linux

```bash
tar -xzf Santa-Linux.tar.gz
./SantaGame/bin/SantaGame
```

To add Santa Game to your applications menu, run `./SantaGame/install.sh` once from the extracted folder.
