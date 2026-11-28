# Arquitectura 6.2 — OTT Nuevo

- Kotlin + Jetpack Compose + Media3 se mantienen como stack nativo Android.
- `ClassicShell` permanece separado; no se rediseña ni se sustituye.
- `OttShell` enruta exclusivamente las pantallas de OTT Nuevo.
- `AppPrefs.playbackPrefs` continúa siendo el origen compartido de ajustes por perfil.
- `PlaybackFactory` centraliza Media3, búfer, bitrate, audio attributes y peticiones HTTP.
- `Surface.setFrameRate` se solicita desde PlayerView cuando está activado y Android 11+ expone una Surface válida.
- El cambio de frecuencia es una solicitud al sistema, no garantía de que el televisor acepte el modo.
- No se simulan controles para shaders, interpolación, passthrough forzado ni audio delay que no estén conectados a una implementación verificable.
- QR y sincronización requieren ampliar el backend DAVIDLG; no se inventan endpoints del servidor.
