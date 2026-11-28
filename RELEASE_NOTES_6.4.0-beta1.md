# DAVIDLG IPTV Player 6.4.0 Beta 1 — integración ampliada con panel PHP + SQLite

## Integración de cuenta y panel
- Se conserva PHP + SQLite como backend único; la app consume exclusivamente la API HTTP del panel.
- El panel conserva `data/panel.sqlite` y aplica migraciones aditivas (`CREATE TABLE IF NOT EXISTS` / `ALTER TABLE` condicional).
- Perfiles remotos por cuenta, límite de cinco perfiles y sincronización de los metadatos de perfil.
- Sincronización versionada de preferencias de perfil (JSON, máximo 512 KB) con control de conflicto por revisión.
- PIN parental de 4–6 dígitos, almacenado con `password_hash` y verificado en servidor. Se puede cambiar y retirar con el PIN actual.
- Vinculación temporal de un televisor mediante código/QR de un solo uso y autorización desde otro dispositivo ya autenticado.
- Se mantiene la modalidad de bloqueo `none`, `full` y `cloud` del panel actualizado.
- Se mantiene el catálogo remoto de canales/VOD/EPG y los metadatos editoriales/DRM existentes.

## Compatibilidad
- No se modifica el código de `ClassicShell` ni la navegación clásica.
- Las nuevas funciones de perfiles y panel son compartidas por las experiencias compatibles.
- OTT Nuevo sigue siendo la experiencia renovada; el catálogo se administra desde el panel.

## Verificación y limitaciones de esta entrega
- PHP: `php -l` correcto en `lib.php`, `app.php` y `api.php`.
- Migración SQL: validada en una copia de `panel.sqlite` con SQLite; se conservaron las tablas y registros existentes.
- Android: no se pudo completar `:app:compileDebugKotlin` porque el entorno no pudo descargar Gradle 8.14.3 (`UnknownHostException: services.gradle.org`). Por tanto, no se declara compilación Android confirmada.
- El flujo de vinculación muestra un QR con el código temporal y también permite introducirlo manualmente desde Ajustes en un dispositivo autenticado.
- Capacidades de decodificación, passthrough, Atmos, MEMC y shaders siguen condicionadas por hardware/firmware; no se anuncian como universales.
