# Costumi — Estado del proyecto (PROGRESS)

> **📌 Este es el PROGRESS del BACKEND** (repo `costumi-backend`). No confundir con los docs de la **app Android**
> (`PLAN_ANDROID.md`, `DISENO_PANTALLAS.md`, `ORDEN_CONSTRUCCION.md`), que son de otro frente. El detalle fino de las
> correcciones de auditoría vive en `AUDITORIA_Y_CORRECCIONES.md`.
>
> Se actualiza **al final de cada sesión**. Es lo primero que se lee (después de
> `CLAUDE.md`) para retomar sin perder el hilo. Regla: mueve ítems entre secciones,
> añade una entrada al registro de sesiones, **no borres el historial**.

## BACK-1 — el disfraz sobrevive al cobro (2026-07-22)

Al confirmar una venta o renta el disfraz se resolvía a sus prendas y **se perdía**: el cliente veía
"Capa Real" cuando había comprado un "Traje Pirata", y el dueño solo podía rankear prendas.

- **V65**: `linea_de_venta` y `renta_linea` ganan `disfraz_id`, `disfraz_grupo`, `disfraz_cantidad` y
  `disfraz_nombre` (CHECK: viajan juntas o ninguna).
  - `disfraz_grupo` identifica **una instancia** del disfraz en el pedido: el mismo disfraz dos veces con
    piezas distintas no debe mezclarse al agrupar.
  - `disfraz_nombre` se **guarda**, no se resuelve al leer: un pedido histórico no cambia si después
    renombran el disfraz, y evita que `ventas`/`rentas` dependan de `disfraces` (**Modulith detectó ese
    ciclo**; el snapshot es la solución correcta, no un parche).
- Dominio: `OrigenDisfraz` en cada módulo; `LineaDeVenta`/`RentaLinea` lo llevan.
- Se etiqueta en el **único punto de paso**: `DisfrazService.itemsVentaDe/itemsRentaDe` (venta directa y
  varios) y `CarritoService` (checkout).
- Respuestas: cada línea de venta/renta expone `disfrazId`, `disfrazNombre`, `disfrazGrupo`,
  `disfrazCantidad`.
- Reportes: **`GET /reportes/disfraces-mas-vendidos`** y **`/disfraces-mas-rentados`** — cuentan
  DISFRACES, no piezas (agrupan por grupo y suman).
- Tests: el disfraz sobrevive al cobro y se rankea; y una prenda suelta NO queda marcada como disfraz.
  **Suite 504/504.**

## PENDIENTE — auditoria de la app del 2026-07-22 (6 hallazgos)

Barrido cruzando el codigo de la app contra el contrato: **158 de 160 operaciones del backend ya tienen
pantalla**. Lo que falta, y que parte le toca a cada lado:

| # | Hallazgo | Backend | Front |
|---|----------|---------|-------|
| 1 | ✅ **HECHO (BACK-1).** ~~El disfraz se disuelve en prendas al cobrar.~~ La venta/renta guarda solo prendas: el cliente ve "Capa Real" cuando compro "Traje_Pirata_Opciones", y el dueno no puede saber que DISFRAZ se vende mas. | **BACK-1** | FRONT-1 |
| 2 | **Las listas no escalan.** Sin paginacion NI busqueda: disfraces, devoluciones, reembolsos, empleados, notificaciones, auditoria. Con paginacion pero sin busqueda: prendas, rentas, ventas. De 14 listas de la app solo Clientes tiene buscador. | **BACK-2** | FRONT-2 |
| 3 | **Buscar tiendas.** `GET /marketplace/empresas?buscar=` ya existe; la app manda `null` y no tiene caja. | — (hecho) | FRONT-3 |
| 4 | **Push no llegan.** `PUT /clientes/{id}/device-token` existe y nunca se llama. | — (hecho) | FRONT-4 |
| 5 | **Perfil del cliente vacio.** No puede editar sus datos ni cambiar contrasena: no hay endpoint para que el propio usuario lo haga. | **BACK-3** | FRONT-5 |
| 6 | No se leen las sucursales asignadas a un empleado (`GET /empleados/{usuarioId}/sucursales` sin usar). | — (hecho) | FRONT-6 |

Falsos positivos descartados en la auditoria (SI estan hechos, usan APIs escritas a mano en la app y no el
cliente generado): subir foto al disfraz, exportar reportes a PDF/CSV, y `/pagos`+`/saldo`+`/deposito` (ya
vienen dentro de `/pagos/comprobante`).

## Nombre de la categoria en la vitrina del disfraz (2026-07-22)

El cliente del marketplace no puede leer la taxonomia de la empresa, asi que con solo `categoriaId` (un UUID)
no podia ni mostrar ni filtrar por categoria en la vitrina de disfraces.

- `DisfrazResponse` gana **`categoria`** (el NOMBRE), igual que ya hacia `PrendaVitrinaResponse`.
- Lo resuelven la lista del dueño, la vitrina publica y el detalle publico, con **una sola consulta de
  categorias por lista** (sin N+1).
- Test nuevo en `DisfrazIntegrationTest`. **Suite 502/502.**

## Quitar un item del carrito (2026-07-22)

El cliente no tenia forma de deshacer lo que agrego al carrito: no habia endpoint ni boton. El caso feo:
si el dueño le cambiaba el **tipo** a un disfraz que un cliente ya tenia en el carrito, la valorizacion
fallaba, el GET del carrito devolvia error y el carrito quedaba **bloqueado para siempre**.

- `LineaDeCarrito` ahora tiene **id propio y estable** (antes el adaptador lo regeneraba en cada guardado,
  asi que no servia para referirse a una linea). El adaptador conserva ese id al persistir y rehidratar.
- `Carrito.quitarLinea(lineaId)` en el dominio; puerto `QuitarItemDelCarrito`; endpoint
  **`DELETE /api/v1/carritos/items/{lineaId}`** (mismos parametros de segmentacion que el GET).
- **La consulta del carrito ya no se cae** por una linea invalida: se pregunta ANTES de valorizar
  (`ResumenDeDisfraz` gana `permiteRenta`/`permiteVenta`) y la linea vuelve con precio nulo y
  `motivoNoDisponible`, para que el cliente la vea y la pueda quitar. El checkout la sigue rechazando.
  No se usa try/catch: la excepcion venia de otro modulo transaccional y marcaba la transaccion para
  rollback, asi que atraparla no alcanzaba.
- Tests nuevos en `CarritoIntegrationTest`: quitar una linea (y 400 al quitar algo que ya no esta) y el
  escenario completo del disfraz al que le cambian el tipo. **Suite 501/501.**

## Fase actual
**Fase 23 — Categorías de DISFRAZ como taxonomía propia, separada de las de prenda (2026-07-21).**

Rama `feat/categorias-de-disfraz` (desde `origin/main`, PENDIENTE de merge). El dueño aclaró que las categorías
de disfraz ("Piratas", etc.) son un concepto **completamente aparte** de las categorías de prenda (Camisa,
Pantalón…). Hasta ahora `disfraz.categoria_id` (V60) apuntaba por error a la tabla `categoria` (de prendas).
- **Migración V63:** nueva tabla `categoria_disfraz` (id, empresa_id, nombre, archivada); se **repunta** el FK
  `disfraz.categoria_id` de `categoria`→`categoria_disfraz` y se limpian los valores viejos (referenciaban
  categorías de prenda).
- **Módulo `disfraces`:** nuevo agregado `CategoriaDeDisfraz` + repo + persistencia; puerto
  `GestionCategoriasDeDisfraz` + `CategoriaDeDisfrazService` (listar/crear/renombrar/archivar/activar);
  controller `CategoriaDeDisfrazController` en **`/api/v1/disfraces/categorias`** (GET/POST/PATCH/archivar/activar);
  `DisfrazService.validarCategoriaDelTenant` ahora valida contra la taxonomía de disfraz (no la de prenda);
  404 nuevo `CategoriaDeDisfrazNoEncontrada`.
- Tests: `CategoriaDeDisfrazIntegrationTest` (crear/listar/renombrar/archivar; disfraz con su categoría + filtro;
  una categoría de PRENDA NO sirve como categoría de disfraz → 400). Se actualizaron 2 tests viejos de
  `DisfrazIntegrationTest` que usaban categoría de prenda para el disfraz. Suite **499/499** (Docker, JDK21).
**Al mergear: regenerar `:api-client`** (endpoints nuevos + modelo `CategoriaDeDisfrazResponse`) y en el FRONT
usar estas categorías en el form y la lista de disfraces (NO las de prenda) + una pantallita para gestionarlas.

---

**Fase 22 — Slice 4: el CARRITO del cliente acepta DISFRACES + total (2026-07-21).**

Rama `feat/carrito-con-disfraces` (desde `origin/main`, PENDIENTE de merge). El carrito (`módulo pedidos`) era solo de
prendas; ahora una línea es una **prenda** O un **disfraz** (con su elección de prenda por slot personalizable). Cambios:
- **Migración V62:** `linea_de_carrito.prenda_id` pasa a NULLABLE, se agrega `disfraz_id` + CHECK "prenda XOR disfraz";
  nueva tabla hija `linea_de_carrito_seleccion(linea_id, empresa_id, orden, prenda_id)` con FK `on delete cascade`.
- **Dominio:** `SeleccionDeSlot` nuevo; `LineaDeCarrito` variante de disfraz + `esPrenda/esDisfraz`; clave de merge de
  disfraz = disfraz + selecciones (ordenadas) + periodo; `Carrito.agregarDisfraz`.
- **Puerto base `ResolucionDeDisfraces`:** se agrega `resumenDeDisfraces(empresaId, ids)` (nombre+foto para pintar el
  carrito). La valorización y los dos checkouts resuelven cada disfraz a sus piezas valuadas con `lineasDeRenta/Venta`
  (precio ya repartido si el disfraz tiene precio general). Modulith: `pedidos → disfraces` SOLO por el paquete base ✓.
- **DTOs:** `AgregarItemRequest` relaja `prendaId` y gana `disfrazId`+`selecciones` (el controller valida uno-u-otro);
  `CarritoResponse`/`CarritoValorizado` distinguen prenda/disfraz y traen `precioUnitario`/`subtotal`/`selecciones`.
- Tests: dominio (merge de disfraz, prenda≠disfraz, fechas) + integración (valorizar venta 180, renta con selección que
  suma a 3 y total 270, checkout venta+renta, 400 si prenda+disfraz juntos). Suite **496/496** en Docker (JDK21).
**Al mergear: regenerar `:api-client`** (cambió `AgregarItemRequest` y `CarritoResponse`) y en el FRONT: agregar
disfraces al carrito del cliente (elegir disfraz → selección por slot → ver total antes de confirmar).

---

**Fase 20 — Pedido MIXTO: prendas + disfraces en una sola renta/venta (2026-07-21).**

Rama `feat/pedido-mixto-prendas-disfraces` (desde `origin/main`, PENDIENTE de merge). El dueño quiere que desde los
botones **Nueva renta** y **Nueva venta** se puedan rentar/vender **prendas Y disfraces** en la misma operación (antes
esos botones eran solo-prendas, y el pedido de disfraces estaba en una pantalla aparte — mala ubicación). Los endpoints
`POST /disfraces/rentar-varios` y `/vender-varios` ahora aceptan además de `items` (disfraces) una lista `lineas`
(prendas sueltas: prendaId, cantidad, precio); `DisfrazService` resuelve los disfraces y suma las prendas en UNA sola
renta/venta. `items` dejó de ser obligatorio; el controller valida que venga al menos uno de `items`/`lineas` (400 si no).
Comandos/requests ganan `lineas`. Test nuevo `rentar_pedido_mixto_prendas_y_disfraces_en_una_sola_renta` (1 disfraz 100/día
+ 2 prendas 40/día × 3 días = 540, 3 líneas, 1 renta). Suite **478/478** en Docker (JDK21).
**Al mergear: regenerar `:api-client` y en el FRONT meter "Agregar disfraz" en el form de Nueva renta y en el POS de
Nueva venta (usando este endpoint cuando el pedido incluya disfraces), y quitar la entrada suelta "Pedido de varios" de
la barra de Disfraces.**

---


Rama `feat/disfraz-varios` (desde `origin/main`, PENDIENTE de merge). Cierra el "carrito" que faltaba: rentar/vender
**varios disfraces distintos** (cada uno con su cantidad y sus selecciones) al mismo cliente en **una sola** renta/venta.
Nuevos endpoints `POST /disfraces/rentar-varios` y `/vender-varios` con `items: [{disfrazId, cantidad?, selecciones}]`.
Se refactorizó `DisfrazService`: la resolución de un disfraz a sus piezas se extrajo a `itemsRentaDe`/`itemsVentaDe`
(reusadas por el flujo single y por el de varios); el de varios recorre los items, acumula todas las líneas y crea una
única renta/venta. Nuevos comandos/puertos `RentarVariosDisfraces`/`VenderVariosDisfraces` (implementados por
`DisfrazService`) y requests `Rentar/VenderVariosDisfracesRequest`. Seguridad: caen en `anyRequest().authenticated()`
igual que el rentar/vender single. Test nuevo `rentar_varios_disfraces_distintos_crea_una_sola_renta_con_todas_las_lineas`
(1× disfraz 100/día + 2× disfraz 50/día × 3 días = 600, 4 líneas, 1 renta). Suite **477/477** en Docker (JDK21).
**Al mergear: regenerar `:api-client` y construir el carrito en el front** (pantalla de pedido: cliente/sucursal/fechas +
agregar disfraces configurados + confirmar).

---


Rama `feat/disfraz-cantidad` (desde `origin/main`, PENDIENTE de merge). El dueño reportó que solo podía rentar/vender
disfraces de a uno. Ahora `POST /disfraces/{id}/rentar` y `/vender` aceptan **`cantidad`** (nulo o <1 = 1): se rentan/venden
N unidades del mismo disfraz en una sola operación. Cada `ItemDeRenta`/`ItemDeVenta` de las piezas lleva la cantidad; el
precio general del disfraz se multiplica por la cantidad al repartirlo (importe = precio × N × días). Con `conteoStock`,
comprometer N unidades de cada pieza reutiliza el estado RENTADA (falla con `SinDisponibilidad` si no alcanza). DTOs y
comandos ganan `cantidad`; el controller la normaliza. Test nuevo `rentar_varias_unidades_del_mismo_disfraz_escala_el_importe_y_las_cantidades`
(3 disfraces, general 100/día × 3 días = 900; líneas con cantidad 3). Suite **476/476** en Docker (JDK21).
**Al mergear: regenerar `:api-client` y agregar un selector de cantidad en `DisfrazAsignarFragment`.** Pendiente a futuro:
carrito de varios disfraces DISTINTOS en una operación.

---


Rama `fix/multa-pendiente-etiqueta-rentas-push-cliente` (desde `origin/main`, PENDIENTE de merge). Dos arreglos que el
dueño reportó:
- **Multa en el comprobante (RF-6.6/11.5):** `ComprobanteResponse` gana `multa` (Σ multa de la renta; 0 en ventas o
  rentas sin multa), resuelto con `ConsultaDeMultas.totalMultaDeRenta`. Así el front puede sumar la multa al
  "Pendiente" de la pantalla de cobros (antes solo mostraba el importe de la renta, por eso la multa "no aparecía" ahí).
  El PDF del comprobante también la lista.
- **"Por etiqueta" cuenta ventas Y rentas (RF-8/9.1):** `RankingJdbcAdapter.ventasPorEtiqueta` ahora hace UNION de
  `linea_de_venta` + `renta_linea`; en un negocio que renta, antes salía siempre vacío. Respeta el filtro de sucursal.
- Tests: nuevo `el_comprobante_de_cobros_expone_la_multa_de_la_renta` (multa 30 en el comprobante) y
  `por_etiqueta_tambien_cuenta_las_rentas_no_solo_las_ventas` (renta de 3 → Azul con 3 unidades). Suite **475/475** en
  Docker (JDK21). **Al mergear: regenerar `:api-client`, sumar la multa al pendiente en la pantalla de cobros, y poner
  mensaje "sin datos" en la sección por-etiqueta.** Nota: push al cliente (device-token) sigue bloqueado por Firebase.

---


Rama `fix/stock-unificado-renta-venta` (desde `origin/main`, PENDIENTE de merge). El dueño reportó que **rentar no
bajaba el stock** y que las operaciones se bloqueaban sin explicación. Antes, las rentas se contabilizaban por un
"calendario" de fechas solapadas que **nunca tocaba `grupo_de_stock`**, mientras que solo las ventas descontaban
`disponibles`; contabilidades desconectadas → stock que no baja al rentar, rentas que se bloquean entre sí y riesgo de
sobreventa. Ahora hay un **5º estado `RENTADA`** en el grupo de stock:
- **Rentar** compromete unidades: `disponible -> rentada` (bajan los disponibles, el **total NO cambia**). Migración
  `V58__grupo_stock_rentadas.sql` (columna `rentadas int not null default 0`); enum `EstadoUnidad.RENTADA`.
- **Devolver** (rápida o detallada) libera: `rentada -> disponible` (bien) o `rentada -> dañada/limpieza/perdida`
  (novedad). **Cancelar/expirar** reserva: `rentada -> disponible`. Todo gated por `conteoStock`.
- **Vender** sigue dando de baja de `disponibles` → como lo rentado ya no está en disponibles, **no se puede sobrevender**
  lo que está en renta, y no se puede rentar lo que ya está fuera hasta devolverlo (modelo simple, sin calendario).
- `AjusteDeInventario` gana `comprometerParaRenta`/`liberarDeRenta`; `procesarRetornoDeRenta` ahora mueve desde `rentada`.
  `RentaService` inyecta `AjusteDeInventario` (compromete al crear, libera al devolver/cancelar). Reportes: `total` y
  `valorInventario` incluyen rentadas; `rentadasAhora` = Σ rentadas del stock; tablero y `GrupoDeStockResponse` exponen
  `rentadas`.
- Tests: reescrito `se_puede_rentar_en_fechas_que_no_se_traslapan` → `no_se_puede_rentar_la_misma_unidad_hasta_devolverla`;
  nuevo `rentar_baja_disponibles_y_devolver_los_restaura` (disponibles 3→2→3, rentadas 0→1→0, total estable). Suite
  completa **473/473** en Docker (JDK21). **Al mergear: regenerar `:api-client` y mostrar `rentadas` en el tablero y en
  la vista de stock del dueño.**

---

**Fase 16 — Reportes por sucursal + fix de esquema del estado de cuenta (2026-07-20).**

Rama `feat/reportes-por-sucursal-y-estado-cuenta-fix` (desde `origin/main`, PENDIENTE de merge). Dos cosas:
- **Reportes por sucursal (de verdad)**: se agregó `sucursalId` opcional a **ingresos, ganancia, tablero de inventario y
  ventas-por-etiqueta** (antes eran siempre a nivel empresa, por eso "el filtro por sucursal no servía"). Se hiló por
  las 5 capas (controller → puerto entrada → `ReporteService` → puerto salida → JdbcAdapter), siguiendo el patrón ya
  existente de `ingresosPorMetodo` (se agrega `and X.sucursal_id = :sucursalId` solo cuando llega). Notas de query:
  ingresos/ganancia filtran `pago.sucursal_id`; el costo de ganancia y ventas-por-etiqueta **unen a `venta`** (sus líneas
  no llevan sucursal); el tablero ahora **agrega por prenda** (`sum(...)::int` + `group by`) y filtra `grupo_de_stock.sucursal_id`.
  También los export CSV/PDF del tablero aceptan `sucursalId`.
- **Fix de esquema**: el record anidado `EstadoDeCuentaResponse.LineaResponse` (Fase 15) colisionaba en OpenAPI con
  `VentaResponse.LineaResponse` (springdoc los fusionaba → el desglose de deuda perdía sus campos). Renombrado a
  `LineaEstadoResponse`. **Al mergear: regenerar `:api-client` y cablear la pantalla de desglose de deuda del cliente**
  (que quedó pendiente en el front por esta colisión) + wire de reportes por sucursal en el front.
- Tests: `ReporteIntegrationTest` **11/11** (nuevo `los_ingresos_se_filtran_por_sucursal`: renta en sucursal A + venta en B;
  filtrar por A da 40, por B da 60, sin filtro 100). Suite completa en Docker (JDK21). Sin migración. Contrato OpenAPI cambia.

---

**Fase 15 — Desgloses, fotos en devolución, estado de cuenta del cliente y email inmutable (2026-07-20).**

Rama `feat/desgloses-fotos-estado-cuenta` (desde `origin/main`, PENDIENTE de merge). Cinco cambios para cerrar huecos
de gestión que el dueño reportó:
- **Fotos en devolución**: `DevolucionResponse.PiezaResponse` gana `nombre`+`fotoUrl` resolviendo cada `prendaId` contra
  `ConsultaDeInventario.resumenDePrendas` (igual que renta/venta); `DevolucionController` inyecta `ConsultaDeInventario`.
- **Estado de cuenta del cliente** (explica "por qué debe $X"): nuevo `GET /api/v1/clientes/{id}/estado-cuenta` →
  `EstadoDeCuentaResponse` (totales + una línea por renta con importe, daños, retraso, depósito, multa, pagado, saldo).
  Read-model `HistorialJdbcAdapter.estadoDeCuenta` **reutiliza** `MULTA_DE_RENTA`/`PAGOS_NETOS_DE_RENTA` para cuadrar
  exacto con el saldo agregado de `CargaDeCliente`. Dominio `LineaDeEstadoDeCuenta`; puerto en `ConsultarHistorial`.
- **Email inmutable**: `Cliente.editar` deja de aceptar email; se quita de `EditarClienteComando`/`EditarClienteRequest`;
  el `PUT /clientes/{id}` ignora cualquier email del body (el correo identifica la ficha y se fija al crear).
- **GET renta/venta por id**: `GET /api/v1/rentas/{id}` y `GET /api/v1/ventas/{id}` devuelven el DTO con líneas
  (nombre+foto) para pintar los artículos con imagen en pagos/cobros/reembolsos. `ConsultarVentas` gana `buscarPorId`.
- Tests: `DevolucionIntegrationTest` **16/16** (pieza trae nombre; estado de cuenta desglosa saldo 90 = importe 60 +
  multa 30, y baja a 50 tras cobro 40), `ClienteIntegrationTest` **10/10** (email inmutable), suite completa **471/471**
  en Docker (JDK21). **Al mergear: regenerar `:api-client` y cablear front**: pantalla de desglose de deuda, fotos en
  devolución/pagos/cobros/reembolsos. Sin migración. Contrato OpenAPI cambia (nuevos endpoints + `PiezaResponse`).

> Nota app (no backend): en la misma sesión se arreglaron 2 crashes de la app Android (ViewBinding: id compartido con
> StateView y `android:id` en TabItem), verificados en emulador. No tocan el backend.

---

**Fase 14 — Cartera de clientes: saldo + multa por cliente en el listado (2026-07-20).**

Rama `feat/cliente-saldos` (desde `origin/main`, PENDIENTE de merge). El front de gestión ya filtra la cartera
(PENDIENTES/VENCIDAS/MULTAS/SALDOS), pero `ClienteResponse` no traía las **cifras**, así que no se podía mostrar
cuánto debe cada cliente. Ahora el DTO del **listado** incluye **`saldoPendiente`** (Σ de sus rentas activas/devueltas
de `max(0, importe + multa − pagado)`) y **`multaTotal`** (Σ de multas), computados en un solo query agrupado por
cliente que **reutiliza los mismos fragmentos SQL** que el filtro de cartera (`MULTA_DE_RENTA`, `PAGOS_NETOS_DE_RENTA`,
consistentes con Devoluciones/Pagos). Nuevo record `dominio.CargaDeCliente`; puerto `HistorialReadRepository`/`ConsultarHistorial`
gana `cargaDeClientes(empresaId, clienteIds)` (solo la página actual, sin N+1); `ClienteController.listar` enriquece cada
`ClienteResponse`. Las respuestas de una sola ficha (crear/editar/estado) van con 0 (no se calcula ahí). Sin migración.
Tests: `ClienteIntegrationTest` **9/9** (asertan `multaTotal>0` y `saldoPendiente>0` para el cliente con multa; multa=$30),
ArchUnit 3/3, Modulith 1/1; suite completa en Docker. **Al mergear: regenerar `:api-client` y mostrar la cifra en la
lista de clientes de gestión** (`ClienteAdapter`).

---

## Fase 13 (cerrada — mergeada, PR #122)
**Fase 13 — Carrito con nombre + foto por línea (2026-07-20).**

Rama `feat/carrito-nombre-foto` (desde `origin/main`, PENDIENTE de merge). El carrito pendiente/agregar sólo devolvía
`prendaId` por línea → la app mostraba "Articulo xN" sin nombre ni imagen. Ahora `LineaDeCarritoResponse` gana
**`nombre`** y **`fotoUrl`**, poblados con `ConsultaDeInventario.resumenDePrendas(empresaId, prendaIds)` (mismo patrón
que las líneas de "Mis Pedidos"). `CarritoController` inyecta `ConsultaDeInventario`; sin migración (no toca esquema).
Tests: `CarritoIntegrationTest` **7/7** (aserción nueva: la línea trae `nombre`), ArchUnit 3/3 (edge pedidos→inventario
ya existía), Modulith 1/1; suite completa en Docker antes de subir. **Al mergear: regenerar `:api-client` y cablear el
adapter del carrito (`CarritoLineaAdapter`) para pintar nombre + foto (ya hay helper `cargarFoto`).**

### Deuda técnica / pendientes a futuro (documentado a pedido del usuario)
- **Push al cliente (notificaciones):** bloqueado en dos frentes. (a) Backend: `PUT /clientes/{id}/device-token` es
  **solo-staff** (`hasAnyRole(DUENO,ENCARGADO,MOSTRADOR,ATENCION)`) → el cliente no puede registrar su token; haría
  falta abrir una vía self-service (patrón del pago del cliente). (b) Infra: falta un **proyecto Firebase/FCM**
  (`google-services.json` + credenciales) que sólo puede crear el usuario. Sin push la app funciona; sólo faltan avisos
  con la app cerrada.
- **Credenciales de producción por aplicar en Railway (env vars):** hoy varias integraciones quedan "listas para
  credencial" y sin ellas el flujo real no cierra:
  - **MercadoPago (pasarela):** sin credencial, `POST /pagos/intento[/cliente]` no crea un checkout real (la app abre la
    URL pero no hay cobro). El **pago con tarjeta del cliente** depende de esto. (S3 de fotos ya está configurado.)
  - **WhatsApp (Meta/Twilio):** el canal registra el mensaje pero no lo envía; los mensajes automáticos ya están cableados.
  - **SMTP (email)** y **FCM (push)**: idem, listos para credencial.
  → Acción del usuario: setear esas env vars en Railway. Nada de código pendiente para estas (salvo push, ver arriba).

---

## Fase 12 (cerrada — mergeada a `main`)
**Fase 12 — Auditoría front↔backend + cierre del pago en línea del cliente (2026-07-20).**

**Contexto:** las 6 ramas de la Fase 11 ya están **mergeadas** en `main` (verificado: `origin/main` en `4ba7166`,
PRs #114–#119). Se hizo una **revisión completa de la app Android (`AppCustomi2`) contra la superficie real del
backend** (147 endpoints). Resultado:
- **Gestión ~90% + SuperAdmin + Auth: completos.** El **cliente** solo tiene un marketplace de **prendas sueltas**
  (explorar→catálogo→detalle→carrito→reserva→historial); **falta TODO el recorrido del disfraz** (tienda con 2
  apartados, detalle de disfraz, **ruleta**, rentar/comprar disfraz), la **pantalla de pago**, el **código de retiro**,
  las **imágenes** (Coil está en el build pero **sin usar**), y `device-token` (push del cliente) nunca se llama.
- El `:api-client` de la app está generado del contrato **viejo (Jul 14)**: no tiene fotoUrl del disfraz/prenda,
  vender/foto de disfraz, `?categoria`, `codigoRetiro`/`lineas` del historial, ni la ruleta con foto. **Regenerar.**

**Hueco de BACKEND encontrado al verificar (y cerrado en esta fase):** el **pago con tarjeta en línea del cliente**
no estaba soportado. `POST /pagos/intento` sacaba la empresa del **token** (`empresa_id`), pero el token de un
CLIENTE del marketplace **no** lo lleva → reventaba/era solo modo asistido por personal (regla de seguridad lo
restringía a DUENO/ENCARGADO/MOSTRADOR/ATENCION).

**Hecho — rama `feat/pago-en-linea-cliente` (PENDIENTE de merge, desde `origin/main`):**
- Nuevo endpoint **`POST /api/v1/pagos/intento/cliente`** (rol **CLIENTE**): el cliente indica `empresaId` (la tienda);
  su ficha y la **propiedad** de la venta/renta salen de su token. Verifica que la operación sea suya
  (`ResolucionDeClientes.fichaDeUsuarioSiExiste` + `clienteDeVenta`/`clienteDeRenta`) → si no, **403**
  (`PagoEnLineaNoAutorizado`, paralelo a `ReembolsoNoAutorizado`). Reutiliza el **mismo** cálculo de "total de golpe"
  (valida el monto contra el total pendiente; parcial → 400) y el switch `pagoEnLinea` de la empresa.
- Patrón calcado del **hermano** `POST /reembolsos/cliente`. Sin migración (no toca esquema). Regla de negocio en la
  **capa de aplicación** (`CrearIntentoDePagoDeCliente`), no en el controller.
- Tests: `IntentoDePagoDeClienteIntegrationTest` **4/4** (paga su propia venta→URL checkout con stub de pasarela;
  parcial→400; venta ajena→403; staff en el endpoint del cliente→403). **ArchUnit 3/3 y Modulith 1/1 en verde.**
  (Suite completa corrida en Docker antes de subir.)

**Falta (lo grande):** **toda la app Android del recorrido del cliente** (mañana). Backend: solo queda **diferido** el
filtro por **cercanía** del marketplace (necesita lat/lng que hoy no se guardan).

---

## Fase 11 (cerrada — todo mergeado a `main`)
**Fase 11 — Recorrido de compra + disfraz 100% + optimización con SigNoz (2026-07-19 → 07-20).**
Sesión larga de mejoras iterativas. Cada rebanada con tests + ArchUnit + Modulith en verde, probada en Docker
(la suite creció a ~460 tests). **OJO con el estado de `main`:** el usuario mergeó #107–#110; **quedan PENDIENTES
de merge** el PR **#111** (`feat/mis-pedidos-y-tiendas-operables`) y la rama **`feat/codigo-de-retiro`** (que trae
TODO el disfraz + código de retiro; se fue construyendo apilada para que entre como un bloque funcional). **Al
mergear: regenerar `:api-client`.**

**Hecho (mergeado, #107–#110):**
- Perf: elimina el N+1 al listar `/disfraces` (batch fetch + `default_batch_fetch_size`).
- RF-11.1: recordatorio de vencidas también al **dueño** (canal nuevo `IN_APP`).
- RF-11.2: aviso proactivo de **stock bajo** al dueño (scheduler + endpoint).
- RF-11.5/11.6: filtros de clientes por **PENDIENTES/VENCIDAS/MULTAS/SALDOS**.
- RF-15.4: **escalación** de solicitudes de empresa vencidas (log operable).
- Vitrina del marketplace expone la **fotoUrl** de la prenda.
- **Desglose** de renta/venta con **nombre + foto** por artículo (API pública `ConsultaDeInventario.resumenDePrendas`).

**Hecho (PENDIENTE de merge — #111):** "Mis Pedidos"/historial con **artículos + imagen** (líneas en `HistorialItem`);
vitrina **solo lista tiendas operables** (activas y con sucursal). (Raíz de "esta tienda no puede recibir pedidos":
la Casa Matriz solo se creaba al aprobar tiendas venidas de una SOLICITUD).

**Hecho (PENDIENTE de merge — `feat/codigo-de-retiro`):**
- **Código de retiro** (`R-XXXXXXXX`/`V-XXXXXXXX`, derivado del id) en renta y venta.
- **Disfraz COMPLETO:** precio de **renta** sugerido + precio de **venta** sugerido (suma de prendas, "desde" en pools);
  precios visibles en el **marketplace**; **foto propia** del disfraz (`POST /disfraces/{id}/foto`; API pública nueva
  `inventario.AlmacenDeImagenesPublico` reutiliza el S3 sin romper fronteras); **venta** del disfraz
  (`POST /disfraces/{id}/vender`, espejo de rentar, vía `ventas.RegistroDeVentas`).

**Decisiones del usuario en esta sesión (IMPORTANTES):**
- **Adelanto ELIMINADO:** todo se paga de golpe — **tarjeta → en línea el total; efectivo → todo en la tienda**. Sin pago
  parcial ni límite de fecha por adelanto.
- **Disfraz SIN depósito:** se paga por su **precio** (el que fija el dueño; la suma de prendas es solo la sugerencia). El
  daño en la devolución se cubre con la **multa** que el dueño define por prenda (`valorDano`/`valorReposicion`), vía la
  devolución normal (aplica al disfraz porque su renta es una renta común).

**Verificado que YA funcionaba (no eran huecos; correcciones a diagnósticos míos errados):**
- Los disfraces **sí** están en el marketplace (listar/detalle/ruleta/disponibilidad) y el cliente **sí** los renta
  (`POST /disfraces/{id}/rentar` acepta CLIENTE).
- La **devolución/multa por daño** aplica a la renta del disfraz.
- **Rentar y vender prendas sueltas** desde el cliente funciona (carrito `checkout-renta`/`checkout`).

**Hecho (PENDIENTE de merge — 4 ramas nuevas, cada una desde main actualizado, sin apilar, sin conflictos):**
- `feat/pagos-total-de-golpe`: **el pago en línea cobra el total pendiente y valida el monto** (sin adelanto). El intento
  ya no confía en el monto del cliente: calcula `importe/total − ya pagado` y exige que el monto lo cubra exacto (si no, 400).
- `feat/codigo-en-mis-pedidos`: **código de retiro también en "Mis Pedidos"** (`HistorialItem.codigoRetiro`).
- `feat/expirar-reservas`: **migración V57 `renta.creada_en`** (default now(), sin tocar dominio) + **job horario** que cancela
  las reservas RESERVADA que a las 24 h siguen **sin pagar** (cubre efectivo no retirado y tarjeta no pagada; respeta las pagadas).
- `feat/marketplace-filtro-categoria`: **`GET /marketplace/.../catalogo?categoria=<id>`** filtra el catálogo por categoría.

**Decisiones del usuario que cerraron el diseño de pagos:** **ADELANTO ELIMINADO** — todo se paga de golpe (tarjeta→en línea el
total; efectivo→todo en la tienda). El "checkout debe exigir el pago" se cumple **vía la expiración** (un pedido sin pagar
expira a las 24 h) + la **validación del monto**; NO hace falta forzar el pago síncrono en el checkout. Se descartó el catálogo
a nivel sucursal (RF-14.1/18.2) por decisión del usuario (no aporta lo suficiente).

**PENDIENTE (backend) — lo único que queda:**
- **Filtro por CERCANÍA** del marketplace (RF-18.1): requiere **coordenadas (lat/lng)** que hoy no se guardan (la sucursal tiene
  dirección y link de Maps, no geolocalización). Diferido: necesita un dato nuevo.
- **Perf (acción del usuario):** verificar en Railway la **región** DB↔app (los ~222 ms uniformes por query lo sugieren).

**PENDIENTE (app Android):** regenerar `:api-client` + todo lo listado en `AppCustomi2/ACTUALIZACION_FRONTEND.md`
(pintar fotos con Coil, pantalla de pago tarjeta/efectivo, dos apartados tienda disfraces/prendas, mostrar código de retiro,
filtros de clientes, foto del disfraz, etiqueta IN_APP).

---

### Fase 10 (historial — cierre de auditoría)
**Fase 10 — Auditoría y cierre profesional del backend + verificación E2E en vivo (2026-07-09 → 07-12).** Tras el flujo del
cliente (Fase 9), se hizo una **auditoría completa** y se cerraron **29 correcciones** por rebanadas (cada una PR + tests +
ArchUnit + Modulith en verde), todas **mergeadas a `main`** (V49→V53). El backend quedó **funcionalmente completo y con la
seguridad cerrada**. Resumen (detalle en `AUDITORIA_Y_CORRECCIONES.md`):
- **CRUD de mantenimiento completo:** familia **editar + archivar/activar** en prenda, categoría, tipo/valor de etiqueta,
  sucursal, empleado (baja/reactivar) y **cliente** (R-E); **borrar físico de grupo de stock** con guarda (R-F); conteo de
  dependencias antes de archivar (R-G); fix global 500→**409** (violación de unicidad).
- **Barrido de seguridad §6 (cerrado):** aislamiento multi-tenant verificado; validación de sucursal/cliente al crear
  (SEC-1/2); `@Filter` en IntentoDePago (SEC-3); **webhook de pago firmado HMAC** (SEC-5); lista negra bloquea renta (B1);
  empleado acotado a su sucursal (B2); **pirámide de roles** en gestión de personal (B3); **rate-limiting** de auth (A2);
  allowlist de imagen por magic bytes (C1); **paginación** de listas (C3); headers/HSTS (C4); **scan de dependencias**
  Dependabot + dependency-review (C5); **refresh JWT revocable con rotación y detección de reuso + logout** (C2).
- **Gestión de personal completa:** `GET /empleados` (G1), `PUT /empleados/{id}/rol` (G2).
- **Pagos en línea listos-para-credenciales:** **P-3** (verificar el pago contra MercadoPago antes de confirmar, gateado) y
  **P-6/reembolsos**: nuevo **workflow de reembolso en 2 pasos** (solicitar → aprobar/rechazar con motivo), con **precondición
  de ítem devuelto**, **escalamiento por pirámide**, refund a tarjeta gateado, y **self-service del cliente** (correcciones 28/29).
- **Contrato OpenAPI** verificado y con `ProblemDetail` documentado; **verificación E2E en vivo** (jar de prod + Postgres real,
  todos los roles, correcciones 1–29): **102/102 OK** (ver `REPORTE_E2E.md`).
- **Pendiente = solo externo:** credenciales MercadoPago (sandbox) para el checkout alojado y el refund real, y —opcional—
  la migración a **Spring Boot 4** (PRs Dependabot en rojo, van juntos). El **frente Android** arranca desde cero con los 3
  docs de la app ya actualizados a estas correcciones.

---

### Fase 9 (historial — fase anterior)
**Fase 9 — Flujo del CLIENTE del marketplace + expansión disfraz/precios/multas/reembolsos (2026-07-08).** Con el backend
desplegado en **Railway** (`just-upliftment-production-cb1f.up.railway.app`, Dockerfile + `server.port=${PORT}`, SuperAdmin
por env), se detectó que el **flujo de compra del CLIENTE** (rol sin `empresa_id`) estaba roto (403/500/FK) y se completó;
luego se expandió el modelo de disfraz y todo el ciclo de precios/multas/devoluciones/reembolsos según nuevos
requerimientos de Juan. Todo por **rebanadas pequeñas, cada una con su PR + tests + ArchUnit + Modulith en verde**.

- **Flujo del cliente del marketplace — HECHO y MERGEADO (PRs #46–#49, migración V41):** el rol **CLIENTE** (token sin
  `empresa_id`) ya puede comprar/rentar en cualquier tienda. (a) **Carrito + checkout** venta y renta para CLIENTE:
  `clientes.ResolucionDeClientes.fichaDeUsuario` crea/enlaza automáticamente la **ficha de cliente por tienda** (columna
  `cliente.usuario_id`, **V41**, índice único empresa×usuario), resolviendo la FK `carrito.cliente_id`; `CarritoController`
  y `DisfrazController` resuelven empresa/cliente **por rol** (staff = del token; CLIENTE = empresaId del request + su
  propia ficha). SecurityConfig habilita CLIENTE en `/carritos/items`,`/checkout`,`/checkout-renta` y en rentar disfraz.
  (b) **Rentar disfraz personalizable** + disponibilidad para CLIENTE. (c) **Vitrina pública** `GET /marketplace/empresas/
  {id}/disfraces` (+ `/{disfrazId}` con estructura completa). (d) **"Mis Pedidos"** `GET /clientes/me/historial` (une las
  fichas del usuario en todas las tiendas). Fue un hueco real: antes solo se había probado como staff.

- **Expansión disfraz/precios/multas/reembolsos — 8 rebanadas, migraciones V42–V48, 332 tests verdes.** Rebanadas 1–4
  MERGEADAS (PRs #50–#53); 5–8 listas para aplicar (parches `slice-N-*.patch`, mismo flujo per-PR):
  1. **Disfraz siempre por-partes (V42):** se elimina el modo `UNIDAD_FIJA` (una pieza = 1 slot fijo); la **talla deja de
     ser eje del slot** (se modela como etiqueta del pool); `activo`/**editar** (`PUT /disfraces/{id}`) + **archivar/activar**.
  2. **Precio general dual del disfraz (V43):** `precioRentaGeneral` (por día) que **anula la suma por prendas**; al rentar
     se reparte proporcional entre líneas para que el total iguale el precio del conjunto.
  3. **Devolución — piezas faltantes (V44):** una pieza que no llegó o `PERDIDA` **no cierra la renta** hasta devolverla o
     marcarla **perdida + cobrada** (`perdidaCobrada`); solo piezas resueltas consumen unidad, mueven inventario y cierran.
  4. **Ruleta (sin migración):** `GET /marketplace/empresas/{e}/disfraces/{d}/slots/{orden}/opciones` (público) lista las
     prendas concretas del pool con **stock, precio y etiquetas**, filtrable por `valores`. Puerto `ConsultaDeInventario.
     opcionesDelPool`/`opcionDePrenda`.
  5. **Valor de multa por prenda (V45):** `Prenda.valorReposicion` (pérdida) y `valorDano` — base para sugerir el cobro.
  6. **Política de retraso fija/acumulativa (V46):** `configuracion.modoRecargoRetraso` (ACUMULATIVA por defecto | FIJA);
     el puerto `ConsultaDeConfiguracion.recargoPorRetraso(empresaId, dias)` encapsula el cálculo.
  7. **Reembolsos por local (V47):** `reembolsosActivos` + `ventanaReembolsoDias` (0 = sin límite); `Venta.creadaEn` para
     la ventana; `POST /ventas/{id}/devolver` **gatea** con la política (→ 409 si no aplica).
  8. **Reembolso PARCIAL de venta (V48):** `LineaDeVenta.cantidadDevuelta`; `Venta.devolver(cantidades)` (vacío = todo lo
     pendiente) → estado **`PARCIALMENTE_DEVUELTA`**/`DEVUELTA`, reingresa solo lo devuelto; el endpoint acepta cuerpo
     opcional `{lineas:[{prendaId,cantidad}]}`; respuesta con `cantidadDevuelta` por línea y `montoReembolsado` proporcional.

**Fase 8 — Cierre TOTAL del backend (work order de Juan, 2026-07-07).** Se cierra todo lo pendiente: Grupo A (infra
enchufable, gateada por credencial → `docs/INFRA_PENDIENTE.md`), Grupo B (lógica diferida: renta multi-artículo, checkout
de renta, disfraz→renta, devolución parcial, stock por sucursal), deuda menor, y barrido final RF-0…18. Rebanada por
rebanada, cada una su PR con tests + ArchUnit + Modulith en verde; nada se declara "cerrado" sin CI verde.

**Correcciones post-barrido RF-0…18 (2026-07-08).** Tras cerrar Grupos A/B/C, un barrido RF contra el código (4 auditores en paralelo) encontró 6 inconsistencias reales; se cierran una por una:
- **Fix 3 (PR `fix/checkout-idempotente`, RF-17.6):** el **checkout del carrito** (venta y renta) es idempotente ante doble submit. El secuencial ya daba 404 (el carrito pasa a CONFIRMADO); el hueco era el **concurrente** — ahora un **advisory lock** (`pg_advisory_xact_lock` por empresa×sucursal×cliente×tipo, mismo patrón que la reserva de renta) serializa el checkout, así el 2º encuentra el carrito ya confirmado → 404, sin crear ventas/rentas duplicadas. Suite completa verde (308).
- **Fix 2 (PR `fix/renta-empleado`, RF-1.4):** la **Renta ahora registra el usuario que la creó** (`empleadoId`, columna `empleado_id` V39, nullable — empleado en asistido o cliente en autoservicio). Se enhebra desde el JWT en `/rentas` (asistido) y en los checkouts de renta (carrito vía `ContextoDeTenant`, disfraz→renta). `RegistroDeRentas.registrar` gana el parámetro. Antes solo Venta/Pago/Turno guardaban el usuario. Suite completa verde (308).
- **Fix 1 (PR `fix/multa-correcta`, RF-6.6/5.2 + RF-12.2):** con el módulo de multas **apagado** la devolución ya **no genera ningún cargo** (antes solo anulaba el recargo por retraso, seguía cobrando daños). Y el **recargo por retraso se deriva** del `recargoPorRetrasoPorDia` configurado × días de atraso (antes se guardaba pero nunca se usaba; lo pasaba el caller a mano — ahora es opcional/override). Nuevos puertos `ConsultaDeConfiguracion.recargoPorRetrasoPorDia` y `ConsultaDeRentas.fechaDevolucionDeRenta`. Suite completa verde (307).

- **Rebanada 11 (PR `feat/grupo-c-deuda-menor`) — HECHA (sin credenciales, cierre real; Grupo C):** deuda menor de auth/empleados.
  **RF-1.5 permisos granulares por empleado:** plantilla de permisos por rol (`PlantillaDeRol`) + overrides por empleado
  (`permiso_empleado`, migración **V37**); un `InterceptorDePermisos` mapea cada request a (sección, acción) y responde
  **403 solo si el dueño desactivó explícitamente esa casilla** (deny-override → sin overrides, el comportamiento es idéntico
  y todos los tests siguen verdes; la línea base por rol la mantiene `SecurityConfig`); editor `GET/PUT /empleados/{id}/permisos`.
  **RF-1.2/8.1 usuario↔N sucursales:** tabla `usuario_sucursal` (**V38**) + `PUT/GET /empleados/{id}/sucursales` (valida
  empleado y sucursales del tenant). **RF-8.2 registro de actividad:** `GET /empleados/{id}/actividad` (nº de ventas y monto
  confirmados del empleado, vía el puerto público `ConsultaDeVentas.actividadDeEmpleado`; los turnos/cortes viven en Caja).
  Suite completa en verde (306 tests) con tests de revocar→403/reactivar→201, asignación de 2 sucursales y actividad 0.
- **Rebanada 10 (PR `feat/devolucion-parcial`) — HECHA (sin credenciales, cierre real; Grupo B):** RF-5.5/5.6 devolución
  parcial. Cada `PiezaRevisada` se liga a su **prenda/artículo** (`prenda_id` en `pieza_revisada`, migración **V36**,
  backfill desde la prenda principal), así el daño/pérdida se atribuye al grupo de stock correcto (RF-5.6) — el retorno de
  inventario ahora es **por artículo**. La devolución puede ser **parcial**: la renta solo pasa a **DEVUELTA** cuando la
  suma de piezas revisadas en todas sus devoluciones cubre todas las unidades rentadas; si no, sigue **ACTIVA** y admite
  más devoluciones. Valida que no se devuelvan más unidades de las rentadas por artículo. Nuevo `ConsultaDeRentas.lineasDeRenta`
  y `DevolucionRepository.listarPorRenta`. Suite completa en verde (303 tests) con 2 tests nuevos (parcial→ACTIVA→DEVUELTA;
  400 al exceder). _Depende de la Rebanada 7._
- **Rebanada 9 (PR `feat/disfraz-renta`) — HECHA (sin credenciales, cierre real; Grupo B):** RF-2.3/3.1 rentar un disfraz
  armado por partes. Nuevo caso de uso **`RentarDisfraz`**: resuelve el disfraz a sus prendas concretas — unidad fija = su
  prenda; por partes = la prenda fija de cada slot obligatorio + la elegida por el cliente en los personalizables
  (validada contra el pool con el nuevo **`ConsultaDeInventario.prendaEnPool`**), respetando los slots opcionales — y crea
  una **renta multi-artículo** vía **`RegistroDeRentas`** (Rebanada 8). `POST /disfraces/{id}/rentar`. Dominio + ArchUnit +
  Modulith (disfraces→rentas/inventario por API pública) + integración (2 tests nuevos: resolución fijo+personalizable →
  renta de 2 líneas, y 400 si la prenda elegida no pertenece al pool) en verde local (suite completa, 301 tests). _Depende
  de Rebanadas 7 y 8._
- **Rebanada 8 (PR `feat/checkout-renta`) — HECHA (sin credenciales, cierre real; Grupo B):** RF-16.4/18.6-7 checkout de
  RENTA por carrito con **fechas por línea**. La `LineaDeCarrito` gana su periodo (retiro/devolución, columnas en
  `linea_de_carrito`, migración **V35**; nulas en venta); al agregar a un carrito de RENTA las fechas son obligatorias y
  la clave de agrupación es (prenda, periodo). Nuevo puerto público **`RegistroDeRentas`** (espejo de `RegistroDeVentas`)
  que Rentas implementa. El checkout de renta **agrupa las líneas por periodo** y crea **una renta multi-artículo por
  periodo distinto** (`POST /carritos/checkout-renta` → lista de renta ids), confirmando el carrito; el precio sale de
  `ConsultaDeInventario.precioRenta` (nuevo). El depósito/garantía se gestiona en el pago (RF-6.2/6.8). Dominio +
  ArchUnit + Modulith (pedidos→rentas por API pública) + carrito integración (con test nuevo de 2 periodos → 2 rentas) en
  verde local; CI confirma. _Depende de la Rebanada 7._
- **Rebanada 7 (PR `feat/renta-multi-articulo`) — HECHA (sin credenciales, cierre real; Grupo B):** RF-3.1/16.2 renta
  multi-artículo. La `Renta` pasa de una prenda a **N líneas** (`RentaLinea`: prenda, cantidad, precio/día); tabla
  **`renta_linea`** (migración **V34**, backfill 1 línea por renta con id = id de la renta). El importe es
  **Σ (precio×cantidad) × días**. La disponibilidad (RF-3.2) se controla **por prenda** sumando cantidades solapadas
  (`sumarCantidadSolapada` sobre `renta_linea`, antes contaba rentas). El request acepta la **forma compatible**
  (`prendaId`+`precioPorDia`, 1 artículo) **o** `lineas[]`; el response expone `lineas` y conserva el artículo
  principal (`prendaId`/`precioPorDia` = 1ª línea) para vistas de una prenda. La cabecera `renta` mantiene el artículo
  principal denormalizado, así **Devoluciones, rentas-vencidas y contrato PDF no cambian**; el reporte **más-rentados**
  ahora cuenta unidades por línea. _Devolución multi-artículo (por pieza/QR) = Rebanada 10._ Dominio + ArchUnit +
  Modulith + integración tocada en verde local (rentas —con 2 tests nuevos—, devoluciones, reportes, clientes,
  notificaciones, pagos, carrito); confirmación vía CI.
- **Rebanada 6 (PR `feat/stock-por-sucursal`) — HECHA (sin credenciales, cierre real; Grupo B):** RF-18.2 stock por
  sucursal + RF-10.3 transferencia entre sucursales. `GrupoDeStock` gana `sucursalId` (migración **V33**, backfill a la
  1ª sucursal de cada empresa por nombre). El API pública de Inventario (`ConsultaDeInventario.unidadesDisponibles`,
  `AjusteDeInventario.descontarDisponibles`/`procesarRetornoDeRenta`) pasa a estar **acotada por sucursal**; rentas,
  ventas y devoluciones enhebran su `sucursalId` (la renta lo aporta vía `ConsultaDeRentas.sucursalDeRenta`). La clave de
  variante duplicada es **por (prenda, sucursal)**: la misma variante puede vivir en 2 sucursales como grupos aparte.
  Nuevo caso de uso `TransferirStock` + `POST /api/v1/grupos-stock/{id}/transferir` (mueve disponibles al grupo de la
  misma variante en la sucursal destino, creándolo si no existe; audita vía `StockAjustado`). `sucursalId` obligatorio al
  crear grupo y expuesto en el `GrupoDeStockResponse`. **Dominio + ArchUnit + Modulith + toda la integración tocada en
  verde localmente con Docker** (inventario, ventas, rentas, devoluciones, reportes, carrito, pagos, clientes,
  notificaciones, disfraces); confirmación final vía CI.
- **Rebanada 5 (PR `feat/pasarela-pago`) — código completo, pendiente credencial:** RF-6.11 pago en línea. Puerto
  `PasarelaDePago` + adaptador **MercadoPago gateado** (sin token → 503) + `IntentoDePago` (migración V32).
  `POST /pagos/intento` (exige switch `pagoEnLinea` activo → si no, 409) crea el checkout; `POST /pagos/webhook` (público)
  confirma y **registra el Pago reutilizando la idempotencia existente** (clave = id externo). `pagoEnLinea` agregado a
  `ConsultaDeConfiguracion`. INFRA_PENDIENTE actualizado. Unit test + ArchUnit + Modulith en verde; integración vía CI.
- **Rebanada 4 (PR `feat/canales-notificacion`) — código completo, pendiente credenciales:** RF-11.4 WhatsApp + RF-18.11 FCM.
  Adaptadores `CanalWhatsApp` (Meta Cloud API) y `CanalFcm` (HTTP) **gateados** + `RouterDeCanales` (@Primary) que despacha
  por `canal` y cae al log si no hay credencial/contacto. `ContactoDelCliente` (JDBC) resuelve teléfono/device_token.
  `device_token` agregado al cliente (migración V31) + `PUT /clientes/{id}/device-token`. INFRA_PENDIENTE actualizado.
  Unit test del router + compila + ArchUnit + Modulith en verde; integración vía CI.
- **Rebanada 3 (PR `feat/fotos-s3`) — código completo, pendiente credencial AWS:** RF-2.9 fotos de prenda.
  `POST /api/v1/prendas/{id}/foto` (multipart) → sube a S3 y guarda `foto_url` (migración V30; `fotoUrl` en el DTO).
  Puerto `AlmacenDeImagenes` + adaptador S3 **gateado** (sin bucket/región → 503 "no configurado"). Credencial en
  `docs/INFRA_PENDIENTE.md`. Unit test del gating + compila + ArchUnit + Modulith en verde; integración vía CI.
- **Rebanada 2 (PR `feat/export-pdf`) — HECHA (sin credenciales, cierre real):** RF-9.2 export de reportes en PDF
  (`/reportes/export/rentas-vencidas.pdf`, `/reportes/export/inventario-tablero.pdf`) + RF-3.4 comprobante de pago
  (`/pagos/comprobante.pdf`) y contrato de renta (`/rentas/{id}/contrato.pdf`). Librería OpenPDF (LGPL/MPL, sin
  credenciales); utilidad compartida `GeneradorDePdf`. Test unitario del generador + integración del endpoint. Compila,
  ArchUnit + Modulith + unit en verde local; integración vía CI.
- **Rebanada 1 (PR `feat/recuperar-contrasena`) — HECHA (código completo, pendiente credencial SMTP; CI verde, PR #26 mergeado):** RF-1.1 recuperar contraseña. `POST /auth/olvide`
  (204, no revela) + `POST /auth/restablecer`; token de un solo uso hasheado (SHA-256) con vencimiento (tabla
  `token_recuperacion`, migración V29); puerto `EnviadorDeEmail` + adaptador SMTP **gateado** (sin SMTP → log). Compila,
  ArchUnit + Modulith + dominio en verde local; **integración pendiente de CI** (Docker local intermitente). Credencial
  SMTP listada en `docs/INFRA_PENDIENTE.md`.


**Fase 7 — Rediseño marketplace + onboarding de tiendas (decisión de Juan, 2026-07-06).** Nuevo enfoque: **todos entran
como CLIENTE** (se auto-registran, ven todas las tiendas tipo marketplace); un botón "Registrar mi tienda" manda la
solicitud (empresa PENDIENTE); el **superadmin la ve en un panel in-app** (sin email por ahora) y la aprueba; al aprobar,
esa **misma cuenta** se promueve a **Dueño** y se le desbloquea "Mi Local". Aislamiento: el dueño solo ve su empresa
(multi-tenant ya lo garantiza); el cliente ve todas (marketplace público).
- **Etapa 1 (PR `feat/registro-cliente`) — HECHA:** nuevo rol `CLIENTE` (usuario sin empresa; `Rol.requiereEmpresa()`
  generaliza la regla de tenant en `Usuario`), y **auto-registro público** `POST /api/v1/auth/registro` (crea cliente +
  auto-login). 26 tests verdes (5 nuevos), ArchUnit + Modulith en verde.
- **Etapa 2 (PR `feat/solicitud-tienda`) — HECHA (backend):** el registro de empresa (`POST /empresas`) ahora acepta
  `ubicacion` + `contacto` y, si viene con token de un CLIENTE, guarda su id como `solicitante_id` (migración V28,
  columnas opcionales). El panel del superadmin (`GET /empresas/pendientes`) expone esos datos + el solicitante, así el
  superadmin sabe a quién promover a Dueño. La "notificación in-app" ES esa cola de pendientes. 28 tests verdes (2 nuevos).
- **Etapa 3 (PR `feat/aprobar-promueve-dueno`) — HECHA:** al aprobar una solicitud de tienda, si tiene solicitante se
  crea la sucursal "Casa Matriz" (con la ubicación cargada) y el **cliente solicitante se promueve a DUEÑO** de la empresa
  (misma cuenta: `Usuario.promoverADueno`). Al re-loguearse ya es DUEÑO de su empresa. El registro clásico sin solicitante
  se comporta igual que antes. `buscarPorId` usa `findFirstById` (respeta el filtro tenant §5.4). Tests verdes (2 nuevos).
- **Etapa 4 (PR `feat/marketplace-catalogo`) — HECHA:** `GET /api/v1/marketplace/empresas/{empresaId}/catalogo`
  (público) devuelve las prendas no archivadas de una tienda ACTIVA (nombre, tipo, precios, categoría) vía el adaptador
  JDBC del marketplace (cruza tenants a propósito, solo lectura pública). El cliente ve todas las tiendas + su catálogo.
  Tests verdes (2 nuevos, incluye el flujo completo cliente→tienda→dueño→prenda→catálogo público).
- **Etapa 5 (pendiente):** apps — registro cliente, ver tiendas + catálogo, "Registrar mi tienda", panel superadmin,
  "Mi Local". Requiere re-sincronizar el cliente Kotlin regenerado (ajustando los métodos renumerados).

**Fase 6 — Integración con las apps Android (2026-07-06).** Conectando las dos apps al backend en Railway.
- PR #20 `feat/sucursales-listado` (MERGEADO): `GET /api/v1/empresas/{empresaId}/sucursales` (cualquier usuario
  autenticado del tenant) + `BootstrapDemo` crea "Casa Matriz" para la empresa demo. Desbloquea rentas/ventas en las apps.
- PR `feat/seed-cliente-demo`: `BootstrapDemo` ahora también siembra (opcional, idempotente) una cuenta de **MOSTRADOR**
  en la empresa demo para la app del cliente, leída de `COSTUMI_DEMO_CLIENTE_EMAIL/PASSWORD`. El rol define la
  experiencia al ingresar (mismo login para dueño y cliente). ArchUnit + Modulith en verde.

**Fase 5 — Cierre del backend (backlog de Juan, 2026-07-06) — COMPLETO en código.** El **núcleo** está mergeado en
`main` (PR #6→#15); el **backlog de cierre completo** está en la **PR final `feat/cierre-final`** (262 tests verdes),
lista para el merge de Juan. Solo queda lo bloqueado por infra/decisión externa (envío real WhatsApp/FCM/email, S3,
pasarela, export PDF) y deuda menor (permisos granulares por-usuario, devolución parcial, transferencias entre sucursales).

> ⚠️ **Dónde vive este PROGRESS:** se actualiza en cada **rama de feature**; en `main` solo aparece cuando Juan
> **mergea** la PR (regla: nada entra a `main` sin su revisión). Si en `main` se ve viejo, es porque la PR con el
> update aún no se ha mergeado — no porque no se actualice. Para verlo al día sin mergear, léelo en la rama de la PR.

### ✅ Hecho y MERGEADO en `main` (PR #6→#15) — núcleo + Pagos + Reportes + Config + Clientes
Auth JWT + seguridad rol/tenant + bootstrap (RF-1 base, RF-17.4) · **§5.4 aislamiento multi-tenant FORZADO**
(filtro Hibernate, cross-ref, find-por-PK, ArchUnit) · Catálogo/taxonomía (categorías, etiquetas, tipo↔categoría,
rename, seed al aprobar) · Inventario (Prenda, GrupoDeStock, variantes) · Disfraz+Slot+disponibilidad derivada ·
Clientes base (ficha, búsqueda, lista negra) · Carrito (RF-16) + checkout→venta · **Renta** ciclo de estados +
disponibilidad por fechas + advisory lock + idempotencia · **Devolución** checklist + multa + inventario + evento ·
**Venta/POS** baja de stock atómica · **Pagos COMPLETO (RF-6)** — reembolsos/saldo, **mixto+vuelto, depósito-retención,
comprobante, impuesto configurable** (mergeado en PR #13/#14, con la validación del mixto vs. saldo pendiente) ·
**Caja/Turno** corte y cuadre por método · **Reportes** ganancia = ingreso−costo (parcial) · Configuración switch de
multas real · Auditoría y notificación por eventos · Marketplace búsqueda por texto. **Bugs de la revisión final
cerrados** (PR #12: multa respeta el switch, advisory lock anti-sobreventa, avisos en AFTER_COMMIT).

**MERGEADO en `main` por la PR #15 (2026-07-06):**
- **Reportes reales (RF-9):** rentas vencidas, depósitos activos, ingresos por método (con fecha/sucursal),
  rankings vendidos/rentados, ventas por empleado, desglose por etiqueta, tablero de inventario (9.3) + resumen
  (valor/utilización/dañados), export **CSV** (9.2). Falta solo export **PDF** (requiere librería = decisión).
- **Config (RF-12):** los switches controlan de verdad (multas, multi-sucursal, conteo-stock); reglas por
  defecto moneda + recargo (12.2); respaldo/restauración export/import (12.3). Falta `pagoEnLinea` (infra).
- **Clientes (RF-7):** historial del cliente (7.2), filtro de pendientes (11.5/11.6).

### 🔵 En revisión (PR abierta, NO mergeada)
- **PR final del cierre (rama `feat/cierre-final`)** — los **6 bloques restantes**, 262 verdes: empleados (RF-8),
  reabastecimiento/ajuste con motivo (RF-10), extensión de renta (RF-3.6), recordatorio de vencidas (RF-11.1),
  refresh token (RF-1.1) y verificación del contrato OpenAPI completo (RF-17.3). **Con este merge el backend queda cerrado.**
- **PR #16 (solo docs)** — trae el PROGRESS al día a `main`.

### ✅ Backlog de cierre COMPLETO (código) — en la PR final del cierre
Todo lo code-doable del backlog de Juan quedó hecho y verde (262 tests):
1. **Empleados (RF-8)** — alta de empleado por la empresa (`POST /empleados`), correo único, no SUPERADMIN, login.
2. **Reabastecimiento (RF-10)** — ajuste de stock con motivo, auditado por evento (`POST /grupos-stock/{id}/ajuste`).
3. **Huecos renta** — extensión/renovación de renta con recálculo de importe (`POST /rentas/{id}/extender`).
4. **Notificaciones (RF-11.1)** — recordatorio de vencidas al cliente (`POST /notificaciones/recordar-vencidas`).
5. **Plataforma (RF-1.1)** — refresh token (`POST /auth/refresh`, rotación).
6. **OpenAPI** — el contrato en `/v3/api-docs` publica TODOS los endpoints (test que lo verifica).

**Deuda menor — CERRADA (Rebanada 11):** permisos granulares por empleado (RF-1.5), usuario↔N sucursales (RF-1.2/8.1) y
registro de actividad (RF-8.2). La devolución parcial (RF-5.5) se cerró en la Rebanada 10.

### 🚫 Bloqueado por decisión/infra (va detrás de config, NO frena el resto)
- ~~Impuestos (RF-6.5/12.2)~~ — **RESUELTO y MERGEADO** (decisión de Juan): tasa única por empresa en
  `ConfiguracionEmpresa`, precio impuesto-incluido, desglose (base+impuesto) en el comprobante. En `main` (PR #14).
- **S3/fotos (RF-2.9)**, **WhatsApp/FCM (RF-11.4/18.11)** y **Pasarela (RF-6.11)** — **CÓDIGO COMPLETO, gateado**, solo
  pendientes de credencial (ver `docs/INFRA_PENDIENTE.md`). Rebanadas 3, 4 y 5 del cierre (PR #28, #29 mergeadas; #31 pasarela).
  **Recuperar contraseña (RF-1.1)** también (PR #26, credencial SMTP). Con esto el **Grupo A queda cerrado en código.**

### Arquitectura fijada (2026-07-06)
**Repos separados:** este repo `costumi-backend` (Java/Spring) es el único que despliega Railway; la app **Android**
(Kotlin) vive en **su propio repo**. Se conectan **solo por el contrato OpenAPI**. Nunca se mete código Android aquí.

### Definición de cierre
Todo lo ⬜ PENDIENTE (menos lo 🚫 bloqueado) verde + **OpenAPI completo publicado**. Con eso, en el repo Android se
genera el cliente Kotlin y arranca la app. Reglas firmes: tests de dominio por feature, 1 commit por feature, sin inventar.

## Estado actual → ver el tablero de arriba y el de módulos
> ⚠️ Las antiguas secciones "Pendiente de revisión" y "Próximo paso concreto" quedaron **obsoletas** y se
> retiraron por causar confusión: todo lo que antes figuraba "en la rama esperando revisión" **ya está
> mergeado en `main` (PR #6→#12)**. La **única verdad del estado actual** es el tablero del principio de este
> archivo (✅ hecho / 🔵 en PR / ⬜ pendiente / 🚫 bloqueado) + el **Tablero de módulos** de abajo. El detalle
> cronológico (qué se hizo y cuándo, con lo que en su día estaba pendiente) vive en el **Registro de sesiones**.

## Tablero de módulos
Estado: ✅ hecho y **mergeado en `main`** · 🔵 en **PR abierta** (sin mergear) · 🟨 parcial · ⬜ sin empezar
> Nota: los ✅ que dicen *"código completo, pendiente credencial"* (S3, WhatsApp/FCM, y —cuando entre— Pasarela) están
> **terminados en código y gateados**; solo falta cargar la credencial externa (listado en `docs/INFRA_PENDIENTE.md`).
> Lo pendiente real de negocio está marcado como **Grupo B** (renta multi-artículo, checkout de renta,
> devolución parcial; **stock por sucursal ya hecho — Rebanada 6**) y **Grupo C** (deuda menor). Sincronizado con el Registro de sesiones (2026-07-07).

| Módulo | Rigor | Estado | Detalle (mergeado salvo que diga PR) |
|---|---|---|---|
| Andamiaje + anti-erosión (ArchUnit/Modulith/CI) | — | ✅ | PR #1 |
| Identidad y tenant (Empresa/Sucursal/Usuario/auth) | Hexagonal | ✅ | Auth JWT + rol/tenant + bootstrap + §5.4 forzado + refresh + alta empleados (RF-8) + **GET /sucursales (#20)** + **marketplace: rol CLIENTE + auto-registro (#22), solicitud de tienda (#23), aprobar→promueve a Dueño+Casa Matriz (#24)** + **recuperación de contraseña (RF-1.1, #26)** + **permisos granulares por empleado (RF-1.5): interceptor de deny-override sobre la plantilla del rol + editor `GET/PUT /empleados/{id}/permisos` (#11)** + **usuario↔N sucursales (RF-1.2/8.1): `PUT/GET /empleados/{id}/sucursales` (#11)** + **registro de actividad de ventas del empleado (RF-8.2): `GET /empleados/{id}/actividad` (#11)**. |
| Catálogo y taxonomía (etiquetas, categorías) | Hexagonal | ✅ | Categoría, TipoEtiqueta/ValorEtiqueta, tipo↔categoría, renombrar, siembra al aprobar |
| Inventario y disponibilidad | Hexagonal | ✅ | Prenda, GrupoDeStock, variantes, stock-bajo + ajuste con motivo auditado (RF-10) + **fotos de prenda en S3 (RF-2.9, #28 — código completo, gateado)** + **stock por sucursal (RF-18.2): `GrupoDeStock.sucursalId` (migración V33 con backfill), disponibilidad/baja/retorno acotados a la sucursal, misma variante en 2 sucursales = grupos aparte, y transferencia entre sucursales `POST /grupos-stock/{id}/transferir` (RF-10.3) — Rebanada 6, Grupo B** + **Fase 9: valores de multa por prenda (`valorReposicion`/`valorDano`, V45) + `ConsultaDeInventario.opcionesDelPool`/`opcionDePrenda` para la ruleta** |
| Disfraces (capa 3) | Hexagonal | ✅ | Disfraz+Slot+pool + disponibilidad derivada + **rentar disfraz (RF-2.3/3.1): resuelve slots (fijo→su prenda; personalizable→prenda elegida validada contra el pool con `ConsultaDeInventario.prendaEnPool`) → renta multi-artículo vía `RegistroDeRentas`, `POST /disfraces/{id}/rentar` — Rebanada 9** + **expansión Fase 9: disfraz siempre por-partes (fuera `UNIDAD_FIJA`; talla = etiqueta del pool) + editar/archivar (`PUT`/`archivar`/`activar`, V42) + precio general dual `precioRentaGeneral` (V43) + rentar/vitrina para el CLIENTE del marketplace + ruleta `GET …/slots/{orden}/opciones`** |
| Pedidos / carrito | Hexagonal | ✅ | Carrito segmentado + checkout→venta + **checkout→renta con fechas por línea (RF-16.4/18.6-7): línea de carrito con periodo (V35), agrupa por (retiro,devolución) → una renta multi-artículo por periodo vía `RegistroDeRentas`, `POST /carritos/checkout-renta` — Rebanada 8**. |
| Rentas | Hexagonal | ✅ | Crear + estados + disponibilidad + advisory lock + idempotencia + extensión/renovación (RF-3.6) + contrato PDF (#27) + **multi-artículo (RF-3.1/16.2): renta con N líneas (`renta_linea`, V34), importe = Σ precio×cantidad×días, disponibilidad por línea, request compatible (1 artículo o `lineas[]`) — Rebanada 7** + armado por partes (Rebanada 9) + devolución parcial (Rebanada 10) |
| Ventas / POS | Hexagonal | ✅ | Venta + descuento + total + baja de stock atómica (anti-sobreventa) + **devolución de venta (RF-4.5): `POST /ventas/{id}/devolver` reingresa stock; el reintegro va por un pago REEMBOLSO** + **política de reembolso por local (Fase 9): `reembolsosActivos` + ventana en días gatean la devolución (→409, V47) + reembolso PARCIAL: devolver unidades por prenda → `PARCIALMENTE_DEVUELTA`, `montoReembolsado` proporcional (V48)** |
| Pagos, caja y depósitos | Hexagonal | ✅ | Reembolsos/saldo/idempotencia + mixto+vuelto, depósito-retención, comprobante (+ PDF #27), impuesto configurable + **pago en línea / pasarela MercadoPago (RF-6.11, #31 — código completo, gateado): /pagos/intento + /pagos/webhook idempotente** |
| Caja / turno | Hexagonal | ✅ | Turno + movimientos + corte y cuadre por método |
| Devoluciones y multas | Hexagonal | ✅ | Checklist + multa (respeta switch) + inventario + evento + **devolución parcial (RF-5.5/5.6): cada pieza se liga a su prenda/artículo (V36), daño por artículo, y la renta solo pasa a DEVUELTA cuando se devolvieron todas las unidades (parcial ⇒ sigue ACTIVA); valida no exceder lo rentado — Rebanada 10** + **Fase 9: pieza faltante/perdida no cierra la renta hasta devolverla o marcarla perdida+cobrada (`perdidaCobrada`, V44) + valor de multa por prenda (`valorReposicion`/`valorDano`, V45) + recargo por retraso fija/acumulativa (V46)** |
| Clientes | Simple | ✅ | Ficha + búsqueda + lista negra + historial (7.2) + filtro de pendientes (11.5/11.6) + **device_token para push (RF-18.11, #29)**. Completo. |
| Empleados | Simple | ✅ | Alta de empleado por la empresa (RF-8) — correo único, sin SUPERADMIN, login. ⬜ Falta usuario↔N sucursales (RF-1.2), turno/actividad (RF-8.2) — Grupo C |
| Reportes | Simple (lectura) | ✅ | Ingresos, ganancia, rentas vencidas, depósitos activos, ingresos por método, rankings, ventas por empleado, desglose por etiqueta, tablero de inventario (9.3)+resumen, export CSV **y PDF (RF-9.2, #27)** + comprobante/contrato PDF (RF-3.4). **Completo.** |
| Notificaciones (WhatsApp/FCM) | Simple (adaptador) | ✅ | Envío por canal + estados + disparador de multas + recordatorio de vencidas (RF-11.1) + **canales WhatsApp/FCM reales gateados + router + device_token (RF-11.4/18.11, #29 — código completo, pendiente credencial)** |
| Configuración de empresa | Simple | ✅ | Switches que controlan de verdad (multas, multi-sucursal, conteo-stock) + reglas por defecto (moneda/recargo, 12.2) + respaldo/restauración (12.3) — mergeado (PR #15). Falta pagoEnLinea (infra) + **Fase 9: política de recargo por retraso `modoRecargoRetraso` FIJA/ACUMULATIVA (V46) + política de reembolso `reembolsosActivos`/`ventanaReembolsoDias` (V47)** |
| Auditoría | Simple | ✅ | Registro por domain events. Auditadas: venta, pago, caja, stock ajustado, devolución, y **todo el ciclo de empresa del SuperAdmin — aprobar + suspender/rechazar/reactivar (RF-15.5, fix post-barrido)** |
| Marketplace (backend) | Simple (lectura) | ✅ | Descubrimiento + búsqueda de empresas ACTIVAS + **catálogo público por tienda (RF-18, #25)** + **checkout de RENTA del cliente por carrito (Rebanada 8)** + **Fase 9: flujo de compra/renta completo del rol CLIENTE (carrito+checkout con ficha auto-enlazada por tienda V41, rentar disfraz, vitrina de disfraces, ruleta de opciones, "Mis Pedidos" `GET /clientes/me/historial`)**. |

## Decisiones aceptadas
- **Plan de cierre (2026-07-04, `CIERRE_BACKEND.md` de Juan):** cerrar el backend en **3 tandas** (T1=P0+P1,
  T2=P2+P3, T3=P4+P5), modo RUN GRANDE. **CHECKPOINT obligatorio tras Tanda 1** (parar y pedir revisión antes
  de seguir). 1 commit por feature, tests de dominio por feature, §5.4 temprano. El cliente Kotlin se genera
  **al final** (tras Tanda 3), no en Tanda 1. El backlog P0–P5 vive en `CIERRE_BACKEND.md`.
- **Decisión (2026-07-04, aprobada por Juan):** se acepta `reactivar` (SUSPENDIDA → ACTIVA)
  como acción del SuperAdmin aunque no figuraba en RF-15.3; se considera complemento natural
  de `suspender`. ✅ Reflejado en `BACKEND_REQUIREMENTS.md` (RF-15.3, 2026-07-08).
- **Decisión (2026-07-08, aprobada por Juan) — modelo de disfraz/precios/multas/reembolsos:**
  el disfraz es **siempre por-partes** (fuera "unidad fija"; "una pieza" = 1 slot fijo) y la
  **talla es una etiqueta** del pool, no un eje; el disfraz admite **editar/archivar** y un
  **precio general** opcional que anula la suma por prendas; la **prenda** lleva valores de
  multa (reposición/daño); el **recargo por retraso** es fijo o acumulativo; en la devolución
  una **pieza faltante/perdida no cierra** hasta devolverla o cobrarla; y la **venta** tiene
  **política de reembolso por local** (sí/no + ventana) con reembolso **total o parcial**.
  ✅ Reflejado en `BACKEND_REQUIREMENTS.md` (RF-2.3/2.4/2.10, RF-4.5, RF-5) e implementado
  (rebanadas 1–8, V42–V48).

## Decisiones pendientes (resolver antes de tocar su tema)
- **Pasarela de pago concreta** (cuando se active el pago en línea, RF-6.11) — 🚫 decisión/infra.
- **UX de descubrimiento del marketplace** (búsqueda, cercanía, filtros, reseñas — RF-18).
- _(Resueltas y retiradas de esta lista: **impuestos** RF-6.5 → tasa única impuesto-incluido en la PR #13;
  **convención de nombres** → de facto aceptada, español en dominio, usado sin objeción en 12 PRs.)_

## Deuda / a sanear
- ✅ **RESUELTO (PR #7)** — ~~Secreto JWT por defecto bloqueante en producción~~: ahora hay fail-fast
  en perfil `prod` si el secreto falta o es el default (`ValidacionSecretoJwt`). Pendiente al desplegar:
  **setear `COSTUMI_JWT_SECRET` por entorno**.
- ✅ **RESUELTO (PR #7)** — ~~Endpoints sin control de rol/tenant~~: ciclo de vida de Empresa y cola de
  pendientes exigen SUPERADMIN; alta de Sucursal exige DUENO/ENCARGADO + dueño del tenant.
- ✅ **RESUELTO (§5.4, PR #9/#10)** — ~~Referencias cross-módulo por id sin validar el tenant~~: ahora toda
  referencia (categoría de la prenda; prenda fija/categoría/valores del pool del disfraz) se valida contra el
  tenant vía las APIs públicas entre módulos, y el `find()` por PK va forzado por el `@Filter` (`findFirstById`).

## A re-verificar cada sesión (invariantes)
- ¿ArchUnit y Modulith siguen en verde?
- ¿Toda tabla nueva lleva `empresa_id` y se filtra por tenant?
- ¿El dominio de los módulos hexagonal sigue sin framework?
- ¿La API solo expone DTOs y el contrato OpenAPI está al día?

## Registro de sesiones
- **2026-07-09 → 07-12** — **Auditoría y cierre profesional del backend (29 correcciones) + E2E en vivo + docs de la app Android.**
  Auditoría completa → **29 correcciones por rebanadas** (cada una PR + tests + ArchUnit + Modulith en verde), todas mergeadas
  a `main` (V49→V53). **CRUD de mantenimiento:** editar+archivar/activar en prenda/categoría/etiquetas/sucursal/empleado(baja)/
  **cliente** (R-E), **borrar grupo de stock** (R-F), conteo de dependencias (R-G), fix 500→409. **Seguridad §6 (cerrada):**
  SEC-1/2/3 (validar sucursal/cliente, @Filter), **webhook firmado HMAC** (SEC-5), B1 lista negra, B2 sucursal, **B3 pirámide**,
  A2 rate-limit, C1 magic-bytes, **C2 refresh revocable con rotación+reuso+logout**, **C3 paginación**, C4 HSTS/headers,
  **C5 scan de deps** (Dependabot + dependency-review). **Personal:** G1 `GET /empleados`, G2 `PUT /empleados/{id}/rol`.
  **Pagos listos-para-credenciales:** P-3 (verificar contra MercadoPago, gateado) y **P-6/reembolsos** = nuevo **workflow en
  2 pasos** (solicitar→aprobar/rechazar con motivo, precondición de ítem devuelto, escalamiento por pirámide, refund a tarjeta
  gateado, **self-service del cliente**). **OpenAPI** verificado + `ProblemDetail`. **E2E en vivo** (jar de prod + Postgres real,
  todos los roles, correcciones 1–29): **102/102 OK** (`REPORTE_E2E.md`, `e2e-full.sh`). **Docs de la app Android** reescritos/
  actualizados a estas correcciones (`DISENO_PANTALLAS.md`, `PLAN_ANDROID.md`, `ORDEN_CONSTRUCCION.md`; el frente Android se
  construye desde cero, contract-first desde `/v3/api-docs`). **Pendiente = solo externo:** credenciales MercadoPago sandbox y
  (opcional) migración a Spring Boot 4. Detalle fino en `AUDITORIA_Y_CORRECCIONES.md`.
- **2026-07-08 (f)** — **Despliegue en Railway + flujo del CLIENTE del marketplace + expansión disfraz/multas/reembolsos.**
  Backend live en Railway (Dockerfile multi-stage temurin-21, `server.port=${PORT}`, SuperAdmin/demo por env). Se
  detectó y cerró el **flujo de compra del rol CLIENTE** (antes 403/500/FK, solo probado como staff): carrito+checkout
  venta/renta con **ficha de cliente auto-enlazada por tienda** (`cliente.usuario_id`, **V41**, `ResolucionDeClientes`),
  rentar disfraz personalizable, **vitrina pública** de disfraces y **"Mis Pedidos"** (PRs #46–#49). Luego la **expansión
  disfraz/precios/multas/reembolsos en 8 rebanadas (V42–V48, 332 tests):** (1) disfraz siempre por-partes + editar/archivar
  + talla como etiqueta; (2) precio general dual del disfraz; (3) devolución: piezas faltantes que bloquean el cierre
  hasta devolver o perdida+cobrada; (4) ruleta de opciones por slot con stock+filtros; (5) valor de multa por prenda;
  (6) recargo por retraso fija/acumulativa; (7) política de reembolso por local (sí/no + ventana); (8) reembolso PARCIAL
  de venta. Rebanadas 1–4 mergeadas (PRs #50–#53); 5–8 entregadas como parches, listas para PR. Cada rebanada validada
  local con la suite completa + ArchUnit + Modulith. _Decisión de Juan reflejada en `BACKEND_REQUIREMENTS.md`
  (RF-2.3/2.4/2.10, RF-4.5, RF-5)._
- **2026-07-06 (e)** — **Cierre final del backend: 6 bloques restantes, todos verdes (rama `feat/cierre-final`).**
  **Notificaciones** recordatorio de vencidas al cliente (RF-11.1); **Renta** extensión/renovación con recálculo de
  importe (RF-3.6); **Inventario** ajuste de stock con motivo auditado por evento `StockAjustado` (RF-10);
  **Empleados** alta por la empresa con correo único, sin SUPERADMIN, y login (RF-8); **Plataforma** refresh token
  con rotación `POST /auth/refresh` (RF-1.1); **OpenAPI** contrato publica todos los endpoints (test que lo prueba,
  RF-17.3). **262 tests verdes.** _Bloqueado por infra/decisión (no código):_ transferencias entre sucursales
  (stock por sucursal), devolución parcial (renta multi-artículo), recuperación de contraseña + WhatsApp/FCM/S3
  (envío real), pasarela (6.11), export PDF, permisos granulares por-usuario. Rama lista para el merge final de Juan.
- **2026-07-06 (d)** — **Config (RF-12) + Clientes (RF-7) en la PR #15 acumulada (con Reportes).** **Config:** los
  switches controlan comportamiento de verdad — `multiSucursal` off ⇒ una sola sucursal (409 la 2ª); `conteoStock`
  off ⇒ venta no descuenta y renta no chequea disponibilidad (multas ya lo hacía). Reglas por defecto **moneda +
  recargo por retraso** (12.2, V27). **Respaldo/restauración** export/import (12.3). **Clientes:** historial del
  cliente (rentas+ventas, read-model, 7.2) + filtro `conPendientes` (11.5/11.6). **252 verdes.** La PR #15 (Reportes +
  Config + Clientes) queda **lista para el merge único de Juan**; los bloques siguientes irán sobre `main` ya con #15.
- **2026-07-06 (c)** — **Reportes reales (RF-9), bloque en curso (rama `feat/cierre-reportes-rf9`).** Read-models
  JdbcClient acotados al tenant, filtros opcionales fecha/sucursal: **rentas vencidas** + **depósitos activos**;
  **ingresos netos por método** (cobros−reembolsos, depósitos no); **rankings** más vendidos/rentados; **ventas por
  empleado**; **tablero de inventario (9.3)** + **resumen** (valor, utilización, dañados/perdidos). 245 verdes.
  ⬜ Falta en el bloque: **desglose por etiqueta** (9.1) y **export PDF/CSV** (9.2). _Nota:_ Juan mergeó la PR #13
  en estado pre-revisión; sus fixes (mixto+impuestos) + saneo de PROGRESS se recuperaron en la **PR #14** (mergeada).
- **2026-07-06 (b)** — **Revisión de Juan sobre PR #13 atendida + saneo del PROGRESS.** (1) El **cobro mixto**
  ahora valida que la suma de porciones = **saldo pendiente** (total del concepto − ya cobrado): cobrar de más o
  de menos → 400; Pagos consulta el total vía `ConsultaDeVentas.totalDeVenta`/`ConsultaDeRentas.importeDeRenta`.
  (2) **Impuestos RESUELTOS** (decisión de Juan, opción a): tasa única por empresa en `ConfiguracionEmpresa`,
  precio **impuesto-incluido**, desglose base+impuesto en el comprobante (V26). (3) **PROGRESS saneado**: se
  retiraron las secciones obsoletas "Pendiente de revisión" y "Próximo paso concreto" (listaban como pendientes
  cosas ya mergeadas) y se reescribió el **Tablero de módulos** con el estado real, porque contradecían el tablero
  de arriba y confundían. **241 verdes.** Todo en PR #13.
- **2026-07-06** — **Cierre del backend (arranque), bloque #1 Pagos completos (RF-6).** Tras aprobar/mergear
  la PR #12, Juan dio el backlog de cierre (Pagos, Reportes, Config, Clientes, permisos/empleados, reabastecimiento,
  huecos renta/dev, recordatorios, plataforma, OpenAPI) y la **regla de repos separados** (backend aquí, Android en su
  repo aparte, unidos solo por OpenAPI). Rama `feat/cierre-pagos-rf6`, 3 commits, 237 verdes:
  (1) **cobro mixto + vuelto (RF-6.7)** — `POST /api/v1/pagos/mixto`, dominio `CobroMixto` reparte en porciones por
  método, calcula vuelto y rechaza efectivo insuficiente; cada porción hereda la idempotencia con sufijo.
  (2) **depósito como retención separada (RF-6.2/6.8)** — `TipoPago.DEPOSITO`/`DEVOLUCION_DEPOSITO` no cuentan como
  ingreso (`montoNeto`=0), se rastrean como garantía; `GET /api/v1/pagos/deposito` da retenido/devuelto/activo; V25
  amplía `tipo_pago` a varchar(20). (3) **comprobante/recibo (RF-6.5)** — `GET /api/v1/pagos/comprobante` agrega
  pagos y totales. Parciales/saldos y reembolsos ya existían.
  **DECISIÓN PENDIENTE (no inventar):** modelo de **impuestos configurables** (RF-6.5/12.2) — el spec lista
  "impuestos" pero no define base/tasa/inclusión; falta que Juan decida. El **render PDF/impresión** del comprobante
  se hará en el export de Reportes (RF-9.2). **Pasarela (RF-6.11), S3 y WhatsApp/FCM** siguen bloqueados por infra.
- **2026-07-05 (av)** — **Fixes de la revisión final de Juan (PR #11).** (1) **Multa respeta el switch (RF-6.6):**
  `DevolucionService` consulta `multasActivas` y con el módulo OFF pone el `cargoPorRetraso` en 0 → no se cobra ni
  reduce el remanente (el daño sí se recupera, no es multa). (2) **Sobreventa:** `AjusteDeInventarioService` toma un
  **advisory lock por prenda** (`pg_advisory_xact_lock`) antes de descontar/mover stock (read-then-write), como la
  renta → dos ventas simultáneas del último ejemplar ya no sobrevenden. (3) **Minor:** auditoría y notificación pasan
  a `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` → un fallo de aviso no revierte la devolución.
  (`Periodo` ya exigía retiro ≤ devolución). **226 verdes.** Con esto Juan da OK para OpenAPI completo + cliente Kotlin.
- **2026-07-05 (au)** — **Tanda 3/P4 · Carrito: checkout → venta (RF-16).** `POST /api/v1/carritos/checkout`
  toma el carrito de VENTA pendiente, resuelve el precio de cada línea (`ConsultaDeInventario.precioVenta`), crea
  la venta vía nuevo puerto público `ventas.RegistroDeVentas` (descuenta stock) y **confirma** el carrito
  (`Carrito.confirmar`). Aristas `pedidos → ventas` y `pedidos → inventario`. Test: checkout crea la venta (2×90=180)
  y confirma el carrito. **226 verdes.** _Nota:_ el checkout de RENTA (necesita fechas por línea) queda pendiente.
- **2026-07-05 (at)** — **Tanda 3/P5 · Renta idempotente al confirmar (RF-17.6, offline/outbox).** `Renta` acepta
  `claveIdempotencia` (**V24**, índice único parcial por empresa+clave). `RentaService`: si la clave ya existe,
  devuelve la renta existente (no duplica por reintento/offline). Tests: misma clave dos veces → una sola renta.
  **225 verdes.**
- **2026-07-05 (as)** — **Tanda 3/P4 · Config-switch que de verdad controla: multas on/off (RF-12.4/6.6).** Nuevo
  puerto público `configuracion.ConsultaDeConfiguracion.multasActivas`. El `DisparadorDeMultas` (notificaciones)
  lo respeta: con el módulo de multas **apagado** no se notifica la multa. Arista `notificaciones → configuracion`.
  Test: multas off → devolución con multa 30 NO genera notificación. **224 verdes.**
- **2026-07-05 (ar)** — **Tanda 2/P3 · Auditoría dirigida por eventos (RF-0.5/15.5).** Nuevo módulo `auditoria`:
  `RegistroDeAuditoria` (inmutable) + `AuditoriaDeEventos` (@EventListener) que registra a partir de los domain
  events (§5.5): `EmpresaAprobada`→EMPRESA_APROBADA, `DevolucionRegistrada`→DEVOLUCION_REGISTRADA. A medida que
  más operaciones publiquen eventos se añaden aquí sin tocar esos módulos. Persistencia **V23** (`@Filter`),
  `GET /api/v1/auditoria` (DUENO/ENCARGADO, acotado al tenant). Test: aprobar empresa deja el registro. **223 verdes.**
- **2026-07-05 (aq)** — **Tanda 2/P3 · Pagos: reembolsos + saldo neto por operación (RF-6.9).** `Pago` gana
  `TipoPago` (COBRO/REEMBOLSO, **V22**) y `montoNeto()` (cobro suma, reembolso resta). Nuevo `GET /api/v1/pagos/
  saldo?conceptoId=` → saldo neto (cobros − reembolsos) de la renta/venta. Tests dominio + integración (cobro 100 −
  reembolso 30 = 70). **221 verdes.** _Pendiente RF-6:_ depósito como retención separada, pago mixto+vuelto, impuestos.
- **2026-07-05 (ap)** — **Tanda 3/P4 · Marketplace: búsqueda por texto (RF-18.1).** `GET /api/v1/marketplace/
  empresas?buscar=texto` (público) filtra empresas ACTIVAS por nombre (read model JdbcClient, like insensible a
  mayúsculas). Test: búsqueda devuelve solo la coincidente. **219 verdes.** _Pendiente RF-18/14:_ búsqueda por
  categoría/cercanía, enlace/QR por empresa/sucursal, selección de sucursal.
- **2026-07-05 (ao)** — **Tanda 3/P4 · Notificaciones: disparador por evento (RF-11.1, §5.5).** Se cierra el
  loop de domain events: `notificaciones` **escucha** `DevolucionRegistrada` (`DisparadorDeMultas`,
  `@EventListener` síncrono) y, si hay **multa** (>0) y cliente, **envía una notificación** al cliente
  (EnviarNotificacion, canal EMAIL). El evento se enriqueció con `clienteId` (nuevo `ConsultaDeRentas.clienteDeRenta`).
  Arista `notificaciones → devoluciones` (evento). Test: devolución con multa 30 → aparece notificación EMAIL.
  **218 verdes.** _Demuestra la arquitectura §5.5 punta a punta (devolución→evento→notificación)._
- **2026-07-05 (an)** — **Tanda 3/P4 · Reabastecimiento: entrada de stock + alerta de stock bajo (RF-10).**
  `GrupoDeStock.reabastecer(cantidad)` (entrada de mercancía) con test. `POST /api/v1/grupos-stock/{id}/entrada`
  (DUENO/ENCARGADO/BODEGA) y `GET /api/v1/grupos-stock/stock-bajo?umbral=N` (grupos con disponibles < umbral).
  Query `listarBajoUmbral`. Tests dominio + integración (entrada sube stock; stock-bajo aparece/desaparece).
  **217 verdes.** _Pendiente RF-10:_ Proveedor, transferencias entre sucursales, ajustes con motivo+auditoría.
- **2026-07-05 (am)** — **Tanda 2/P3 · Reportes: ganancia = ingreso − costo (RF-9).** Read model
  `ResumenDeGanancia` (JdbcClient): **ingresos** = suma de pagos; **costo de ventas** = Σ(línea.cantidad ×
  prenda.costo_adquisicion) por join `linea_de_venta`×`prenda`; **ganancia** = ingresos − costo. Endpoint
  `GET /api/v1/reportes/ganancia` (DUENO/ENCARGADO). Test integración (venta con costo → ganancia correcta).
  **215 verdes.** _Pendiente RF-9:_ más cortes (utilización, vencidas, por empleado, desglose por etiqueta, export).
- **2026-07-05 (al)** — **Tanda 2/P3 · Caja/Turno/MovimientoDeCaja + corte y cuadre (RF-6.3/6.10, rigor dinero).**
  Nuevo módulo `caja`. `Turno` (agregado): se **abre** con fondo inicial (efectivo), acumula **movimientos**
  (ingreso/egreso por método EFECTIVO/TARJETA/TRANSFERENCIA), y se **cierra** con el efectivo contado. Dominio con
  **corte por método** (`totalPorMetodo`, el efectivo incluye el fondo) y **cuadre** (`diferenciaDeEfectivo` =
  contado − esperado), todo en `BigDecimal`. Estados ABIERTO/CERRADO (no se mueve/cierra un turno cerrado →
  `TurnoNoAbierto` 409). Persistencia agregado (turno + movimientos hijo) **V21**, con `@Filter` y `findFirstById`.
  `POST /api/v1/caja/turnos`, `.../{id}/movimientos`, `.../{id}/cerrar`, `GET` (DUENO/ENCARGADO/MOSTRADOR/ATENCION).
  Tests dominio (corte/cuadre, turno cerrado) + integración (flujo completo, 409, 404 cross-tenant). **214 verdes.**
- **2026-07-05 (ak)** — **Tanda 2/P3 · Prenda: costo de adquisición + depósito sugerido (RF-2.10).** `Prenda`
  gana `costoAdquisicion` y `depositoSugerido` (opcionales, no negativos), migración **V20**, en dominio/entidad/
  DTOs. Es la base del **margen** para los reportes (ganancia = ingreso − costo). Tests dominio + integración.
  **206 verdes.**
- **2026-07-05 (aj)** — **Tanda 2 · Devolución: multa automática + renta→DEVUELTA + domain event (P2, RF-5.1/5.2, §5.5).**
  `Devolucion.multa()` = exceso de (daños+retraso) sobre el depósito (0 si el depósito cubre), con tests y
  expuesta en el response. Al registrar la devolución: se **cierra la renta** (`ConsultaDeRentas.marcarDevuelta`,
  exige ACTIVA → si no, revierte) y se **publica `DevolucionRegistrada`** (empresa, devolución, renta, multa) como
  **primer domain event del ciclo operativo** (§5.5), listo para que Caja/Notificaciones lo consuman. Test:
  la renta queda DEVUELTA, multa 0 cuando el depósito cubre. **203 verdes.** _Pendiente RF-5:_ devolución
  **parcial** (RF-5.5) y un consumidor del evento (registrar la multa como saldo del cliente / notificar).
- **2026-07-05 (ai)** — **Tanda 2 · Devolución actualiza el inventario según el checklist (P2, RF-5.4/5.6).** Al
  registrar una devolución: (1) valida que la **renta sea del tenant** vía nuevo puerto público
  `rentas.ConsultaDeRentas.prendaDeRenta` (400 si no existe/ajena); (2) agrega el checklist por estado y
  **mueve unidades de disponible → dañadas/en-limpieza/perdidas** vía `AjusteDeInventario.procesarRetornoDeRenta`
  (las que vuelven BIEN quedan disponibles); (3) liquida el depósito como ya hacía. Aristas nuevas
  `devoluciones → rentas` y `devoluciones → inventario`. Manejador de errores de devoluciones nuevo. Test:
  devolución con pieza DAÑADA deja el grupo en disponibles 0 / dañadas 1; renta inexistente → 400. **202 verdes.**
  _Pendiente RF-5 (para próximas rebanadas):_ **multa automática** (RF-5.2), transición de la renta a DEVUELTA
  al registrar (checklist "conectado" completo) y **domain events** (devolución→multa), devolución parcial (RF-5.5).
- **2026-07-05 (ah)** — **Tanda 2 · Venta: baja de stock al confirmar (P2 CRÍTICO, RF-4.4).** La venta nace
  CONFIRMADA → al registrarla se **descuenta el stock**. `GrupoDeStock.darDeBaja(cantidad)` (las unidades salen
  del inventario) con tests. Nuevo puerto público de **escritura** `inventario.AjusteDeInventario.descontarDisponibles`
  (reparte la baja entre los grupos de la prenda; `StockInsuficiente` público → 409) + impl acotada al tenant.
  `VentaService`: valida que cada prenda de línea exista en el tenant (400) y descuenta su cantidad; si no alcanza,
  `StockInsuficiente` **revierte toda la venta** (atómico en la tx). Manejador de errores de ventas nuevo
  (IllegalArgument→400, StockInsuficiente→409). Arista nueva `ventas → inventario`. Tests: baja efectiva (5−2=3),
  vender de más → 409, prenda inexistente → 400 (+ dominio de `darDeBaja`). **201 verdes.** _Pendiente RF-4:_
  comprobante, devoluciones/cambios de venta, modo asistido.
- **2026-07-05 (ag)** — **Tanda 2 · Renta: disponibilidad por fechas SIN traslapes + concurrencia (P2 CRÍTICO, RF-3.2/0.4).**
  Value object de dominio **`Periodo`** (retiro/devolución) con `seSolapaCon` (extremos **inclusivos**) + `dias()`,
  con tests. Al crear una renta: (1) cross-ref — la prenda debe existir en el tenant (400 si no);
  (2) **advisory lock por prenda** (`pg_advisory_xact_lock`, se libera al commit) para **serializar reservas
  concurrentes** y evitar doble asignación; (3) se cuentan las rentas **vigentes** (RESERVADA/ACTIVA) que se
  **traslapan** (`RentaRepository.contarSolapadas`) y se comparan con las **unidades disponibles** de la prenda
  (`ConsultaDeInventario.unidadesDisponibles`); si ocupadas ≥ disponibles → **409** (`SinDisponibilidad`). Arista
  nueva `rentas → inventario` (puerto público). Tests: traslape → 409, fechas disjuntas → 201, prenda inexistente
  → 400 (+ dominio de `Periodo`). **196 verdes.** _Decisiones:_ (a) traslape **inclusivo** en extremos (no se
  asume rotación el mismo día); (b) disponibilidad = suma de `disponibles` de los grupos de la prenda (a nivel
  prenda; el detalle por variante/unidad concreta queda para el armado multi-artículo). _Pendiente RF-3:_
  multi-artículo/armado por partes, extensión/renovación, cobro al retiro + contrato, asistido, vencidas como proceso.
- **2026-07-05 (af)** — **§5.4 APROBADO por Juan (checkpoint Tanda 1 cerrado). LUZ VERDE: correr Tanda 2 → Tanda 3
  de largo, SIN checkpoint intermedio; revisión final completa al terminar la Tanda 3 (esa reemplaza los
  checkpoints).** Condiciones firmes de Juan: tests de dominio por CADA feature (no desactivar ninguno); **rigor
  extra** en disponibilidad de renta por fechas (traslapes/concurrencia) y en dinero/caja (idempotencia, BigDecimal,
  depósito-retención, cuadre); **1 commit por feature**; OpenAPI crece con cada endpoint; PROGRESS al día; no
  inventar (ambiguo → PROGRESS). Añadido su "cheap insurance": **regla ArchUnit** que prohíbe `findById` en los
  adaptadores (excepto Empresa/Configuración) para que el hueco de tenant no se reabra sin fallar el build.
  PR #8/#9 (y #10 del find-por-PK) listos para mergear a `main`.
- **2026-07-05 (ae)** — **§5.4 · `find()` por PK forzado por construcción — 3er pedido de Juan.** El `@Filter` no
  cubre `findById` (em.find); el hueco se tapaba con `.filter(empresaId)` manual por servicio (RentaRepositoryAdapter
  iba sin guard). Cerrado en los adaptadores: `buscarPorId` pasa de `findById` (em.find) a **`findFirstById`**
  (query derivada) → atraviesa el `@Filter` forzado ya aprobado → devuelve **empty si `empresa_id ≠ tenant`**, sin
  chequeo manual. Aplicado a los **13 adaptadores** con `empresa_id` (se excluyen `Empresa` = el propio tenant, y
  `Configuracion` = PK es `empresa_id`). Tests: `buscarPorId` con id de otro tenant → empty; cargar-por-PK-y-mover
  (dinero-adyacente) cruzando tenant → 404. **186 verdes.** _Nota:_ los `.filter(empresaId)` que quedan en los
  servicios son ahora **redundantes** (defensa en profundidad sobre el find forzado), no la línea de defensa.
  _RLS Postgres:_ 2º cinturón opcional; con `@Filter` (queries) + `findFirstById` (PK) + cross-ref, §5.4 queda cerrado.
- **2026-07-05 (ad)** — **§5.4 · Validación cross-ref por tenant (escritura) — pedido de Juan tras el checkpoint.**
  Toda referencia por id se valida contra el tenant vía las APIs públicas entre módulos: `PrendaService` exige
  que la **categoría** sea de la empresa (`ConsultaDeTaxonomia.categoriaExiste`); `DisfrazService` exige que la
  **prenda fija**, la **categoría del pool** y los **valores del pool** sean del tenant
  (`ConsultaDeInventario.prendaExiste` + `ConsultaDeTaxonomia.categoriaExiste`/`valorPerteneceATipo`). Cruzar
  tenant → 400. Tests: B no puede crear prenda con categoría de A, ni disfraz con prenda de A. **184 verdes.**
  (Nota: la validación usa el chequeo manual `empresaId` porque los filtros Hibernate no aplican a `find()` por PK.)
- **2026-07-05 (ac)** — **§5.4 · Aislamiento multi-tenant FORZADO (lectura) — pedido de Juan tras el checkpoint.**
  Filtro Hibernate `@FilterDef`/`@Filter` (`empresa_id = :empresaId`) en las **19 entidades** con `empresa_id`
  (definido una vez en `SucursalJpaEntity`). Se activa por sesión con el `empresa_id` del token en
  **`FiltroDeTenantAspect`** (aspecto `@Around` sobre `*RepositoryAdapter`), enganchado en el repositorio y no en
  el request porque **OSIV está off** (el adaptador siempre corre dentro de la tx del servicio, con sesión viva).
  SuperAdmin/login sin tenant → filtro no se activa. Se añadió `spring-boot-starter-aop`. Test que prueba que un
  tenant no ve por **consulta** los datos de otro (sorteando el caché de 1er nivel; los filtros aplican a queries,
  no a `find()` por PK — el `find` queda cubierto por el chequeo manual de los servicios). **182 verdes.**
  _Decisión:_ RLS Postgres queda como posible 2º cinturón futuro; con el `@Filter` + cross-ref basta para cerrar §5.4.
- **2026-07-05 (ab)** — **Tanda 1 · Siembra de taxonomía básica al aprobar (RF-2.7.7 / RF-13.5) → CIERRA TANDA 1.**
  Al **aprobar** una empresa, Identidad publica el evento **`EmpresaAprobada`** (§5.5) y Catálogo lo escucha
  (`SembradorDeTaxonomiaBasica`, síncrono en la tx) para **sembrar** categorías básicas (Camisa, Pantalón,
  Vestido, Sombrero, Zapatos, Accesorio) y los tipos de variante **Color** (Rojo/Azul/Negro/Blanco) y **Talla**
  (S/M/L/XL). Se siembra al **aprobar** (no al registrar) para no chocar con empresas de prueba no aprobadas y
  porque es cuando la empresa opera. Modulith verde con la nueva arista `catalogo → identidad` (evento). Test de
  integración (aprobar siembra; pendiente no). **181 verdes.** _Decisión:_ el set de básicos es el de arriba
  (elegido; ampliable por el dueño). **Con esto la Tanda 1 queda COMPLETA → CHECKPOINT: se para y se pide
  revisión a Juan antes de la Tanda 2.**
- **2026-07-05 (aa)** — **Tanda 1 · Taxonomía: renombrar tipo/valor (RF-2.7.6).** `PATCH /api/v1/tipos-etiqueta/{id}`
  y `.../{tipoId}/valores/{valorId}` renombran (DUENO/ENCARGADO), acotados al tenant (404 ajeno). Como prendas,
  variantes y pools guardan solo **ids**, el cambio **propaga** sin tocarlos. `ValorEtiqueta.renombrar` +
  `ValorEtiquetaRepository.buscarPorId`. Tests dominio + integración (renombra tipo y valor conservando id,
  404 de otra empresa). **179 verdes.**
- **2026-07-05 (z)** — **Tanda 1 · Tooling OpenAPI contract-first (P1, RF-17.3, §5.6).** Se instala
  **springdoc-openapi** (starter webmvc-ui): el backend expone el contrato en `/v3/api-docs` y la UI en
  `/swagger-ui.html`, **públicos** (permitAll en `SecurityConfig`). `OpenApiConfig` documenta el título/versión
  y el **esquema de seguridad JWT (bearer)**. Es la fuente única de la que, **al cerrar el backend tras la
  Tanda 3**, se generará el cliente Kotlin (NO ahora). Test de integración del contrato. **176 verdes.**
  _Decisión:_ hoy es **code-first con salida OpenAPI** (los endpoints ya existen); migrar a contract-first
  estricto (spec→stubs) no aporta en esta fase y el contrato completo solo existe al final (Tandas 2/3 añaden
  endpoints), así que se difiere; lo que se "monta" ahora es la herramienta y la disciplina del contrato.
- **2026-07-05 (y)** — **Tanda 1 · `X-Sucursal-Id`: sucursal activa por cabecera (P1, RF-17.4).**
  `ContextoDeTenant` gana `sucursalActiva()` / `sucursalActivaRequerida()`, que leen la cabecera
  `X-Sucursal-Id` de la petición (vía `RequestContextHolder`); si falta, `SucursalNoIndicada` → 400
  (Problem Details). Tests unitarios (con/sin cabecera). **175 verdes.** _Decisión:_ la validación de que la
  sucursal **pertenece a la empresa del token** se aplica en el caso de uso que la consuma (ninguno en Tanda 1
  aún); parte del endurecimiento §5.4.
- **2026-07-05 (x)** — **Tanda 1 · Disfraz + Slot (Capa 3) + disponibilidad DERIVADA (P0, RF-2.3/2.4).** Nuevo
  módulo `disfraces`. `Disfraz` con **modo** `UNIDAD_FIJA` (una prenda fija) o `POR_PARTES` (**1..8 `Slot`**).
  Cada `Slot` con los **dos ejes** (talla FIJA/LIBRE; prenda FIJA/PERSONALIZABLE) + **opcional**; el
  personalizable lleva un **`PoolDeSlot`** (categoría + valores de etiqueta permitidos por dimensión, RF-2.7.5).
  **Disponibilidad derivada:** no es un contador; se **calcula** en el dominio (`Disfraz.estaDisponible`) — unidad
  fija disponible si su prenda tiene stock; por partes disponible si **cada slot obligatorio** se cubre (los
  opcionales no bloquean). El cálculo usa el puerto de dominio `ConsultaDeStockDePool`, puenteado en aplicación
  al nuevo puerto público **`inventario.ConsultaDeInventario`** (`prendaTieneStockDisponible` /
  `poolTieneStockDisponible`). Persistencia agregado (cabecera+slots+pool) en **V19** (`disfraz`,
  `disfraz_slot`, `disfraz_slot_etiqueta`). `POST/GET /api/v1/disfraces` y
  `GET /api/v1/disfraces/{id}/disponibilidad` (POST DUENO/ENCARGADO). Tests de dominio (disponibilidad con stub:
  unidad-fija, por-partes, opcionales no bloquean, talla fija, límites 1..8) + integración (disponibilidad
  true/false derivada del stock, pool personalizable, 400/403/401). **173 verdes.** _Decisión:_ validación
  cross-ref de `prendaFijaId`/`categoría`/valores del pool contra el tenant se difiere al **endurecimiento §5.4**
  (hoy el dominio garantiza integridad estructural y el tenant se acota en el propio disfraz).
- **2026-07-05 (w)** — **Tanda 1 · Taxonomía: el tipo de etiqueta aplica a categorías (P0, RF-2.7.2).**
  `TipoEtiqueta` gana **`categoriasQueAplica`** (conjunto): **vacío = aplica a todas** (dimensión global tipo
  "Color"); con valores = solo esas. Persistencia en tabla hija `tipo_etiqueta_categoria` (**V18**,
  `@ElementCollection<UUID>`). Al crear el tipo se validan las categorías contra el tenant (400 si no son suyas).
  Nuevo método del puerto `ConsultaDeTaxonomia.tipoAplicaACategoria`, **impuesto** en Inventario: al etiquetar
  una prenda y al crear una variante, el tipo debe aplicar a la categoría de la prenda (400 si no). Tests de
  dominio (`aplicaACategoria`) + integración (tipo acotado / categoría de otra empresa 400 / prenda con tipo que
  no aplica 400). **162 verdes.** _Decisión:_ conjunto vacío = aplica a todas (evita tener que enumerar en
  dimensiones globales). _Pendiente de la taxonomía completa (task #5):_ "seleccionable por cliente **en qué
  slots**" (RF-2.7.5, depende de Slot), endpoints de **renombrar** tipo/valor, **siembra de básicos** (RF-2.7.7).
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
