# Infra pendiente de credenciales

Features cuyo **código está completo** pero que quedan **inactivas hasta cargar una credencial externa**.
El adaptador está gateado: si la credencial no está, cae a un modo seguro (no-op / log) y **no rompe nada**.
No se declaran "hechas y funcionando" hasta que las credenciales estén cargadas por el equipo.

| Feature | RF | Estado | Variables de entorno a cargar | Comportamiento sin credencial |
|---|---|---|---|---|
| Recuperar contraseña (email) | 1.1 | Código completo, pendiente SMTP | `COSTUMI_SMTP_HOST`, `COSTUMI_SMTP_PORT` (587), `COSTUMI_SMTP_USER`, `COSTUMI_SMTP_PASS`, `COSTUMI_EMAIL_FROM`, `COSTUMI_RESET_URL_BASE` (opcional) | El token se genera y persiste; el email se **registra en el log** en vez de enviarse. El endpoint funciona igual. |
| Fotos de prenda (S3) | 2.9 | Código completo, pendiente AWS | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (cadena por defecto del SDK), `AWS_REGION`, `COSTUMI_S3_BUCKET` | `POST /prendas/{id}/foto` responde **503** ("almacenamiento no configurado") hasta cargar bucket+región+credenciales. El resto del inventario funciona igual. |
| WhatsApp | 11.4 | Código completo, pendiente Meta | `COSTUMI_WHATSAPP_TOKEN`, `COSTUMI_WHATSAPP_PHONE_ID` (Meta Cloud API) | Sin credencial, la notificación por WhatsApp se **registra en el log** (router → log). El cliente debe tener `telefono`. |
| Push FCM | 18.11 | Código completo, pendiente Firebase | `COSTUMI_FCM_SERVER_KEY` (HTTP legacy). Moderno: `COSTUMI_FCM_CREDENTIALS` (service account, v1 — requiere lib de auth de Google) | Sin credencial, la push FCM se **registra en el log**. El cliente debe tener `device_token` (`PUT /clientes/{id}/device-token`, columna V31). |

## Notas
- **Recuperar contraseña**: endpoints `POST /api/v1/auth/olvide` (siempre 204, no revela si el correo existe) y `POST /api/v1/auth/restablecer` (token + nueva contraseña). El token se guarda **hasheado** (SHA-256), de un solo uso y con vencimiento (`costumi.email.recuperacion.duracion-minutos`, 60 por defecto). Adaptador: `EnviadorDeEmailSmtp` (busca `// TODO(credenciales)`).
