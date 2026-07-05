# Costumi — Estado del proyecto (PROGRESS)

> Se actualiza **al final de cada sesión**. Es lo primero que se lee (después de
> `CLAUDE.md`) para retomar sin perder el hilo. Regla: mueve ítems entre secciones,
> añade una entrada al registro de sesiones, **no borres el historial**.

## Fase actual
**Fase 2 — Módulo Identidad/tenant (auth y autorización).** Empresa (registro, ciclo de vida,
plazo) y Sucursal ya en `main`. Cerrado el cimiento de **auth por token** y la **autorización
por rol/tenant**, y **cerradas las dos deudas de seguridad**. Todo en la rama
`chore/scaffolding-modulith` (regla de Juan: nada a `main` sin su aprobación por PR).

## Pendiente de revisión (Juan sin recursos por el momento)
> Por acuerdo con el responsable, se siguió ejecutando en slices **sin esperar la revisión**.
> Lo de abajo está en la rama, **verde en CI**, a la espera de que Juan revise/mergee.
- **PR #7 — cierre de seguridad de auth** (sobre `main`, que ya tiene auth del PR #6):
  1. **Fail-fast del secreto JWT** en producción → cierra la deuda bloqueante.
  2. **Autorización por rol/tenant:** SUPERADMIN para ciclo de vida de Empresa y cola de
     pendientes; DUENO/ENCARGADO + dueño del tenant para alta de Sucursal → cierra la deuda de endpoints.
  3. **Bootstrap del SuperAdmin** por seed (auth usable en despliegue nuevo).
  4. **Catálogo — Categoría (RF-2.8)**: nuevo módulo `catalogo`; alta/listado de categorías
     **acotadas al tenant del token** (primer uso real del aislamiento multi-tenant; una empresa
     no ve las de otra). DUENO/ENCARGADO para crear.
  51 tests verdes en local.

## Próximo paso concreto
1. ✅ Andamiaje (mergeado a `main`) + check `build` requerido enganchado por Juan.
2. ✅ Módulo Identidad — rebanada 1: **auto-registro de Empresa** (nace PENDIENTE), `POST /api/v1/empresas` (PR #2).
3. ✅ Módulo Identidad — rebanada 2: **aprobar / rechazar / suspender / reactivar** Empresa (RF-15.3),
   endpoints `POST /{id}/{accion}`, errores en Problem Details (404/409) (PR #3).
4. ✅ Módulo Identidad — rebanada 3: **Sucursal** (1..N por Empresa) con `empresa_id` (RF-15.1);
   solo una empresa ACTIVA puede abrir sucursales (RF-15.4). `POST /api/v1/empresas/{id}/sucursales` (PR #4).
5. ✅ RF-15.4 (plazo de resolución): cola `GET /api/v1/empresas/pendientes` (SuperAdmin) con marca
   de **vencida** y plazo configurable (PR #5). ⬜ Falta la escalada/recordatorio automático (RF-11).
6. ✅ **Auth por token (RF-17.4/§5.6):** circuito base (login JWT HS256, `/me`, PR #6) + **autorización
   por rol/tenant** y **bootstrap del SuperAdmin** (PR #7). ⬜ Falta: refresh token, permisos granulares (RF-1.5).
7. 🟨 **Aislamiento multi-tenant (§5.4):** ✅ chequeo de tenant a nivel de endpoint (Sucursal, PR #7).
   ⬜ Falta el filtro **forzado** por `empresa_id` en un contexto de request (para todo módulo futuro).
8. ⬜ Auditoría del SuperAdmin (RF-15.5) — usando el actor del token.
9. 🟨 **Catálogo y taxonomía (RF-2.7)** — el más delicado. ✅ Categoría (PR #7). ⬜ Motor de
   etiquetas: `TipoEtiqueta` + `ValorEtiqueta` con interruptores (¿define variante?, ¿seleccionable
   por cliente?, ¿a qué categorías aplica?) (RF-2.7.1–2.7.7).

## Tablero de módulos
Estado: ⬜ sin empezar · 🟨 en curso · ✅ hecho

| Módulo | Rigor | Estado | Ref |
|---|---|---|---|
| Andamiaje + control anti-erosión (ArchUnit/Modulith/CI) | — | ✅ | §5.3 — mergeado a `main` (PR #1) |
| Identidad y tenant (Empresa/Sucursal/Usuario/permisos/auth) | Hexagonal | 🟨 | RF-1, RF-15, RF-17.4 — Empresa (PR #2/#3/#5), Sucursal (PR #4), auth+autorización (PR #6/#7). Falta refresh token y permisos granulares |
| Catálogo y taxonomía (etiquetas, categorías) | Hexagonal | 🟨 | RF-2.7 — Categoría + aislamiento tenant (PR #7); falta motor de etiquetas |
| Inventario y disponibilidad | Hexagonal | ⬜ | RF-2 |
| Pedidos / carrito | Hexagonal | ⬜ | RF-16 |
| Rentas | Hexagonal | ⬜ | RF-3 |
| Ventas / POS | Hexagonal | ⬜ | RF-4 |
| Pagos, caja y depósitos | Hexagonal | ⬜ | RF-6 |
| Devoluciones y multas | Hexagonal | ⬜ | RF-5 |
| Clientes | Simple | ⬜ | RF-7 |
| Empleados | Simple | ⬜ | RF-8 |
| Reportes | Simple | ⬜ | RF-9 |
| Notificaciones (WhatsApp / FCM) | Simple | ⬜ | RF-11 |
| Configuración de empresa | Simple | ⬜ | RF-12 |
| App cliente (marketplace) | — | ⬜ | RF-18 |

## Decisiones aceptadas
- **Decisión (2026-07-04, aprobada por Juan):** se acepta `reactivar` (SUSPENDIDA → ACTIVA)
  como acción del SuperAdmin aunque no figuraba en RF-15.3; se considera complemento natural
  de `suspender`. Pendiente reflejarlo en `BACKEND_REQUIREMENTS.md` (RF-15.3).

## Decisiones pendientes (resolver antes de tocar su tema)
- **Convención de nombres (a confirmar por Juan).** Se usó **lenguaje de dominio en español**
  también en métodos/puertos (`Empresa`, `EstadoEmpresa`, `registrar`, `aprobar`, `guardar`,
  `ejecutar`), interpretando el glosario §0 + "lenguaje de dominio en español" de CLAUDE.md.
  Si Juan prefiere inglés para lo no-dominio (`save`/`findById`/`execute`), se renombra
  (es mecánico). Decidir antes de crecer el módulo.
- Pasarela de pago concreta (cuando se active el pago en línea, RF-6.11).
- UX de descubrimiento del marketplace (búsqueda, cercanía, filtros, reseñas — RF-18).

## Deuda / a sanear
- ✅ **RESUELTO (PR #7)** — ~~Secreto JWT por defecto bloqueante en producción~~: ahora hay fail-fast
  en perfil `prod` si el secreto falta o es el default (`ValidacionSecretoJwt`). Pendiente al desplegar:
  **setear `COSTUMI_JWT_SECRET` por entorno**.
- ✅ **RESUELTO (PR #7)** — ~~Endpoints sin control de rol/tenant~~: ciclo de vida de Empresa y cola de
  pendientes exigen SUPERADMIN; alta de Sucursal exige DUENO/ENCARGADO + dueño del tenant.
- (vacío por ahora)

## A re-verificar cada sesión (invariantes)
- ¿ArchUnit y Modulith siguen en verde?
- ¿Toda tabla nueva lleva `empresa_id` y se filtra por tenant?
- ¿El dominio de los módulos hexagonal sigue sin framework?
- ¿La API solo expone DTOs y el contrato OpenAPI está al día?

## Registro de sesiones
- **2026-07-04 (h)** — Iniciado el módulo **Catálogo/taxonomía (RF-2.7)**: **Categoría (RF-2.8)** con
  aislamiento multi-tenant (scope por `empresa_id` del token; una empresa no ve las de otra). Dominio
  puro (archivar/renombrar, RF-2.7.6), puertos, servicio, JPA (`V4__crear_categoria.sql`, índice único
  parcial por empresa entre activas), `POST/GET /api/v1/categorias` (DUENO/ENCARGADO). Build local
  **verde (51 tests)**. En **PR #7** (run autónomo, pendiente de revisión). Sigue el motor de etiquetas.
- **2026-07-04 (g)** — Run largo autónomo (Juan sin recursos), 3 slices de cierre de seguridad sobre
  `main`+auth: (1) **fail-fast del secreto JWT** en perfil `prod`; (2) **autorización por rol/tenant** —
  SUPERADMIN para ciclo de vida de Empresa y cola de pendientes, DUENO/ENCARGADO + dueño del tenant para
  Sucursal (401 sin token, 403 por rol/tenant); tests de integración actualizados para autenticar;
  (3) **bootstrap del SuperAdmin** por seed (configurable por entorno). **Cerradas las 2 deudas de
  seguridad.** Build local **verde (43 tests)**. En **PR #7**, pendiente de revisión de Juan.
- **2026-07-04 (f)** — Módulo **Identidad/tenant**, rebanada 5: **auth por token (RF-17.4/§5.6, base)**.
  Spring Security + OAuth2 Resource Server; **JWT HS256** (secreto configurable, override por
  `COSTUMI_JWT_SECRET`). Dominio `Usuario` + `Rol` (SUPERADMIN + plantillas RF-1.3) con invariante
  "SuperAdmin sin empresa". Puertos `UsuarioRepository` + `EmisorDeTokens`; adaptador JWT (`JwtEncoder`).
  `POST /api/v1/auth/login` emite token con `empresa+rol`; `GET /api/v1/auth/me` protegido. Migración
  `V3__crear_usuario.sql` (`empresa_id` nulo para SuperAdmin). API **stateless**, CSRF off, resto
  `permitAll` (la deuda de autorización sigue abierta a propósito). Build local **verde (34 tests)**. PR #6.
  Pendiente: refresh token, permisos granulares (RF-1.5), bootstrap del SuperAdmin por seed, y blindar
  los endpoints de la deuda.
- **2026-07-04 (e)** — Módulo **Identidad/tenant**, rebanada 4: **plazo de resolución (RF-15.4)**.
  `Empresa.solicitudVencida(plazo, ahora)` en dominio; plazo configurable
  `costumi.empresa.plazo-resolucion-dias` (default 2). Cola `GET /api/v1/empresas/pendientes` (para
  el SuperAdmin) que marca las **vencidas**. Build local **verde (26 tests)**. PR #5. Pendiente:
  escalada/recordatorio automático (RF-11) y restringir el endpoint a rol SuperAdmin cuando exista auth.
- **2026-07-04 (d)** — Módulo **Identidad/tenant**, rebanada 3: **Sucursal (RF-15.1)**. Entidad
  `Sucursal` anclada a Empresa con **`empresa_id`** (primera tabla hija de negocio, con FK a
  `empresa` e índice por tenant), migración `V2__crear_sucursal.sql`. Regla RF-15.4 aplicada:
  solo una empresa **ACTIVA** puede abrir sucursales (`EmpresaNoOperativa` → 409). Endpoint anidado
  `POST /api/v1/empresas/{empresaId}/sucursales`. Build local **verde (22 tests)**. PR #4.
- **2026-07-04 (c)** — Módulo **Identidad/tenant**, rebanada 2: **ciclo de vida de la Empresa
  por el SuperAdmin (RF-15.3)**. Casos de uso `aprobar/rechazar/suspender/reactivar`, endpoints
  `POST /api/v1/empresas/{id}/{accion}`, `ManejadorDeErrores` con Problem Details (404 no
  encontrada, 409 transición inválida). Build local **verde (16 tests)**. Andamiaje mergeado a
  `main` por Juan; módulo en el **PR #3**. Pendiente: aislamiento multi-tenant (§5.4) y auditoría
  (RF-15.5). Juan pidió continuar por §7.
- **2026-07-04 (b)** — Módulo **Identidad/tenant**, rebanada 1: **auto-registro de Empresa
  (RF-15.2)** end-to-end sobre `chore/scaffolding-modulith`. Dominio puro `Empresa` +
  máquina de estados `EstadoEmpresa` (PENDIENTE→ACTIVA→SUSPENDIDA/RECHAZADA, RF-15.3);
  puerto `EmpresaRepository`; caso de uso `RegistrarEmpresa`; adaptador JPA; `EmpresaController`
  (`POST /api/v1/empresas`, 201, DTOs); migración `V1__crear_empresa.sql`. Tests: 6 de dominio
  (sin BD) + 2 de integración (Testcontainers). Build local **verde** (12 tests), ArchUnit y
  Modulith en verde con código real. Nota: la tabla `empresa` **es la raíz de tenant**, su `id`
  es el `empresa_id`, por eso no lleva columna `empresa_id` (las tablas hijas sí la llevarán).
- **2026-07-04 (a)** — Andamiaje del backend en PR #1: Spring Boot 3.5.16 + PostgreSQL + Flyway
  (Maven, Java 21), control anti-erosión (ArchUnit + Spring Modulith), CI en GitHub Actions
  (check `build`), plantilla de PR. CI verde. Elegido Maven. Regla de Juan: todo va a la rama,
  nada a `main` sin su aprobación.
- **2026-07-03** — Cerrada la fase de planeación. `BACKEND_REQUIREMENTS.md` completo
  (RF-0…18, arquitectura §5, comunicación §5.6, offline §5.7) y revisado (preámbulo,
  glosario §0, numeración de RF-2 normalizada, token/cabecera alineados). Creado el
  sistema de gobernanza (`CLAUDE.md` + este archivo) y el modelo de colaboración
  (`COLLABORATION.md`: constructor con Claude Code Max + revisor vía PRs en GitHub).
  Siguiente: crear el repo, commitear los documentos en la raíz, y montar el andamiaje
  del backend con CI (build + tests + ArchUnit + Modulith) y branch protection.
