# Costumi — Estado del proyecto (PROGRESS)

> Se actualiza **al final de cada sesión**. Es lo primero que se lee (después de
> `CLAUDE.md`) para retomar sin perder el hilo. Regla: mueve ítems entre secciones,
> añade una entrada al registro de sesiones, **no borres el historial**.

## Fase actual
**Fase 1 — Andamiaje.** Montado el esqueleto del backend con el control anti-erosión
(ArchUnit + Spring Modulith) y CI en GitHub Actions. En PR, pendiente de revisión/merge.
Aún **sin módulos de negocio** (el primero va después de enganchar el CI en el ruleset).

## Próximo paso concreto
1. ✅ Proyecto Spring Boot + PostgreSQL + Flyway (Maven, Java 21).
2. ✅ Convención de paquetes por módulo documentada (`docs/PLANTILLA_MODULO.md`, §5.2/§5.3).
3. ✅ ArchUnit + Spring Modulith + CI (GitHub Actions) que **fallan el build** ante violaciones.
4. ⏳ Cuando el CI corra **verde** una vez en el PR de andamiaje, el revisor engancha el
   check **`build`** (workflow `CI`) como **requerido** en el ruleset de `main`.
5. ⬜ Primer módulo según §7: **base técnica / multi-tenant** (Empresa, estados, `empresa_id`).

## Tablero de módulos
Estado: ⬜ sin empezar · 🟨 en curso · ✅ hecho

| Módulo | Rigor | Estado | Ref |
|---|---|---|---|
| Andamiaje + control anti-erosión (ArchUnit/Modulith/CI) | — | ⬜ | §5.3 — va primero |
| Identidad y tenant (Empresa/Sucursal/Usuario/permisos/auth) | Hexagonal | ⬜ | RF-1, RF-15, RF-17.4 |
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
- **2026-07-03** — Cerrada la fase de planeación. `BACKEND_REQUIREMENTS.md` completo
  (RF-0…18, arquitectura §5, comunicación §5.6, offline §5.7) y revisado (preámbulo,
  glosario §0, numeración de RF-2 normalizada, token/cabecera alineados). Creado el
  sistema de gobernanza (`CLAUDE.md` + este archivo) y el modelo de colaboración
  (`COLLABORATION.md`: constructor con Claude Code Max + revisor vía PRs en GitHub).
  Siguiente: crear el repo, commitear los documentos en la raíz, y montar el andamiaje
  del backend con CI (build + tests + ArchUnit + Modulith) y branch protection.
