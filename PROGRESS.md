# Costumi — Estado del proyecto (PROGRESS)

> Se actualiza **al final de cada sesión**. Es lo primero que se lee (después de
> `CLAUDE.md`) para retomar sin perder el hilo. Regla: mueve ítems entre secciones,
> añade una entrada al registro de sesiones, **no borres el historial**.

## Fase actual
**Fase 2 — Primer módulo (Identidad/tenant).** Andamiaje + CI listos y verdes. Arrancado
el módulo **Identidad y tenant** con la primera rebanada vertical: **auto-registro de
Empresa (RF-15.2)** end-to-end. Todo el trabajo vive en la rama `chore/scaffolding-modulith`
(regla de Juan: nada va a `main` sin su aprobación por PR).

## Próximo paso concreto
1. ✅ Andamiaje (mergeado a `main`) + check `build` requerido enganchado por Juan.
2. ✅ Módulo Identidad — rebanada 1: **auto-registro de Empresa** (nace PENDIENTE), `POST /api/v1/empresas` (PR #2).
3. ✅ Módulo Identidad — rebanada 2: **aprobar / rechazar / suspender / reactivar** Empresa (RF-15.3),
   endpoints `POST /{id}/{accion}`, errores en Problem Details (404/409) (PR #2).
4. ⬜ **Aislamiento multi-tenant (§5.4, RF-15.1):** `empresa_id` en el contexto de request +
   filtro forzado, para que sea imposible leer datos de otra empresa. Depende de auth (RF-17.4).
5. ⬜ Auditoría de acciones del SuperAdmin (RF-15.5) y plazo de resolución de 2 días (RF-15.4).
6. ⬜ **Sucursal** (1..N por Empresa, con `empresa_id`), luego Usuario/roles/permisos + auth (RF-1, RF-17.4).

## Tablero de módulos
Estado: ⬜ sin empezar · 🟨 en curso · ✅ hecho

| Módulo | Rigor | Estado | Ref |
|---|---|---|---|
| Andamiaje + control anti-erosión (ArchUnit/Modulith/CI) | — | ✅ | §5.3 — verde en CI (PR pendiente de merge) |
| Identidad y tenant (Empresa/Sucursal/Usuario/permisos/auth) | Hexagonal | 🟨 | RF-1, RF-15, RF-17.4 — auto-registro de Empresa hecho |
| Catálogo y taxonomía (etiquetas, categorías) | Hexagonal | ⬜ | RF-2.7 — el más delicado |
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

## Decisiones pendientes (resolver antes de tocar su tema)
- **Convención de nombres (a confirmar por Juan).** Se usó **lenguaje de dominio en español**
  también en métodos/puertos (`Empresa`, `EstadoEmpresa`, `registrar`, `aprobar`, `guardar`,
  `ejecutar`), interpretando el glosario §0 + "lenguaje de dominio en español" de CLAUDE.md.
  Si Juan prefiere inglés para lo no-dominio (`save`/`findById`/`execute`), se renombra
  (es mecánico). Decidir antes de crecer el módulo.
- Pasarela de pago concreta (cuando se active el pago en línea, RF-6.11).
- UX de descubrimiento del marketplace (búsqueda, cercanía, filtros, reseñas — RF-18).

## Deuda / a sanear
- (vacío por ahora)

## A re-verificar cada sesión (invariantes)
- ¿ArchUnit y Modulith siguen en verde?
- ¿Toda tabla nueva lleva `empresa_id` y se filtra por tenant?
- ¿El dominio de los módulos hexagonal sigue sin framework?
- ¿La API solo expone DTOs y el contrato OpenAPI está al día?

## Registro de sesiones
- **2026-07-04 (c)** — Módulo **Identidad/tenant**, rebanada 2: **ciclo de vida de la Empresa
  por el SuperAdmin (RF-15.3)**. Casos de uso `aprobar/rechazar/suspender/reactivar`, endpoints
  `POST /api/v1/empresas/{id}/{accion}`, `ManejadorDeErrores` con Problem Details (404 no
  encontrada, 409 transición inválida). Build local **verde (16 tests)**. Andamiaje mergeado a
  `main` por Juan; módulo en el **PR #2**. Pendiente: aislamiento multi-tenant (§5.4) y auditoría
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
