// ============================================================================
// SEED 03 — Operaciones (rentas/ventas/devoluciones/multas/reembolsos/caja/pagos)
// Cubre TODOS los estados. NO idempotente: correr sobre operaciones limpias
// (usar seed/00_reset_catalogo.sql + 02_catalogo.mjs antes).
// Uso:  node seed/03_operaciones.mjs
// ============================================================================
const BASE = process.env.BASE || 'https://just-upliftment-production-cb1f.up.railway.app';

async function api(method, path, token, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json; try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return { status: res.status, ok: res.ok, json, text };
}
const login = async e => (await api('POST', '/api/v1/auth/login', null, { email: e, password: 'Passw0rd!' })).json.accessToken;
const asList = x => Array.isArray(x) ? x : (x?.contenido || []);
const d = n => new Date(Date.now() + n * 86400000).toISOString().slice(0, 10);
const find = (arr, sub) => arr.find(x => (x.nombre || '').toLowerCase().includes(sub.toLowerCase()));
let OK = 0, FAIL = 0;
async function step(label, fn) {
  try { const r = await fn(); if (r && r.ok === false) { FAIL++; console.log(`  ✗ ${label} [${r.status}] ${(r.text||'').slice(0,160)}`); return null; } OK++; console.log(`  ✓ ${label}`); return r?.json ?? r; }
  catch (e) { FAIL++; console.log(`  ✗ ${label} ${e.message}`); return null; }
}

// Registra una renta y devuelve su id (o null)
async function crearRenta(t, sucId, cliId, prId, precio, retiro, dev, deposito) {
  const r = await api('POST', '/api/v1/rentas', t, {
    sucursalId: sucId, clienteId: cliId, fechaRetiro: retiro, fechaDevolucion: dev,
    deposito, lineas: [{ prendaId: prId, cantidad: 1, precioPorDia: precio }],
  });
  if (!r.ok) { FAIL++; console.log(`  ✗ crearRenta [${r.status}] ${r.text.slice(0,160)}`); return null; }
  OK++; return r.json.id;
}
async function crearVenta(t, sucId, cliId, lineas, descuento) {
  const r = await api('POST', '/api/v1/ventas', t, { sucursalId: sucId, clienteId: cliId, descuento: descuento || null, lineas });
  if (!r.ok) { FAIL++; console.log(`  ✗ crearVenta [${r.status}] ${r.text.slice(0,160)}`); return null; }
  OK++; return r.json.id;
}

// Flujo de reembolso: venta pagada -> (si aprobar: devuelta) -> solicitar -> decisión
async function reembolsoFlujo(dueno, mostrador, sid, cliId, prendaId, precio, decision) {
  const v = await crearVenta(mostrador, sid, cliId, [{ prendaId, cantidad: 1, precioUnitario: precio }]);
  if (!v) return;
  const pg = await api('POST', '/api/v1/pagos', dueno, { sucursalId: sid, tipoConcepto: 'VENTA', conceptoId: v, monto: precio, tipoPago: 'COBRO', metodo: 'EFECTIVO' });
  if (!pg.ok) { FAIL++; console.log(`  ✗ pago reembolso [${pg.status}] ${pg.text.slice(0,120)}`); return; }
  OK++;
  if (decision === 'aprobar') await step('venta devuelta (previo a reembolso)', () => api('POST', `/api/v1/ventas/${v}/devolver`, dueno, { lineas: [{ prendaId, cantidad: 1 }] }));
  const rb = await api('POST', '/api/v1/reembolsos', dueno, { tipoConcepto: 'VENTA', conceptoId: v, monto: precio, motivo: decision === 'rechazar' ? 'Reclamo fuera de política' : 'Devolución del cliente' });
  if (!rb.ok) { FAIL++; console.log(`  ✗ solicitar reembolso [${rb.status}] ${rb.text.slice(0,140)}`); return; }
  OK++;
  if (decision === 'aprobar') await step('reembolso APROBADO', () => api('POST', `/api/v1/reembolsos/${rb.json.id}/aprobar`, dueno, { motivo: 'Aprobado por el dueño' }));
  else if (decision === 'rechazar') await step('reembolso RECHAZADO', () => api('POST', `/api/v1/reembolsos/${rb.json.id}/rechazar`, dueno, { motivo: 'Fuera de política' }));
  else console.log('  ✓ reembolso PENDIENTE');
}

async function operarTienda(nombre, duenoEmail, mostradorEmail, sucNombre) {
  console.log(`\n===== OPERACIONES · ${nombre} =====`);
  const dueno = await login(duenoEmail);
  const mostrador = await login(mostradorEmail);
  const me = await api('GET', '/api/v1/auth/me', dueno);
  const empresaId = me.json.empresaId;
  const suc = asList((await api('GET', `/api/v1/empresas/${empresaId}/sucursales`, dueno)).json).find(s => s.nombre.includes(sucNombre));
  const clientes = asList((await api('GET', '/api/v1/clientes?tamano=500', dueno)).json).filter(c => !c.archivada && !c.enListaNegra);
  const prendas = asList((await api('GET', '/api/v1/prendas?tamano=500', dueno)).json);
  const sid = suc.id;
  const cli = i => clientes[i % clientes.length].id;
  const P = sub => find(prendas, sub)?.id;
  return { dueno, mostrador, empresaId, sid, cli, P, clientes, prendas };
}

// ---------------- TIENDA 1 ----------------
async function tienda1() {
  const ctx = await operarTienda('Fiesta & Fantasía', 'dueno@costumi.co', 'carlos@ff.ec', 'Centro');
  const { dueno, mostrador, sid, cli, P } = ctx;

  // --- RENTAS (creadas por Carlos = mostrador) ---
  console.log('-- rentas --');
  // R1 RESERVADA
  await crearRenta(mostrador, sid, cli(0), P('Capa de vampiro'), 6, d(2), d(4), 20);
  // R2 ACTIVA (en curso)
  const r2 = await crearRenta(mostrador, sid, cli(1), P('Vestido victoriano'), 15, d(0), d(3), 25);
  if (r2) await step('R2 entregar (ACTIVA)', () => api('POST', `/api/v1/rentas/${r2}/entregar`, mostrador));
  // R3 ACTIVA VENCIDA
  const r3 = await crearRenta(mostrador, sid, cli(2), P('Traje torso de héroe'), 7, d(-6), d(-1), 15);
  if (r3) await step('R3 entregar (vencida)', () => api('POST', `/api/v1/rentas/${r3}/entregar`, mostrador));
  // R4 CANCELADA
  const r4 = await crearRenta(mostrador, sid, cli(0), P('Sombrero de bruja'), 3, d(5), d(7), 10);
  if (r4) await step('R4 cancelar', () => api('POST', `/api/v1/rentas/${r4}/cancelar`, dueno));
  // R5 feliz -> DEVUELTA -> CERRADA (sin multa: dev futuro)
  const r5 = await crearRenta(mostrador, sid, cli(1), P('Frac'), 9, d(-3), d(1), 20);
  if (r5) { await step('R5 entregar', () => api('POST', `/api/v1/rentas/${r5}/entregar`, mostrador));
            await step('R5 devolver', () => api('POST', `/api/v1/rentas/${r5}/devolver`, dueno));
            await step('R5 cerrar', () => api('POST', `/api/v1/rentas/${r5}/cerrar`, dueno)); }
  // R6 con MULTA (dev pasado -> retraso)
  const r6 = await crearRenta(mostrador, sid, cli(2), P('Capa de héroe'), 4, d(-5), d(-2), 15);
  if (r6) { await step('R6 entregar', () => api('POST', `/api/v1/rentas/${r6}/entregar`, mostrador));
            await step('R6 devolver (multa)', () => api('POST', `/api/v1/rentas/${r6}/devolver`, dueno));
            await step('R6 cerrar', () => api('POST', `/api/v1/rentas/${r6}/cerrar`, dueno)); }
  // R7 con DAÑO (devolución detallada)
  const r7 = await crearRenta(mostrador, sid, cli(0), P('Mameluco animal'), 6, d(-4), d(-1), 20);
  if (r7) { await step('R7 entregar', () => api('POST', `/api/v1/rentas/${r7}/entregar`, mostrador));
            await step('R7 devolución con daño', () => api('POST', '/api/v1/devoluciones', dueno, {
              rentaId: r7, deposito: 20, cargoPorDanos: 8, cargoPorRetraso: 2, fechaDevolucionReal: d(0),
              piezas: [{ prendaId: P('Mameluco animal'), descripcion: 'Mancha y descosido', llego: true, estado: 'DANADA', perdidaCobrada: false }] }));
            await step('R7 cerrar', () => api('POST', `/api/v1/rentas/${r7}/cerrar`, dueno)); }
  // R8 con PÉRDIDA
  const r8 = await crearRenta(mostrador, sid, cli(1), P('Vestido de princesa'), 6, d(-4), d(-1), 20);
  if (r8) { await step('R8 entregar', () => api('POST', `/api/v1/rentas/${r8}/entregar`, mostrador));
            await step('R8 devolución con pérdida', () => api('POST', '/api/v1/devoluciones', dueno, {
              rentaId: r8, deposito: 20, cargoPorDanos: 0, cargoPorRetraso: 0, fechaDevolucionReal: d(0),
              piezas: [{ prendaId: P('Vestido de princesa'), descripcion: 'No devuelta', llego: false, estado: 'PERDIDA', perdidaCobrada: true }] }));
            await step('R8 cerrar', () => api('POST', `/api/v1/rentas/${r8}/cerrar`, dueno)); }

  // --- VENTAS (creadas por Carlos) ---
  console.log('-- ventas --');
  const v1 = await crearVenta(mostrador, sid, cli(0), [{ prendaId: P('Antifaz'), cantidad: 1, precioUnitario: 4 }]);
  const v2 = await crearVenta(mostrador, sid, cli(1), [{ prendaId: P('Corona'), cantidad: 1, precioUnitario: 5 }]);
  const v4 = await crearVenta(mostrador, sid, cli(2), [{ prendaId: P('Máscara de calavera'), cantidad: 2, precioUnitario: 4 }], 2); // con descuento
  const v5 = await crearVenta(mostrador, sid, cli(0), [{ prendaId: P('Máscara de calavera'), cantidad: 2, precioUnitario: 4 }]);
  if (v5) await step('V5 devolución PARCIAL', () => api('POST', `/api/v1/ventas/${v5}/devolver`, dueno, { lineas: [{ prendaId: P('Máscara de calavera'), cantidad: 1 }] }));
  const v6 = await crearVenta(mostrador, sid, cli(1), [{ prendaId: P('Varita'), cantidad: 1, precioUnitario: 3 }]);
  if (v6) await step('V6 devolución TOTAL', () => api('POST', `/api/v1/ventas/${v6}/devolver`, dueno, { lineas: [{ prendaId: P('Varita'), cantidad: 1 }] }));

  // --- PAGOS ---
  console.log('-- pagos --');
  if (v1) await step('pago venta v1 efectivo', () => api('POST', '/api/v1/pagos', dueno, { sucursalId: sid, tipoConcepto: 'VENTA', conceptoId: v1, monto: 4, tipoPago: 'COBRO', metodo: 'EFECTIVO' }));
  if (v2) await step('pago venta v2 tarjeta', () => api('POST', '/api/v1/pagos', dueno, { sucursalId: sid, tipoConcepto: 'VENTA', conceptoId: v2, monto: 5, tipoPago: 'COBRO', metodo: 'TARJETA' }));
  if (r2) await step('pago renta r2 mixto', () => api('POST', '/api/v1/pagos/mixto', dueno, { sucursalId: sid, tipoConcepto: 'RENTA', conceptoId: r2, porciones: [{ metodo: 'EFECTIVO', monto: 25 }, { metodo: 'TARJETA', monto: 20 }] }));

  // --- REEMBOLSOS ---
  console.log('-- reembolsos --');
  await reembolsoFlujo(dueno, mostrador, sid, cli(0), P('Antifaz'), 4, 'pendiente');
  await reembolsoFlujo(dueno, mostrador, sid, cli(1), P('Corona'), 5, 'aprobar');
  await reembolsoFlujo(dueno, mostrador, sid, cli(2), P('Máscara de calavera'), 4, 'rechazar');

  // --- CAJA ---
  console.log('-- caja --');
  const tc = await api('POST', '/api/v1/caja/turnos', dueno, { sucursalId: sid, fondoInicial: 50 });
  if (tc.ok) { OK++; const turno = tc.json.id;
    await step('mov INGRESO', () => api('POST', `/api/v1/caja/turnos/${turno}/movimientos`, dueno, { tipo: 'INGRESO', concepto: 'Venta mostrador', monto: 20, metodo: 'EFECTIVO' }));
    await step('mov EGRESO', () => api('POST', `/api/v1/caja/turnos/${turno}/movimientos`, dueno, { tipo: 'EGRESO', concepto: 'Compra de fundas', monto: 8, metodo: 'EFECTIVO' }));
    await step('cerrar turno', () => api('POST', `/api/v1/caja/turnos/${turno}/cerrar`, dueno, { efectivoContado: 62 }));
  } else { FAIL++; console.log('  ✗ abrir turno', tc.status, tc.text.slice(0,120)); }
  // turno abierto HOY
  const tc2 = await api('POST', '/api/v1/caja/turnos', dueno, { sucursalId: sid, fondoInicial: 40 });
  if (tc2.ok) { OK++; await step('mov turno abierto', () => api('POST', `/api/v1/caja/turnos/${tc2.json.id}/movimientos`, dueno, { tipo: 'INGRESO', concepto: 'Renta del día', monto: 15, metodo: 'TARJETA' })); }
  else { FAIL++; console.log('  ✗ abrir turno2', tc2.status, tc2.text.slice(0,120)); }
}

// ---------------- TIENDA 2 ----------------
async function tienda2() {
  const ctx = await operarTienda('El Baúl del Disfraz', 'dueno@baul.ec', 'sofia@baul.ec', 'Cuenca');
  const { dueno, mostrador, sid, cli, P } = ctx;

  console.log('-- rentas --');
  await crearRenta(mostrador, sid, cli(0), P('Disfraz Batman'), 20, d(2), d(4), 40);           // RESERVADA
  const r2 = await crearRenta(mostrador, sid, cli(1), P('Disfraz de bruja'), 12, d(0), d(3), 30); // ACTIVA
  if (r2) await step('R2 entregar', () => api('POST', `/api/v1/rentas/${r2}/entregar`, mostrador));
  const r3 = await crearRenta(mostrador, sid, cli(2), P('Disfraz Superman'), 20, d(-6), d(-1), 40); // vencida
  if (r3) await step('R3 entregar (vencida)', () => api('POST', `/api/v1/rentas/${r3}/entregar`, mostrador));
  const r5 = await crearRenta(mostrador, sid, cli(0), P('Disfraz de Catrina'), 15, d(-3), d(1), 30); // feliz
  if (r5) { await step('R5 entregar', () => api('POST', `/api/v1/rentas/${r5}/entregar`, mostrador));
            await step('R5 devolver', () => api('POST', `/api/v1/rentas/${r5}/devolver`, dueno));
            await step('R5 cerrar', () => api('POST', `/api/v1/rentas/${r5}/cerrar`, dueno)); }
  const r6 = await crearRenta(mostrador, sid, cli(1), P('Camisa de pirata'), 5, d(-5), d(-2), 10); // multa
  if (r6) { await step('R6 entregar', () => api('POST', `/api/v1/rentas/${r6}/entregar`, mostrador));
            await step('R6 devolver (multa)', () => api('POST', `/api/v1/rentas/${r6}/devolver`, dueno));
            await step('R6 cerrar', () => api('POST', `/api/v1/rentas/${r6}/cerrar`, dueno)); }

  console.log('-- ventas --');
  const v1 = await crearVenta(mostrador, sid, cli(0), [{ prendaId: P('Máscara de esqueleto'), cantidad: 1, precioUnitario: 5 }]);
  const v2 = await crearVenta(mostrador, sid, cli(1), [{ prendaId: P('Peluca de payaso'), cantidad: 1, precioUnitario: 8 }]);
  const v3 = await crearVenta(mostrador, sid, cli(2), [{ prendaId: P('Disfraz Spiderman'), cantidad: 1, precioUnitario: 60 }], 5); // descuento
  if (v3) await step('V devolución TOTAL', () => api('POST', `/api/v1/ventas/${v3}/devolver`, dueno, { lineas: [{ prendaId: P('Disfraz Spiderman'), cantidad: 1 }] }));

  console.log('-- pagos / reembolsos / caja --');
  if (v1) await step('pago v1 efectivo', () => api('POST', '/api/v1/pagos', dueno, { sucursalId: sid, tipoConcepto: 'VENTA', conceptoId: v1, monto: 5, tipoPago: 'COBRO', metodo: 'EFECTIVO' }));
  await reembolsoFlujo(dueno, mostrador, sid, cli(1), P('Peluca de payaso'), 8, 'aprobar');
  const tc = await api('POST', '/api/v1/caja/turnos', dueno, { sucursalId: sid, fondoInicial: 30 });
  if (tc.ok) { OK++; await step('mov ingreso', () => api('POST', `/api/v1/caja/turnos/${tc.json.id}/movimientos`, dueno, { tipo: 'INGRESO', concepto: 'Venta', monto: 5, metodo: 'EFECTIVO' })); }
  else { FAIL++; console.log('  ✗ abrir turno T2', tc.status, tc.text.slice(0,120)); }
}

(async () => {
  await tienda1();
  await tienda2();
  console.log(`\n✅ Operaciones cargadas. OK=${OK} FAIL=${FAIL}`);
})().catch(e => { console.error('\n❌', e.message); process.exit(1); });
