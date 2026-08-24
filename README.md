# Aura Defense

Aura Defense es una aplicación Android de defensa local y auditoría de seguridad. La aplicación prioriza el procesamiento en el dispositivo, muestra el estado observable del sistema y evita afirmar capacidades que Android no expone mediante APIs públicas.

## Construcción local

Requisitos:

- Android SDK con compile SDK 34.
- JDK 17.
- Acceso a Internet para resolver dependencias de Gradle.

Desde la raíz del repositorio:

```bash
chmod +x ./gradlew
./gradlew assembleRelease
```

El APK de release se genera bajo `app/build/outputs/apk/release/`. El proyecto define variantes `privateRelease` y `playRelease`; `assembleRelease` construye las variantes release disponibles.

## Construcción en Codemagic

El workflow `aura-android-release` de `codemagic.yaml` usa Java 17, configura `local.properties` con `ANDROID_SDK_ROOT`, hace ejecutable el wrapper, limpia el proyecto y ejecuta:

```bash
./gradlew assembleRelease --stacktrace
```

El artefacto publicado por el workflow es `app/build/outputs/apk/release/*.apk`. En Codemagic se debe seleccionar este repositorio y el workflow `Aura Defense Android Release`; las credenciales de firma y distribución deben configurarse como secretos del proyecto si se requiere una distribución firmada.

## Permisos y justificación

| Permiso o vínculo de servicio | Motivo |
| --- | --- |
| `android.permission.INTERNET` | Comunicación UDP multicast local y resolución/uso de funciones de red. |
| `android.permission.ACCESS_NETWORK_STATE` | Consultar conectividad, VPN activa y tipo de red para la postura de seguridad. |
| `android.permission.CAMERA` | Capturar la previsualización solicitada por el escáner QR. Se pide solo al entrar al escáner. |
| `android.permission.POST_NOTIFICATIONS` | Permitir notificaciones en versiones que lo requieren; el usuario debe concederlo cuando corresponda. |
| `android.permission.BIND_VPN_SERVICE` | Vínculo protegido del servicio VPN local; no es un permiso que la app pueda concederse a sí misma. Android gestiona el consentimiento VPN. |
| `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | Vínculo protegido de Notification Guard; el usuario debe habilitar el acceso en Ajustes. |
| `android.permission.BIND_QUICK_SETTINGS_TILE` | Vínculo protegido del Quick Settings Tile. |

La aplicación no declara `ACCESS_FINE_LOCATION` ni `ACCESS_COARSE_LOCATION`. Por diseño, no solicita ubicación; el modo de compartir ubicación de Auras no debe interpretarse como ubicación real en esta versión.

## Funcionalidades implementadas

- Evaluación local de postura de seguridad con score 0-100, hallazgos y telemetría del dispositivo.
- Detección de VPN, DNS privado, red validada, bloqueo de pantalla, ADB, opciones de desarrollador, fuentes desconocidas y parche de seguridad.
- Escaneo real de aplicaciones visibles instaladas, permisos concedidos, instalador, modo depurable y solicitudes sensibles.
- Servicio VPN local y carga de lista local de dominios bloqueados.
- Analizador local de enlaces con clasificación `Seguro`, `Sospechoso` o `Peligroso`.
- Auditor local de contraseñas y generación/exportación de informes TXT y JSON.
- Share Scanner mediante intents de texto.
- Notification Guard mediante `NotificationListenerService`, con acceso guiado por Ajustes.
- Descubrimiento UDP multicast de otras Auras con ID, estado, timestamp y `lastSeen`.
- Escáner QR con cámara y ML Kit; los enlaces se analizan antes de cualquier apertura y no se abren automáticamente.
- WorkManager para escaneos diarios, semanales o desactivados.
- Vault cifrado con clave del Android Keystore para resúmenes, logs y eventos almacenados por la aplicación.
- Quick Settings Tile que refleja el estado VPN y abre la aplicación cuando falta autorización del sistema.
- Estados vacíos, contraste para tema oscuro, controles táctiles de tamaño estándar y etiquetas TalkBack en controles clave.

## Limitaciones reales de Android sin root

- No se puede garantizar ni inspeccionar el firewall interno del sistema con una API pública general.
- Un servicio VPN no equivale a un firewall completo: el tráfico puede depender de las decisiones de Android, otras VPN y límites del sistema.
- Android requiere consentimiento explícito del usuario para activar una VPN.
- Las notificaciones solo pueden leerse después de que el usuario habilite Notification Guard en Ajustes.
- La cámara y las notificaciones requieren permisos o accesos controlados por el usuario.
- No se pueden leer datos privados de otras aplicaciones ni modificar sus permisos silenciosamente.
- El escaneo de paquetes está limitado a la visibilidad que conceden las reglas de Android y la configuración de la aplicación.
- WorkManager es oportunista: las ejecuciones periódicas no ocurren necesariamente en una hora exacta y pueden retrasarse por batería, red o políticas del sistema.
- El Quick Settings Tile no puede saltarse consentimiento VPN ni garantizar que el servicio sobreviva a políticas de segundo plano.
- Multicast UDP puede estar filtrado por el router, aislamiento Wi-Fi, ahorro de energía o políticas de red; el descubrimiento solo funciona en redes compatibles.
- La ubicación no está implementada como permiso ni como telemetría real en esta versión; no debe mostrarse como disponible.

## Auditoría final

Se revisaron los callbacks de botones de las pantallas Kotlin: no se encontraron expresiones `onClick` vacías en los controles existentes. Las acciones que no están implementadas en elementos del Centro Aura muestran explícitamente `Función en desarrollo`.

La auditoría de datos no queda completamente limpia y se documenta aquí para evitar una afirmación falsa: Home inicia el ID con `AURA-001`, y el modo de compartir ubicación de UDP usa `0.0,0.0` como coordenadas mock cuando el usuario lo activa. Tampoco existe permiso ni pantalla guiada de ubicación, porque la app no solicita ubicación. VPN, Notifications y Camera sí tienen flujos guiados.

La compilación local requiere JDK 17. El contenedor usado durante el desarrollo solo tenía Java 11; Codemagic está configurado con Java 17.
