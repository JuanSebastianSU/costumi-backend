# Costumi — Colaboración y revisión (equipo)

Cómo trabajamos dos personas sobre un mismo repositorio de GitHub.

## Roles
- **Constructor** (compañero, con Claude Code Max): escribe y ejecuta el código en ramas.
- **Revisor / saneador** (tú): revisa y aprueba vía Pull Requests en GitHub; **no ejecuta
  el código localmente**.

## Dónde viven las reglas
Los tres documentos (`CLAUDE.md`, `BACKEND_REQUIREMENTS.md`, `PROGRESS.md`) viven en la
**raíz del repositorio**, versionados junto al código. **No se pasan por chat: se
commitean.** Así:
- El Claude Code del constructor **carga `CLAUDE.md` automáticamente** en cada sesión.
- Ambos ven siempre la misma versión (la del repo); los cambios quedan en el historial.

## Flujo por cada cambio
1. El constructor trabaja en una **rama** y hace **rebanadas pequeñas** (un caso de uso).
2. Abre un **Pull Request** hacia `main`.
3. **CI corre solo** en el PR: build + tests + ArchUnit + Spring Modulith.
4. Tú revisas en GitHub: **CI verde**, cumple la Definition of Done (plantilla del PR) y
   respeta las reglas de `CLAUDE.md`.
5. Se **mergea solo si**: CI verde **+** tu aprobación.
6. El constructor actualiza `PROGRESS.md` (en el mismo PR o en uno de cierre).

## Lo que hace que tu revisión sea real (sin correr el código)
- **CI obligatorio (GitHub Actions):** como no ejecutas localmente, CI es tu "ojos". Si
  CI falla, no se mergea. Punto.
- **Branch protection en `main`:** exige (a) CI en verde y (b) 1 aprobación (la tuya)
  antes de permitir merge. Convierte tu revisión en una compuerta obligatoria.
- **PRs pequeños:** si un PR es enorme, pídelo dividido. Revisar 300 líneas es posible;
  revisar 3000 no.

## Qué revisar en cada PR (saneación)
- ¿**CI verde**? (build, tests, ArchUnit, Modulith)
- ¿Respeta la arquitectura de `CLAUDE.md`? (dominio sin framework, DTOs en la frontera,
  `empresa_id` en tablas nuevas, migración Flyway incluida)
- ¿Está dentro del alcance de `BACKEND_REQUIREMENTS.md`, o **inventó** algo?
- ¿Actualizó `PROGRESS.md`?
- Tip: usa tu propio Claude Code en modo revisión (`/code-review` sobre el PR) sin
  ejecutar nada.

## Setup inicial (una vez, en GitHub)
- [ ] Crear el repo y commitear los tres documentos (`CLAUDE.md`,
      `BACKEND_REQUIREMENTS.md`, `PROGRESS.md`) en la **raíz**.
- [ ] Configurar **GitHub Actions** (CI: build + tests + ArchUnit + Modulith).
- [ ] Activar **branch protection** en `main`: requerir CI + 1 review.
- [ ] Añadir la plantilla de PR en `.github/pull_request_template.md` (abajo).

---

## Plantilla de Pull Request
Guarda esto como `.github/pull_request_template.md` en el repo del backend:

```markdown
## Qué hace este PR
<!-- 1-2 líneas. Enlaza el/los RF que cubre (ej. RF-15.2). -->

## Definición de "hecho"
- [ ] Rebanada pequeña (un caso de uso / módulo).
- [ ] Puertos definidos; dominio hexagonal sin Spring/JPA/web.
- [ ] DTOs en la frontera (no se exponen entidades de persistencia).
- [ ] `empresa_id` en toda tabla nueva y filtrado por tenant.
- [ ] Migración Flyway incluida.
- [ ] Tests de dominio pasan sin BD; ArchUnit y Modulith en verde (CI).
- [ ] Contrato OpenAPI actualizado si cambió la API.
- [ ] `PROGRESS.md` actualizado.

## Alcance
- [ ] Está dentro de `BACKEND_REQUIREMENTS.md`. Si no, lo anoté como decisión en `PROGRESS.md`.

## Notas para el revisor
<!-- Qué mirar con atención, dudas, decisiones tomadas. -->
```
