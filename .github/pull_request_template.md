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
