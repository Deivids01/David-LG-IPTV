# DavidLG 6.3.0 Beta 1 — integración de catálogo DAVIDLG

## Fuente de contenido
La aplicación consume el catálogo remoto mediante la API pública PHP del panel (`app.php?r=content&t=TOKEN`). No accede directamente a SQLite. Se conserva la posibilidad de listas locales como fuentes adicionales y respaldo.

## Cambios de esta integración
- El cliente interpreta metadatos VOD del panel: tipo (película/serie/episodio/anime), sinopsis, póster, fondo, año, género, duración, calificación, temporada, episodio, identificador de serie e ID externo.
- Se respeta `tvg_id` de los canales administrados para relacionar la programación XMLTV.
- Las guías EPG configuradas desde el panel se descargan y se fusionan con las guías de listas locales.
- El catálogo remoto se vuelve a consultar periódicamente mientras la sesión esté activa.
- Se diferencian los bloqueos de cuenta completa (`full_block`) y del catálogo del panel (`cloud_block`). El segundo permite conservar el acceso a fuentes locales.
- Una suscripción vencida o catálogo suspendido ya no impide guardar la sesión de la cuenta; el endpoint de contenido aplica la restricción al catálogo remoto.

## Contrato de contenido
`content` entrega `user`, `channels`, `vod` y `epg`. Cada elemento VOD puede incluir `media_type`, `description`, `poster`, `backdrop`, `year`, `genre`, `duration`, `rating`, `season`, `episode`, `series_id`, `external_id`; los canales incluyen `tvg_id`.

## Pendiente para siguientes fases
QR de vinculación TV/móvil, perfiles sincronizados entre dispositivos, PIN parental con verificación, interfaz completa de temporadas/episodios y filtros editoriales avanzados, además de capacidades avanzadas de audio/vídeo condicionadas por el hardware. No se muestran controles que aparenten capacidades todavía no conectadas.

## Verificación
La estructura del ZIP y los cambios estáticos se verifican por separado. La compilación Android requiere descargar Gradle 8.14.3; en el entorno de trabajo la descarga está bloqueada por DNS (`UnknownHostException: services.gradle.org`), por lo que no se declara compilación confirmada.
