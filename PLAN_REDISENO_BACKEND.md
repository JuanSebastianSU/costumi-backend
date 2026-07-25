# Costumi — Plan maestro del BACKEND del rediseño

> **Qué es esto.** La lista **única, completa y ordenada** de lo que el backend debe hacer para cerrar el
> rediseño de la app + el epic de identidad/permisos. Nace el **2026-07-24**.
>
> **★ IMPORTANTE (2026-07-24): este plan está COTEJADO CONTRA EL CÓDIGO REAL de `main`, no contra los .md.**
> Se verificó cada ítem con 3 barridos del código (evidencia archivo:línea). Varios documentos del repo
> (`INFRA_PENDIENTE.md`, `CIERRE_BACKEND.md`) están **desactualizados** y NO se usan como fuente. Lo que
> diga «EXISTE» acá se confirmó en el código; lo que diga «FALTA» se confirmó con grep exhaustivo.
>
> Fuentes del *qué necesita el front* (el detalle fino sigue ahí): `AppCustomi2/PROGRESS.md` («🧱 Lote de
> backend»), `AppCustomi2/AUDITORIA_FRONT.md` (hallazgos A–E), `AppCustomi2/PLAN_IDENTIDAD_PERMISOS.md` (§7).
>
> **Reglas que mandan (Juan):** front ideal → backend cumple · **todo real, nada simulado** · **recencia
> primero** · **filtro por sucursal** en toda vista de datos · **TODO es permiso configurable** (rol=preset)
> · una membresía de trabajo a la vez · T&C · re-auth al entrar a trabajo. Al terminar: prueba rol por rol
> y acción por acción con datos reales.

## Leyenda
- ⬜ falta (verificado) · 🚧 en curso · ✅ ya en `main` · ⏭ no es backend / falsa alarma.
- «regen» = al mergear, regenerar `:api-client` y cablear el front (cambia el contrato OpenAPI).
- Base: `main` `4ca5462`, última migración **V66**, próxima **V67**. ArchUnit: `dominio` sin Spring; nada de
  `findById` en adaptadores; toda tabla con `empresa_id`/`sucursal_id`.

---

## ✅ YA ESTÁ HECHO (verificado en `main`) — NO rehacer, evita cruzar cables
Cosas que algún documento daba por pendientes pero **ya están en el código**:
- ✅ **Fotos con S3 real** (AWS SDK): `POST /prendas/{id}/foto` y `POST /disfraces/{id}/foto` multipart
  (`AlmacenDeImagenesS3`). Da 503 solo si falta bucket/región. → la subida de imágenes NO se reimplementa.
- ✅ **FCM push (API HTTP v1, service account)**, **SMTP** (recuperar contraseña, `/auth/olvide`+`/restablecer`),
  **WhatsApp** (Meta Cloud), **Pasarela de pago MercadoPago** (`POST /pagos/intento` + `/intento/cliente` +
  `/webhook` firmado + switch `pagoEnLinea` en Config). **Todo el CÓDIGO existe y está gateado**; lo único
  que falta son **variables de entorno en producción**, no código. (`INFRA_PENDIENTE.md` está viejo.)
  - ⚠️ **FCM: el código está bien, pero la push NO llega a dispositivos reales** (sí al emulador). Eso es un
    problema de **Firebase/Android en el dispositivo**, no del backend. Se diagnostica aparte (hay
    `GET /notificaciones/estado-canales` y `POST /probar-push/{clienteId}`).
- ✅ **`X-Sucursal-Id` (sucursal activa) ya se lee** (`ContextoDeTenant.sucursalActiva()`), pero **solo se
  usa en inventario** → aplicar a más listados es *usar algo que ya existe*, no inventarlo.
- ✅ **Empleado↔sucursales (N:N)**: tabla `usuario_sucursal` (V38) + `GET /empleados/{usuarioId}/sucursales`
  + `PUT` para asignar. + **desactivar/activar** empleado (`POST /empleados/{id}/desactivar`|`/activar`).
- ✅ **Permisos ya se APLICAN server-side** (`InterceptorDePermisos` → 403). Falta exponerlos al front (B1).
- ✅ **`GET /disfraces?categoriaId=`** ya filtra server-side (paginado en BD).
- ✅ **Código de retiro** ya viene en `VentaResponse`/`RentaResponse` (falta solo en la respuesta del *cobro*).
- ✅ **Saldo por operación** existe: `GET /pagos/saldo?conceptoId=` → `saldoNeto` (falta el «pendiente» real).

---

## Decisión de orden (la tomé yo, 2026-07-24)

Dos fases. **FASE A = lo aditivo del rediseño** (campos/fechas/filtros/paginación/identidad de tienda),
riesgo bajo, el front ya está maquetado esperándolo. **FASE B = el epic estructural** (identidad, membresías,
permisos, multi-sucursal), que toca token/auth y es lo delicado → al final, sobre read-models ya completos.

**Arranque:** **PR-1 = Recencia (A1)** — es el más limpio y de menor riesgo, y estrena el flujo
Docker→push→PR→Railway con algo seguro. **Enseguida PR-2 = Identidad de tienda (A7-i)**, que vos marcaste
como «primer frente» y que ahora confirmé que **falta entero** (desbloquea C1/C2/G17). El resto de FASE A va
por tamaño/afinidad. Si preferís invertir PR-1↔PR-2, es un cambio de una línea: decidilo vos.

**Decisiones dentro:**
- El **hallazgo B (filtro por sucursal en listas)** se hace en **B4**, aprovechando que `X-Sucursal-Id` ya
  existe (solo hay que consumirlo en ventas/rentas/caja/clientes). No se hace suelto.
- **Hallazgo D (impuesto POS) = falsa alarma** (precios impuesto-incluido; el servidor no suma impuesto). No
  es backend, solo verificar el front. Lo real de D: exponer un «pendiente» del servidor (→ A5).
- **Hallazgo E (formato `comoPrecio`) = 100% front.**

---

# FASE A — Datos del rediseño (aditivo, front ya maquetado)

### A1 — ★ Recencia (fecha de registro + orden)  · regen · 🚧 HECHO en rama `feat/recencia-y-fechas-de-registro` (RED-1, sin mergear)
- ✅ Mapear `renta.creada_en` (V57) en `RentaJpaEntity` + `creadaEn` en el dominio `Renta`.
- ✅ Exponer `creadaEn` en `VentaResponse` y `RentaResponse`.
- ✅ **Orden DESC**: rentas ahora por `creadaEn` (antes `fechaRetiro`); ventas ya lo hacía; pagos por `fecha`.
- ✅ **Historial del cliente arreglado**: ventas ya no salen con fecha nula; ambas usan `creada_en` para
  fechar/ordenar; `ClienteService` reordena el total al cruzar tiendas.
- Sin migración (las columnas ya existían). **Suite 544/544.** Al mergear: regen `:api-client`.

### A1-bis — Hitos del ciclo  · regen · 🚧 HECHO en `feat/bloque-a-fechas-del-ciclo-y-campos` (RED-2, sin mergear)
- ✅ **Hitos reales de la renta** (V67): `entregadaEn`/`devueltaRealEn`/`cerradaEn`, seteados en las
  transiciones del dominio y expuestos en `RentaResponse`.
- ✅ **Fecha de la devolución** (V69): `Devolucion` ahora persiste `registradaEn` (ordena G10) y
  `fechaDevolucionReal`; expuestas en `DevolucionResponse`; la lista ordena por `registradaEn` DESC.
- ✅ **Timestamps del turno de caja** (V68, `G14`): `abiertoEn`/`cerradoEn` en `Turno` + `TurnoResponse`.

### A2/B3 — Períodos financieros  · regen · 🚧 núcleo HECHO en `feat/bloque-b3-periodos-financieros` (RED-4, sin mergear)
- ✅ `/reportes/ingresos` y `/ganancia`: aceptan `desde`/`hasta` (ingresos por `pago.fecha`, costo por
  `venta.creada_en`). Con esto Reportes filtra coherente y el Panel puede pedir «hoy».
- ✅ **B3b** (RED-5, sin mergear): serie de ingresos por día (`/reportes/ingresos-por-dia`, para el gráfico
  + variación) · contar rentas `DEVUELTA` (`/reportes/devoluciones-por-cerrar`, alerta del panel).
- ⬜ **Totales del período en ventas** (`/ventas/totales?estado=&desde=&hasta=`) — chico, pendiente.

### A3 — Campos que faltan en respuestas  · aditivo puro · regen
- ✅ `ClienteResponse.tieneRentaEnCurso` (RESERVADA/ACTIVA), resuelto en la consulta de carga con `bool_or`
  (sin N+1). Hecho en RED-2 (`G11`).

### A3-bis — Nombres + actor en respuestas  · su propio PR (mismo tema: trazabilidad)
Se agrupan porque comparten el patrón «resolver quién es cada fila»; el actor de auditoría además es
transversal (los eventos no llevan el actor → hay que hacerlo llegar al sink):
- ⬜ `SolicitudDeReembolsoResponse`: **nombre** del solicitante (hoy solo `solicitanteClienteId`; resolver por
  lote vía `ResolucionDeClientes`). (`G13`)
- ⬜ `NotificacionResponse`: **`clienteNombre`** (hoy la app lo resuelve contra una lista topada en 100). (`G19`)
- ⬜ **Usuario/actor en auditoría** (`G21`): ★ **transversal** — `RegistroDeAuditoria` **no guarda quién**
  (solo accion/detalle/fecha) y **los eventos de dominio no llevan el actor**. Hay que hacer llegar el actor
  al sink (enriquecer eventos o capturar el usuario del contexto) + columna + exponerlo. No es un campo más.

### A4/B1 — Filtros server-side de gestión  · regen · 🚧 HECHO en `feat/bloque-b1-filtros-de-gestion` (RED-3, sin mergear)
- ✅ **Bandejas de rentas (`G9`)**: `GET /rentas?filtro=POR_ENTREGAR|ACTIVAS|VENCIDAS|CERRADAS` +
  `GET /rentas/resumen`. `FiltroDeBandeja` traduce la bandeja a estados+flags; el JPQL no conoce el enum.
- ✅ **Ventas (`G8`)**: `GET /ventas?estado=<estado>`. *(Rango de fechas + totales → B3 períodos.)*
- ✅ **Auditoría (`G21`)**: `GET /auditoria?tipo=<primera palabra>` · **Reembolsos (`G13`)**:
  `GET /reembolsos?filtro=PENDIENTES|RESUELTAS`. *(`GET /disfraces?categoriaId=` ✅ ya estaba.)*

### B2 — Historial del cliente paginado (reescribe el read-model)  · regen
- ⬜ **★ Historial del cliente (`C8`/`C7`)**: paginar `GET /clientes/me/historial` (hoy lista plana concatenada
  en memoria) con UNA consulta por `cliente.usuario_id` (molde: `estadoDeCuentaDeUsuario`) + `?filtro=
  POR_PAGAR|POR_RETIRAR|ACTIVOS|CERRADOS` + `saldoPendiente`/`estadoPago` en `HistorialItem` (falta el
  fragmento de saldo de VENTAS, hoy solo existe para rentas).
- ⬜ **Operación por id para el cliente** (`C7`): `GET /clientes/me/operaciones/{id}` (por `usuario_id`,
  patrón `misDeudas`).

### A5 — Cobros, saldos y comprobante del servidor  · regen
- ⬜ **«Pendiente» calculado por el servidor** en `ComprobanteResponse` (hoy solo `saldoNeto`; la app
  recomputa `importe + multa − saldoNeto`). El número que se cobra sale del servidor.
- ⬜ **«Cobrado hoy por método»** (agregado del día) en Pagos (`G12`) — hoy no existe (lo más cercano es el
  corte por turno).
- ⬜ **Código de retiro en la respuesta del cobro** (hoy viene en venta/renta, no en el `PagoResponse`/cobro). (`G7`)
- ⬜ **Pagar una multa online (`C9`)**: la pasarela ✅ ya existe; falta `sucursalId` en `MiDeudaResponse` **y**
  que `POST /pagos/intento/cliente` acepte cobrar el **saldo con multa** (hoy calcula sobre el importe
  original y exige monto == pendiente sin multa).

### A6 — Fixes de dominio  · 🚧 HECHO en `feat/a6-prenda-archivada-en-disfraz` (RED-6, sin mergear)
- ✅ **Archivar una prenda que usa un disfraz** (bug): `opcionDePrenda` filtra archivada (no se ofrece) +
  `resolverPrenda` rechaza la fija/explícita archivada (no se vende/renta) + `GET
  /disfraces/conteo-por-prenda/{id}` para el aviso de impacto.
- ✅ **«Nuevo cliente» en línea desde el POS**: `crear-cliente` ya aceptaba teléfono/email — nada que hacer.

### A7 — Media & identidad de tienda  · regen + migraciones
- ⬜ **★ (A7-i) IDENTIDAD DE TIENDA** (tu «primer frente») — **falta entero**. Desbloquea `C1/C2/G17`:
  - `Empresa`: `logoUrl`, `portadaUrl`, `descripcion`, `ciudad`/`barrio`, `direccion`, `horario` (por día →
    abierto/cerrado), `disfracesCount`. (Hoy solo `ubicacion`/`contacto` en texto.) Endpoints multipart para
    **subir logo y portada** (reusando el `AlmacenDeImagenesS3` que ✅ ya existe).
  - `EmpresaVitrinaResponse` (marketplace): hoy **solo id+nombre** → sumar logoUrl/ciudad/disfracesCount/abierto.
  - `Sucursal` (`G17`): foto/portada, descripción, horario, **lat/lng** (hoy solo un link de Maps en texto).
  - `C1`: endpoint de **destacados** (no existe) + **facetas/categorías** públicas del marketplace (parcial:
    el catálogo ya filtra por categoría). Ciudad del usuario para el saludo.
- ⬜ **(A7-ii) Paginar el marketplace de tiendas** (`C1`): hoy `GET /marketplace/empresas` devuelve `List`
  completa (sin `Pageable`). Server-side + RemoteMediator en la app.
- ⬜ **(A7-iii) Foto de perfil del cliente** (`C10`): `PerfilResponse` no tiene `fotoUrl` (V66 solo agregó
  nombre/teléfono). Agregar columna + endpoint multipart (reusa S3).
- ⬜ **(A7-iv) Favoritos sincronizados** (`C4`): **no existe nada** (0 hits). `GET/POST/DELETE
  /clientes/me/favoritos` + persistencia (hoy son locales en Room).
- ⬜ **(A7-v) Carrito (`C5`)**: depósito reembolsable por línea + total (no existe) · **variante (talla/color)
  por línea** (`SeleccionDeSlot` solo trae `orden`+`prendaId`) · **editar cantidad** (hoy solo add/delete) ·
  **fecha de creación** del carrito/línea (no se guarda).
- ⬜ **(A7-vi) Reportes: gráficas de serie + `fotoUrl` en rankings** (`G15`) — usa la serie de A2.

---

# FASE B — Epic de identidad, membresías, permisos y multi-sucursal

> Estructural y delicado (token/`/auth/me`/sucursal). Orden = §7 de `PLAN_IDENTIDAD_PERMISOS.md`. Cada paso
> = una rama/PR. **Realidad verificada:** hoy `Usuario` tiene **un** `rol` + **un** `empresaId` (excluyentes,
> email único) → H1 confirmado; `/auth/me` solo trae id/email/rol/empresaId.

- ⬜ **B1 — `GET /empleados/me/permisos`** (o permisos en `/auth/me`) + navegación por permisos en la app.
  Hoy **no existe** el endpoint self (solo `matriz(id)` admin-only); la aplicación ya es server-side
  (`InterceptorDePermisos`), falta que el front la **conozca** para filtrar menú/acciones. Convierte el filtro
  por rol (H2/H3) en fallback. *Mayor impacto, menor alcance.*
- ⬜ **B2 — Modelo de membresía** persona↔tienda (estado+motivo) + **separación identidad/empresa (H1)**: que
  `rol`/`empresaId` dejen de ser un campo excluyente del `Usuario` y el token exprese **persona + membresía
  activa**. + **cambio de contexto** «Comprando ↔ Trabajando en X» en la app. + extras de perfil (`C10`):
  direcciones guardadas (CRUD), preferencias de notificación por canal.
- ⬜ **B3 — Invitación/aceptación** (invitar por email → `INVITADA` → aceptar con **T&C** → crea cuenta si no
  existe) + **alta = invitar** (reemplaza `AltaDeEmpleadoRequest(email,pass,rol)` que crea cuenta directa) +
  formalizar **desvinculación de dos vías** (hoy hay desactivar/activar; falta el «me voy» del empleado y el
  motivo/estado de baja).
- ⬜ **B4 — Multi-sucursal (= acá se resuelve el hallazgo B, bien):** **aplicar `X-Sucursal-Id` (que ✅ ya se
  lee)** al alcance de datos de ventas/rentas/caja/clientes (hoy solo inventario lo respeta); selector de
  sucursal activa; reasignación **piramidal** (empleado↔sucursales ✅ ya es N:N). Regla mono→multi aprobada.
- ⬜ **B5 — Permisos expandidos**: hoy la matriz es **12 secciones × VER/ACCION** y las **capacidades de
  gestión** (invitar/dar de baja/cambiar rol/editar permisos/asignar sucursales) se gobiernan por **jerarquía
  de roles** (`Rol.puedeGestionarA`), no por casillas. Decidir si se mueven a la matriz (decisión #8: sí, todo
  es permiso) + secciones que faltan (Sucursales/Mensajes/Auditoría/Reembolsos) + **rediseño de la pantalla**.
- ⬜ **B6 — Re-auth / step-up** al entrar a modo trabajo, **configurable por tienda** (hoy **no existe** nada).

---

## ⏭ NO es backend pendiente (para no contarlo como falta)
- ⏭ **Hallazgo D — total del POS con impuesto**: falsa alarma (precios impuesto-incluido). Solo verificar el front.
- ⏭ **Hallazgo E — formato de dinero** (`comoPrecio`): 100% front.
- ⏭ **FCM no llega a dispositivos reales**: el **código del backend está bien**; es config de Firebase/Android
  en el dispositivo. Se diagnostica del lado de la app, no es un ítem de este plan.
- ⏭ Credenciales de producción (S3/SMTP/WhatsApp/FCM/MercadoPago): las cargás vos en Railway; el código ya está.

---

## Registro de sesiones
- **2026-07-24 (1)** — Consolidado el plan desde los 3 docs del front.
- **2026-07-24 (2)** — ★ **Cotejado contra el código real de `main`** (3 barridos). Corregido: S3/FCM/SMTP/
  WhatsApp/MercadoPago/`X-Sucursal-Id`/empleado↔sucursales/`disfraces?categoriaId`/código de retiro/saldo por
  operación **ya existen** (docs viejos). Confirmadas las ausencias reales con evidencia. Backend en `main
  4ca5462`, **sin tocar aún** (no se ha escrito código).
