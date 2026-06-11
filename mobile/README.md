# Beam — native apps

Beam projects an exported PDF fullscreen from a computer and turns your phone
into the remote — slides, live ink, spotlight, speaker notes, and a shared
timer. Local-first: no accounts, no cloud storage of user content; devices
discover and talk to each other directly over the LAN.

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
:core              domain models + wire protocol (com.jacksonfdam.beam.protocol) + BeamJson + transport interfaces + notes sidecar
:transport         Ktor PresenterClient (multiplatform) + PresenterServer (JVM/CIO) + handshake
:pdf               PdfDocument expect/actual rendering (Desktop PDFBox, Android PdfRenderer, iOS CoreGraphics)
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

### Speaker-notes sidecar

A PDF carries no notes, so the host pairs a deck with a `<deck>.notes.json`
file (`DeckNotes` in `:core`) and pushes the current slide's note on
`SlideChanged`:

```json
{ "version": 1, "notes": { "0": "Open warm", "3": "Pause for the demo" } }
```

Keys are zero-based slide indices; missing indices simply have no notes. The
file is read by the host only — it never leaves the device.

## Status

The native flow works end to end: `:core` wire protocol (with round-trip
tests), `:transport` Ktor server/client with the PIN handshake (handshake tests
in `:transport` jvmTest), `:pdf` rendering on all three platforms plus the notes
sidecar, the desktop presenter (load PDF, fullscreen projector on the external
display, QR/IP/PIN, presenter view, live ink), and the shared-Compose remote
(connect, deck picker, navigation, slide indicator, notes, host-owned timer,
drawing, reconnect-restores-state). The UI is localized (EN/PT-BR/ES/SV). QR
scanning is an `expect/actual` seam with manual entry as the always-working
fallback; on-device camera scanning is still being polished.

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