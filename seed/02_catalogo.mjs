// ============================================================================
// SEED 02 — Catálogo + Inventario + Disfraces + Clientes  (por API real)
// Dos tiendas con catálogos DISTINTOS. Idempotente (crea por nombre si falta).
// Uso:  node seed/02_catalogo.mjs
// ============================================================================
const BASE = process.env.BASE || 'https://just-upliftment-production-cb1f.up.railway.app';

// ---------- helpers HTTP ----------
async function api(method, path, token, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json; try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status}: ${text}`);
  return json;
}
async function login(email) {
  const r = await api('POST', '/api/v1/auth/login', null, { email, password: 'Passw0rd!' });
  return r.accessToken;
}
const norm = s => (s || '').trim().toLowerCase();
// desenvuelve respuestas paginadas ({contenido,...}) o arrays directos
const asList = x => Array.isArray(x) ? x : (x?.contenido || x?.content || []);

// ============================================================================
// CATÁLOGOS
// ============================================================================
// tipos: {nombre, defineVariante, seleccionable, aplicaA:[catNombre], valores:[...]}
// prendas: {code, nombre, cat, tipo:RENTA|VENTA|AMBOS, r, v, clase:{Tipo:Valor}, vars:[[talla,color,...stockPorSucursal]]}
//   stockPorSucursal sigue el orden de cfg.sucursales
// disfraces: {nombre, dcat, tipo, general:{r,v}|null, slots:[{n, opc, fija:code | opciones:[code]}]}

const TIENDA1 = {
  email: 'dueno@costumi.co',
  sucursales: ['Centro Histórico', 'Cumbayá'],
  categorias: ['Cabeza y tocados','Máscaras y caretas','Antifaces','Torso','Piernas',
               'Trajes de una pieza','Capas, mantos y alas','Calzado','Guantes','Utilería','Complementos'],
  tipos: [
    { nombre:'Talla', defineVariante:true, seleccionable:true,
      aplicaA:['Torso','Piernas','Trajes de una pieza','Capas, mantos y alas','Calzado'],
      valores:['XS','S','M','L','XL','Niño 4-6','Niño 8-10'] },
    { nombre:'Color', defineVariante:true, seleccionable:true, aplicaA:[],
      valores:['Negro','Rojo','Blanco','Dorado','Morado','Azul','Verde','Rosa','Café','Gris'] },
    { nombre:'Ocasión', defineVariante:false, seleccionable:true, aplicaA:[],
      valores:['Halloween','Carnaval','Fiestas de Quito','Inti Raymi','Año Viejo','Cumpleaños infantil','Teatro/Escolar'] },
    { nombre:'Temática', defineVariante:false, seleccionable:true, aplicaA:[],
      valores:['Terror','Superhéroes','Histórico','Fantasía','Animales','Profesiones'] },
    { nombre:'Público', defineVariante:false, seleccionable:true, aplicaA:[],
      valores:['Adulto','Niño','Unisex'] },
    { nombre:'Material', defineVariante:false, seleccionable:false, aplicaA:[],
      valores:['Poliéster','Satén','Terciopelo','Licra'] },
  ],
  prendas: [
    { code:'Q01', nombre:'Capa de vampiro', cat:'Capas, mantos y alas', tipo:'RENTA', r:6,
      clase:{Temática:'Terror',Ocasión:'Halloween',Público:'Adulto',Material:'Satén'},
      vars:[['M','Negro',4,2],['L','Negro',3,1],['M','Rojo',2,1]] },
    { code:'Q02', nombre:'Sombrero de bruja', cat:'Cabeza y tocados', tipo:'RENTA', r:3,
      clase:{Temática:'Terror',Ocasión:'Halloween',Público:'Adulto'},
      vars:[[null,'Negro',5,2],[null,'Morado',2,1]] },
    { code:'Q03', nombre:'Máscara de calavera', cat:'Máscaras y caretas', tipo:'AMBOS', r:3, v:4,
      clase:{Temática:'Terror',Ocasión:'Halloween',Público:'Unisex'},
      vars:[[null,'Blanco',6,3],[null,'Negro',3,2]] },
    { code:'Q04', nombre:'Camisa victoriana', cat:'Torso', tipo:'AMBOS', r:5, v:25,
      clase:{Temática:'Histórico',Ocasión:'Teatro/Escolar',Público:'Adulto',Material:'Poliéster'},
      vars:[['S','Blanco',3,2],['M','Blanco',4,2],['L','Blanco',3,1]] },
    { code:'Q05', nombre:'Frac / levita', cat:'Torso', tipo:'RENTA', r:9,
      clase:{Temática:'Histórico',Público:'Adulto',Material:'Poliéster'},
      vars:[['M','Negro',3,1],['L','Negro',2,1]] },
    { code:'Q06', nombre:'Sombrero de copa', cat:'Cabeza y tocados', tipo:'RENTA', r:4,
      clase:{Temática:'Histórico',Público:'Adulto'},
      vars:[[null,'Negro',4,2]] },
    { code:'Q07', nombre:'Vestido victoriano', cat:'Trajes de una pieza', tipo:'RENTA', r:15,
      clase:{Temática:'Histórico',Ocasión:'Teatro/Escolar',Público:'Adulto',Material:'Satén'},
      vars:[['S','Dorado',2,1],['M','Dorado',2,1],['M','Morado',1,1]] },
    { code:'Q08', nombre:'Traje torso de héroe', cat:'Torso', tipo:'RENTA', r:7,
      clase:{Temática:'Superhéroes',Público:'Unisex',Material:'Licra'},
      vars:[['S','Rojo',3,1],['M','Rojo',4,2],['M','Azul',3,1]] },
    { code:'Q09', nombre:'Capa de héroe', cat:'Capas, mantos y alas', tipo:'RENTA', r:4,
      clase:{Temática:'Superhéroes',Público:'Unisex',Material:'Satén'},
      vars:[[null,'Rojo',5,2],[null,'Azul',4,2]] },
    { code:'Q10', nombre:'Antifaz', cat:'Antifaces', tipo:'AMBOS', r:2, v:4,
      clase:{Temática:'Superhéroes',Ocasión:'Carnaval',Público:'Unisex'},
      vars:[[null,'Negro',10,5]] },
    { code:'Q11', nombre:'Vestido de princesa', cat:'Trajes de una pieza', tipo:'RENTA', r:6,
      clase:{Temática:'Fantasía',Ocasión:'Cumpleaños infantil',Público:'Niño',Material:'Satén'},
      vars:[['Niño 4-6','Rosa',3,1],['Niño 8-10','Rosa',3,2],['Niño 8-10','Azul',2,1]] },
    { code:'Q12', nombre:'Corona', cat:'Cabeza y tocados', tipo:'AMBOS', r:2, v:5,
      clase:{Temática:'Fantasía',Público:'Unisex'},
      vars:[[null,'Dorado',8,4]] },
    { code:'Q13', nombre:'Alas de hada', cat:'Capas, mantos y alas', tipo:'RENTA', r:3,
      clase:{Temática:'Fantasía',Ocasión:'Cumpleaños infantil',Público:'Niño'},
      vars:[[null,'Blanco',5,2],[null,'Rosa',3,2]] },
    { code:'Q14', nombre:'Mameluco animal', cat:'Trajes de una pieza', tipo:'RENTA', r:6,
      clase:{Temática:'Animales',Público:'Unisex',Material:'Poliéster'},
      vars:[['S','Café',3,1],['M','Café',3,2],['M','Gris',2,1]] },
    { code:'Q15', nombre:'Varita mágica', cat:'Utilería', tipo:'AMBOS', r:2, v:3,
      clase:{Temática:'Fantasía',Público:'Unisex'},
      vars:[[null,null,20,10]] },
  ],
  disfrazCategorias: ['Terror','Superhéroes','Histórico','Infantil','Animales'],
  disfraces: [
    { nombre:'Conde Vampiro', dcat:'Terror', tipo:'RENTA', general:{r:12},
      slots:[{n:'Capa',fija:'Q01'},{n:'Torso',fija:'Q04'},{n:'Máscara',opc:true,fija:'Q03'}] },
    { nombre:'Bruja Clásica', dcat:'Terror', tipo:'RENTA', general:null,
      slots:[{n:'Cabeza',fija:'Q02'},{n:'Traje',fija:'Q07'},{n:'Varita',opc:true,fija:'Q15'}] },
    { nombre:'Superhéroe', dcat:'Superhéroes', tipo:'RENTA', general:null,
      slots:[{n:'Torso',opciones:['Q08']},{n:'Capa',fija:'Q09'},{n:'Antifaz',fija:'Q10'}] },
    { nombre:'Caballero Victoriano', dcat:'Histórico', tipo:'RENTA', general:null,
      slots:[{n:'Torso',fija:'Q05'},{n:'Cabeza',fija:'Q06'}] },
    { nombre:'Dama Victoriana', dcat:'Histórico', tipo:'RENTA', general:{r:22},
      slots:[{n:'Traje',fija:'Q07'},{n:'Corona',opc:true,fija:'Q12'}] },
    { nombre:'Princesa', dcat:'Infantil', tipo:'RENTA', general:{r:10},
      slots:[{n:'Traje',fija:'Q11'},{n:'Corona',fija:'Q12'},{n:'Varita',opc:true,fija:'Q15'},{n:'Alas',opc:true,fija:'Q13'}] },
    { nombre:'Animalito', dcat:'Animales', tipo:'RENTA', general:null,
      slots:[{n:'Cuerpo',fija:'Q14'}] },
    { nombre:'Halloween a tu gusto', dcat:'Terror', tipo:'RENTA', general:null,
      slots:[{n:'Torso',opciones:['Q04','Q08']},{n:'Cabeza',opciones:['Q02','Q06']},{n:'Máscara',opc:true,fija:'Q03'}] },
  ],
  clientes: [
    { nombre:'María Andrade', telefono:'0991112233', documento:'0102030405', direccion:'La Floresta, Quito' },
    { nombre:'Juan Cabrera',  telefono:'0992223344', documento:'0102030406', direccion:'La Carolina, Quito' },
    { nombre:'Lucía Paredes', telefono:'0993334455', documento:'0102030407', direccion:'El Batán, Quito', listaNegra:true },
    { nombre:'Andrés Villacís', telefono:'0994445566', documento:'0102030408', direccion:'Cumbayá', email:'andres.villacis@example.com' },
    { nombre:'Camila Suárez', telefono:'0995556677', documento:'0102030409', direccion:'La Mariscal, Quito', archivar:true },
  ],
};

const TIENDA2 = {
  email: 'dueno@baul.ec',
  sucursales: ['Cuenca Centro'],
  categorias: ['Disfraces completos','Pelucas y cabello','Máscaras','Accesorios de personaje',
               'Calzado y botas','Ropa base','Sombrerería'],
  tipos: [
    { nombre:'Talla', defineVariante:true, seleccionable:true,
      aplicaA:['Disfraces completos','Calzado y botas','Ropa base'],
      valores:['S','M','L','XL','Única','Infantil'] },
    { nombre:'Color', defineVariante:true, seleccionable:true, aplicaA:[],
      valores:['Negro','Rojo','Azul','Blanco','Verde','Morado'] },
    { nombre:'Personaje', defineVariante:false, seleccionable:true, aplicaA:[],
      valores:['Batman','Superman','Spiderman','Bruja','Vampiro','Pirata','Princesa','Catrina','Payaso','Esqueleto'] },
    { nombre:'Licencia', defineVariante:false, seleccionable:false, aplicaA:[],
      valores:['DC','Marvel','Disney','Genérico'] },
    { nombre:'Línea', defineVariante:false, seleccionable:false, aplicaA:[],
      valores:['Premium','Estándar','Económica'] },
    { nombre:'Edad', defineVariante:false, seleccionable:true, aplicaA:[],
      valores:['Adulto','Infantil'] },
  ],
  prendas: [
    { code:'B01', nombre:'Disfraz Batman', cat:'Disfraces completos', tipo:'RENTA', r:20,
      clase:{Personaje:'Batman',Licencia:'DC',Línea:'Premium',Edad:'Adulto'},
      vars:[['M','Negro',3],['L','Negro',2]] },
    { code:'B02', nombre:'Disfraz Superman', cat:'Disfraces completos', tipo:'RENTA', r:20,
      clase:{Personaje:'Superman',Licencia:'DC',Línea:'Premium',Edad:'Adulto'},
      vars:[['M','Azul',2],['L','Azul',2]] },
    { code:'B03', nombre:'Disfraz Spiderman', cat:'Disfraces completos', tipo:'AMBOS', r:18, v:60,
      clase:{Personaje:'Spiderman',Licencia:'Marvel',Línea:'Estándar',Edad:'Adulto'},
      vars:[['M','Rojo',3],['L','Rojo',2]] },
    { code:'B04', nombre:'Disfraz Spiderman niño', cat:'Disfraces completos', tipo:'RENTA', r:10,
      clase:{Personaje:'Spiderman',Licencia:'Marvel',Línea:'Estándar',Edad:'Infantil'},
      vars:[['Infantil','Rojo',4]] },
    { code:'B05', nombre:'Disfraz de bruja', cat:'Disfraces completos', tipo:'RENTA', r:12,
      clase:{Personaje:'Bruja',Licencia:'Genérico',Línea:'Estándar',Edad:'Adulto'},
      vars:[['M','Negro',3],['L','Negro',2]] },
    { code:'B06', nombre:'Disfraz de Catrina', cat:'Disfraces completos', tipo:'RENTA', r:15,
      clase:{Personaje:'Catrina',Licencia:'Genérico',Línea:'Premium',Edad:'Adulto'},
      vars:[['M','Morado',2],['L','Morado',1]] },
    { code:'B07', nombre:'Peluca de payaso', cat:'Pelucas y cabello', tipo:'AMBOS', r:3, v:8,
      clase:{Personaje:'Payaso',Licencia:'Genérico',Línea:'Económica'},
      vars:[[null,'Rojo',6],[null,'Verde',3]] },
    { code:'B08', nombre:'Peluca de princesa', cat:'Pelucas y cabello', tipo:'RENTA', r:4,
      clase:{Personaje:'Princesa',Licencia:'Disney',Línea:'Estándar'},
      vars:[[null,'Blanco',4]] },
    { code:'B09', nombre:'Máscara de esqueleto', cat:'Máscaras', tipo:'VENTA', v:5,
      clase:{Personaje:'Esqueleto',Licencia:'Genérico',Línea:'Económica'},
      vars:[[null,'Blanco',10],[null,'Negro',5]] },
    { code:'B10', nombre:'Botas de pirata', cat:'Calzado y botas', tipo:'RENTA', r:5,
      clase:{Personaje:'Pirata',Licencia:'Genérico',Línea:'Estándar'},
      vars:[['M','Negro',3],['L','Negro',2]] },
    { code:'B11', nombre:'Camisa de pirata', cat:'Ropa base', tipo:'RENTA', r:5,
      clase:{Personaje:'Pirata',Licencia:'Genérico',Línea:'Estándar',Edad:'Adulto'},
      vars:[['M','Blanco',4],['L','Blanco',2]] },
    { code:'B12', nombre:'Sombrero de pirata', cat:'Sombrerería', tipo:'RENTA', r:3,
      clase:{Personaje:'Pirata',Licencia:'Genérico',Línea:'Económica'},
      vars:[[null,'Negro',6]] },
  ],
  disfrazCategorias: ['Superhéroes','Terror','Piratas','Infantil'],
  disfraces: [
    { nombre:'Batman', dcat:'Superhéroes', tipo:'RENTA', general:{r:20}, slots:[{n:'Cuerpo',fija:'B01'}] },
    { nombre:'Superman', dcat:'Superhéroes', tipo:'RENTA', general:{r:20}, slots:[{n:'Cuerpo',fija:'B02'}] },
    { nombre:'Spiderman', dcat:'Superhéroes', tipo:'AMBOS', general:{r:18}, slots:[{n:'Cuerpo',fija:'B03'}] },
    { nombre:'Bruja', dcat:'Terror', tipo:'RENTA', general:null,
      slots:[{n:'Cuerpo',fija:'B05'},{n:'Peluca',opc:true,fija:'B08'}] },
    { nombre:'Catrina', dcat:'Terror', tipo:'RENTA', general:{r:15}, slots:[{n:'Cuerpo',fija:'B06'}] },
    { nombre:'Pirata', dcat:'Piratas', tipo:'RENTA', general:null,
      slots:[{n:'Camisa',fija:'B11'},{n:'Sombrero',fija:'B12'},{n:'Botas',opc:true,fija:'B10'}] },
  ],
  clientes: [
    { nombre:'Diego Ortega',   telefono:'0987651122', documento:'0103040501', direccion:'El Vergel, Cuenca' },
    { nombre:'Paola Zamora',   telefono:'0987652233', documento:'0103040502', direccion:'El Centro, Cuenca' },
    { nombre:'Marco Vintimilla', telefono:'0987653344', documento:'0103040503', direccion:'Totoracocha, Cuenca', email:'marco.vintimilla@example.com' },
  ],
};

// ============================================================================
// LÓGICA DE CARGA
// ============================================================================
async function seedTienda(cfg) {
  const t = await login(cfg.email);
  const me = await api('GET', '/api/v1/auth/me', t);
  const empresaId = me.empresaId;
  console.log(`\n===== ${me.email} · empresa ${empresaId} =====`);

  // sucursales (ya existen por SQL) name->id
  const sucList = asList(await api('GET', `/api/v1/empresas/${empresaId}/sucursales`, t));
  const sucId = {};
  for (const s of sucList) sucId[norm(s.nombre)] = s.id;

  // ---- categorías de prenda ----
  const catExist = asList(await api('GET', '/api/v1/categorias', t));
  const catId = {}; for (const c of catExist) catId[norm(c.nombre)] = c.id;
  for (const nombre of cfg.categorias) {
    if (catId[norm(nombre)]) continue;
    const c = await api('POST', '/api/v1/categorias', t, { nombre });
    catId[norm(nombre)] = c.id;
  }
  console.log(`categorías: ${Object.keys(catId).length}`);

  // ---- tipos de etiqueta + valores ----
  const tipoExist = asList(await api('GET', '/api/v1/tipos-etiqueta', t));
  const tipoId = {}; for (const tp of tipoExist) tipoId[norm(tp.nombre)] = tp.id;
  const valorId = {}; // key `${tipoNombre}:${valor}` -> id
  for (const tp of cfg.tipos) {
    let id = tipoId[norm(tp.nombre)];
    if (!id) {
      const created = await api('POST', '/api/v1/tipos-etiqueta', t, {
        nombre: tp.nombre,
        defineVariante: tp.defineVariante,
        seleccionablePorCliente: tp.seleccionable,
        categoriasQueAplica: (tp.aplicaA || []).map(n => catId[norm(n)]).filter(Boolean),
      });
      id = created.id; tipoId[norm(tp.nombre)] = id;
    }
    // valores existentes
    const vExist = asList(await api('GET', `/api/v1/tipos-etiqueta/${id}/valores`, t));
    for (const v of vExist) valorId[`${norm(tp.nombre)}:${norm(v.valor)}`] = v.id;
    for (const valor of tp.valores) {
      const k = `${norm(tp.nombre)}:${norm(valor)}`;
      if (valorId[k]) continue;
      const created = await api('POST', `/api/v1/tipos-etiqueta/${id}/valores`, t, { valor });
      valorId[k] = created.id;
    }
  }
  console.log(`tipos etiqueta: ${Object.keys(tipoId).length} · valores: ${Object.keys(valorId).length}`);

  const etq = (tipoNombre, valor) => ({
    tipoEtiquetaId: tipoId[norm(tipoNombre)],
    valorEtiquetaId: valorId[`${norm(tipoNombre)}:${norm(valor)}`],
  });

  // ---- prendas + grupos de stock ----
  const prendaExist = asList(await api('GET', '/api/v1/prendas?tamano=500', t));
  const prendaByName = {}; for (const p of prendaExist) prendaByName[norm(p.nombre)] = p.id;
  const prendaId = {}; // code -> id
  for (const p of cfg.prendas) {
    let id = prendaByName[norm(p.nombre)];
    if (!id) {
      const etiquetas = Object.entries(p.clase || {}).map(([tn, v]) => etq(tn, v));
      const created = await api('POST', '/api/v1/prendas', t, {
        categoriaId: catId[norm(p.cat)],
        nombre: p.nombre,
        tipoArticulo: p.tipo,
        precioRenta: p.tipo === 'VENTA' ? null : p.r,
        precioVenta: p.tipo === 'RENTA' ? null : p.v,
        etiquetas,
      });
      id = created.id;
    }
    prendaId[p.code] = id;
    // grupos de stock (variantes) — solo si la prenda no tiene ya grupos
    const gExist = asList(await api('GET', `/api/v1/prendas/${id}/grupos-stock`, t));
    if (gExist.length === 0) {
      for (const variant of p.vars) {
        const talla = variant[0], color = variant[1];
        const stocks = variant.slice(2); // por sucursal, en orden cfg.sucursales
        const combinacion = [];
        if (talla) combinacion.push(etq('Talla', talla));
        if (color) combinacion.push(etq('Color', color));
        for (let i = 0; i < cfg.sucursales.length; i++) {
          const qty = stocks[i] || 0;
          if (qty <= 0) continue;
          await api('POST', `/api/v1/prendas/${id}/grupos-stock`, t, {
            sucursalId: sucId[norm(cfg.sucursales[i])],
            combinacion,
            cantidadInicial: qty,
          });
        }
      }
    }
  }
  console.log(`prendas: ${Object.keys(prendaId).length}`);

  // ---- categorías de disfraz ----
  const dcatExist = asList(await api('GET', '/api/v1/disfraces/categorias', t));
  const dcatId = {}; for (const c of dcatExist) dcatId[norm(c.nombre)] = c.id;
  for (const nombre of cfg.disfrazCategorias) {
    if (dcatId[norm(nombre)]) continue;
    const c = await api('POST', '/api/v1/disfraces/categorias', t, { nombre });
    dcatId[norm(nombre)] = c.id;
  }

  // ---- disfraces ----
  const disfExist = asList(await api('GET', '/api/v1/disfraces?tamano=500', t));
  const disfByName = {}; for (const d of disfExist) disfByName[norm(d.nombre)] = d.id;
  let nDisf = 0;
  for (const d of cfg.disfraces) {
    if (disfByName[norm(d.nombre)]) { nDisf++; continue; }
    const slots = d.slots.map((s, idx) => {
      const base = { orden: idx + 1, nombre: s.n, opcional: !!s.opc };
      if (s.fija) return { ...base, ejePrenda: 'FIJA', prendaFijaId: prendaId[s.fija] };
      return { ...base, ejePrenda: 'PERSONALIZABLE', prendasOpcion: s.opciones.map(c => prendaId[c]) };
    });
    await api('POST', '/api/v1/disfraces', t, {
      nombre: d.nombre,
      categoriaId: dcatId[norm(d.dcat)],
      precioRentaGeneral: d.general?.r ?? null,
      precioVentaGeneral: d.general?.v ?? null,
      tipo: d.tipo,
      slots,
    });
    nDisf++;
  }
  console.log(`disfraces: ${nDisf}`);

  // ---- clientes ----
  const cliExist = asList(await api('GET', '/api/v1/clientes?tamano=500', t));
  const cliByName = {}; for (const c of cliExist) cliByName[norm(c.nombre)] = c.id;
  let nCli = 0;
  for (const c of cfg.clientes) {
    let id = cliByName[norm(c.nombre)];
    if (!id) {
      const created = await api('POST', '/api/v1/clientes', t, {
        nombre: c.nombre, telefono: c.telefono, email: c.email || null,
        documento: c.documento, direccion: c.direccion,
      });
      id = created.id;
    }
    if (c.listaNegra) await api('POST', `/api/v1/clientes/${id}/lista-negra`, t, { enListaNegra: true }).catch(()=>{});
    if (c.archivar)  await api('POST', `/api/v1/clientes/${id}/archivar`, t).catch(()=>{});
    nCli++;
  }
  console.log(`clientes: ${nCli}`);
}

(async () => {
  await seedTienda(TIENDA1);
  await seedTienda(TIENDA2);
  console.log('\n✅ Catálogo + inventario + disfraces + clientes cargado.');
})().catch(e => { console.error('\n❌', e.message); process.exit(1); });
