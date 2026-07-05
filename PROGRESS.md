# Costumi — Estado del proyecto (PROGRESS)

> Se actualiza **al final de cada sesión**. Es lo primero que se lee (después de
> `CLAUDE.md`) para retomar sin perder el hilo. Regla: mueve ítems entre secciones,
> añade una entrada al registro de sesiones, **no borres el historial**.

## Fase actual
**Fase 4 — Cierre del backend (handoff `CIERRE_BACKEND.md` de Juan).** Los 14 módulos de §7 tienen su
1ª rebanada (ancho pero **delgado**). Ahora se cierra el backend en **3 tandas por dependencia**, modo
"RUN GRANDE" (por tiempo, sin revisión rebanada a rebanada):
- **Tanda 1 = P0 (núcleo del modelo) + P1 (seguridad/frontera).** ⛔ **CHECKPOINT al terminar: PARAR y pedir
  revisión a Juan ANTES de construir encima** (si el modelo núcleo o §5.4 quedan mal, todo lo de arriba se rehace).
- **Tanda 2 = P2+P3** (ciclo operativo + dinero) · **Tanda 3 = P4+P5** (resto).
- Reglas: **1 commit por feature**, **tests de dominio por cada feature**, **§5.4 temprano**, ambiguo → decisión aquí.
- **En curso:** Tanda 1 en **PR #8** (`chore/scaffolding-modulith` → `main`; **PR #7 ya lo mergeó Juan**).
  Hecho en la tanda: (1) **§5.4 base** — `ContextoDeTenant`; (2) **motor de variantes real** — `GrupoDeStock`
  = combinación real de valores de etiqueta; (3) **Prenda↔etiquetas (Capa 2)** — la prenda porta sus valores
  de etiqueta de clasificación, validados contra la taxonomía del tenant. **Falta en Tanda 1:** taxonomía
  completa (tipo↔categoría, por-slot, rename propaga, seed), Disfraz/Slot + disponibilidad derivada RF-2.4,
  `X-Sucursal-Id`, tooling OpenAPI contract-first. Al terminar → ⛔ CHECKPOINT.

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
  5. **Catálogo — motor de etiquetas (RF-2.7.1/2.7.2)**: `TipoEtiqueta` (con interruptores
     ¿define variante? / ¿seleccionable por cliente?) + `ValorEtiqueta`; `POST/GET /api/v1/tipos-etiqueta`
     y `.../{id}/valores`, acotado al tenant (404 al tocar un tipo de otra empresa).
  6. **Inventario — Prenda (RF-2.1/2.10)**: nuevo módulo `inventario`; ítem con categoría, tipo
     (renta/venta/ambos) y precios (con reglas de precio en el dominio). `POST/GET /api/v1/prendas`,
     acotado al tenant.
  7. **Inventario — GrupoDeStock (RF-2.2/2.11)**: conteo por estado (disponibles/dañadas/en limpieza/
     perdidas) y **movimientos entre estados**; `POST/GET /api/v1/prendas/{id}/grupos-stock` y
     `POST /api/v1/grupos-stock/{id}/mover`, acotado al tenant.
  8. **Clientes (RF-7)**: nuevo módulo `clientes`; ficha (teléfono/correo/documento/dirección),
     **búsqueda por texto** y **lista negra** (RF-7.3); `POST/GET /api/v1/clientes` (?buscar=) y
     `POST /api/v1/clientes/{id}/lista-negra`, acotado al tenant.
  9. **Carrito (RF-16)**: nuevo módulo `pedidos`; carrito persistente **segmentado por (empresa ×
     sucursal × cliente × tipo)** con líneas; agregar suma cantidades; renta y venta en carritos
     separados. `POST /api/v1/carritos/items`, `GET /api/v1/carritos`.
  10. **Renta (RF-3)**: nuevo módulo `rentas`; crear renta (fechas, **importe = precio × días**, depósito),
      máquina de estados RESERVADA→ACTIVA→DEVUELTA→CERRADA (+ CANCELADA) y detección de vencidas.
      `POST /api/v1/rentas`, `GET /api/v1/rentas`, `POST /api/v1/rentas/{id}/{entregar|devolver|cerrar|cancelar}`.
  11. **Devolución (RF-5)**: nuevo módulo `devoluciones`; **checklist por pieza** (¿llegó? + estado) y
      **liquidación del depósito** (garantía − daños − recargos = remanente, sin bajar de 0).
      `POST/GET /api/v1/devoluciones`.
  12. **Venta/POS (RF-4)**: nuevo módulo `ventas`; venta con líneas **a nombre del empleado del token**,
      descuento y **total** (subtotal − descuento); cliente opcional. `POST/GET /api/v1/ventas`.
  13. **Pagos (RF-6)**: nuevo módulo `pagos`; pago ligado a renta/venta, método (efectivo/tarjeta/
      transferencia) y **clave de idempotencia** (no duplica cobros, RF-17.6). `POST/GET /api/v1/pagos`.
  14. **Reportes (RF-9)**: nuevo módulo `reportes` (solo lectura); **resumen de ingresos** por renta/venta
      (JdbcClient sobre los pagos), restringido a DUENO/ENCARGADO. `GET /api/v1/reportes/ingresos`.
  15. **Configuración (RF-12)**: nuevo módulo `configuracion`; **interruptores de módulos** por empresa
      (conteo de stock, multas, multi-sucursal, pago en línea), defaults sensatos. `GET/PUT /api/v1/configuracion`.
  16. **Notificaciones (RF-11)**: nuevo módulo `notificaciones`; envío por canal (WhatsApp/FCM/EMAIL) vía
      adaptador (log por ahora), estados PENDIENTE→ENVIADA. `POST/GET /api/v1/notificaciones`.
  17. **Marketplace / App cliente (RF-18.1/RF-15.6)**: nuevo módulo `marketplace` (lectura, público);
      descubrimiento de empresas **ACTIVAS**. `GET /api/v1/marketplace/empresas`.
  **135 tests verdes en local. Todo el listado de módulos de §7 tiene su primera rebanada.**

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
9. 🟨 **Catálogo y taxonomía (RF-2.7)** — el más delicado. ✅ Categoría + `TipoEtiqueta`/`ValorEtiqueta`
   con interruptores (PR #7). ⬜ Falta: aplicabilidad tipo↔categoría (RF-2.7.2), ciclo de vida
   archivar/renombrar por API (RF-2.7.6), y el **GrupoDeStock** (combinación de valores, RF-2.7.3/2.7.4)
   — este último ya es parte de Inventario (RF-2).
10. 🟨 **Inventario y disponibilidad (RF-2)** — ✅ Prenda + GrupoDeStock (conteo por estado + movimientos).
    ⬜ Falta: combinación de valores de etiqueta que define la variante (RF-2.7.3), y disponibilidad
    derivada del Disfraz por slots (RF-2.4) — esto último ya es el módulo de Disfraz (capa 3).
11. ✅ **Clientes (RF-7)**: ficha + búsqueda + lista negra (adelantado, es prerequisito del Carrito).
12. ✅ **Carrito (RF-16)**: persistente y segmentado por (sucursal × cliente × tipo), renta/venta separados.
13. ✅ **Renta (RF-3)**: crear (fechas/importe/depósito) + máquina de estados + vencidas.
14. ✅ **Devolución (RF-5)**: checklist por pieza + liquidación del depósito.
15. ✅ **Venta/POS (RF-4)**: venta con líneas, descuento y total, a nombre del empleado.
16. ✅ **Pagos (RF-6)**: pago ligado a renta/venta, métodos e idempotencia. ⬜ Falta caja/turno y corte (RF-6.3/6.10).
17. ✅ **Reportes (RF-9)**: resumen de ingresos por renta/venta (read-model). ⬜ Falta ganancia, filtros y export.
18. ✅ **Notificaciones (RF-11)** · ✅ **Configuración (RF-12)** · ✅ **Marketplace/App cliente (RF-18.1)**.
19. 🎉 **Listado de módulos de §7 completo** (una primera rebanada vertical, verde y multi-tenant por módulo).
    **Siguiente fase — profundizar:** completar cada módulo (ver los "Falta" del Tablero y del registro), y las
    piezas transversales: aislamiento multi-tenant **forzado** (filtro por contexto, §5.4), **eventos de dominio**
    (devolución→multa, venta→baja de stock, §5.5), **OpenAPI contract-first** (§5.6) y **offline/outbox** (§5.7).

## Tablero de módulos
Estado: ⬜ sin empezar · 🟨 en curso · ✅ hecho

| Módulo | Rigor | Estado | Ref |
|---|---|---|---|
| Andamiaje + control anti-erosión (ArchUnit/Modulith/CI) | — | ✅ | §5.3 — mergeado a `main` (PR #1) |
| Identidad y tenant (Empresa/Sucursal/Usuario/permisos/auth) | Hexagonal | 🟨 | RF-1, RF-15, RF-17.4 — Empresa (PR #2/#3/#5), Sucursal (PR #4), auth+autorización (PR #6/#7). Falta refresh token y permisos granulares |
| Catálogo y taxonomía (etiquetas, categorías) | Hexagonal | 🟨 | RF-2.7 — Categoría + TipoEtiqueta/ValorEtiqueta (PR #7); falta aplicabilidad tipo↔categoría y GrupoDeStock |
| Inventario y disponibilidad | Hexagonal | 🟨 | RF-2 — Prenda + GrupoDeStock (PR #7); falta variante por etiquetas y disponibilidad derivada |
| Pedidos / carrito | Hexagonal | 🟨 | RF-16 — carrito segmentado + líneas (PR #7); falta confirmar/checkout y offline |
| Rentas | Hexagonal | 🟨 | RF-3 — crear + importe + estados (PR #7); falta disponibilidad por fechas y extensión |
| Ventas / POS | Hexagonal | 🟨 | RF-4 — venta con líneas + descuento + total (PR #7); falta descuento de stock y comprobante |
| Pagos, caja y depósitos | Hexagonal | 🟨 | RF-6 — pago ligado + método + idempotencia (PR #7); falta caja/turno y corte |
| Devoluciones y multas | Hexagonal | 🟨 | RF-5 — checklist + liquidación (PR #7); falta multas auto y actualizar inventario |
| Clientes | Simple | 🟨 | RF-7 — ficha + búsqueda + lista negra (PR #7); falta historial (RF-7.2) |
| Empleados | Simple | ⬜ | RF-8 |
| Reportes | Simple (lectura) | 🟨 | RF-9 — resumen de ingresos (PR #7); falta ganancia, filtros y export |
| Notificaciones (WhatsApp / FCM) | Simple (adaptador) | 🟨 | RF-11 — envío por canal (log) + estados (PR #7); falta WhatsApp/FCM reales y recordatorios automáticos |
| Configuración de empresa | Simple | 🟨 | RF-12 — interruptores de módulos (PR #7); falta aplicarlos en cada módulo |
| App cliente (marketplace) | — | 🟨 | RF-18 — descubrimiento de empresas ACTIVAS (PR #7); falta catálogo/checkout del cliente |

## Decisiones aceptadas
- **Plan de cierre (2026-07-04, `CIERRE_BACKEND.md` de Juan):** cerrar el backend en **3 tandas** (T1=P0+P1,
  T2=P2+P3, T3=P4+P5), modo RUN GRANDE. **CHECKPOINT obligatorio tras Tanda 1** (parar y pedir revisión antes
  de seguir). 1 commit por feature, tests de dominio por feature, §5.4 temprano. El cliente Kotlin se genera
  **al final** (tras Tanda 3), no en Tanda 1. El backlog P0–P5 vive en `CIERRE_BACKEND.md`.
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
- **Referencias cross-módulo por id sin validar el tenant.** `Prenda.categoria_id` (inventario→catalogo)
  tiene FK a `categoria` (garantiza existencia) pero **no valida por código que la categoría sea del
  mismo tenant** (falta un puerto/API pública de `catalogo` consumible desde `inventario`, estilo
  Spring Modulith `@NamedInterface`). Riesgo bajo hoy; cerrar al definir las APIs entre módulos.

## A re-verificar cada sesión (invariantes)
- ¿ArchUnit y Modulith siguen en verde?
- ¿Toda tabla nueva lleva `empresa_id` y se filtra por tenant?
- ¿El dominio de los módulos hexagonal sigue sin framework?
- ¿La API solo expone DTOs y el contrato OpenAPI está al día?

## Registro de sesiones
- **2026-07-05 (v)** — **Tanda 1 · Prenda lleva sus valores de etiqueta (P0, RF-2.7, Capa 2).** La `Prenda`
  porta ahora una **`EtiquetasDePrenda`** (value object inmutable, mapa `tipoEtiquetaId → valorEtiquetaId`,
  una por dimensión) que la **clasifica** — concepto distinto de la combinación de variante del grupo de stock
  (esa solo abarca los tipos "definen variante"; esta clasifica el ítem con cualquier tipo/valor). El caso de
  uso valida cada etiqueta contra la taxonomía del tenant (`catalogo.ConsultaDeTaxonomia.valorPerteneceATipo`,
  sin exigir que defina variante) y rechaza dimensión repetida (400). Persistencia en tabla hija
  `prenda_valor_etiqueta` (**V17**), `@ElementCollection`. Solo se guardan **ids** → renombrar un valor
  **propaga** sin tocar la prenda. Tests de dominio (`EtiquetasDePrenda`, Prenda con/sin etiquetas) +
  integración (etiquetas válidas de vuelta / valor de otro tipo 400). **157 verdes.** _Decisiones:_ (a) una
  prenda lleva **un valor por dimensión** (no multi-valor); (b) por ahora **no** se valida que el tipo "aplique
  a la categoría" de la prenda — ese constraint llega con la taxonomía completa (RF-2.7.2, task #5), porque
  `TipoEtiqueta` aún no tiene el campo "categorías que aplica". _Deuda §5.4:_ validar `Prenda.categoria_id`
  contra el tenant (cross-ref) queda para el endurecimiento del aislamiento.
- **2026-07-05 (u)** — **Tanda 1 · Motor de variantes real (P0, RF-2.7.3/2.7.4).** `GrupoDeStock` deja de
  tener una "etiqueta" suelta y pasa a definirse por una **`CombinacionDeVariante`** (value object inmutable:
  mapa `tipoEtiquetaId → valorEtiquetaId`, igualdad por combinación sin importar orden → habilita unicidad y
  resolución pool→variante→stock). El caso de uso valida **combinaciones reales** contra el nuevo puerto
  público **`catalogo.ConsultaDeTaxonomia`** (el tipo debe **definir variante**, el valor debe **pertenecer al
  tipo**, sin repetir dimensión) y **rechaza variantes duplicadas** en la prenda (409). Persistencia en tabla
  hija `grupo_de_stock_valor` (**V16**, se elimina `etiqueta`). DTOs por combinación (400 inválida / 409 duplicada).
  Tests de dominio nuevos (`CombinacionDeVariante`, `mismaVariante`) + integración (real/duplicado/valor cruzado/
  tipo no-variante/variante única). **149 tests verdes.** _Decisión:_ una combinación **vacía** = variante única
  de una prenda sin dimensiones. _Pendiente relacionado:_ Prenda↔etiquetas (Capa 2) y validar cross-ref
  `Prenda.categoria_id` contra tenant (parte del endurecimiento §5.4).
- **2026-07-05 (t)** — **Tanda 1 · Base del aislamiento forzado §5.4 (P1, temprano).** Módulo `compartido` con
  **`ContextoDeTenant`** (lee `empresa_id`/rol/usuario del JWT del `SecurityContext` en un solo lugar) +
  `AccesoSinEmpresa` → 403 Problem Details. `CategoriaController` migrado a usarlo (ejercita la frontera
  `catalogo → compartido`). Test de dominio sin Spring. Base para endurecer luego con filtro Hibernate/RLS.
  Se abrió **PR #8** para la fase de cierre (**PR #7 lo mergeó Juan**).
- **2026-07-04 (s)** — Cerrados los **3 módulos que faltaban** de §7: **Configuración (RF-12)** — interruptores
  de módulos por empresa (`GET/PUT /api/v1/configuracion`); **Notificaciones (RF-11)** — envío por canal
  (WhatsApp/FCM/EMAIL) vía adaptador log, estados PENDIENTE→ENVIADA (`POST/GET /api/v1/notificaciones`);
  **Marketplace/App cliente (RF-18.1/RF-15.6)** — descubrimiento **público** de empresas ACTIVAS
  (`GET /api/v1/marketplace/empresas`, read-model con JdbcClient). Migraciones `V14` (config) y `V15` (notif).
  Build local **verde (135 tests, 14 módulos)**. **Todo el listado de §7 tiene su primera rebanada.** En **PR #7**.
- **2026-07-04 (r)** — Nuevo módulo **Reportes (RF-9)** (solo lectura, §5.2): **resumen de ingresos**
  (`ResumenDeIngresos`) por renta/venta, calculado con **JdbcClient** sobre la tabla `pago` (read-model
  sobre el esquema compartido, sin dependencia de código a otros módulos). `GET /api/v1/reportes/ingresos`,
  restringido a DUENO/ENCARGADO. Sin migración. Build local **verde (126 tests, 11 módulos)**. En **PR #7**.
  Falta (deferido): ganancia (ingreso − costo), más rentados/vendidos, utilización, filtros por fecha/sucursal, export.
- **2026-07-04 (q)** — Nuevo módulo **Pagos (RF-6)**: `Pago` ligado a un concepto (RENTA/VENTA) con
  monto, método (EFECTIVO/TARJETA/TRANSFERENCIA), referencia y **clave de idempotencia** (índice único
  parcial por empresa → no duplica cobros, RF-17.6/CLAUDE.md). `POST/GET /api/v1/pagos?conceptoId=`.
  Migración `V13`. Build local **verde (121 tests, 10 módulos)**. En **PR #7**. Falta (deferido): caja por
  sucursal/turno + corte y cuadre (RF-6.3/6.10), saldos y reembolsos (RF-6.9), depósito como retención (RF-6.2).
- **2026-07-04 (p)** — Nuevo módulo **Ventas/POS (RF-4)**: `Venta` (agregado con líneas) **a nombre del
  empleado del token** (RF-4.2), con descuento y **total = subtotal − descuento**; cliente opcional.
  `POST/GET /api/v1/ventas`. Migración `V12` (venta + linea_de_venta; `empleado_id`→usuario). Build local
  **verde (115 tests, 9 módulos)**. En **PR #7**. Falta (deferido): descuento automático de stock (RF-4.4,
  evento hacia GrupoDeStock), comprobante (RF-4.3/6.5) y devoluciones/cambios de venta (RF-4.5).
- **2026-07-04 (o)** — Nuevo módulo **Devoluciones (RF-5)**: `Devolucion` (agregado con checklist
  `PiezaRevisada`: ¿llegó? + estado BIEN/DANADA/EN_LIMPIEZA/PERDIDA, RF-5.1) y **liquidación del
  depósito** (RF-5.3): remanente = depósito − daños − recargos, floored en 0. `POST/GET /api/v1/devoluciones`.
  Migración `V11` (devolucion + pieza_revisada). Build local **verde (108 tests, 8 módulos)**. En **PR #7**.
  Falta (deferido): multa/cargo automático (RF-5.2, depende de config de multas RF-6.6), actualizar inventario
  (RF-5.4, evento hacia GrupoDeStock) y devolución parcial (RF-5.5).
- **2026-07-04 (n)** — Nuevo módulo **Rentas (RF-3)**: `Renta` con fechas de retiro/devolución,
  **importe = precio × días** (mínimo 1) y depósito; máquina de estados RESERVADA→ACTIVA→DEVUELTA→CERRADA
  (+ CANCELADA desde RESERVADA) y `estaVencida`. `POST /api/v1/rentas`, `GET /api/v1/rentas?clienteId=`,
  `POST /api/v1/rentas/{id}/{entregar|devolver|cerrar|cancelar}` (409 en transición inválida, 404 fuera del tenant).
  Migración `V10`. Build local **verde (102 tests, 7 módulos)**. En **PR #7**. Falta: verificación de disponibilidad
  por fechas (RF-3.2, sin traslapes) y extensión/renovación (RF-3.6); precio derivado de la prenda.
- **2026-07-04 (m)** — Nuevo módulo **Pedidos/Carrito (RF-16)**: agregado `Carrito` (agregado con líneas)
  **segmentado estrictamente** por (empresa × sucursal × cliente × tipo) con índice único parcial sobre
  los PENDIENTE; agregar la misma prenda **suma** cantidades; **renta y venta quedan en carritos separados**
  (RF-16.4). `POST /api/v1/carritos/items` (crea o actualiza), `GET /api/v1/carritos?sucursalId&clienteId&tipo`.
  Migración `V9` (carrito + linea_de_carrito). Decisión anotada: modo asistido (el empleado indica el cliente);
  el carrito del cliente-app (RF-18) reusará el mismo motor. Build local **verde (93 tests, 6 módulos)**. En **PR #7**.
- **2026-07-04 (l)** — Nuevo módulo **Clientes (RF-7)** (adelantado por ser prerequisito del Carrito):
  ficha del cliente (teléfono/correo/documento/dirección), **búsqueda por texto** (nombre/documento/teléfono,
  RF-7.3) y **lista negra** (RF-7.3). `POST/GET /api/v1/clientes` (con `?buscar=`), `POST /api/v1/clientes/{id}/lista-negra`
  (DUENO/ENCARGADO), acotado al tenant. Migración `V8`. Build local **verde (86 tests, 5 módulos)**. En **PR #7**.
- **2026-07-04 (k)** — Inventario, **GrupoDeStock (RF-2.2/2.11)**: variante con conteo por estado
  (disponibles/dañadas/en limpieza/perdidas) y **movimientos entre estados** (validando no mover más
  de las que hay). `POST/GET /api/v1/prendas/{id}/grupos-stock`, `POST /api/v1/grupos-stock/{id}/mover`
  (DUENO/ENCARGADO/BODEGA), con validación de que la prenda/grupo son del tenant (404 si no). Migración
  `V7`. Build local **verde (78 tests)**. En **PR #7**. Sigue: variante por combinación de etiquetas y Disfraz.
- **2026-07-04 (j)** — Iniciado **Inventario (RF-2)**: nuevo módulo `inventario` con **Prenda (RF-2.1/2.10)**
  — ítem con categoría, `TipoArticulo` (renta/venta/ambos) y precios, con reglas de precio en el dominio
  (renta exige precioRenta, etc.). `POST/GET /api/v1/prendas` (DUENO/ENCARGADO/BODEGA), acotado al tenant.
  Migración `V6`; errores de dominio → 400. Build local **verde (69 tests)**. En **PR #7**. Anotada deuda:
  falta validar por código que `categoria_id` sea del mismo tenant (API cross-módulo). Sigue GrupoDeStock.
- **2026-07-04 (i)** — Catálogo, **motor de etiquetas (RF-2.7.1/2.7.2)**: `TipoEtiqueta` (interruptores
  ¿define variante?/¿seleccionable por cliente?) + `ValorEtiqueta`, ambos con `empresa_id`. Casos de uso
  crear tipo / listar tipos / agregar valor / listar valores; validación de que el tipo pertenece al
  tenant (404 si no). `POST/GET /api/v1/tipos-etiqueta` y `.../{id}/valores` (DUENO/ENCARGADO para crear).
  Migración `V5`. Manejador de errores propio del módulo. Build local **verde (59 tests)**. En **PR #7**.
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
