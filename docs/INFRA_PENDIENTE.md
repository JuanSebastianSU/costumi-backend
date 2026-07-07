# Infra pendiente de credenciales

Features cuyo **código está completo** pero que quedan **inactivas hasta cargar una credencial externa**.
El adaptador está gateado: si la credencial no está, cae a un modo seguro (no-op / log) y **no rompe nada**.
No se declaran "hechas y funcionando" hasta que las credenciales estén cargadas por el equipo.

| Feature | RF | Estado | Variables de entorno a cargar | Comportamiento sin credencial |
|---|---|---|---|---|
| Recuperar contraseña (email) | 1.1 | Código completo, pendiente SMTP | `COSTUMI_SMTP_HOST`, `COSTUMI_SMTP_PORT` (587), `COSTUMI_SMTP_USER`, `COSTUMI_SMTP_PASS`, `COSTUMI_EMAIL_FROM`, `COSTUMI_RESET_URL_BASE` (opcional) | El token se genera y persiste; el email se **registra en el log** en vez de enviarse. El endpoint funciona igual. |

## Notas
- **Recuperar contraseña**: endpoints `POST /api/v1/auth/olvide` (siempre 204, no revela si el correo existe) y `POST /api/v1/auth/restablecer` (token + nueva contraseña). El token se guarda **hasheado** (SHA-256), de un solo uso y con vencimiento (`costumi.email.recuperacion.duracion-minutos`, 60 por defecto). Adaptador: `EnviadorDeEmailSmtp` (busca `// TODO(credenciales)`).
