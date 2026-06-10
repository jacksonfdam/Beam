# Beam — native apps

Beam turns any exported PDF (Keynote, PowerPoint, Canva, Figma) into a flawless
fullscreen presentation, controlled from a phone. Local-first, LocalSend-style:
no accounts, no cloud storage of user content, devices discover and talk to each
other directly over the LAN. **Open → Present → Done.**

## Platforms & roles

| Target | Role |
| --- | --- |
| Desktop (Compose Multiplatform / JVM) | Presenter + host: renders the deck fullscreen, runs the presenter view, hosts the LAN WebSocket server, shows the QR / IP / PIN, owns the single source of truth (current slide, timer, notes). |
| Android + iOS (Compose Multiplatform) | The remote: connects by QR or manual IP, picks a deck, navigates, controls the timer, draws on the slide, reads speaker notes. |
| Web (`../web`) | Landing page + browser remote (WebRTC). Built and documented separately. |

## Module map

This project keeps the JetBrains KMP scaffold's module names and maps the build
prompt's intended layout onto them (decision recorded here per the prompt):

```
:core              domain models + wire protocol (com.jacksonfdam.beam.protocol) + BeamJson + transport interfaces
:app:shared        Compose Multiplatform UI shared by the apps (presenter + remote features)
:app:androidApp    Android remote entry point
:app:iosApp        iOS remote entry point (Xcode)
:app:desktopApp    Desktop presenter/host entry point
:server            Ktor server host pieces (JVM)
```

The wire protocol lives **once** in `:core`; the desktop server and every client
depend on that single definition (DRY). Transport, PDF rendering, and UI depend
on the abstractions in `:core` (`PresenterClient`, `PresenterServer`), never on a
concrete engine (OCP/DIP).

## Wire protocol (source of truth)

`com.jacksonfdam.beam.protocol` in `:core` is canonical for the native apps and
is mirrored by `../web/lib/protocol.ts` for the browser remote — the two must
change together. Transport is JSON over WebSocket, polymorphic sealed
hierarchies discriminated by a `"type"` key via `BeamJson`. Ink is normalized
(`NormPoint`, `0f..1f`) and streamed `start → point* → end`. Notes ride on
`SlideChanged` from a host-side sidecar.

## Build status

- [x] Milestone 2 — `:core` wire protocol + JSON round-trip tests.
- [x] Milestone 3 — `:transport` Ktor server/client + handshake + PIN (handshake tests in `:transport` jvmTest).
- [ ] Milestone 4 — `:pdf` rendering (Android / iOS / Desktop).
- [ ] Milestone 5 — Desktop presenter (load PDF, fullscreen, QR/IP, presenter view).
- [ ] Milestone 6 — Mobile remote (connect, deck picker, navigation, notes).
- [ ] Milestone 7 — Live ink overlay.
- [ ] Milestone 8 — Host-owned timer + reconnect-survives-state.

---

This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM), Server.

* [/app/iosApp](./app/iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./app/shared/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :app:desktopApp:hotRun --auto`
  - Standard run: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…