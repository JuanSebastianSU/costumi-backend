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
| Pasarela / pago en línea | 6.11 | Código completo, pendiente MercadoPago | `COSTUMI_MP_ACCESS_TOKEN` (MercadoPago). Alternativa: Stripe (`STRIPE_SECRET_KEY` — otro adaptador) | Requiere el switch `pagoEnLinea` activo (Config). `POST /pagos/intento` responde **503** sin credencial. `POST /pagos/webhook` confirma el pago y registra el Pago idempotente. |

## Observabilidad (OpenTelemetry → SigNoz)

El backend trae el **agente de OpenTelemetry** incluido en la imagen (`Dockerfile`), pero **apagado por
defecto** (`OTEL_SDK_DISABLED=true`): no exporta nada ni agrega latencia hasta que se configure. Auto-
instrumenta HTTP (Spring MVC), JDBC (Postgres) y la JVM, y manda **trazas + métricas + logs** por OTLP a
SigNoz (u otro backend compatible). Para activarlo, setear en el entorno (Railway):

| Variable | Valor | Notas |
|---|---|---|
| `OTEL_SDK_DISABLED` | `false` | Enciende el agente (por defecto `true` = no-op). |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `https://ingest.<region>.signoz.cloud:443` | Endpoint OTLP de SigNoz Cloud (según tu región). |
| `OTEL_EXPORTER_OTLP_HEADERS` | `signoz-ingestion-key=<tu-key>` | La ingestion key que da SigNoz Cloud. |
| `OTEL_SERVICE_NAME` | `costumi-backend` | Ya viene seteado en la imagen; se puede overridear. |

Sin estas variables (o con `OTEL_SDK_DISABLED=true`), la app corre igual y no envía telemetría.
Self-host de SigNoz (ClickHouse + collector) es posible pero mucho más pesado; recomendado SigNoz Cloud.

## Notas
- **Recuperar contraseña**: endpoints `POST /api/v1/auth/olvide` (siempre 204, no revela si el correo existe) y `POST /api/v1/auth/restablecer` (token + nueva contraseña). El token se guarda **hasheado** (SHA-256), de un solo uso y con vencimiento (`costumi.email.recuperacion.duracion-minutos`, 60 por defecto). Adaptador: `EnviadorDeEmailSmtp` (busca `// TODO(credenciales)`).
