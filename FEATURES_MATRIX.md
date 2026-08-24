# Matriz de funcionalidades

| Implementado | Parcial | No posible por restricción Android |
| --- | --- | --- |
| Postura de seguridad local con score y hallazgos | La postura depende de señales que Android expone y no representa un diagnóstico absoluto | Inspección universal del firewall interno |
| Escaneo real de apps visibles instaladas | La visibilidad de paquetes depende de las reglas de Android | Lectura de datos privados de otras apps |
| Lista local de dominios amenazantes | El bloqueo depende de que el tráfico pase por la VPN de Aura | Firewall de sistema completo sin root |
| Servicio VPN local con consentimiento del usuario | Puede coexistir con límites y políticas de otras VPN | Activación silenciosa de VPN |
| Análisis local de enlaces | Clasificación heurística, no verificación remota de reputación | Garantía de detectar todo phishing o malware |
| Auditoría local de contraseñas | Evalúa la contraseña proporcionada, sin conocer filtraciones externas | Confirmar seguridad contra todas las bases de datos públicas |
| Share Scanner para texto recibido | El resultado depende del contenido compartido | Leer contenido de apps sin que lo compartan |
| Notification Guard después de habilitarlo en Ajustes | Requiere acceso especial concedido manualmente | Lectura de notificaciones sin consentimiento |
| Descubrimiento UDP multicast en LAN | Depende del router, aislamiento Wi-Fi y soporte multicast | Descubrir dispositivos fuera de la red local |
| Privacy Shield apagado por defecto | La ubicación real no está implementada; el modo opt-in usa coordenadas mock | Acceder a ubicación sin declarar y obtener consentimiento |
| Escáner QR con cámara y ML Kit | Analiza el contenido, no abre automáticamente los enlaces | Acceso a cámara sin permiso del usuario |
| WorkManager diario/semanal/desactivado | La hora exacta de ejecución la decide el sistema | Ejecución periódica exacta garantizada en segundo plano |
| Vault cifrado con Android Keystore | Solo protege los datos que la app guarda en el Vault | Recuperar datos si se pierde la clave del dispositivo |
| Quick Settings Tile con estado VPN | Debe respetar consentimiento VPN y restricciones de segundo plano | Saltarse controles del sistema desde el tile |
| Informes TXT y JSON | Son informes de señales locales disponibles | Informe forense completo del dispositivo sin root |
| Pantallas de cámara, VPN y notificaciones guiadas | La pantalla de ubicación no existe porque la app no solicita ubicación | Solicitar ubicación sin permiso declarado |
| Estados vacíos y controles Compose con acciones | Algunas herramientas del Centro Aura aún muestran “Función en desarrollo” | No aplica |
| Navegación Auras integrada | Las pestañas Defensa y Apps aún conservan placeholder en `MainActivity` | No aplica |
