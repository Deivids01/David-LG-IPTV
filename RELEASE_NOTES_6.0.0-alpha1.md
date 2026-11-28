# DavidLG IPTV Player 6.0.0-alpha1

Primera base de la nueva generación, construida sobre el ZIP compilado aportado por el usuario (`5.14.2-register-icons-fix`).

## Cambios de esta entrega
- Las listas IPTV, listas activas y filtro de fuente pasan a ser datos independientes por perfil.
- Migración compatible: los perfiles existentes reciben una copia inicial de las listas compartidas de versiones anteriores; después cada perfil puede modificarlas sin afectar a los demás.
- Al cambiar de perfil se recargan sus preferencias, listas, canales y EPG.
- La sección independiente de guía en el modo Clásico se identifica como EPG.
- El reloj del modo Clásico muestra segundos y respeta la preferencia de 12/24 horas.
- La pantalla de Ajustes sigue siendo la misma para ambos estilos de interfaz, compartiendo el comportamiento de las preferencias que consume cada experiencia.

## Conservación
- Se mantiene el modo Clásico y el modo OTT Nuevo como experiencias separadas.
- Se conservan los componentes existentes de autenticación, perfiles, TV en vivo, EPG, VOD, reproducción y panel.

## Pendiente para las siguientes etapas
- Campos de PIN e idioma por perfil y su interfaz de edición.
- Rediseño integral de la experiencia de inicio y catálogos.
- Vinculación/sincronización de perfiles con la cuenta principal y emparejamiento QR (requiere ampliar la API del panel).
- Ampliaciones del motor multimedia sujetas a capacidades reales del dispositivo.

## Verificación
No se pudo completar `assembleDebug` en este entorno porque Gradle 8.14.3 no está disponible localmente y la descarga desde `services.gradle.org` falló por `UnknownHostException`. Esta entrega no se declara compilada.
