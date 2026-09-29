# AI Edu Studio (Native Android)
Package: `com.aiedustudio.editor`

This is the native Kotlin + Jetpack Compose Android project, separate from the existing web prototype. Open the `android/` directory in Android Studio and use JDK 17 / Android Gradle Plugin 8.7.3.

## Implemented in this first native increment
- Compose dark editor workspace with large preview area and compact clip list timeline
- Video, multi-image, and multi-audio import using Android document picker
- ExoPlayer preview playback for selected video clips
- Add/remove/select timeline clips; basic duration adjustment, split-at-midpoint and duplicate actions
- Aspect ratio selection (16:9, 9:16, 1:1, 4:5)
- Save/load imported project metadata and persisted document URI grants on-device

## Not yet implemented
Rendering/export, precise frame-accurate trim and split-at-playhead, image rendering in preview, true multi-track lane arrangement, undo/redo, text/overlay compositor, quiz automation, thumbnail editor, SEO/publishing checks, and AI provider integrations. The export button explicitly reports this status; it does not fake a rendered video. Media3 Transformer is included as the next export integration point.

Build with Android Studio or `gradle :app:assembleDebug` from this directory after installing Android SDK/JDK 17.
