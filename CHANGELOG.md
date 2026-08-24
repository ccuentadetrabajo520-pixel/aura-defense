# Changelog

## Fase 13 - Pulido visual, animaciones reactivas y auditoría UI

- Radar reactivo al score y a hallazgos reales de severidad alta/crítica.
- Score con color por umbral: verde desde 85, amarillo entre 60 y 84, rojo por debajo de 60.
- Escudo de Defensa basado en VPN y DNS observables.
- Eliminación de botones muertos y adición de estados vacíos.
- Revisión de contraste, objetivos táctiles y etiquetas de accesibilidad.

## Fase 12 - WorkManager, Vault y Quick Settings Tile

- Escaneos periódicos diarios, semanales o desactivados con WorkManager.
- Escaneo real de aplicaciones instaladas desde `ScanWorker`.
- Almacenamiento cifrado con Android Keystore y borrado de historial.
- Pantallas de configuración de WorkManager y consulta del Vault.
- Quick Settings Tile para reflejar y controlar el estado VPN.

## Fase 11 - QR Scanner y Camera

- Captura de imagen real mediante `TakePicturePreview`.
- Escaneo de códigos con ML Kit Barcode Scanning.
- Solicitud de cámara solo al acceder al escáner.
- Análisis anti-phishing antes de cualquier acción sobre una URL.
- Integración del escáner en Centro Aura.

## Fase 10 - UDP LAN Auras

- Descubrimiento UDP multicast local con `DatagramSocket` y `MulticastSocket`.
- Intercambio de ID, estado y timestamp.
- Lista de Auras descubiertas con cálculo de `lastSeen`.
- Privacy Shield apagado por defecto para compartir ubicación.
- Integración de la pestaña Auras con su pantalla real.

## Fase 9 - Reportes y herramientas locales

- Generación de informes TXT y JSON.
- Herramientas locales de análisis de enlaces y auditoría de contraseñas.
- Estados y resultados visibles en Centro Aura.

## Fase 8 - Share Scanner

- Recepción de texto mediante `ACTION_SEND` y `PROCESS_TEXT`.
- Procesamiento del contenido compartido sin abrir enlaces automáticamente.

## Fase 7 - Notification Guard

- Servicio `NotificationListenerService` para analizar texto localmente.
- Pantalla guiada hacia los ajustes de acceso de notificaciones.

## Fase 6 - Defensa VPN

- Servicio VPN local basado en APIs estándar de Android.
- Flujo de consentimiento mediante `VpnService.prepare`.
- Indicador de estado de VPN en Defensa.

## Fase 5 - Lista de amenazas DNS

- Carga de una lista local de dominios amenazantes.
- Integración con la lógica de bloqueo del servicio VPN.

## Fase 4 - Postura de seguridad y telemetría

- Motor de postura con score 0-100.
- Hallazgos priorizados y acciones hacia Ajustes.
- Telemetría de dispositivo, red, batería, apps y parche de seguridad.

## Fase 3 - Escáner de aplicaciones

- Enumeración de aplicaciones visibles instaladas.
- Inspección de permisos, instalador, modo depurable y configuración de backup.

## Fase 2 - Navegación y UI base

- Navegación principal con Inicio, Auras, Defensa y Apps.
- Centro Aura para herramientas y configuración.
- Tema oscuro y componentes Compose reutilizables.

## Fase 1 - Base Android

- Estructura Android nativa y actividad principal.
- Configuración inicial de variantes release y pipeline de Codemagic.
- Incorporación de AndroidX, Compose y servicios base.
