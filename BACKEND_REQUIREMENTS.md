# Costumi — Guía del backend (renta y venta de disfraces)

**Qué es esto:** la **fuente única de verdad** del sistema Costumi — una plataforma tipo
marketplace donde negocios de renta/venta de disfraces (empresas con una o varias
sucursales) publican su catálogo, y los clientes reservan, rentan y compran. Cubre el
**backend**, la **app del dueño**, la **app del cliente** y la **plataforma**.

**Punto de partida:** existen dos apps Android de *diseño* (dueño y cliente) con las
pantallas armadas pero **sin backend real** (datos fijos, mensajes "(simulado)"). Esta
guía define qué debe hacer el sistema y cómo se construye.

---

## 0. Cómo usar esta guía

**Sistema de documentos** (léelos en este orden al empezar cualquier sesión):
1. `CLAUDE.md` — **reglas de implementación** y protocolo de trabajo. Manda sobre el *cómo*.
2. Este archivo (`BACKEND_REQUIREMENTS.md`) — **el QUÉ**: alcance y requerimientos. Fuente
   de verdad estable; **no se cambia sin una decisión explícita** anotada.
3. `PROGRESS.md` — **el DÓNDE VAMOS**: estado vivo, se actualiza al final de cada sesión.

**Convenciones:**
- Los requerimientos se numeran `RF-x.y`. Una referencia como "(RF-2.7)" apunta a otro
  requerimiento de este documento; "§5.3" apunta a una sección.
- **Decidido / cerrado** = no se rediscute sin una decisión explícita nueva.
- Si algo aquí contradice un **principio rector** (§1), gana el principio.
- Código en **inglés**; texto visible y lenguaje de dominio en **español** (ver glosario).

**Glosario de términos canónicos** (usar EXACTAMENTE estos nombres en código y en la charla):

| Término | Significado |
|---|---|
| Empresa (tenant) | Un negocio. Nivel superior bajo la Plataforma. (= "Negocio") |
| Sucursal | Local físico de una empresa. Ancla de inventario / ventas / reservas. |
| SuperAdmin | Rol de plataforma que aprueba empresas. |
| Prenda | Ítem concreto de la biblioteca (una categoría + valores de etiqueta). |
| Categoría | "Parte del cuerpo" (camisa, pantalón…), editable por el dueño. |
| TipoEtiqueta / ValorEtiqueta | Dimensión (Color) y su valor (Rojo). Taxonomía dinámica. |
| GrupoDeStock (Variante) | Combinación de valores de etiqueta con cantidad y estado. |
| Disfraz | Producto que se renta/vende: modo unidad fija o por partes (slots). |
| Slot / Sección | Una parte de un disfraz (hasta 8), con sus dos ejes. |
| Pedido / Carrito | Segmentado por (cliente × sucursal × tipo renta\|venta). |

---

## 1. Principios rectores

Cinco ideas gobiernan todo lo demás. Si un requerimiento contradice uno de estos
principios, gana el principio.

1. **Configurabilidad por local.** El backend no asume un solo modo de operar. Cada
   negocio/sucursal activa, desactiva y ajusta módulos según su tamaño y estilo
   (conteo de stock, granularidad, QR, multas, multi-sucursal, permisos finos, etc.).
   Regla de oro: **valores por defecto sensatos + activar lo avanzado solo si el
   local lo pide.**

2. **Un motor, dos puertas.** Reservar / rentar / vender es **una sola lógica** que
   vive una vez en el backend. Tiene dos puertas de entrada:
   - **Puerta del consumidor** (app cliente, autoservicio).
   - **Puerta del dueño/empleado** (pantalla del dueño, modo asistido/presencial).
   No se programa el flujo dos veces.

3. **Se define una vez, se reutiliza.** El dueño no re-etiqueta nada por disfraz.
   El vocabulario (etiquetas y categorías) se define una vez; las prendas se
   etiquetan una vez al registrarlas; al armar un disfraz el dueño solo **marca
   casillas** sobre ese vocabulario.

4. **Todo se ancla a una sucursal.** Inventario, stock, disponibilidad, precios,
   reservas, caja y reportes existen *dentro de una sucursal*. Jerarquía completa:
   **Plataforma → Empresa (tenant) → Sucursal → (inventario, reservas, ventas,
   rentas)**. Con un solo local, la parte de sucursal es transparente. (`Negocio` y
   `Empresa` son lo mismo; se unifica el término en **Empresa**.)

5. **La complejidad se encapsula.** El modelo puede ser complejo por dentro
   (taxonomía dinámica, stock por variante, disponibilidad derivada), pero la
   experiencia del dueño debe sentirse simple (revelación progresiva, asistentes,
   plantillas, vista previa). Ver sección **RF-13**.

---

## 2. Modelo conceptual de tres capas

El inventario y los disfraces se entienden en tres capas separadas. Separarlas es lo
que evita que el modelo se sienta inabarcable.

### Capa 1 — Vocabulario (se define una vez, se reutiliza)
- **Etiquetas**: son **tipos (dimensiones) → valores**. Ej. tipo "Color" → {rojo,
  azul}; tipo "Talla" → {S, M, L}. Personalizables: la app siembra unas básicas y el
  dueño crea, renombra y archiva las suyas.
- **Categorías**: la "parte del cuerpo" (camisa, pantalón, sombrero…). Editables por
  el dueño (no una lista fija).

### Capa 2 — Inventario de piezas
- **Prenda**: un ítem concreto de la biblioteca; pertenece a una categoría y **lleva
  valores de etiqueta**.
- **Grupo de stock (variante)**: la combinación de valores de las etiquetas que el
  local marcó como "definen variante", con su **cantidad** y su desglose de estado
  (disponibles / dañadas / en limpieza / perdidas). Aquí cuelga la numeración o el QR
  de la pieza física y las observaciones de daño.

### Capa 3 — Disfraz (hasta 8 secciones)
- Un disfraz tiene un **modo**: se renta/vende como **unidad fija** (indivisible, no
  personalizable) o **por partes**.
- "Por partes" = una **lista de hasta 8 secciones (slots)**. "Varias superiores",
  "varias inferiores", "accesorios" son simplemente más slots en la lista.
- Cada slot lleva los **dos ejes** + opcionalidad:
  - **Eje de talla**: fija (se escribe) o libre (se ajusta al cliente).
  - **Eje de prenda**: fija (siempre la misma) o personalizable (el cliente elige
    dentro de un pool filtrado por categoría + etiquetas permitidas).
  - **Opcional**: el cliente puede omitir la sección.
- **Disponibilidad derivada**: la disponibilidad de un disfraz personalizable **no es
  un número propio**; se **calcula**: está disponible si *cada slot obligatorio* tiene
  al menos una prenda disponible en su pool. Los slots opcionales no bloquean.

### Dónde vive cada decisión del dueño
| Decisión | Dónde se hace | Frecuencia |
|---|---|---|
| Crear etiquetas y categorías | Vocabulario (ajustes avanzados) | Rara vez |
| Qué etiquetas tiene una prenda y su stock | Al registrar la prenda | Una vez por prenda |
| Qué etiquetas puede elegir el cliente en un slot | Al armar el disfraz, por slot | Una vez por disfraz |

---

## 3. Entidades núcleo (para modelar)

`Plataforma/SuperAdmin` · `Empresa` (tenant, con estado de aprobación) · `Sucursal` ·
`Usuario/Empleado` (rol, permisos) · `Cliente` · `TipoEtiqueta` (dimensión) ·
`ValorEtiqueta` · `Categoría` · `Prenda` · `GrupoDeStock/Variante` (cantidad, estado,
numeración/QR) · `Disfraz` (modo) · `Slot/Sección` (dos ejes, opcional, etiquetas
permitidas) · `Carrito` + `LíneaDeCarrito` · `Reserva` · `Renta` (fechas, estado,
depósito) · `Venta` · `LíneaDeTransacción` · `Pago` · `Depósito/Garantía` · `Multa` ·
`Devolución` · `MovimientoDeCaja` · `EntradaDeStock/Proveedor` · `Transferencia` ·
`Notificación` · `RegistroDeAuditoría` · `ConfiguraciónDeEmpresa` (interruptores de
módulos).

---

## 4. Requerimientos funcionales

### RF-0. Fundamentos (modelo y plataforma)
- **RF-0.1** Entidades y relaciones persistentes con identificador único (ver §3).
- **RF-0.2** Persistencia real con sincronización: opera **offline** y concilia al
  reconectar (la tienda no puede parar si se cae internet).
- **RF-0.3** Multi-sucursal opcional: todo filtrable por sucursal; con un solo local,
  transparente.
- **RF-0.4** Concurrencia/bloqueo: reservar o entregar una unidad la marca como no
  disponible de inmediato para evitar doble asignación. Para disfraces personalizables,
  el bloqueo ocurre a nivel de la prenda/variante concreta que se toma del pool
  (ver RF-2 y disponibilidad derivada).
- **RF-0.5** Bitácora de auditoría en toda operación que mueva dinero o inventario.

### RF-1. Autenticación, usuarios y roles
- **RF-1.1** Login real: sesión persistente, cierre de sesión, recuperación de
  contraseña, "recordar sesión".
- **RF-1.2** Cuenta por empleado, ligada a una o varias sucursales.
- **RF-1.3** Roles como plantilla (Mostrador / Bodega / Atención / Encargado / Dueño).
- **RF-1.4** Toda transacción queda asociada al usuario logueado que la realizó.
- **RF-1.5** **Editor de permisos granular pero intuitivo.** Por empleado, permisos de
  **visualización** y de **acción** sobre cada sección/operación (inventario, ventas,
  rentas, devoluciones, caja, reportes, clientes, etc.). Se parte de un rol-plantilla y
  se activan/desactivan casillas puntuales encima, para no configurar todo desde cero.

### RF-2. Inventario con granularidad configurable
- **RF-2.1** Catálogo de artículos con **tipo**: solo renta, solo venta, o ambos.
  Hay artículos que solo se venden y no se rentan (maquillaje, pelucas, desechables).
- **RF-2.2 — Grupo de stock.** El inventario se cuenta por *grupos*, y el local elige
  el nivel: por unidad / por modelo / por combinación de valores de etiqueta
  (p. ej. modelo+color+talla) / por parte del cuerpo (pool compartido). **El esquema
  es dinámico**, definido por las etiquetas (ver RF-2.7).
- **RF-2.3 — Composición mixta.** Un disfraz por partes = varios slots; cada slot es
  **fijo** (su stock = el de esa prenda) o **variable** (stock a nivel de pool/variante).
  Un mismo disfraz puede mezclar slots fijos y variables.
- **RF-2.4 — Disponibilidad derivada.** La disponibilidad de un disfraz personalizable
  se **calcula** a partir de sus slots (ver §2, Capa 3). No es un contador plano.
- **RF-2.5 — Conteo opcional.** El local decide si **rastrea y muestra cantidades** o
  solo maneja disponible/no disponible sin números.
- **RF-2.6 — Identificación flexible.** La pieza se identifica por **QR (opcional)** o
  por **numeración simple dentro de su grupo de stock** (ej. "camisa pirata roja M —
  #3 de 8"). El QR queda disponible para quien lo implemente a futuro, sin ser
  obligatorio; muchos locales no ponen identificación.
- **RF-2.7 — Motor de etiquetas (taxonomía dinámica).** Es lo más complejo del
  backend y **debe diseñarse primero**, porque todo lo demás depende de él:
  - **RF-2.7.1** Tipo de etiqueta (dimensión) con sus valores; ambos definidos por el
    dueño; se siembran básicos.
  - **RF-2.7.2** Cada tipo de etiqueta lleva interruptores: *¿define variantes de
    stock?*, *¿es seleccionable por el cliente (y en qué slots)?*, *¿a qué categorías
    aplica?*.
  - **RF-2.7.3** El grupo de stock es la **combinación de valores** de los tipos que
    definen variante (dinámico, no fijo).
  - **RF-2.7.4** Solo existen las combinaciones **reales** (el dueño da de alta las
    variantes que tiene, no el producto cartesiano completo).
  - **RF-2.7.5** Personalización derivada: por slot, el dueño elige qué tipos de
    etiqueta puede elegir el cliente y con qué valores permitidos → filtra el pool →
    resuelve a la variante → toma su stock.
  - **RF-2.7.6** Ciclo de vida con datos existentes: **archivar en vez de borrar** lo
    que está en uso; renombrar propaga; las rentas históricas conservan el valor con
    el que se hicieron.
  - **RF-2.7.7** Taxonomía **por negocio** (multi-tenant): básicos iguales, evolución
    independiente.
- **RF-2.8** Biblioteca de prendas y **gestión de categorías y etiquetas** editable.
- **RF-2.9** **Fotos** por artículo/prenda.
- **RF-2.10** **Precio de renta** (por día/periodo), **precio de venta** y **costo de
  adquisición** (para margen). Depósito/garantía sugerido por artículo.
- **RF-2.11** Transiciones de estado automáticas: al rentar → rentada; al devolver
  limpio → disponible; al devolver dañado → reparación/limpieza; al vender →
  vendida / baja de stock.

### RF-3. Renta (ciclo completo)
- **RF-3.1** Crear renta: artículo (con su armado por partes si aplica), **cliente
  identificable**, **fechas** de retiro y devolución.
- **RF-3.2** **Verificación de disponibilidad por fechas** y **reservaciones**
  (calendario, sin traslapes).
- **RF-3.3** Importe = precio × periodo (+ extras) y **registro de depósito/garantía**.
- **RF-3.4** Cobro al retiro (parcial o total) y **contrato/comprobante**.
- **RF-3.5** **Estados**: reservada → activa → devuelta → cerrada; **detección
  automática de vencidas**.
- **RF-3.6** **Detalle de renta** (cliente, artículos, fechas, saldo, depósito, estado)
  con **extensión/renovación** y **cancelación**.
- **RF-3.7 — Modo asistido (presencial).** Desde la pantalla del dueño/empleado se
  pueden crear **reservas y rentas** para clientes que llegan al local, con el mismo
  motor que la app del consumidor.

### RF-4. Venta (POS)
- **RF-4.1** Flujo de venta con **carrito** de uno o varios artículos.
- **RF-4.2** Venta a nombre del **empleado logueado**; opción de adjuntar
  **código/nombre** de cliente si se solicita (sin exigir ficha completa).
- **RF-4.3** Descuentos/promociones y cobro; **comprobante** de venta.
- **RF-4.4** **Descuento automático de stock** al confirmar.
- **RF-4.5** **Devoluciones/cambios** de venta con reintegro o nota.
- **RF-4.6 — Modo asistido (presencial).** El POS opera desde la pantalla del
  dueño/empleado para clientes presenciales (misma lógica que RF-3.7).

### RF-5. Devoluciones de renta
- **RF-5.1** Checklist por pieza: ¿llegó? + estado (bien / dañada / limpieza / perdida).
- **RF-5.2** Genera **multa/cargo automático** por daño, pérdida o retraso
  (si el módulo de multas está activo — ver RF-6.6).
- **RF-5.3** **Liquidación del depósito**: garantía − daños − recargos = remanente a
  devolver.
- **RF-5.4** **Actualiza inventario** (libera unidad o la manda a limpieza/reparación).
- **RF-5.5** Soporta **devolución parcial**.
- **RF-5.6** El daño/pérdida se registra contra el **grupo de stock + número/QR** de la
  pieza.

### RF-6. Pagos, caja y finanzas
- **RF-6.1** Todo pago ligado a su **renta o venta**, con **saldos, pagos parciales y
  reembolsos**.
- **RF-6.2** **Depósito/garantía** rastreado como retención (no ingreso) hasta
  liquidarse.
- **RF-6.3** **Caja por sucursal/turno**: apertura, movimientos, **corte y cuadre**.
- **RF-6.4** **Multas** ligadas a renta y a la cuenta del cliente, generadas por el
  sistema.
- **RF-6.5** Comprobantes/recibos exportables (PDF/impresión) e impuestos configurables.
- **RF-6.6 — Módulo de multas opcional.** Se puede **activar/desactivar** por local.
  Apagado: la devolución no genera cargos (o los deja solo como observación).
- **RF-6.7 — Métodos de pago:** efectivo, tarjeta, transferencia (lista **extensible**);
  soporta **pago mixto** (parte efectivo, parte tarjeta).
  - **Efectivo:** registra monto recibido y **calcula el vuelto**; entra a la **caja
    física** (se cuenta en el corte).
  - **Tarjeta:** guarda **referencia/autorización**; no entra a la caja de efectivo
    (se concilia contra la terminal).
  - **Transferencia:** guarda **comprobante/referencia** (se concilia contra el banco).
- **RF-6.8** El depósito/garantía es un movimiento aparte, con su propio método y su
  **devolución del remanente**.
- **RF-6.9** Abonos (pagos parciales que dejan saldo) y reembolsos (devolución de venta
  o remanente de depósito) con su método.
- **RF-6.10** **Corte de caja por método:** separa efectivo (físico, se cuenta) de
  tarjeta/transferencia (se concilia); reportes de ingreso desglosados por método.
- **RF-6.11 — Modo de pago del cliente, configurable por empresa (RF-12.4).** Cada empresa
  decide si acepta **pago en línea** (pasarela: tarjeta/transferencia, con retención de
  depósito y reembolsos) y/o solo **apartar en línea y pagar en el local**. El checkout
  del cliente (RF-18.7) se adapta al modo habilitado.

### RF-7. Clientes
- **RF-7.1** Ficha ampliada (teléfono, correo, **documento de identidad**, dirección,
  foto/ID de garantía).
- **RF-7.2** **Historial**: rentas y ventas, saldos, depósitos activos, multas
  (puede abarcar varias sucursales — ver RF-14.4).
- **RF-7.3** **Estado de confianza / lista negra** y búsqueda por nombre/código/
  documento.

### RF-8. Empleados
- **RF-8.1** Alta ligada a cuenta de acceso y a sucursal(es).
- **RF-8.2** Horario/turno y **registro de actividad** (ventas, rentas, cortes de caja).

### RF-9. Reportes y analítica
- **RF-9.1** Datos **reales** con **filtro por fecha y por sucursal**: ingresos por
  renta y por venta, **ganancia (ingreso − costo)**, más rentados/vendidos, tasa de
  utilización, rentas vencidas, ventas por empleado, depósitos activos, valor de
  inventario, dañados/perdidos. Desglosable **por cualquier dimensión de etiqueta**
  marcada como variante (por color, por talla…).
- **RF-9.2** **Exportación** (PDF/CSV) y comparativa entre sucursales cuando aplique.
- **RF-9.3 — Tablero de estado de inventario.** Muestra, por cada grupo de stock,
  cuántas piezas están **dañadas / en limpieza / perdidas** y con **observaciones
  libres**. Filtrable. Visible para el dueño o los perfiles autorizados.

### RF-10. Reabastecimiento e inventario entrante
- **RF-10.1** Registro de **entradas de mercancía / proveedores** que suben stock de
  venta; alerta de **stock bajo**.
- **RF-10.2** **Ajustes de inventario** (mermas, correcciones) con motivo y auditoría.
- **RF-10.3** **Transferencias de stock entre sucursales** (cuando aplique).

### RF-11. Notificaciones y alertas
- **RF-11.1** Recordatorios de **devolución vencida** (al cliente y al dueño).
- **RF-11.2** Avisos de reserva/retiro, **stock bajo** y unidades en reparación
  pendientes.
- **RF-11.3** Los interruptores de notificaciones de Ajustes controlan esto de verdad.
- **RF-11.4 — Recordatorios por WhatsApp** al **número registrado del cliente**
  (devolución próxima/vencida, etc.).
- **RF-11.5 — Indicador de pendientes en el cliente.** En la lista/tarjeta de clientes,
  un indicador visual marca si tiene **pendientes** (rentas por vencer/vencidas,
  saldos, multas).
- **RF-11.6 — Filtros de gestión.** En Clientes (y secciones dedicadas): filtrar por
  **pendientes, infracciones/multas, rentas vencidas, saldos**.

### RF-12. Configuración del negocio
- **RF-12.1** Datos de tienda **persistentes** por sucursal; alta/baja de sucursales.
- **RF-12.2** Reglas por defecto: precio/depósito, **recargo por retraso**, impuestos,
  moneda, horario.
- **RF-12.3** Respaldo/restauración de datos.
- **RF-12.4 — Interruptores de módulos** (materializa el principio de configurabilidad):
  conteo de stock on/off, granularidad, QR vs. numeración, multas on/off, multi-sucursal
  on/off, permisos finos, tipos de etiqueta seleccionables por el cliente, **modo de pago
  (en línea / apartar y pagar en local)**.

### RF-13. Usabilidad — abstracción de la complejidad para el dueño
La complejidad interna **no se traslada al dueño**. Requerimiento explícito, no adorno.
- **RF-13.1** **Revelación progresiva:** la complejidad aparece solo cuando se necesita
  (unidad → sin config; por partes → aparecen slots; personalizable → aparecen
  etiquetas).
- **RF-13.2** **Asistente guiado (wizard)** para crear disfraz: una decisión por
  pantalla, en lenguaje humano ("¿quieres llevar la cuenta por color?"), nunca términos
  técnicos.
- **RF-13.3** **Plantillas:** el dueño arranca de un molde ("traje de 3 piezas",
  "disfraz de una pieza") y ajusta.
- **RF-13.4** **Vista previa en vivo:** mientras configura, ve lo que verá el cliente
  (la ruleta).
- **RF-13.5** **Valores por defecto sensatos:** etiquetas y categorías básicas
  precargadas; sin seguimiento de variantes salvo que se encienda.
- **RF-13.6** **Separar "administrar vocabulario" de "dar de alta":** crear
  etiquetas/categorías vive escondido en ajustes avanzados; registrar prendas y armar
  disfraces es la tarea diaria que solo **usa** el vocabulario.

### RF-14. Multi-sucursal y selección de sucursal (lado del cliente)
- **RF-14.1** **Todo se ancla a una sucursal:** disponibilidad, precios y reservas
  siempre se muestran para la sucursal seleccionada.
- **RF-14.2 — Entrada del cliente (modelo marketplace):** la app es un **marketplace**
  donde el cliente **descubre y busca empresas y sucursales** (texto, categoría,
  cercanía); solo se listan empresas **aprobadas/ACTIVAS** (RF-15). Un **enlace/QR** abre
  directo la tienda de un negocio dentro de la app (atajo). Ya dentro de una empresa:
  - **Un solo local:** entra directo a esa sucursal; **sin selector**.
  - **Varios locales:** **selector de sucursal** (lista, y opcionalmente **mapa / más
    cercana**), **cambiable en cualquier momento**; disponibilidad y precios se
    actualizan al instante.
- **RF-14.3** **Enlace/QR por negocio o por sucursal** para entrar directo.
- **RF-14.4** **Identidad vs. transacción:** la cuenta del cliente vive a nivel
  negocio/plataforma; cada reserva/renta/venta vive en una sucursal. El historial puede
  abarcar varios locales, pero cada operación sabe de qué local salió.
- **RF-14.5 (opcional)** "¿Lo hay en otro local?": si algo no está disponible aquí,
  mostrar disponibilidad en otras sucursales. Configurable.

### RF-15. Multi-tenant y alta de empresas (onboarding)
- **RF-15.1** Entidad **Empresa** (tenant) en el nivel superior; una Empresa tiene
  1..N sucursales. Todo el sistema es multi-tenant: cada Empresa ve **solo lo suyo**
  (taxonomía, inventario, usuarios, clientes, reportes), scoped por empresa y sucursal.
- **RF-15.2 — Auto-registro.** El dueño puede **crear/instanciar una Empresa sin estar
  aún aprobada**: nace en estado **PENDIENTE**. (La tabla `Empresa` puede instanciarse
  antes de que exista una empresa "oficial".)
- **RF-15.3 — Rol SuperAdmin de plataforma** (por encima del Dueño): revisa las
  empresas registradas, las **aprueba**, les **asigna identidad** ("empresa 1") y
  plan/permisos. Estados de la Empresa: **PENDIENTE → ACTIVA → SUSPENDIDA / RECHAZADA**.
- **RF-15.4 — Empresa en estado PENDIENTE.** Mientras no se apruebe, la Empresa **no
  puede hacer nada operativo** (ni configurar, ni operar, ni ser visible a clientes):
  solo espera la resolución. La plataforma se **compromete a responder (aprobar o
  rechazar) dentro de un plazo configurable (por defecto 2 días)**. Si el SuperAdmin no
  responde en ese plazo, la solicitud se marca como **vencida** y se **escala /
  recuerda** automáticamente hasta obtener respuesta. No existe ninguna otra capacidad
  en estado PENDIENTE.
- **RF-15.5** El registro de auditoría (RF-0.5) incluye las acciones del SuperAdmin
  (aprobar, suspender, asignar).
- **RF-15.6 — Vitrina del marketplace.** Solo las empresas **ACTIVAS/aprobadas** aparecen
  en el marketplace del cliente (RF-18.1). La aprobación del SuperAdmin es, por tanto, la
  puerta para ser visible públicamente.

### RF-16. Carrito persistente y segmentado por local y tipo
- **RF-16.1 — Carrito persistente.** Si el cliente sale sin completar la renta/compra,
  su pedido **no se pierde ni se borra**: queda como **pendiente / agregado** y lo
  retoma después.
- **RF-16.2 — Segmentación estricta: un pedido nunca mezcla locales ni tipos.** Cada
  pedido pendiente se identifica por **(cliente × sucursal × tipo)**, donde
  `tipo ∈ {renta, venta}`. Un mismo pedido **jamás** contiene artículos de más de una
  sucursal, ni combina renta con venta. Dentro de un local sí puede llevar **varios
  artículos**.
- **RF-16.3 — Múltiples pedidos pendientes, independientes.** El cliente puede tener
  **varios pedidos sin confirmar a la vez, pero uno por (local × tipo)**. Al salir de un
  local, sus pedidos **no se tocan**: siguen guardados. Al reingresar a un local se
  muestra el pedido de **ese** local: si lo modifica, se actualiza; si no lo toca, queda
  igual. Los pedidos **nunca se acumulan ni se mezclan entre locales**.
- **RF-16.4 — Renta y venta separadas dentro del mismo local.** Un cliente puede tener a
  la vez un **pedido de renta** y un **pedido de venta** del mismo local: coexisten y se
  confirman **por separado** (no se anulan entre sí). Entre locales distintos nunca se
  combinan; comprar/rentar en otro local es simplemente **otro pedido aparte**.
- **RF-16.5 — Fuente de verdad en el servidor (PostgreSQL).** El carrito vive ligado a
  la cuenta del cliente en el backend, así **sobrevive** a cierre de app, reinstalación
  o cambio de dispositivo.
- **RF-16.6 — Caché local (Room / SQLite) en Android.** Copia local para offline y
  respuesta inmediata; se **sincroniza** con el servidor al reconectar (ver RF-0.2 y
  RF-17.6).
- **RF-16.7** El carrito también existe en el **modo asistido presencial** (RF-3.7 /
  RF-4.6): el empleado puede armar un pedido para un cliente en el mostrador.

### RF-17. Plataforma técnica y capas de comunicación
> Definición de la frontera y el stack de comunicación. La **arquitectura interna,
> patrones, lenguaje y principios** del backend se discuten en un paso aparte (ver
> RF-17.7).
- **RF-17.1 — Base de datos: PostgreSQL.**
- **RF-17.2 — Diseño code-first.** Las entidades se definen en **código** y el esquema
  + migraciones se generan desde ahí (herramienta según el lenguaje elegido). Nota
  clave: la **taxonomía dinámica (RF-2.7) se modela como datos, no como esquema** —
  filas en tablas de tipos/valores de etiqueta y sus relaciones, **nunca** columnas o
  tablas creadas al vuelo. Así el code-first se mantiene limpio y estable.
- **RF-17.3 — Contrato backend ↔ Android (Kotlin): API REST sobre HTTPS con JSON**,
  versionada (`/api/v1`), con DTOs de request/response, errores estandarizados y
  paginación. **Definida contract-first con OpenAPI**, generando el cliente Kotlin desde
  el contrato (detalle en §5.6).
- **RF-17.4 — Autenticación por token** (JWT / OAuth2 bearer), sesión persistente; el
  token transporta **empresa + rol + permisos**. La **sucursal activa** viaja en cabecera
  `X-Sucursal-Id` (cambiable al vuelo; ver §5.6), no en el token. Multi-tenant; alimenta
  RF-1 y RF-15.
- **RF-17.5 — Capas del cliente Android:** HTTP (Retrofit/OkHttp) → repositorio →
  **caché local Room (SQLite)** → UI, con separación UI / estado / repositorio.
- **RF-17.6 — Sincronización offline (estrategia híbrida):** las acciones sin conexión se
  **encolan (outbox)** y se concilian al reconectar; **idempotencia obligatoria** en
  operaciones sensibles (pagos, confirmación de renta/venta) para no duplicar. El alcance
  exacto de qué opera offline y qué queda "pendiente de confirmar" está en §5.7.
- **RF-17.7 — Arquitectura interna: decidida.** Java + Spring Boot, monolito modular,
  hexagonal pragmático, PostgreSQL con esquema compartido + `empresa_id`. El detalle,
  la clasificación de módulos por rigor y el control anti-erosión están en **§5**.

### RF-18. Vista del consumidor (app cliente)
Contexto: **modelo marketplace** (una sola app, muchas empresas). El cliente descubre
negocios y sucursales, y también puede entrar directo por enlace/QR. Identidad de cliente
a nivel **plataforma** (RF-14.4). La lógica ya vive en el backend definido arriba; aquí
se listan las brechas y requerimientos propios del cliente respecto a lo que la app ya
tiene (catálogo, detalle, ruleta de personalización, reserva→checkout→confirmación,
alquileres, notificaciones, perfil).
- **RF-18.1 — Descubrimiento (marketplace).** Buscar y explorar empresas y sucursales
  (por texto, categoría, cercanía/ubicación); solo se listan empresas **ACTIVAS/aprobadas**
  (RF-15). Un **enlace/QR** abre directo la tienda de un negocio dentro de la app (atajo,
  no reemplazo del descubrimiento).
- **RF-18.2 — Selección de sucursal.** Catálogo, precios y disponibilidad siempre de la
  sucursal elegida (RF-14.1); cambiable al vuelo.
- **RF-18.3 — Catálogo con renta y venta.** Distingue artículos rentables / vendibles /
  ambos; búsqueda y filtros (precio, talla, disponibilidad, etiquetas).
- **RF-18.4 — Personalización (ruleta) con datos reales.** Las opciones salen del **pool
  del dueño filtrado por categoría + etiquetas**, con **disponibilidad derivada**
  (RF-2.4, RF-2.7), no listas fijas.
- **RF-18.5 — Carrito persistente y segmentado.** Por **(empresa × sucursal × tipo)**
  (RF-16); no se pierde, con pendientes/agregados visibles.
- **RF-18.6 — Disponibilidad y reserva por fechas.** Calendario de retiro/devolución,
  verificación real y sin traslapes (RF-3.2).
- **RF-18.7 — Checkout con depósito y pago configurable.** Muestra **depósito/garantía**;
  según la empresa, **pago en línea** (pasarela) o **apartar y pagar en el local**
  (RF-6.11); acepta **términos/contrato**.
- **RF-18.8 — Cuenta real.** Registro/login (RF-1); perfil con **teléfono** (WhatsApp,
  RF-11.4) y **documento** (garantía, RF-7.1); datos guardados.
- **RF-18.9 — Estados e historial.** Reservas, rentas
  (reservada→lista→activa→vencida→devuelta→cancelada) y **compras**; **extender/cancelar**
  reserva desde el cliente (RF-3.5/3.6, RF-7.2).
- **RF-18.10 — Cargos y saldos del cliente.** Ver multas/daños/retrasos pendientes y su
  saldo (RF-6).
- **RF-18.11 — Notificaciones reales.** **FCM** + recordatorios por evento (confirmación,
  devolución próxima/vencida); WhatsApp como canal paralelo (RF-11).
- **RF-18.12 — Persistencia/offline.** Caché Room, carrito en servidor y sincronización
  híbrida (RF-16.5, §5.7).
- **RF-18.13 — (Opcional)** "¿lo hay en otro local?" (RF-14.5), favoritos, reseñas/
  valoraciones.

---

## 5. Arquitectura del backend (decisiones cerradas)

### 5.1 Stack y estilo
- **Lenguaje/plataforma:** Java + Spring Boot (JVM).
- **Base de datos:** PostgreSQL, code-first con JPA/Hibernate + migraciones **Flyway**.
- **Estilo:** monolito **modular** con **arquitectura hexagonal pragmática** —
  puertos/adaptadores en los contextos ricos, capas simples en los CRUD.
- **Multi-tenant:** esquema compartido + `empresa_id`, con filtrado forzado por el
  tenant del token.
- **Contrato con Android (Kotlin):** REST/HTTPS/JSON versionado (RF-17.3); auth por
  token (empresa + rol + permisos) con la sucursal activa en cabecera (RF-17.4, §5.6).

### 5.2 Módulos (bounded contexts) y su nivel de rigor
Para que "pragmático" no se vuelva arbitrario, se decide **desde el inicio y por
escrito** qué módulo lleva hexagonal completo y cuál capas simples:

| Módulo | Nivel | Por qué |
|---|---|---|
| Identidad y tenant (empresa, sucursal, usuarios, permisos, auth) | Hexagonal | Seguridad + reglas de aprobación/estado |
| Catálogo y taxonomía (etiquetas, categorías) | Hexagonal | Modelo dinámico, el más delicado |
| Inventario y disponibilidad | Hexagonal | Stock por variante, disponibilidad derivada |
| Pedidos / carrito | Hexagonal | Reglas de segmentación local × tipo |
| Rentas | Hexagonal | Ciclo de estados, fechas, depósito |
| Ventas / POS | Hexagonal | Stock, cobros |
| Pagos, caja y depósitos | Hexagonal | Dinero → idempotencia y reglas |
| Devoluciones y multas | Hexagonal | Eventos, liquidación de depósito |
| Clientes | Simple | CRUD + historial (consultas) |
| Empleados | Simple | CRUD ligado a auth |
| Reportes | Simple (lectura) | Modelos de lectura, sin escritura de dominio |
| Notificaciones (WhatsApp) | Simple (adaptador) | Poca lógica, mucho adaptador |
| Configuración de empresa | Simple | Interruptores de módulos |

Regla para el futuro: un módulo "simple" **sube a hexagonal** cuando acumula reglas de
negocio propias; **nunca al revés** sin justificarlo.

### 5.3 Control para que la arquitectura NO se descuadre entre etapas
No se confía en la disciplina manual: **se automatiza** y **falla el build** si algo se
rompe.
- **Estructura de paquetes idéntica por módulo:** `dominio/` · `aplicacion/` ·
  `adaptadores/entrada/` · `adaptadores/salida/`. Misma forma en todos los módulos.
- **Reglas de dependencia verificadas por build con ArchUnit** (tests de arquitectura),
  por ejemplo:
  - el `dominio` **no** importa Spring, JPA ni nada de web;
  - los adaptadores dependen de **puertos**, no al revés;
  - un módulo **no** accede a las clases internas de otro, solo a su API pública.
- **Spring Modulith** para declarar y **verificar los límites entre módulos** del
  monolito (qué módulo puede hablar con cuál) y documentarlos.
- **Definición de "hecho" por módulo:** puertos definidos, sin fugas del framework al
  dominio, migración Flyway incluida, tests de dominio sin BD, `empresa_id` aplicado.
- **Plantilla/esqueleto de módulo** para crear uno nuevo ya con la estructura correcta.
- **CI** corre ArchUnit + Modulith + tests en cada cambio: si se descuadra, **no
  mergea**.

Así el pragmatismo queda acotado por reglas mecánicas, no por buena voluntad.

### 5.4 Cómo se fuerza el aislamiento multi-tenant
- Cada tabla de negocio lleva **`empresa_id`** (y donde aplique `sucursal_id`).
- El `empresa_id` viaja en el **token**, se coloca en un **contexto de request** y un
  **filtro/interceptor** lo aplica a toda consulta (filtros de Hibernate y/o
  **Row-Level Security** de Postgres como segundo cinturón), para que sea **imposible**
  leer datos de otra empresa por olvido.
- El **SuperAdmin de plataforma** es el único rol que cruza tenants (aprobación de
  empresas); todos sus accesos quedan en auditoría.

### 5.5 Patrones de diseño previstos
Repository (puerto) · Strategy (granularidad de stock, cálculo de precio, método de
pago) · Máquina de estados (ciclo de renta, estado de empresa) · Specification
(disponibilidad y filtros por etiqueta) · Domain events (devolución→multa,
venta→baja de stock, aprobación de empresa) · Outbox (sync offline y WhatsApp confiable)
· Factory/Builder (armado del disfraz por partes) · Policy/feature flags
(configurabilidad por empresa).

**Principios:** DDD táctico con lenguaje ubicuo (en español), SOLID con inversión de
dependencias como corazón del hexágono, dominio puro y testeable sin BD, idempotencia
en toda operación de dinero.

### 5.6 Comunicación backend ↔ Android (decisiones cerradas)
- **API REST/HTTPS/JSON** versionada (`/api/v1`).
- **Contrato contract-first con OpenAPI**: el archivo OpenAPI es la fuente única; el
  cliente Retrofit/Kotlin se **genera** desde él (backend y Android no pueden divergir —
  es el equivalente, en la frontera, al control de ArchUnit de §5.3).
- **Errores** en formato **Problem Details (RFC 7807)** (código, mensaje, errores por
  campo).
- **Autenticación:** **JWT de acceso (corto) + refresh (largo)**; el token lleva
  `empresa + rol + permisos`. La **sucursal activa** viaja en cabecera **`X-Sucursal-Id`**
  (cambiable al vuelo, RF-14), validada contra la empresa del token. Tokens en
  **EncryptedSharedPreferences / Android Keystore**; refresco transparente con
  Authenticator de OkHttp.
- **Capas del cliente:** UI + ViewModel (StateFlow, coroutines) → Repository (fuente de
  verdad) → Room (caché/offline) + Retrofit/OkHttp (interceptores de token, sucursal y
  reintento). Mapeo DTO ↔ modelo también en el cliente.
- **Fotos/media:** no van en el JSON; se suben a **almacenamiento de objetos
  (S3-compatible) con URLs prefirmadas**; la BD guarda solo la URL.
- **Push: Firebase Cloud Messaging (FCM)** desde el inicio (avisos al dueño —vencidas,
  stock bajo— y al cliente). WhatsApp sigue siendo un canal aparte (RF-11.4).

### 5.7 Estrategia offline (decisión: híbrida)
Principio: **nunca se pierde el trabajo, pero lo que compromete recursos físicos
compartidos (disponibilidad/reserva) necesita al servidor como árbitro.** Se descarta
el "offline total en mostrador" por ahora.
- **Mecanismos:** borradores locales en Room; **outbox** con reintento y **clave de
  idempotencia**; **IDs generados en el cliente (UUID)** para identidad estable offline;
  el **servidor gana** los conflictos; las operaciones sensibles quedan **"pendiente de
  confirmar"** hasta sincronizar (WorkManager en segundo plano).
- **Clasificación por operación:**

| Operación | ¿Offline? | Nota |
|---|---|---|
| Ver catálogo / disponibilidad | Sí (lectura, caché) | Disponibilidad "según última sincronización" |
| Armar carrito / pedido | Sí | Borrador local |
| Registrar cliente | Sí | ID local; dedupe por teléfono/documento al sincronizar |
| Pago / abono / movimiento de caja | Sí (provisional) | Aditivo; se concilia al reconectar |
| Procesar devolución (checklist) | Sí (provisional) | El artículo físico está en mano |
| Confirmar reserva de unidad contendida | Preferible en línea | Offline = reserva **provisional**, sujeta a confirmación |
| Confirmar venta del último ejemplar | En línea (o con buffer) | Evita sobreventa si dos la toman a la vez |
| Corte / cierre de caja | En línea | Consolida; hacerlo con red |
| Folio / comprobante numerado | El número lo asigna el servidor | Offline usa número **provisional** |

- **Descartado por ahora** (se puede añadir si un local lo exige): garantía fuerte de
  no-sobreventa 100% offline, cierre de caja multi-dispositivo offline, merge offline
  entre dispositivos.

---

## 6. Módulos opcionales (resumen del principio de configurabilidad)

| Módulo / función | ¿Configurable? | Nota |
|---|---|---|
| Conteo y visibilidad de stock | Sí | Algunos locales no llevan cantidades |
| Granularidad del stock | Sí | Unidad / modelo / combinación de etiquetas / parte |
| Identificación por QR | Sí | Alternativa: numeración simple por grupo de stock |
| Módulo de multas | Sí | Algunos lo quieren, otros no |
| Multi-sucursal | Sí | Un local o varios |
| Permisos finos por empleado | Sí | Rol-plantilla + ajustes encima |
| Etiquetas seleccionables por el cliente | Sí, por slot | Define la personalización |
| "¿Lo hay en otro local?" | Sí | Nice-to-have |

---

## 7. Orden sugerido de implementación (de más crítico a menos)

1. **Base técnica (RF-17):** PostgreSQL, code-first, contrato REST y autenticación
   multi-tenant — es el cimiento sobre el que se construye todo.
2. **Multi-tenant y alta de empresas (RF-15)** — la Empresa (tenant) y el aislamiento
   de datos condicionan cada tabla.
3. **Motor de etiquetas / taxonomía (RF-2.7)** — todo el inventario depende de él.
4. **Modelo de datos + persistencia (RF-0)** — nada real funciona sin esto.
5. **Inventario con granularidad y disponibilidad derivada (RF-2)**.
6. **Carrito persistente y segmentado por local y tipo (RF-16)**.
7. **Proceso completo de renta (RF-3)** — cliente + fechas + disponibilidad + cobro +
   depósito + estados, incluido el modo asistido presencial.
8. **Devolución cerrando el ciclo (RF-5)** — inventario + multas + depósito.
9. **Venta / POS (RF-4)** y **pagos/caja/finanzas (RF-6)**.
10. **Auth y roles/permisos reales (RF-1)**.
11. **Multi-sucursal y selección de sucursal (RF-14)**.
12. **Reportes reales (RF-9)**, **reabastecimiento (RF-10)**, **clientes/empleados
    enriquecidos (RF-7, RF-8)**, **notificaciones/WhatsApp (RF-11)**, **configuración
    (RF-12)**.
13. **Usabilidad (RF-13)** — transversal: se diseña junto con cada pantalla, no al final.

---

## 8. Pendiente

- **Vista del consumidor:** integrada como **RF-18** (modelo marketplace + pago
  configurable por empresa). Queda para la fase de implementación: elegir la **pasarela
  de pago** concreta cuando se active el pago en línea, y detallar la **UX de
  descubrimiento** (búsqueda, cercanía, filtros, reseñas).
