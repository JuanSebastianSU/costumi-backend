# Costumi — Reglas de implementación (léeme al iniciar cada sesión)

Este archivo manda sobre **cómo** se construye Costumi. Claude Code lo carga
automáticamente al inicio de cada sesión. Si vas a escribir código, lee esto completo
primero.

## Los tres documentos (léelos en este orden al empezar)
1. `CLAUDE.md` (este) — reglas y protocolo de trabajo.
2. `BACKEND_REQUIREMENTS.md` — **el QUÉ** (alcance y requerimientos). Fuente de verdad.
   **No lo cambies sin una decisión explícita del responsable**; si la hay, aplícala y
   anótala en `PROGRESS.md`.
3. `PROGRESS.md` — **el estado vivo**. Léelo para saber dónde vamos; actualízalo al
   terminar.

> Nota: cuando exista el repositorio del backend, estos tres archivos viven en su raíz
> (CLAUDE.md solo se auto-carga desde el proyecto que abres en Claude Code).

## Qué es Costumi (una frase)
Marketplace multi-tenant de renta y venta de disfraces: **Plataforma → Empresa →
Sucursal**. Backend Java + Spring Boot, PostgreSQL, cliente Android en Kotlin. Detalle en
`BACKEND_REQUIREMENTS.md`.

## Stack bloqueado (no cambiar sin decisión explícita)
- Backend: **Java + Spring Boot**, monolito **modular**, hexagonal **pragmático**.
- BD: **PostgreSQL**, code-first (JPA/Hibernate) + migraciones **Flyway**. Nunca
  `ddl-auto=update`.
- Multi-tenant: **esquema compartido + `empresa_id`**, filtrado forzado por el token.
- API: REST/JSON, **contract-first con OpenAPI**; el cliente Kotlin se **genera** del
  contrato.
- Cliente Android: Retrofit/OkHttp → repositorio → Room → UI (ver §5.6 del spec).

## Reglas de arquitectura (NO negociables — las verifica el build)
- Estructura de paquetes **idéntica** por módulo: `dominio/`, `aplicacion/`,
  `adaptadores/entrada/`, `adaptadores/salida/`.
- El `dominio` de un módulo hexagonal **NO importa** Spring, JPA ni web. Cero anotaciones
  de framework ahí.
- Las dependencias apuntan hacia adentro: **adaptadores → puertos → dominio**. Nunca al
  revés.
- Un módulo **no** usa clases internas de otro módulo; solo su API pública / eventos.
- Toda tabla de negocio lleva **`empresa_id`** (y `sucursal_id` donde aplique) y se filtra
  por tenant.
- **Nunca** se exponen entidades JPA/dominio por la API: siempre **DTOs**.
- Operaciones de dinero y confirmaciones: **idempotentes** (clave de idempotencia).
- Taxonomía = **datos, no esquema**: nunca crear tablas/columnas al vuelo por etiquetas.
- Respeta el **nivel de rigor por módulo** de §5.2 del spec (hexagonal vs. simple).

## Cómo trabajar (protocolo por tarea)
1. Trabaja en **rebanadas verticales pequeñas** (un caso de uso / un módulo a la vez).
2. Lógica de dominio: **test primero** (el dominio se prueba sin BD ni Spring).
3. Antes de dar algo por hecho, **haz pasar**: tests + ArchUnit + Spring Modulith.
4. Incluye la **migración Flyway** con cualquier cambio de esquema.
5. Al terminar, **actualiza `PROGRESS.md`** (qué hiciste, qué sigue, qué quedó pendiente).

## Definición de "hecho" (Definition of Done) — no marcar completo sin esto
- [ ] Puertos definidos (módulos hexagonal); sin fugas de framework al dominio.
- [ ] Tests de dominio pasan sin BD; ArchUnit y Modulith en verde.
- [ ] Migración Flyway incluida y aplicada.
- [ ] `empresa_id` aplicado y verificado (no se pueden leer datos de otra empresa).
- [ ] DTOs en la frontera; contrato OpenAPI actualizado si cambió la API.
- [ ] `PROGRESS.md` actualizado.

## NUNCA
- Cambiar el stack o la arquitectura por conveniencia momentánea.
- Meter reglas de negocio en controllers o adaptadores.
- Saltarte o desactivar los tests de arquitectura "para que pase".
- Exponer entidades de persistencia por la API.
- **Inventar requerimientos.** Si algo no está en `BACKEND_REQUIREMENTS.md`: pregunta, o
  anótalo como decisión pendiente en `PROGRESS.md`. No lo asumas.
- Dejar la sesión sin actualizar `PROGRESS.md`.

## Convenciones de código
- Código, nombres e identificadores en **inglés**; texto visible y lenguaje de dominio en
  **español** (usa el glosario de §0 del spec: Empresa, Sucursal, Prenda, GrupoDeStock,
  Disfraz, Slot, Pedido…).
- Commits pequeños y descriptivos, uno por rebanada.
