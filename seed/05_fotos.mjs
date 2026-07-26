// ============================================================================
// SEED 05 — Fotos reales a S3 (vía API). Requiere S3 configurado en Railway.
// Descarga de Pexels (prendas/disfraces/sucursales/empresa) + avatares (usuarios)
// y los sube por los endpoints de foto -> el backend los guarda en TU bucket S3.
// Uso:  node seed/05_fotos.mjs
// ============================================================================
const BASE = process.env.BASE || 'https://just-upliftment-production-cb1f.up.railway.app';
const px = id => `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&w=800`;

// Bancos de imágenes por concepto (Pexels, CDN público)
const C = {
  vampire:   [px(14395498), px(15124243), px(14395496)],
  witch:     [px(5415422),  px(5422768),  px(5415412)],
  superhero: [px(6800567),  px(701771),   px(6800567)],
  spiderman: [px(38691366)],
  pirate:    [px(7000035),  px(18333468), px(16332770)],
  princess:  [px(6800538),  px(28761334), px(6800544)],
  clown:     [px(36531425), px(30906950), px(14726493)],
  catrina:   [px(34464583), px(34466391), px(34095285)],
  skeleton:  [px(29243708), px(27629719), px(29721195)],
  mask:      [px(8405047),  px(8405034),  px(8404539)],
  crown:     [px(10541687), px(9468016),  px(32937223)],
  victorian: [px(34477498), px(20866776), px(18454811)],
  shop:      [px(38175319), px(19001603), px(32388451)],
};
const pick = (c, i) => C[c][i % C[c].length];

async function api(method, path, token, body) {
  const r = await fetch(BASE + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text(); let j; try { j = t ? JSON.parse(t) : null; } catch { j = t; }
  return { status: r.status, ok: r.ok, json: j, text: t };
}
const login = async e => (await api('POST', '/api/v1/auth/login', null, { email: e, password: 'Passw0rd!' })).json.accessToken;
const asList = x => Array.isArray(x) ? x : (x?.contenido || []);
let OK = 0, FAIL = 0;

async function subir(token, path, imageUrl, label) {
  try {
    const img = await fetch(imageUrl);
    if (!img.ok) throw new Error('descarga ' + img.status);
    const buf = Buffer.from(await img.arrayBuffer());
    const ct = img.headers.get('content-type') || 'image/jpeg';
    const fd = new FormData();
    fd.append('archivo', new Blob([buf], { type: ct }), 'foto' + (ct.includes('png') ? '.png' : '.jpg'));
    const r = await fetch(BASE + path, { method: 'POST', headers: { Authorization: 'Bearer ' + token }, body: fd });
    if (!r.ok) { FAIL++; console.log(`  ✗ ${label} [${r.status}] ${(await r.text()).slice(0,120)}`); return; }
    OK++; console.log(`  ✓ ${label} (${Math.round(buf.length/1024)}KB)`);
  } catch (e) { FAIL++; console.log(`  ✗ ${label} ${e.message}`); }
}

async function fotosTienda(nombre, email, mapPrenda, mapDisfraz, mapSuc, logoC, portadaC) {
  console.log(`\n===== FOTOS · ${nombre} =====`);
  const t = await login(email);
  const empresaId = (await api('GET', '/api/v1/auth/me', t)).json.empresaId;
  const prendas = asList((await api('GET', '/api/v1/prendas?tamano=500', t)).json);
  const disfraces = asList((await api('GET', '/api/v1/disfraces?tamano=500', t)).json);
  const sucs = asList((await api('GET', `/api/v1/empresas/${empresaId}/sucursales`, t)).json);

  console.log('-- prendas --');
  for (const [sub, [c, i]] of Object.entries(mapPrenda)) {
    const p = prendas.find(x => x.nombre.toLowerCase().includes(sub.toLowerCase()));
    if (p) await subir(t, `/api/v1/prendas/${p.id}/foto`, pick(c, i), p.nombre);
  }
  console.log('-- disfraces --');
  for (const [name, [c, i]] of Object.entries(mapDisfraz)) {
    const dsf = disfraces.find(x => x.nombre.toLowerCase() === name.toLowerCase());
    if (dsf) await subir(t, `/api/v1/disfraces/${dsf.id}/foto`, pick(c, i), dsf.nombre);
  }
  console.log('-- sucursales --');
  for (const [sub, [c, i]] of Object.entries(mapSuc)) {
    const s = sucs.find(x => x.nombre.toLowerCase().includes(sub.toLowerCase()));
    if (s) await subir(t, `/api/v1/empresas/${empresaId}/sucursales/${s.id}/foto`, pick(c, i), s.nombre);
  }
  console.log('-- empresa logo/portada --');
  await subir(t, '/api/v1/empresas/mia/logo', pick(logoC[0], logoC[1]), 'logo');
  await subir(t, '/api/v1/empresas/mia/portada', pick(portadaC[0], portadaC[1]), 'portada');
}

async function fotosUsuarios() {
  console.log('\n===== FOTOS · usuarios (avatares) =====');
  const users = {
    'dueno@costumi.co': 'Dueño FF', 'ana@ff.ec': 'Ana Torres', 'carlos@ff.ec': 'Carlos Andrade',
    'beto@ff.ec': 'Beto Cruz', 'dueno@baul.ec': 'Dueño Baul', 'sofia@baul.ec': 'Sofia Leon',
    'cliente@costumi.co': 'Cliente Demo', 'owner@costumi.co': 'Super Admin',
  };
  for (const [email, name] of Object.entries(users)) {
    const t = await login(email);
    const avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&size=256&background=random&color=fff&format=png`;
    await subir(t, '/api/v1/perfil/foto', avatar, email);
  }
}

(async () => {
  await fotosTienda('Fiesta & Fantasía', 'dueno@costumi.co',
    { // prendas (substring -> [concepto, idx])
      'Capa de vampiro':['vampire',0], 'Sombrero de bruja':['witch',0], 'Máscara de calavera':['skeleton',0],
      'Camisa victoriana':['victorian',0], 'Frac':['victorian',1], 'Sombrero de copa':['victorian',2],
      'Vestido victoriano':['victorian',0], 'Traje torso de héroe':['superhero',0], 'Capa de héroe':['superhero',1],
      'Antifaz':['mask',0], 'Vestido de princesa':['princess',0], 'Corona':['crown',0], 'Alas de hada':['princess',1],
      'Mameluco animal':['clown',1], 'Varita':['princess',2] },
    { 'Conde Vampiro':['vampire',1], 'Bruja Clásica':['witch',2], 'Superhéroe':['superhero',1],
      'Caballero Victoriano':['victorian',1], 'Dama Victoriana':['victorian',0], 'Princesa':['princess',0],
      'Animalito':['clown',2], 'Halloween a tu gusto':['skeleton',2] },
    { 'Centro Histórico':['shop',0], 'Cumbayá':['shop',1] },
    ['crown',2], ['shop',0]);

  await fotosTienda('El Baúl del Disfraz', 'dueno@baul.ec',
    { 'Disfraz Batman':['superhero',0], 'Disfraz Superman':['superhero',2], 'Disfraz Spiderman niño':['spiderman',0],
      'Disfraz Spiderman':['spiderman',0], 'Disfraz de bruja':['witch',1], 'Disfraz de Catrina':['catrina',0],
      'Peluca de payaso':['clown',0], 'Peluca de princesa':['princess',0], 'Máscara de esqueleto':['skeleton',1],
      'Botas de pirata':['pirate',0], 'Camisa de pirata':['pirate',1], 'Sombrero de pirata':['pirate',2] },
    { 'Batman':['superhero',0], 'Superman':['superhero',2], 'Spiderman':['spiderman',0], 'Bruja':['witch',1],
      'Catrina':['catrina',1], 'Pirata':['pirate',0] },
    { 'Cuenca Centro':['shop',2] },
    ['skeleton',0], ['shop',1]);

  await fotosUsuarios();
  console.log(`\n✅ Fotos subidas a S3. OK=${OK} FAIL=${FAIL}`);
})().catch(e => { console.error('\n❌', e.message); process.exit(1); });
