# Costumi — Plan de cierre del backend (handoff)

> **Para:** la IA/constructor que continúa el backend.
> **Qué es:** lista completa y priorizada de lo que **falta para dar por cerrado el backend**,
> contrastada **RF por RF** contra `BACKEND_REQUIREMENTS.md` y contra el código real de la rama
> `chore/scaffolding-modulith`. Cada ítem cita su requerimiento. **No inventes alcance**: si algo
> no está aquí ni en `BACKEND_REQUIREMENTS.md`, anótalo como decisión pendiente en `PROGRESS.md`.

## 0. Estado de partida (a la fecha de este documento)
- 14 módulos con **1ª rebanada vertical**; CI/ArchUnit/Modulith **verdes**; ~135 tests; migraciones V1–V15.
- **Deudas de seguridad YA cerradas:** fail-fast del secreto JWT en prod, autorización por rol/tenant en
  endpoints, bootstrap del SuperAdmin, login timing-safe, validación de issuer del JWT.
- **Lectura clave:** el sistema es **ancho pero delgado**. El núcleo que el spec manda hacer *primero*
  (taxonomía RF-2.7 + Disfraz/Slot Capa 3) es justo lo **más superficial**. Cerrar el backend NO es pulir:
  los bloques P0–P1 son **construcción de fondo**.

## 1. Reglas de trabajo (no negociables — de `CLAUDE.md`)
- [ ] **Test-first en dominio**: el dominio se prueba **sin BD ni Spring**. Añade tests de dominio a cada feature.
- [ ] **Migración Flyway** incluida con todo cambio de esquema. Nunca `ddl-auto=update`.
- [ ] **ArchUnit + Modulith + tests en verde** antes de dar algo por hecho. No desactivar tests "para que pase".
- [ ] **DTOs en la frontera**; nunca exponer entidades JPA/dominio. **Problem Details** en errores.
- [ ] **`empresa_id`** en toda tabla de negocio (+ `sucursal_id` donde aplique), filtrado por tenant.
- [ ] **Idempotencia** en toda operación de dinero/confirmación.
- [ ] **Actualizar `PROGRESS.md`** al terminar cada feature. **No inventar requerimientos.**

## 1.5 Modo de ejecución — RUN GRANDE (decisión del equipo, por tiempo)
Por tiempo, esta fase **NO** se hará en rebanadas pequeñas revisadas una a una: se cierra el backend en
empujones grandes. Al perder la revisión temprana, **estas reglas la reemplazan** (son obligatorias):
- [ ] **Tres tandas ordenadas por dependencia** (no un solo blob gigante):
      **Tanda 1 = P0 + P1** (fundación) · **Tanda 2 = P2 + P3** (ciclo operativo + dinero) · **Tanda 3 = P4 + P5** (resto).
- [ ] **CHECKPOINT tras la Tanda 1:** parar y pedir revisión **antes** de construir encima. Si el modelo núcleo
      (variante real / Disfraz / disponibilidad derivada) o el aislamiento §5.4 quedan mal, **todo lo de arriba se rehace**.
- [ ] **Tests de dominio por CADA feature** — nunca relajar/desactivar tests para pasar. Es la única red que queda sin revisión por rebanada.
- [ ] **Un commit por feature** (NO squash gigante) para poder **bisecar/revertir granular** si algo sale mal.
- [ ] **§5.4 (aislamiento forzado) TEMPRANO** dentro de la Tanda 1: retrofitear el filtro por contexto de request
      sobre 14 módulos es más caro cuanto más tarde se haga.
- [ ] **Lo ambiguo → decisión en `PROGRESS.md`**, no asumir (el riesgo de inventar alcance se multiplica en un run largo).

---

## 2. Backlog priorizado (de más crítico a menos)

### P0 — Núcleo del modelo (destraba todo lo demás)
- [ ] **Motor de variantes real (RF-2.7.3/2.7.4/2.7.5).** Hoy `GrupoDeStock` es un contador de estados con
      una etiqueta suelta. Debe ser la **combinación de valores de etiqueta** que definen variante; solo
      **combinaciones reales** (no producto cartesiano); resolución pool→variante→stock.
- [ ] **`Prenda` lleva sus valores de etiqueta (RF-2.7, Capa 2).** Hoy `Prenda` no referencia etiquetas → roto.
- [ ] **Completar taxonomía (RF-2.7.2/2.7.6):** en `TipoEtiqueta` falta *"a qué categorías aplica"* y el
      *"seleccionable por cliente **en qué slots**"* (hoy es un boolean grueso); **renombrar propaga** y las
      **rentas históricas conservan el valor**; **siembra de básicos** por empresa (RF-2.7.7).
- [ ] **`Disfraz` + `Slot`/Sección (Capa 3, §2, RF-2.3):** modo unidad-fija vs por-partes (≤8 slots), los
      **dos ejes** (talla fija/libre; prenda fija/personalizable), slot **opcional**.
- [ ] **Disponibilidad DERIVADA (RF-2.4):** un disfraz personalizable está disponible si **cada slot
      obligatorio** tiene ≥1 prenda disponible en su pool. Patrón Specification (§5.5).

### P1 — Seguridad y frontera (antes de crecer más)
- [ ] **Aislamiento multi-tenant FORZADO (§5.4).** `empresa_id` del token → **contexto de request** →
      **filtro/interceptor** que lo aplica a toda consulta (filtros Hibernate y/o **RLS Postgres**). Validar
      las **referencias cruzadas por ID** contra el tenant (p.ej. `Prenda.categoria_id`). Hoy solo hay chequeo por endpoint.
- [ ] **OpenAPI contract-first — montar la disciplina (RF-17.3, §5.6).** En la Tanda 1 se **instala la
      herramienta** para que **de aquí en adelante todo endpoint nazca del contrato** (fuente única), con
      errores Problem Details, paginación y versionado `/api/v1`. **OJO de secuencia:** el **contrato
      COMPLETO** solo existe cuando el backend está cerrado (las Tandas 2 y 3 agregan muchos endpoints), así
      que **el cliente Kotlin se genera AL FINAL, tras la Tanda 3 — NO en la Tanda 1** (ver §5).
- [ ] **`X-Sucursal-Id` en cabecera (RF-17.4):** sucursal activa por header, validada contra la empresa del token.

### P2 — Ciclo operativo (renta → devolución → venta), con domain events
- [ ] **Renta: disponibilidad por fechas SIN traslapes + reservas (RF-3.2, RF-0.4).** Calendario y bloqueo de
      concurrencia (evitar doble asignación). **Crítico.**
- [ ] **Renta: multi-artículo / armado por partes (RF-3.1), extensión/renovación (RF-3.6), cobro al retiro +
      contrato/comprobante (RF-3.4), modo asistido presencial (RF-3.7), detección de vencidas como proceso (RF-3.5).**
- [ ] **Venta: descuento automático de stock al confirmar (RF-4.4 — crítico), comprobante (RF-4.3),
      devoluciones/cambios (RF-4.5), modo asistido (RF-4.6).**
- [ ] **Devolución que cierra el ciclo:** checklist por pieza conectado (llegó + bien/dañada/limpieza/perdida,
      RF-5.1), **multa automática (RF-5.2)**, **actualizar inventario (RF-5.4)**, **parcial (RF-5.5)**, daño
      contra grupo de stock + número/QR (RF-5.6).
- [ ] **Domain events (§5.5):** devolución→multa, venta→baja de stock, aprobación de empresa. **Transiciones
      automáticas de estado de inventario (RF-2.11).**

### P3 — Dinero y analítica
- [ ] **Caja / Turno / MovimientoDeCaja (RF-6.3/6.10):** apertura, movimientos, **corte y cuadre por método**
      (efectivo físico vs tarjeta/transferencia conciliadas).
- [ ] **Pagos completos (RF-6):** saldos/pagos parciales/reembolsos (RF-6.1/6.9), **depósito como retención
      separada (RF-6.2/6.8)**, **pago mixto + vuelto en efectivo (RF-6.7)**, comprobantes/impuestos (RF-6.5),
      módulo de multas on/off (RF-6.6). Pago en línea/pasarela (RF-6.11) — depende de decisión de pasarela.
- [ ] **Agregar `costo de adquisición` y `depósito sugerido` a `Prenda` (RF-2.10).** Sin costo **no hay margen**.
- [ ] **Auditoría (RF-0.5, RF-15.5):** `RegistroDeAuditoría` en toda operación de dinero/inventario + acciones del SuperAdmin.
- [ ] **Reportes reales (RF-9):** **ganancia = ingreso − costo**, más rentados/vendidos, utilización, vencidas,
      por empleado, depósitos activos, valor de inventario, dañados/perdidos, **desglose por dimensión de
      etiqueta**, **export PDF/CSV (RF-9.2)**, **tablero de estado de inventario (RF-9.3)**.

### P4 — Módulos faltantes y completar existentes
- [ ] **Reabastecimiento / Proveedor / Transferencia (RF-10):** entradas de mercancía + **alerta de stock bajo**,
      ajustes con motivo+auditoría, **transferencias entre sucursales**.
- [ ] **Empleados enriquecidos (RF-8) + permisos granulares (RF-1.5):** usuario↔**1..N sucursales (RF-1.2)**,
      turno/actividad, **editor de permisos** visualización/acción por sección, sobre el rol-plantilla.
- [ ] **Clientes (RF-7):** foto/ID de garantía (RF-7.1), **historial** rentas/ventas/saldos/depósitos/multas
      (RF-7.2), búsqueda por nombre/código/documento (RF-7.3), **indicador de pendientes + filtros (RF-11.5/11.6)**.
- [ ] **Notificaciones reales (RF-11):** envío **WhatsApp (RF-11.4)** y **FCM (RF-18.11)**, disparadores por
      evento (vencidas RF-11.1, stock bajo RF-11.2), **interruptores que de verdad controlen (RF-11.3)**,
      **outbox** confiable (§5.5).
- [ ] **Configuración (RF-12):** **interruptores de módulos (RF-12.4)** que **activen/desactiven** de verdad
      (conteo stock, granularidad, QR/numeración, multas, multi-sucursal, permisos finos, modo de pago),
      reglas por defecto (precio/depósito/**recargo por retraso**/impuestos/moneda/horario, RF-12.2),
      respaldo/restauración (RF-12.3).
- [ ] **Marketplace (RF-18.1/RF-14.2):** búsqueda por **texto/categoría/cercanía**, **enlace/QR** directo por
      empresa/sucursal (RF-14.3), selección de sucursal (RF-14.2).
- [ ] **Carrito:** garantía "**uno por (local×tipo)**" a nivel repositorio (RF-16.3/16.4) y **checkout→conversión**
      a renta/venta.

### P5 — Plataforma / transversal
- [ ] **Refresh token (§5.6) + recuperación de contraseña + logout/"recordar sesión" (RF-1.1).**
- [ ] **Offline/outbox (RF-17.6, §5.7):** idempotencia también en confirmar renta/venta; outbox + IDs UUID de cliente.
- [ ] **Media/Fotos (RF-2.9, §5.6):** subida a **object storage S3-compatible + URLs prefirmadas**; BD guarda solo la URL.
- [ ] **Feature flags (Principio 1, RF-12.4)** que gobiernen el comportamiento de los módulos opcionales (§6).

---

## 3. Detalle por módulo (qué existe hoy / qué falta)

**Identidad** — el más avanzado. OK: Empresa (ciclo de vida), Sucursal, Usuario/Rol, auth JWT, authz por rol,
bootstrap. Falta: RF-1.1 (refresh/recuperación/logout), RF-1.2 (usuario↔sucursales), RF-1.5 (permisos),
RF-8 (empleado), RF-15.5 (auditoría), §5.4 (aislamiento forzado).

**Catálogo/Taxonomía** — el "más delicado", hoy parcial. OK: Categoria, TipoEtiqueta (defineVariante,
seleccionablePorCliente, archivada), ValorEtiqueta, taxonomía como **datos** (bien). Falta: RF-2.7.2 "aplica a
categorías" + slots; RF-2.7.5 personalización por slot; RF-2.7.6 renombrar-propaga/históricos; siembra básicos.

**Inventario** — OK: Prenda (tipo renta/venta/ambos, precioRenta, precioVenta, archivada), GrupoDeStock (estados
disponibles/dañadas/limpieza/perdidas + mover()). Falta: **etiquetas en Prenda**, **costo de adquisición**,
depósito sugerido, fotos, QR/numeración, conteo opcional, variante=combinación real, transiciones por evento.

**Carrito** — **core bien resuelto** (segmentación empresa×sucursal×cliente×tipo, persistente, líneas, dedupe).
Falta: "uno por (local×tipo)" a nivel repo, checkout→conversión.

**Rentas** — OK: fechas, importe, depósito, estados (RESERVADA→ACTIVA→DEVUELTA→CERRADA/CANCELADA), estaVencida().
Falta: **disponibilidad por fechas/traslapes (crítico)**, multi-artículo/armado, extensión/renovación, cobro+contrato, asistido.

**Ventas/POS** — OK: líneas, descuento, total, empleadoId, clienteId. Falta: **baja de stock (crítico)**,
comprobante, devoluciones/cambios.

**Devoluciones** — OK: liquidación de depósito (garantía−daños−recargos, piso 0), lista PiezaRevisada.
Falta: checklist conectado, multa automática, actualizar inventario, parcial, daño contra grupo+número/QR.

**Pagos** — **buena base**: Pago con BigDecimal, monto>0, idempotencia única por (empresa, clave), métodos,
ligado a renta/venta. Falta: caja/turno+corte, saldos/parciales/reembolsos, depósito-retención, mixto+vuelto, comprobantes.

**Clientes** — OK: ficha (teléfono/email/documento/dirección) + lista negra. Falta: foto garantía, historial,
búsqueda, indicador de pendientes/filtros.

**Reportes** — apenas iniciado (solo ingresos). Falta: ganancia (necesita costo en Prenda), utilización, vencidas,
por empleado, depósitos, valor inventario, dañados, desglose por etiqueta, export, tablero (RF-9.3).

**Configuración** — existe (V14); **verificar** switches (RF-12.4), reglas default (RF-12.2), respaldo (RF-12.3).

**Notificaciones** — esqueleto (registro con estado). Falta: envío real WhatsApp/FCM, disparadores, interruptores, outbox.

**Marketplace** — mínimo (lista ACTIVAS). Falta: búsqueda texto/categoría/cercanía, enlace/QR, selección de sucursal.

---

## 4. Recordatorio de gobernanza
- Cada feature = tests de dominio + migración (si aplica) + actualización de `PROGRESS.md`; commits granulares (§1.5).
- **Empieza por P0→P1 (Tanda 1)**: sin el motor de taxonomía/variante/Disfraz y el aislamiento forzado, lo demás se construye sobre arena.
- Todo lo que aquí aparezca como decisión abierta (p.ej. pasarela de pago RF-6.11) → **anótalo en `PROGRESS.md`, no lo asumas.**

## 5. Puente a Android (criterio de cierre del backend)
El backend NO se da por cerrado —ni se empieza Android— hasta cumplir estos criterios de salida:
- [ ] **Contrato OpenAPI COMPLETO y estable:** cubre **todos** los endpoints de las 3 tandas (no solo la fundación).
- [ ] **El cliente Kotlin se genera del contrato SIN errores.** Este es el **primer paso de la fase Android**, no de la Tanda 1.
- [ ] ArchUnit + Modulith + CI verdes; deudas transversales cerradas o explícitamente diferidas en `PROGRESS.md`.
- [ ] Revisión final completa (módulo por módulo, con este documento como checklist).

**Nota sobre "diseño XML/Kotlin":** las pantallas **ya existen** — hay dos apps Android de diseño (dueño y
cliente) con las vistas armadas pero con datos fijos/"(simulado)". La fase Android es sobre todo **integración**:
cliente Kotlin generado → Retrofit/OkHttp → repositorio → Room (offline) → **cablear a las pantallas existentes**
(RF-17.5, §5.6) y rellenar los huecos de UI de RF-18. No es diseñar desde cero.

Secuencia de fase: **Tanda 1 → Tanda 2 → Tanda 3 → OpenAPI completo → generar cliente Kotlin → Android.**
