# DavidLG IPTV Player 6.2.0 Beta 1 — OTT Next

## Base preservada
- Se parte de 6.1.0 Beta 1, conservando el modo Clásico como experiencia independiente.
- OTT Nuevo mantiene navegación y pantallas independientes.
- Se conservan perfiles, listas por perfil, preferencias compartidas, EPG y compatibilidad M3U/Xtream ya presentes.

## Cambios de esta entrega
- Se reorganizan los ajustes con apartados separados de Sonido y Video avanzado.
- Se mantiene la selección de idioma de audio conectada al TrackSelectionParameters de Media3.
- Se incorpora la solicitud de igualación de frecuencia de pantalla mediante Surface.setFrameRate cuando Android 11+ y la superficie del reproductor lo permiten; se restablece a frecuencia predeterminada al desactivar.
- Se conserva el control real de búfer bajo/predeterminado/alto y bitrate adaptativo de Media3.
- Se documentan las limitaciones de passthrough, Atmos/TrueHD, sincronización manual, escalado IA y MEMC para no exponer controles falsos.
- Se actualizan versionCode a 34 y versionName a 6.2.0-beta1-ott-complete.

## Capacidades que requieren fases adicionales
- QR: requiere endpoints de sesión temporal y asociación de dispositivo en el backend.
- PIN parental: falta completar el flujo de verificación y persistencia segura por perfil.
- Sincronización de perfiles: requiere endpoints de backend y estrategia de conflictos.
- Anime4K/FSR/NIS y MEMC: requieren pipeline GPU/libmpv y pruebas térmicas/rendimiento por dispositivo.
- Passthrough forzado y retardo de audio: requieren integración de AudioSink/AudioTrack específica y validación de salida HDMI.
- Decoder HW/HW+/SW: Media3 selecciona decodificadores disponibles; forzar software requiere renderers personalizados y validación de codecs.

## Verificación
- ZIP estructural: se validará con `unzip -t`.
- Compilación Android: no verificada en este entorno porque Gradle 8.14.3 no está disponible en caché y la descarga de services.gradle.org falla por DNS/red.
