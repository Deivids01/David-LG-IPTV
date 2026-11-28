# Hoja de ruta técnica — OTT Nuevo

## Límites de arquitectura

- Kotlin + Jetpack Compose sigue siendo la plataforma Android.
- `ClassicShell` se mantiene como experiencia independiente.
- `OttShell` concentra la nueva experiencia.
- `AppPrefs.playbackPrefs` es la fuente de preferencias compartidas entre interfaces y se guarda por perfil.
- `PlaybackFactory` es el punto único de creación y configuración de Media3.
- Room/SQLite se evaluará para cachear catálogos grandes; no se sustituirá el almacenamiento actual sin migración.
- El backend DAVIDLG existente se ampliará para QR/perfiles/sincronización; no se migra de proveedor sin una necesidad demostrada.

## Fases

1. Base: estabilizar preferencias por perfil, fuentes y EPG; definir contratos de datos.
2. OTT Nuevo: home editorial, filas de contenido, detalles, búsqueda y navegación D-pad.
3. Live/EPG: parrilla virtualizada, progreso, ventana temporal y sinopsis contextual.
4. VOD: filtros reales sobre metadatos existentes, temporadas/episodios y continuar viendo.
5. Cuentas/perfiles: QR con sesiones temporales del backend, CRUD, PIN con hash seguro y sincronización.
6. Motor: User-Agent, buffers configurables, selección de pistas y diagnóstico de capacidades; después evaluar passthrough y frame-rate matching.
7. Laboratorio multimedia: prototipo aislado de libmpv/shaders y pruebas de consumo/latencia antes de integrar.

## Regla de honestidad de capacidades

Una preferencia solo se expondrá como funcional si tiene implementación verificable. Funciones dependientes de firmware, API, GPU, códec, licencia o salida HDMI se mostrarán como no compatibles/no disponibles cuando corresponda.
