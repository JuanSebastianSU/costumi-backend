# PLAN — Seed de datos de prueba "de verdad" (Costumi) · Ecuador · 2 tiendas

> Objetivo: poblar la base (hoy vacía, esquema intacto en V79) con datos **coherentes, realistas y
> suficientes** para probar TODO el sistema. Clave de este diseño: **dos tiendas con catálogos GENUINAMENTE
> distintos**, porque el catálogo (categorías, tipos de etiqueta, valores) es el espacio configurable de
> **cada dueño** — no una plantilla compartida.
>
> **Este documento es el diseño previo.** No se inserta nada hasta validarlo. Las **fotos** de prendas y
> disfraces se resuelven en una fase aparte (imágenes reales por prenda/disfraz).

---

## 0. Escenario · Ecuador

País **Ecuador** (dolarizado). Base para ambas tiendas:

| | Valor |
|---|---|
| Moneda | **USD** |
| IVA (`tasaImpuesto`) | **0.15** (15%, vigente 2024) |
| Documento cliente | **Cédula** (10 dígitos) |
| Teléfono | **09xxxxxxxx** |
| Rango de precios | renta disfraz **$12–40**, prenda suelta **$2–15**, accesorio de venta **$2–10** |

**Dos empresas, dos dueños, dos catálogos:**

| | **Tienda 1** | **Tienda 2** |
|---|---|---|
| Nombre | **Fiesta & Fantasía** | **El Baúl del Disfraz** |
| Ciudad | **Quito** | **Cuenca** |
| Sucursales | 2 (Centro Histórico + Cumbayá) → prueba multi-sucursal | 1 (Cuenca Centro) |
| **Mentalidad del dueño** | organiza por **OCASIÓN + TEMÁTICA** | organiza por **PERSONAJE + LICENCIA + LÍNEA** |
| Dueño | el del bootstrap (`…+dueno@gmail.com`) | insertado por SQL (`…+cuenca@gmail.com`) |

Esto ejercita: navegación por rol, multi-sucursal (Tienda 1), catálogos distintos, aislamiento multi-tenant
(que el catálogo de una NO se ve en la otra), inventario con variantes, disfraces compuestos, y reportes.

---

## 1. Orden de construcción (por tienda)

```
Para CADA tienda:
  L0  Empresa (ACTIVA) + Configuración
  L1  Sucursales
  L2  Usuarios de trabajo + Membresías + asignación a sucursales + overrides de permisos
  L3  Clientes
  L4  Catálogo:  Categorías de prenda  +  Tipos de etiqueta (+ interruptores)  +  Valores
  L5  Inventario: Prendas (con etiquetas que clasifican) + Grupos de stock (variantes) por sucursal
  L6  Disfraces:  Categorías de disfraz + Disfraces con slots + precios
  L7  Operaciones: caja, ventas, rentas (varios estados), pagos, reembolsos, notificaciones, favoritos
Global:
  L8  Backdating (SQL): reparte fechas de operaciones en ~30 días para dar serie a los reportes
```

---

## 2. Configuración por tienda

Ambas: `conteoStock` on, `multasActivo` on, `reembolsosActivos` on (ventana 8 días), `pagoEnLinea` off,
`tasaImpuesto` 0.15, `moneda` USD, `recargoPorRetrasoPorDia` **$2**, `modoRecargoRetraso` ACUMULATIVA.

| | Tienda 1 (Quito) | Tienda 2 (Cuenca) |
|---|---|---|
| `multiSucursal` | **true** | false |

---

## 3. Sucursales

**Tienda 1 — Fiesta & Fantasía (Quito)**
| # | Nombre | Dirección |
|---|---|---|
| S1a | Centro Histórico | García Moreno N4-20, Quito |
| S1b | Cumbayá | Av. Interoceánica km 12, Cumbayá |

**Tienda 2 — El Baúl del Disfraz (Cuenca)**
| # | Nombre | Dirección |
|---|---|---|
| S2 | Cuenca Centro | Gran Colombia 7-40, Cuenca |

El stock se reparte S1a/S1b en la Tienda 1 (para probar "sin stock aquí" + transferencias).

---

## 4. Usuarios, roles y permisos

**Los empleados se PLANTAN directo (por SQL)** — así el equipo ya está listo para operar/probar. (Vos probás
el **flujo de invitación** aparte, invitando gente nueva desde la app.) Cada empleado = `usuario` +
`membresia` ACTIVA + `usuario_sucursal` + filas de `permiso_empleado` para sus overrides. Password con hash
**bcrypt** (mismo `Passw0rd!` para todos, se cambia en la prueba).

**Tienda 1 (Quito)**
| Persona | Email | Rol | Sucursal(es) |
|---|---|---|---|
| Dueño (bootstrap) | `…+dueno@gmail.com` | DUEÑO | S1a + S1b |
| Ana | `…+ana@gmail.com` | ENCARGADO | **S1b** (prueba pirámide de sucursal) |
| Carlos | `…+carlos@gmail.com` | MOSTRADOR | S1a |
| Beto | `…+beto@gmail.com` | BODEGA | S1a + S1b |

**Tienda 2 (Cuenca)**
| Persona | Email | Rol | Sucursal |
|---|---|---|---|
| Dueño 2 (SQL) | `…+cuenca@gmail.com` | DUEÑO | S2 |
| Sofía | `…+sofia@gmail.com` | MOSTRADOR | S2 |

### 4.1 ★ Permisos por persona (efectiva = preset del rol ± overrides)

El rol da un **preset** (`PlantillaDeRol`); encima, el dueño concede/niega capacidades concretas. Estos
overrides están pensados para que se **vea el catálogo en acción** y para contar una historia realista de
"cada dueño reparte confianza distinto". (Dueños = las 65, sin overrides.)

**Tienda 1 — Ana (ENCARGADO, preset = todo):** el dueño le **recorta** lo más sensible.
| Override | Capacidad | Por qué |
|---|---|---|
| ❌ niega | `EMPLEADOS_INVITAR` | no suma personal por su cuenta |
| ❌ niega | `EMPLEADOS_EDITAR_PERMISOS` | no reparte confianza (eso es del dueño) |
| ❌ niega | `CONFIGURACION_EDITAR` | no cambia las reglas del local |

**Tienda 1 — Carlos (MOSTRADOR, preset operativo):** un mostrador de confianza pero acotado.
| Override | Capacidad | Por qué |
|---|---|---|
| ✅ concede | `REPORTES_VER` | el dueño confía en que Carlos vea reportes (no está en el preset) |
| ❌ niega | `VENTAS_DEVOLVER` | no ejecuta devoluciones de venta (plata que vuelve) |

**Tienda 1 — Beto (BODEGA, preset de inventario):**
| Override | Capacidad | Por qué |
|---|---|---|
| ✅ concede | `CATALOGO_ETIQUETAS_GESTIONAR` | además del stock, mantiene tipos/valores de etiqueta (variantes) |
| ❌ niega | `INVENTARIO_STOCK_AJUSTAR` | ajustar puede tapar faltantes → solo con visto bueno del encargado |

**Tienda 2 — Sofía (MOSTRADOR, preset operativo):** tienda chica, hace más cosas… pero no todo.
| Override | Capacidad | Por qué |
|---|---|---|
| ✅ concede | `INVENTARIO_PRENDA_GESTIONAR` | también carga/edita prendas |
| ❌ niega | `RENTAS_CERRAR` | no decide el depósito al cerrar (lo cierra el dueño) |

> Esto ejercita el interceptor de permisos (403 al usar algo negado), la matriz `GET/PUT
> /empleados/{id}/permisos`, `GET /empleados/me/permisos` (la app arma la navegación con esto) y las
> invariantes (pirámide, "no concedés lo que no tenés", dueño no restringible).

---

## 5. Clientes (por tienda)

**Tienda 1:** CL1 María Andrade · CL2 Juan Cabrera · CL3 Lucía Paredes (**lista negra**) ·
CL4 Andrés Villacís (marketplace) · CL5 Camila Suárez (archivada).
**Tienda 2:** CL6 Diego Ortega · CL7 Paola Zamora · CL8 Marco Vintimilla (marketplace).
Cédulas 10 díg., teléfonos 09xxxxxxxx. Deudas/estado de cuenta salen de las rentas/ventas de L7.

---

## 6. ★ CATÁLOGOS DISTINTOS (el corazón del ejercicio)

### 6.1 Tienda 1 — "Fiesta & Fantasía" (piensa por OCASIÓN + TEMÁTICA)

**Categorías de prenda (parte del cuerpo) — 11:**
Cabeza y tocados · Máscaras y caretas · Antifaces · Torso · Piernas · Trajes de una pieza ·
Capas, mantos y alas · Calzado · Guantes · Utilería (varitas/espadas/bastones) · Complementos.

**Tipos de etiqueta — 6:**
| Tipo | defineVariante | seleccionableCliente | aplica a | Valores |
|---|:---:|:---:|---|---|
| **Talla** | ✅ | ✅ | ropa/calzado | XS, S, M, L, XL, Niño 4-6, Niño 8-10 |
| **Color** | ✅ | ✅ | todas | Negro, Rojo, Blanco, Dorado, Morado, Azul, Verde, Rosa, Café, Gris |
| **Ocasión** | ❌ | ✅ | todas | Halloween, Carnaval, **Fiestas de Quito**, **Inti Raymi**, Año Viejo, Cumpleaños infantil, Teatro/Escolar |
| **Temática** | ❌ | ✅ | todas | Terror, Superhéroes, Histórico, Fantasía, Animales, Profesiones |
| **Público** | ❌ | ✅ | todas | Adulto, Niño, Unisex |
| **Material** | ❌ | ❌ | todas | Poliéster, Satén, Terciopelo, Licra |

> Esta tienda **NO** usa Personaje/Licencia/Línea. Su eje mental es *para qué evento* y *qué temática*.

### 6.2 Tienda 2 — "El Baúl del Disfraz" (piensa por PERSONAJE + LICENCIA + LÍNEA)

**Categorías de prenda — 7 (más gruesas, orientadas a producto/personaje):**
Disfraces completos · Pelucas y cabello · Máscaras · Accesorios de personaje · Calzado y botas ·
Ropa base (camisas/pantalones) · Sombrerería.

**Tipos de etiqueta — 6:**
| Tipo | defineVariante | seleccionableCliente | aplica a | Valores |
|---|:---:|:---:|---|---|
| **Talla** | ✅ | ✅ | ropa/calzado/completos | S, M, L, XL, Única, Infantil |
| **Color** | ✅ | ✅ | todas | Negro, Rojo, Azul, Blanco, Verde, Morado |
| **Personaje** | ❌ | ✅ | todas | Batman, Superman, Spiderman, Bruja, Vampiro, Pirata, Princesa, Catrina, Payaso, Esqueleto |
| **Licencia** | ❌ | ❌ | todas | DC, Marvel, Disney, Genérico |
| **Línea** | ❌ | ❌ | todas | Premium, Estándar, Económica |
| **Edad** | ❌ | ✅ | todas | Adulto, Infantil |

> Esta tienda **NO** usa Ocasión/Temática/Público/Material. Su eje mental es *qué personaje*, *de qué
> franquicia* y *en qué gama de precio*. **Solo Talla y Color coinciden con la Tienda 1** → catálogos de
> verdad distintos.

---

## 7. ★ Inventario — Prendas + Variantes

Lectura: `tipo` R=renta V=venta A=ambos · precios USD · "clasifica" = etiquetas que NO son variante ·
"variantes" = combinaciones Talla×Color con stock por sucursal.

### 7.1 Tienda 1 (Quito) — 15 prendas · stock S1a / S1b

| id | Prenda | Categoría | Tipo | R | V | Clasifica | Variantes (→ S1a/S1b) |
|---|---|---|---|---|---|---|---|
| Q01 | Capa de vampiro | Capas | R | 6 | — | Terror · Halloween · Adulto · Satén | (M,Negro)4/2 · (L,Negro)3/1 · (M,Rojo)2/1 |
| Q02 | Sombrero de bruja | Cabeza | R | 3 | — | Terror · Halloween · Adulto | (Negro)5/2 · (Morado)2/1 |
| Q03 | Máscara de calavera | Máscaras | A | 3 | 4 | Terror · Halloween · Unisex | (Blanco)6/3 · (Negro)3/2 |
| Q04 | Camisa victoriana | Torso | A | 5 | 25 | Histórico · Teatro · Adulto · Poliéster | (S,Blanco)3/2 · (M,Blanco)4/2 · (L,Blanco)3/1 |
| Q05 | Frac / levita | Torso | R | 9 | — | Histórico · Adulto · Poliéster | (M,Negro)3/1 · (L,Negro)2/1 |
| Q06 | Sombrero de copa | Cabeza | R | 4 | — | Histórico · Adulto | (Negro)4/2 |
| Q07 | Vestido victoriano | Trajes 1 pieza | R | 15 | — | Histórico · Teatro · Adulto · Satén | (S,Dorado)2/1 · (M,Dorado)2/1 · (M,Morado)1/1 |
| Q08 | Traje torso de héroe | Torso | R | 7 | — | Superhéroes · Unisex · Licra | (S,Rojo)3/1 · (M,Rojo)4/2 · (M,Azul)3/1 |
| Q09 | Capa de héroe | Capas | R | 4 | — | Superhéroes · Unisex · Satén | (Rojo)5/2 · (Azul)4/2 |
| Q10 | Antifaz | Antifaces | A | 2 | 4 | Superhéroes · Carnaval · Unisex | (Negro)10/5 |
| Q11 | Vestido de princesa | Trajes 1 pieza | R | 6 | — | Fantasía · Cumpleaños infantil · Niño · Satén | (Niño 4-6,Rosa)3/1 · (Niño 8-10,Rosa)3/2 · (Niño 8-10,Azul)2/1 |
| Q12 | Corona | Cabeza | A | 2 | 5 | Fantasía · Unisex | (Dorado)8/4 |
| Q13 | Alas de hada | Capas y alas | R | 3 | — | Fantasía · Cumpleaños infantil · Niño | (Blanco)5/2 · (Rosa)3/2 |
| Q14 | Mameluco animal | Trajes 1 pieza | R | 6 | — | Animales · Unisex · Poliéster | (S,Café)3/1 · (M,Café)3/2 · (M,Gris)2/1 |
| Q15 | Varita mágica | Utilería | V | — | 3 | Fantasía · Unisex | **única** 20/10 |

### 7.2 Tienda 2 (Cuenca) — 12 prendas · stock S2

| id | Prenda | Categoría | Tipo | R | V | Clasifica | Variantes (→ S2) |
|---|---|---|---|---|---|---|---|
| B01 | Disfraz Batman | Disfraces completos | R | 20 | — | Batman · DC · Premium · Adulto | (M,Negro)3 · (L,Negro)2 |
| B02 | Disfraz Superman | Disfraces completos | R | 20 | — | Superman · DC · Premium · Adulto | (M,Azul)2 · (L,Azul)2 |
| B03 | Disfraz Spiderman | Disfraces completos | A | 18 | 60 | Spiderman · Marvel · Estándar · Adulto | (M,Rojo)3 · (L,Rojo)2 |
| B04 | Disfraz Spiderman niño | Disfraces completos | R | 10 | — | Spiderman · Marvel · Estándar · Infantil | (Infantil,Rojo)4 |
| B05 | Disfraz de bruja | Disfraces completos | R | 12 | — | Bruja · Genérico · Estándar · Adulto | (M,Negro)3 · (L,Negro)2 |
| B06 | Disfraz de Catrina | Disfraces completos | R | 15 | — | Catrina · Genérico · Premium · Adulto | (M,Morado)2 · (L,Morado)1 |
| B07 | Peluca de payaso | Pelucas | A | 3 | 8 | Payaso · Genérico · Económica | (Rojo)6 · (Verde)3 |
| B08 | Peluca de princesa | Pelucas | R | 4 | — | Princesa · Disney · Estándar | (Dorado→usa Blanco/Amarillo)… (Blanco)4 |
| B09 | Máscara de esqueleto | Máscaras | V | — | 5 | Esqueleto · Genérico · Económica | (Blanco)10 · (Negro)5 |
| B10 | Botas de pirata | Calzado | R | 5 | — | Pirata · Genérico · Estándar | (M,Negro)3 · (L,Negro)2 |
| B11 | Camisa de pirata | Ropa base | R | 5 | — | Pirata · Genérico · Estándar · Adulto | (M,Blanco)4 · (L,Blanco)2 |
| B12 | Sombrero de pirata | Sombrerería | R | 3 | — | Pirata · Genérico · Económica | (Única,Negro)6 |

> Nota: la Tienda 2 vende sobre todo **disfraces armados de una pieza** (categoría "Disfraces completos"),
> coherente con un dueño que piensa por personaje. La Tienda 1, en cambio, arma disfraces por **slots** a
> partir de prendas sueltas (abajo).

---

## 8. ★ Disfraces (productos con slots)

Notación: **[FIJA X]** prenda fija · **[OPC X|Y]** personalizable con opciones explícitas · (opcional) no
bloquea disponibilidad · precio *suma* (de prendas) o *general N* (anula la suma).

### 8.1 Tienda 1 (Quito) — arma disfraces por slots. Categorías de disfraz: Terror, Superhéroes, Histórico, Infantil, Animales.

| id | Disfraz | Tema | Tipo | Precio | Slots |
|---|---|---|---|---|---|
| QD1 | Conde Vampiro | Terror | R | general 12 | Capa **[FIJA Q01]** · Torso **[FIJA Q04]** · Máscara opc **[FIJA Q03]** |
| QD2 | Bruja Clásica | Terror | R | suma | Cabeza **[FIJA Q02]** · Traje **[FIJA Q07]** · Utilería opc **[FIJA Q15]** |
| QD3 | Superhéroe | Superhéroes | A | suma | Torso **[OPC Q08]** · Capa **[FIJA Q09]** · Antifaz **[FIJA Q10]** |
| QD4 | Caballero Victoriano | Histórico | R | suma | Torso **[FIJA Q05]** · Cabeza **[FIJA Q06]** |
| QD5 | Dama Victoriana | Histórico | R | general 22 | Traje **[FIJA Q07]** · Complemento opc **[FIJA Q12]** |
| QD6 | Princesa | Infantil | R | general 10 | Traje **[FIJA Q11]** · Corona **[FIJA Q12]** · Varita opc **[FIJA Q15]** · Alas opc **[FIJA Q13]** |
| QD7 | Animalito | Animales | R | suma | Cuerpo **[FIJA Q14]** |
| QD8 | Halloween a tu gusto | Terror | R | suma | Torso **[OPC Q04|Q08]** · Cabeza **[OPC Q02|Q06]** · Máscara opc **[FIJA Q03]** |

### 8.2 Tienda 2 (Cuenca) — casi todo es "1 slot fijo" (disfraz completo). Categorías: Superhéroes, Terror, Piratas, Infantil.

| id | Disfraz | Tema | Tipo | Precio | Slots |
|---|---|---|---|---|---|
| BD1 | Batman | Superhéroes | R | general 20 | Cuerpo **[FIJA B01]** |
| BD2 | Superman | Superhéroes | R | general 20 | Cuerpo **[FIJA B02]** |
| BD3 | Spiderman | Superhéroes | A | general 18 | Cuerpo **[FIJA B03]** |
| BD4 | Bruja | Terror | R | suma | Cuerpo **[FIJA B05]** · Peluca opc **[FIJA B08]** |
| BD5 | Catrina | Terror | R | general 15 | Cuerpo **[FIJA B06]** |
| BD6 | Pirata | Piratas | R | suma | Camisa **[FIJA B11]** · Sombrero **[FIJA B12]** · Botas opc **[FIJA B10]** |

> Contraste intencional: **Tienda 1 = disfraz modular por slots** desde prendas sueltas; **Tienda 2 =
> disfraz-producto (1 slot fijo)** que refleja su inventario de trajes completos. Dos formas válidas y
> distintas de usar el mismo modelo.

---

## 9. ★ Operaciones — cobertura de TODOS los estados

La idea es que exista al menos un caso de **cada estado y cada flujo**, para probar la app entera y llenar
los reportes. Estados reales del modelo:
`Renta {RESERVADA→ACTIVA→DEVUELTA→CERRADA, RESERVADA→CANCELADA}` ·
`Venta {CONFIRMADA, PARCIALMENTE_DEVUELTA, DEVUELTA}` ·
`Pedido/Carrito {PENDIENTE, CONFIRMADO}` ·
`Reembolso {PENDIENTE, APROBADA, RECHAZADA}` ·
`Pieza en devolución {BIEN, DANADA, EN_LIMPIEZA, PERDIDA}` ·
`Caja/Turno {ABIERTO, CERRADO}`.

### 9.1 Rentas (Tienda 1 ~8 · Tienda 2 ~5) — un caso por estado/variante
| # | Estado / variante | Qué prueba |
|---|---|---|
| R1 | **RESERVADA** (entrega futura) | reserva que descuenta disponibilidad sin entregar |
| R2 | **ACTIVA** (entregada, en curso, dentro de plazo) | renta normal en curso |
| R3 | **ACTIVA vencida** (fecha fin ya pasó, sin devolver) | aviso de "renta vencida" + tablero |
| R4 | RESERVADA→**CANCELADA** | cancelación libera stock |
| R5 | ACTIVA→**DEVUELTA** a tiempo, piezas **BIEN** → **CERRADA** con depósito **devuelto** | flujo feliz completo |
| R6 | ACTIVA→**DEVUELTA con retraso** → **multa** (recargo $2/día × días) → **CERRADA** | cálculo de multa por retraso |
| R7 | DEVUELTA con pieza **DAÑADA** → cobro de **daño** (`valorDano`) → **CERRADA** reteniendo parte del depósito | daño + retención de depósito |
| R8 | DEVUELTA con pieza **PERDIDA** → cobro de **reposición** (`valorReposicion`) | pérdida/reposición |

### 9.2 Ventas (Tienda 1 ~6 · Tienda 2 ~5)
| # | Estado | Qué prueba |
|---|---|---|
| V1–V4 | **CONFIRMADA** | ventas normales; métodos **efectivo / tarjeta / mixto**; una con **descuento** (`VENTAS_DESCUENTO`) |
| V5 | **PARCIALMENTE_DEVUELTA** | devolución de parte de las unidades |
| V6 | **DEVUELTA** | devolución total (reintegra stock y plata) |

### 9.3 Pedidos/Carritos
| # | Estado | Qué prueba |
|---|---|---|
| C1 | **PENDIENTE** (carrito abierto de renta) | carrito abandonado del cliente de marketplace |
| C2 | **CONFIRMADO** | pedido que se convirtió en renta/venta |

### 9.4 Reembolsos
1 **PENDIENTE** + 1 **APROBADA** + 1 **RECHAZADA** (con motivo). Prueba `REEMBOLSOS_APROBAR/RECHAZAR` y el
recorte de permisos (Carlos no puede devolver ventas; Ana sí aprueba).

### 9.5 Caja
Por sucursal: 1 **turno CERRADO** en días pasados (con arqueo) + 1 **turno ABIERTO hoy** con **movimientos**
(ingresos por ventas/rentas, un egreso de gasto). Alimenta el arqueo y los ingresos por método.

### 9.6 Otros
- **Pagos:** anticipos, saldos, depósitos y recargos que generan las rentas/ventas de arriba.
- **Notificaciones:** aviso de renta próxima a vencer (R2), de renta vencida (R3) y de stock bajo (Q07, B06).
- **Favoritos:** 3–4 disfraces marcados por clientes de marketplace (CL4, CL8).

---

## 10. Backdating (SQL) — serie temporal

Las APIs sellan fecha "ahora". Tras crear las operaciones, un **UPDATE** reparte fechas de ventas/rentas/
pagos/turnos en los **últimos ~30 días**, para que **Reportes → ingresos por día** muestre serie y los
rankings tengan sentido. Único paso de SQL sobre datos ya válidos.

---

## 11. Mecanismo de inserción

**Regla:** el catálogo/inventario/disfraces/clientes/operaciones va por **API real** (respeta invariantes,
tablas puente y de paso prueba los endpoints). Lo que va por **SQL** (identidad y ajustes que la API no
expone):
1. **Empleados plantados** (Tienda 1 y 2): `usuario` + `membresia` ACTIVA + `usuario_sucursal` +
   `permiso_empleado` (overrides de §4.1). Password **bcrypt**.
2. **Dueño de la Tienda 2** (empresa ACTIVA + usuario DUEÑO bcrypt + config + membresía): no hay API para el
   primer dueño de una empresa.
3. **Backdating** de fechas (L10).

Flujo del script (idempotente, repetible → "vaciar + re-seed" en 1 comando):
```
SQL:      crear Empresa 2 + Dueño 2 (bcrypt) · plantar empleados de ambas tiendas + permisos
Tienda 1: login dueño(bootstrap) → config → sucursales → clientes →
          catálogo → prendas+stock → disfraces → operaciones (por Carlos/Ana/Beto)
Tienda 2: login dueño2 → config → sucursal → clientes →
          catálogo (DISTINTO) → prendas+stock → disfraces → operaciones (por Sofía)
SQL:      backdating de fechas
```
> Nota: las operaciones se registran con el token del empleado que corresponde (p. ej. las ventas de Carlos)
> para que **Reportes → ventas por empleado** tenga datos reales.

---

## 12. Volúmenes

| Entidad | Tienda 1 | Tienda 2 | Total |
|---|---:|---:|---:|
| Sucursales | 2 | 1 | 3 |
| Usuarios de trabajo | 4 | 2 | 6 |
| Clientes | 5 | 3 | 8 |
| Categorías de prenda | 11 | 7 | 18 |
| Tipos de etiqueta / valores | 6 / ~40 | 6 / ~30 | 12 / ~70 |
| Prendas | 15 | 12 | 27 |
| Grupos de stock (variantes) | ~35 | ~18 | ~53 |
| Categorías de disfraz | 5 | 4 | 9 |
| Disfraces | 8 | 6 | 14 |
| Ventas / Rentas | 6 / 8 | 5 / 5 | 11 / 13 |

---

## 13. Fotos reales (fuente definida)

**Fuente: Pexels** (banco gratis, uso comercial, sin atribución obligatoria). Como `fotoUrl` es una URL que
el front carga con su image loader, apuntamos **directo al CDN de Pexels** (`images.pexels.com/...`) →
**una foto real por prenda y por disfraz, SIN necesidad de S3** ni de configurar el almacén.

Mecánica (fase posterior al seed de datos):
1. Por cada ítem, una búsqueda por palabra clave en inglés (p. ej. *"witch costume"*, *"vampire cape"*,
   *"superhero costume"*, *"princess dress child"*).
2. Se toma el mejor resultado y se fija `fotoUrl` = su URL de `images.pexels.com` (se puede sufijar
   `?auto=compress&w=800` para pedir un tamaño razonable).
3. Personajes con licencia (Batman/Spiderman) → los bancos evitan marcas; se usa una foto **genérica**
   temática ("superhero costume"). Aceptable para demo.

**Probado que funciona** (ejemplos reales de *"vampire costume"*):
`https://images.pexels.com/photos/14395498/pexels-photo-14395498.jpeg` ·
`https://images.pexels.com/photos/15124243/pexels-photo-15124243.jpeg`

> Alternativas equivalentes por si Pexels no tiene buen match para algún ítem: **Unsplash** (permite/pide
> hotlinking, alta calidad) y **Pixabay** (permisivo, permite cachear). Se puede mezclar según el ítem.

---

## 14. A confirmar antes de generar
1. ¿Nombres de tiendas OK (**Fiesta & Fantasía** / **El Baúl del Disfraz**) o preferís otros?
2. ¿Las **dos mentalidades** (Ocasión+Temática vs Personaje+Licencia+Línea) te representan bien la idea de
   catálogos distintos, o querés afinar las dimensiones?
3. ¿Volúmenes ok o querés más operaciones para reportes?
4. ¿Mecanismo **API + SQL puntual** (dueño 2 + backdating) ok?
