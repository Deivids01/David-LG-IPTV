# DavidLG IPTV Player 6.1.0 Beta 1 — OTT foundation

## Alcance de esta entrega

Esta entrega continúa sobre 6.0.0 Alpha 1. OTT Clásico conserva su implementación; el trabajo nuevo se concentra en OTT Nuevo y en preferencias de reproducción compartidas.

### Cambios implementados

- El reproductor principal y la vista previa de TV en vivo aceptan un User-Agent personalizado configurado desde Ajustes.
- Precedencia de User-Agent: valor específico de la fuente, después preferencia del perfil, y finalmente el valor predeterminado de la aplicación.
- Ajuste compartido para ocultar canales que no tienen programación EPG disponible; se aplica al listado y a la parrilla de OTT Nuevo.
- Se conserva la carga virtualizada de canales y guía mediante LazyColumn.
- Se mantiene la configuración de listas y preferencias asociada al perfil activo.
- Versión de aplicación incrementada a 6.1.0-beta1-ott-foundation (versionCode 33).

### Pendiente para las siguientes fases

- Renovación visual y funcional completa de OTT Nuevo: Hero editorial ampliado, filas dinámicas, navegación y fichas de contenido.
- Flujo de acceso por QR y API de sesión temporal en el backend.
- CRUD de perfiles ampliado con avatar, idioma y PIN parental seguro.
- Catálogos VOD con filtros por género/año/calidad y búsqueda predictiva.
- EPG avanzada: virtualización por franjas horarias, navegación temporal y sinopsis contextual.
- Integración y detección de capacidades del dispositivo para passthrough, cambio de frecuencia de pantalla y decodificación.
- Evaluación técnica separada de libmpv/shaders. No se presentan IA, MEMC, Atmos ni passthrough como disponibles hasta integrarlos y comprobar soporte real.

## Verificación

Se verificó la integridad del código fuente y del archivo ZIP. La compilación Android no pudo ejecutarse en este entorno porque Gradle 8.14.3 no está disponible en caché y el entorno no pudo resolver services.gradle.org. Por tanto, esta entrega no se declara compilada.
