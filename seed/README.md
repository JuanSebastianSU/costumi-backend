# Seed de datos de prueba — Costumi (Ecuador, 2 tiendas)

Datos "de verdad" para probar toda la app. Diseño en [`../PLAN_SEED_DATOS_PRUEBA.md`](../PLAN_SEED_DATOS_PRUEBA.md).

## Qué carga
- **Tienda 1 — Fiesta & Fantasía (Quito)**: piensa por *Ocasión + Temática*. 2 sucursales (multi-sucursal).
  11 categorías, 6 tipos de etiqueta, 15 prendas, 64 grupos de stock, 8 disfraces (modulares por slots),
  5 clientes, 8 ventas, 8 rentas.
- **Tienda 2 — El Baúl del Disfraz (Cuenca)**: piensa por *Personaje + Licencia + Línea*. 1 sucursal.
  7 categorías, 6 tipos de etiqueta, 12 prendas, 21 grupos, 6 disfraces (disfraz-completo), 3 clientes,
  4 ventas, 5 rentas.
- Operaciones en **todos los estados**: rentas RESERVADA/ACTIVA/VENCIDA/CANCELADA/CERRADA + multa + daño +
  pérdida; ventas CONFIRMADA/PARCIALMENTE_DEVUELTA/DEVUELTA; pagos (efectivo/tarjeta/mixto); reembolsos
  PENDIENTE/APROBADO/RECHAZADO; caja (turno cerrado + turno abierto). Fechas repartidas en ~30 días.

## Archivos (orden de ejecución)
1. `01_identidad.sql` — empresas, config, sucursales, empleados, membresías y **permisos** (SQL).
2. `02_catalogo.mjs` — categorías, etiquetas, prendas+stock, disfraces, clientes (API). Idempotente.
3. `03_operaciones.mjs` — rentas/ventas/devoluciones/multas/reembolsos/caja/pagos (API). NO idempotente.
4. `04_backdating.sql` — reparte fechas para los reportes (SQL).
5. `05_fotos.mjs` — sube fotos reales (Pexels) + avatares a **S3** vía la API (prendas, disfraces,
   sucursales, logo/portada de empresa, foto de usuario). Requiere S3 configurado en Railway
   (`COSTUMI_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) y bucket de lectura
   pública. Idempotente (re-subir reemplaza). El campo multipart es `archivo`.
- `00_reset_catalogo.sql` — trunca catálogo+operaciones y conserva identidad (para re-correr 02+03).

## Cómo ejecutar
Requisitos: Docker (para `psql` vía imagen `postgres`), Node 18+. La `DATABASE_PUBLIC_URL` de Railway y la
`BASE` del backend se pasan por entorno (no van en los archivos).

```bash
export DBURL="postgresql://postgres:PASS@HOST.proxy.rlwy.net:PORT/railway"
export BASE="https://just-upliftment-production-cb1f.up.railway.app"
psql() { docker run --rm -i -e PGCONNECT_TIMEOUT=15 postgres:16-alpine psql "$DBURL" "$@"; }

# 1) identidad
psql -f - < seed/01_identidad.sql
# (re-seed limpio de catálogo+operaciones)
psql -f - < seed/00_reset_catalogo.sql
# 2) catálogo   3) operaciones   4) backdating
node seed/02_catalogo.mjs
node seed/03_operaciones.mjs
psql -f - < seed/04_backdating.sql
```

> Para volver a sembrar desde cero: `00_reset_catalogo.sql` → `02` → `03` → `04` (la identidad se conserva).
> Para vaciar TODO: ver el truncate general (conserva `flyway_schema_history`).

## Credenciales (todas password `Passw0rd!`)
| Tienda | Rol | Email |
|---|---|---|
| Fiesta & Fantasía (Quito) | DUEÑO | `dueno@costumi.co` |
| | ENCARGADO (Cumbayá) | `ana@ff.ec` |
| | MOSTRADOR (Centro) | `carlos@ff.ec` |
| | BODEGA | `beto@ff.ec` |
| El Baúl del Disfraz (Cuenca) | DUEÑO | `dueno@baul.ec` |
| | MOSTRADOR | `sofia@baul.ec` |

Permisos de ejemplo (overrides): Ana sin invitar/editar-permisos/config · Carlos con reportes y sin
devolver-ventas · Beto con etiquetas y sin ajustar-stock · Sofía con gestionar-prendas y sin cerrar-rentas.
