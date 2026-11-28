# Integración DAVIDLG 6.4 — PHP + SQLite

La aplicación no se conecta directamente a SQLite. Toda operación de cuenta y catálogo pasa por `panel/app.php`.

## API existente conservada
- `register`, `login`, `activate`, `redeem`, `me`, `content`, `device_remove`, `logout`.
- Catálogo remoto de TV/VOD, EPG, metadatos VOD, DRM, bloqueos `none/full/cloud`.

## API añadida
- `profiles`: lectura, alta/actualización y eliminación de perfiles asociados al usuario autenticado. Máximo cinco; no permite eliminar el último.
- `profile_pin`: alta/cambio, verificación y retirada de PIN. El servidor guarda un hash con `password_hash`; nunca devuelve el PIN ni su hash.
- `profile_state`: sincronización JSON por perfil con revisiones incrementales y respuesta `409 sync_conflict` ante escrituras concurrentes. Tamaño máximo 512 KB.
- `qr_create`, `qr_status`, `qr_authorize`: vinculación temporal de dispositivos. El código expira a los tres minutos; la autorización requiere una sesión válida de otro dispositivo y el token nuevo se entrega al dispositivo que creó el reto.

## Estado sincronizado
Las preferencias exportadas por perfil incluyen fuentes M3U, selección/filtro de fuentes, favoritos VOD, progreso, metadatos editoriales, recientes y preferencias del reproductor/apariencia. Las claves locales se mantienen con el prefijo por perfil existente para no mezclar perfiles.

## Migración
`lib.php` crea tablas nuevas y añade columnas únicamente cuando faltan. No borra las tablas ni los registros de usuarios, dispositivos, códigos, paquetes, reglas, elementos o fuentes EPG. En producción, conservar siempre el `config.php` y `data/panel.sqlite` existentes.
